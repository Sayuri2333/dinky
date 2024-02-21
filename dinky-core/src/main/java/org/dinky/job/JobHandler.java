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

package org.dinky.job;

import org.dinky.data.exception.JobException;

import java.util.ServiceLoader;

/**
 * jobHandler
 *
 * @since 2021/6/26 23:22
 */
public interface JobHandler {

    boolean init(Job job);

    boolean ready();

    boolean running();

    boolean success();

    boolean failed();

    boolean callback();

    boolean close();

    static JobHandler build() {
        // ServiceLoader 机制来动态加载并返回一个 JobHandler 接口的实现
        // ServiceLoader会扫描应用的类路径，寻找META-INF/services目录下名为JobHandler全限定名的文件。
        // 在META-INF/services目录下的对应文件中，应列出所有JobHandler接口实现类的全限定名，每个实现类名占一行。
        // 对于文件中列出的每个实现类，ServiceLoader将尝试加载并实例化这些类。
        ServiceLoader<JobHandler> jobHandlers = ServiceLoader.load(JobHandler.class);
        for (JobHandler jobHandler : jobHandlers) {
            return jobHandler;
        }
        throw new JobException("There is no corresponding implementation class for this interface!");
    }
}
