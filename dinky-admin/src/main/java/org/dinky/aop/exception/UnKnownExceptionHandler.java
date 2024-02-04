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

package org.dinky.aop.exception;

import org.dinky.data.result.Result;

import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//全局控制器增强配置，用来给所有controller层的方法进行全局异常处理、数据绑定、模型增强等功能
@ControllerAdvice
// 这里设置的order能够按照默认的优先级处理异常，其中的value越小优先级越高
@Order
public class UnKnownExceptionHandler {

    // 标注ExceptionHandler的方法可以用于处理控制器层抛出的异常
    @ExceptionHandler
    // 普通的controllerAdvice注解需要在方法中配合ResponseBody注解才能将对象返回成json字符串
    @ResponseBody
    // 处理其他类型的错误
    public Result<String> unknownException(Exception e) {
        // log记录错误信息
        log.error(e.getMessage(), e);
        // 返回带有错误类型的result
        return Result.exception(e.getMessage(), e);
    }
}
