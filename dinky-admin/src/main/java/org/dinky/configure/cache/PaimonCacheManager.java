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

import java.util.Collection;
import java.util.LinkedHashSet;

import org.springframework.cache.Cache;
import org.springframework.cache.support.AbstractCacheManager;

// 自定义paimon缓存管理器，需要重写两个方法
public class PaimonCacheManager extends AbstractCacheManager {

    // 重写loadCaches方法，加载和返回所有的缓存实例。
    @Override
    protected Collection<? extends Cache> loadCaches() {
        Collection<Cache> caches = new LinkedHashSet<>();
        for (String cacheName : this.getCacheNames()) {
            Cache cache = this.getCache(cacheName);
            caches.add(cache);
        }
        return caches;
    }

    // 重写这个方法，用于定义当请求的缓存不存在时如何创建新的缓存。
    @Override
    protected Cache getMissingCache(String name) {
        // 使用PaimonCache来进行缓存
        return new PaimonCache(name);
    }
}
