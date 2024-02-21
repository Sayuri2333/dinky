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

package org.dinky.context;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * SpringContextUtils
 *
 * @since 2021/6/29 15:36
 */
// 用来获取bean的工具。实现ApplicationContextAware接口使一个Bean意识到ApplicationContext的存在。
// 实现这个接口的类可以获得一个对ApplicationContext的引用，从而能够访问Spring容器中的其他Bean。
@Component
public class SpringContextUtils implements ApplicationContextAware {

    public static ApplicationContext applicationContext;

    @Override
    // spring在创建这个Bean时，会调用这个方法，并传入当前的applicationContext实例
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 将这个实例初始化为类的成员变量
        SpringContextUtils.applicationContext = applicationContext;
    }

    // 后续就能通过静态方法来获取Bean了
    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return applicationContext.getBean(name, requiredType);
    }

    public static <T> T getBeanByClass(Class<T> tClass) {
        return applicationContext.getBean(tClass);
    }

    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public static boolean isSingleton(String name) {
        return applicationContext.isSingleton(name);
    }

    public static Class<?> getType(String name) {
        return applicationContext.getType(name);
    }
}
