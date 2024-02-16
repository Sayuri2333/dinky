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

package org.dinky.aop;

import org.dinky.data.exception.DinkyException;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/** @since 0.7.0 */
@Aspect
@Component
@Slf4j
public class UdfClassLoaderAspect {

    // 定义切点
    @Pointcut("execution(* org.dinky.service.TaskService.*(..))")
    public void taskServicePointcut() {}

    @Pointcut("execution(* org.dinky.service.APIService.*(..))")
    public void apiServicePointcut() {}

    @Pointcut("execution(* org.dinky.service.StudioService.*(..))")
    public void studioServicePointcut() {}

    // 复合切点
    @Pointcut("apiServicePointcut() || taskServicePointcut() || studioServicePointcut()")
    public void allPointcut() {}

    // 调用allPoint()方法中定义的切点，使用环绕方法切入。使用环绕方法时，需要调用proceedingJoinPoint.proceed()方法才会执行。
    @Around("allPointcut()")
    public Object round(ProceedingJoinPoint proceedingJoinPoint) {
        Object proceed = null;
        // 获得当前的类加载器
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            // 执行方法
            proceed = proceedingJoinPoint.proceed();
        } catch (Throwable e) {
            if (!(e instanceof DinkyException)) {
                throw new DinkyException(e);
            }
            e.printStackTrace();
            throw (DinkyException) e;
        } finally {
            // 在方法执行完成之后把类加载器换回来（可能是类加载器在方法执行过程中被换了）
            if (contextClassLoader != Thread.currentThread().getContextClassLoader()) {
                Thread.currentThread().setContextClassLoader(contextClassLoader);
            }
        }
        return proceed;
    }
}
