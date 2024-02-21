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

package org.dinky.executor;

import org.dinky.classloader.DinkyClassLoader;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

/**
 * RemoteBatchExecutor
 * 远程的批的执行器
 *
 * @since 2022/2/7 22:10
 */
public class RemoteBatchExecutor extends Executor {

    public RemoteBatchExecutor(ExecutorConfig executorConfig, DinkyClassLoader classLoader) {
        this.executorConfig = executorConfig;
        if (executorConfig.isValidConfig()) { // 如果flink配置不为空
            // 从Map中读取flink的配置
            Configuration configuration = Configuration.fromMap(executorConfig.getConfig());
            // 创建Flink执行环境，把配置以及额外的jar的路径也传进去
            this.environment = StreamExecutionEnvironment.createRemoteEnvironment(
                    executorConfig.getHost(), executorConfig.getPort(), configuration, executorConfig.getJarFiles());
        } else { // 如果配置为空就创建默认的
            this.environment = StreamExecutionEnvironment.createRemoteEnvironment(
                    executorConfig.getHost(), executorConfig.getPort(), executorConfig.getJarFiles());
        }
        // 初始化类加载器
        init(classLoader);
    }

    // 返回一个自定义的Table sql的执行环境
    @Override
    CustomTableEnvironment createCustomTableEnvironment(ClassLoader classLoader) {
        // 创建批模式的环境
        return CustomTableEnvironmentImpl.createBatch(environment, classLoader);
    }
}
