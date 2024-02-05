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

import org.dinky.context.ConsoleContextHolder;
import org.dinky.data.annotations.ExecuteProcess;
import org.dinky.data.annotations.ProcessId;
import org.dinky.data.annotations.ProcessStep;
import org.dinky.data.enums.ProcessStatus;
import org.dinky.data.enums.ProcessStepType;
import org.dinky.data.enums.ProcessType;
import org.dinky.data.exception.DinkyException;
import org.dinky.data.model.ProcessStepEntity;

import org.apache.http.util.TextUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Slf4j
@Component
// process切面类，根据提交进程的生命周期更新其log和状态
public class ProcessAspect {

    public static String PROCESS_NAME = "PROCESS_NAME";
    public static String PROCESS_STEP = "PROCESS_STEP";
    public ConsoleContextHolder contextHolder = ConsoleContextHolder.getInstances();

    /**
     * Block all {@link ExecuteProcess} annotations,
     * As the beginning of the process, set all initialization information
     */
    // 环绕切面，这个切面中程序不会自动执行（调用joinPoint.proceed才会执行），可以同时在程序执行前后配置逻辑
    @Around(value = "@annotation(executeProcess)")
    public Object processAround(ProceedingJoinPoint joinPoint, ExecuteProcess executeProcess) throws Throwable {

        Object result;
        // 获取processId的值。逻辑上processId会出现在controller传入的参数上，或者如果传入的是DTO的话，会出现在DTO的属性中
        Object processId = getProcessId(joinPoint);
        String name = StrFormatter.format("{}/{}", executeProcess.type().getValue(), String.valueOf(processId));
        ProcessType type = executeProcess.type();
        contextHolder.registerProcess(type, name);
        /**
         * Mapped Diagnostic Context）是SLF4J（Simple Logging Facade for Java）日志框架中的一个功能，用于提供一种将额外的用户定义数据与日志消息关联起来的方法。
         * MDC主要用于在日志输出中包含与当前线程相关的上下文信息。
         * 用途1：存储上下文信息：MDC允许你存储每个线程的键值对数据。你可以将用户ID、事务ID或其他相关信息存储在MDC中，这些信息将与执行中的线程关联。
         * 用途2：日志关联：在配置日志输出格式时，可以指定包含MDC中的数据。MDC.put("userId", "12345"); 然后就在日志中可以用%X{userId}获取这个值
         */
        MDC.put(PROCESS_NAME, name);

        try {
            result = joinPoint.proceed();
            contextHolder.finishedProcess(name, ProcessStatus.FINISHED, null);
        } catch (Throwable e) {
            contextHolder.finishedProcess(name, ProcessStatus.FAILED, e);
            throw e;
        } finally {
            // Note that this must be cleaned up，Otherwise, the situation of OOM may occur
            MDC.clear();
        }
        return result;
    }

    /**
     * Block all {@link ProcessStep} annotations,
     * As a specific task step
     */
    @Around(value = "@annotation(processStep)")
    public Object processStepAround(ProceedingJoinPoint joinPoint, ProcessStep processStep) throws Throwable {

        String processName = MDC.get(PROCESS_NAME);
        if (TextUtils.isEmpty(processName)) {
            log.warn(
                    "Process {} does not exist, This registration step {} was abandoned",
                    processName,
                    processStep.type());
            return joinPoint.proceed();
        }

        Object result;
        // Record the current step and restore it after the execution is completed
        String parentStep = MDC.get(PROCESS_STEP);
        ProcessStepType processStepType = processStep.type();
        ProcessStepEntity step = contextHolder.registerProcessStep(processStepType, MDC.get(PROCESS_NAME), parentStep);
        MDC.put(PROCESS_STEP, step.getKey());
        contextHolder.appendLog(processName, step.getKey(), "Start Process Step:" + step.getType(), true);

        try {
            result = joinPoint.proceed();
            contextHolder.finishedStep(MDC.get(PROCESS_NAME), step, ProcessStatus.FINISHED, null);
        } catch (Exception e) {
            contextHolder.finishedStep(MDC.get(PROCESS_NAME), step, ProcessStatus.FAILED, e);
            throw e;
        } finally {
            // restored after the execution is complete
            MDC.put(PROCESS_STEP, parentStep);
        }
        return result;
    }

    // TODO 这段可以参考一下判断参数或者DTO上是否有指定注解
    private Object getProcessId(ProceedingJoinPoint joinPoint) throws IllegalAccessException {
        // 获取参数列表
        Object[] params = joinPoint.getArgs();
        if (params.length == 0) {
            // 没有参数直接报错
            throw new IllegalArgumentException("Must have ProcessId params");
        }

        Object processIdObj = null;
        // Get the method, here you can convert the signature strong to MethodSignature
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        // 获取参数上的Annotation，二维数组第一维为参数index，第二维为注解index（每个参数可能不止一个注解）
        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {
            Object param = params[i];
            // 如果本参数为null直接忽略
            if (param == null) continue;
            // Check whether the parameters on the method have the Process Id annotation
            Annotation[] paramAnn = annotations[i];
            for (Annotation annotation : paramAnn) {
                // 判断参数上的注解是不是processId，如果是，那就把参数值给返回
                if (annotation instanceof ProcessId) {
                    processIdObj = param;
                    break;
                }
            }
            // If there is no Process Id annotation on the parameter,
            // continue to find out whether there is a variable in the object with the Process Id annotation
            // 如果上面的找不到，那就判断这个参数本身的属性是不是被processId注解了
            if (processIdObj == null) {
                Field[] fields = param.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(ProcessId.class)) {
                        // 设置可访问为true之后才能get到
                        field.setAccessible(true);
                        processIdObj = field.get(param);
                    }
                }
            }
        }
        // 判断这个processId是不是基本类型或者其包装类型
        if (ObjectUtil.isBasicType(processIdObj)) {
            return processIdObj;
        } else {
            throw new DinkyException(
                    "The type of the parameter annotated with @ProcessId must be a basic type and not null");
        }
    }
}
