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

import org.dinky.data.dto.UserDTO;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** UserInfoContextHolder */
// 这是个不受spring管理的类，对于Spring程序来说就是一个全局、静态的静态类。Spring程序中与用户交互的各个线程均可以访问类中的内容、方法。
public class UserInfoContextHolder {

    // 这个ContextHolder单纯就是用一个Map来存放每个用户对应的DTO
    private static final Map<Integer, UserDTO> USER_INFO = new ConcurrentHashMap<>();

    public static void set(Integer userId, UserDTO userInfo) {
        USER_INFO.put(userId, userInfo);
    }

    public static void remove(Integer userId) {
        USER_INFO.remove(userId);
    }

    public static UserDTO get(Integer userId) {
        return USER_INFO.get(userId);
    }

    public static void refresh(Integer userId, UserDTO userInfo) {
        set(userId, userInfo);
    }
}
