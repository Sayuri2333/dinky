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

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 自定义缓存管理器
@Configuration
public class CacheConfiguration {
    // 把自定义的缓存管理器加入spring容器的话会使用这个
    // spring的缓存支持使用注解来操作，使用@Cacheable, @Cacheput, @CacheEvict等注解支持缓存查询、缓存更新、缓存删除等功能。使用@Caching注解可以同时指定多个注解
    @Bean
    public CacheManager cacheManager() {
        return new PaimonCacheManager();
    }
}
