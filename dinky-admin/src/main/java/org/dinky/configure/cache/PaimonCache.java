/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.configure.cache;

import org.dinky.data.constant.PaimonTableConstant;
import org.dinky.data.paimon.CacheData;
import org.dinky.shaded.paimon.data.BinaryString;
import org.dinky.utils.PaimonUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.cache.support.AbstractValueAdaptingCache;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.convert.Convert;

/**
 * paimon缓存
 */
public class PaimonCache extends AbstractValueAdaptingCache {
    private static final Class<CacheData> clazz = CacheData.class;
    // 缓存名称
    private final String cacheName;
    private static final String TABLE_NAME = PaimonTableConstant.DINKY_CACHE;
    /**
     * TIMEOUT CACHE
     * 本地内存的限时cache，跟paimon组成两层缓存
     */
    private final cn.hutool.cache.Cache<Object, Object> cache = new TimedCache<>(1000 * 60 * 30);

    public PaimonCache(String cacheName) {
        // 允许缓存空值
        super(true);
        this.cacheName = cacheName;
    }

    @Override
    public String getName() {
        return cacheName;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    // 重写这个方法主要是为了能够使用get直接返回泛型类，而不是一个Cache.ValueWrapper，本质其实还是调用lookup方法
    @Override
    public <T> T get(Object key, Class<T> type) {
        return (T) get(key).get();
    }

    /**
     * 自定义缓存行为的核心方法。需要在这个方法中实现查找缓存的逻辑。
     * @param key
     * @return
     */
    @Override
    protected Object lookup(Object key) {
        // 转换成str
        String strKey = Convert.toStr(key);
        // 内存先查一下
        Object o = cache.get(strKey);
        if (o == null) {
            // 没有的话就从paimon里面拿表
            PaimonUtil.createOrGetTable(TABLE_NAME, clazz);
            // 根据cacheName以及key来获取缓存内容
            List<CacheData> cacheData = PaimonUtil.batchReadTable(
                    TABLE_NAME,
                    clazz,
                    x -> Arrays.asList(
                            x.equal(x.indexOf("cache_name"), BinaryString.fromString(cacheName)),
                            x.equal(x.indexOf("key"), BinaryString.fromString(strKey))));
            // 如果空的话就确实没有缓存
            if (cacheData.isEmpty()) {
                return null;
            }
            // 反序列化
            return deserialize(cacheData.get(0).getData());
        }
        return o;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return (T) get(key).get();
    }

    // 写入
    @Override
    public void put(Object key, Object value) {
        String strKey = Convert.toStr(key);
        // 双写
        cache.put(strKey, value);
        PaimonUtil.createOrGetTable(TABLE_NAME, clazz);
        CacheData cacheData = CacheData.builder()
                .cacheName(cacheName)
                .key(strKey)
                .data(serialize(value))
                .build();
        // 写到paimon里面
        PaimonUtil.write(TABLE_NAME, Collections.singletonList(cacheData), clazz);
    }

    @Override
    public void evict(Object key) {
        // 双删
        String strKey = Convert.toStr(key);
        cache.remove(strKey);
        // 用一个空值覆盖之前的记录
        CacheData cacheData =
                CacheData.builder().cacheName(cacheName).key(strKey).data("").build();
        PaimonUtil.write(TABLE_NAME, Collections.singletonList(cacheData), clazz);
    }

    @Override
    public void clear() {
        // 双清空
        cache.clear();
        // 直接删表了
        PaimonUtil.dropTable(TABLE_NAME);
    }

    public String serialize(Object object) {
        // 使用JSONWriter.Feature.WriteClassName会在序列化的时候向json中添加@type字段，说明这个类的地址，配合JSONReader.Feature.SupportAutoType使用
        return JSON.toJSONString(object, JSONWriter.Feature.WriteClassName);
    }

    // 使用JSONReader.Feature.SupportAutoType能够让反序列化成对象时支持自动类型推断。如果json字符串中有字段"@type"指定类型，反序列化时则会将其转换成这里指定的类型
    public Object deserialize(String json) {
        return JSON.parseObject(json, Object.class, JSONReader.Feature.SupportAutoType);
    }
}
