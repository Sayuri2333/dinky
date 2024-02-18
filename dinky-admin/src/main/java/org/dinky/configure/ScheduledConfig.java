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

package org.dinky.configure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

// 用于配置定时任务执行的线程池
@Configuration
public class ScheduledConfig {

    // 通过定义ThreadPoolTaskScheduler的Bean，它自定义了Spring的调度任务使用的线程池
    // 用于@Scheduled注解方法
    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setPoolSize(10);
        // 设置线程池中线程名称的前缀
        threadPoolTaskScheduler.setThreadNamePrefix("scheduler-");
        // 当任务被取消时将其从调度器中删除，有助于防止资源泄露。
        threadPoolTaskScheduler.setRemoveOnCancelPolicy(true);
        return threadPoolTaskScheduler;
    }
}
