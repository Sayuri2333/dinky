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

package org.dinky.utils;

import org.dinky.assertion.Asserts;

import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KerberosUtil {

    private static final Logger logger = LoggerFactory.getLogger(KerberosUtil.class);

    // 简单的单线程定时任务执行器
    private static ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // TODO 为了不让每次认证方法被调用时都创建新的定时任务导致任务数量爆炸，我们会检查这个静态变量是否有值，有的话就不创建新的了
    private static ScheduledFuture<?> lastTask = null; // 用于跟踪最后一个任务

    private static void reset() {
        try {
            if (UserGroupInformation.isLoginKeytabBased()) {
                Method reset = UserGroupInformation.class.getDeclaredMethod("reset");
                reset.invoke(UserGroupInformation.class);
                logger.info("Reset kerberos authentication...");
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | IOException e) {
            logger.error("Reset kerberos authentication error.", e);
            throw new RuntimeException(e);
        }
    }

    public static void authenticate(Map<String, String> configuration) {
        configuration.forEach((k, v) -> logger.debug("Flink configuration key: [{}], value: [{}]", k, v));
        String krb5ConfPath = (String) configuration.getOrDefault("security.kerberos.krb5-conf.path", "");
        String keytabPath = (String) configuration.getOrDefault("security.kerberos.login.keytab", "");
        String principal = (String) configuration.getOrDefault("security.kerberos.login.principal", "");

        if (Asserts.isAllNullString(krb5ConfPath, keytabPath, principal)) {
            logger.info("Simple authentication mode");
            return;
        }
        logger.info("Kerberos authentication mode");
        if (Asserts.isNullString(krb5ConfPath)) {
            logger.error("Parameter [security.kerberos.krb5-conf.path] is null or empty.");
            return;
        }

        if (Asserts.isNullString(keytabPath)) {
            logger.error("Parameter [security.kerberos.login.keytab] is null or empty.");
            return;
        }

        if (Asserts.isNullString(principal)) {
            logger.error("Parameter [security.kerberos.login.principal] is null or empty.");
            return;
        }

        reset();

        System.setProperty("java.security.krb5.conf", krb5ConfPath);
        org.apache.hadoop.conf.Configuration config = new org.apache.hadoop.conf.Configuration();
        config.set("hadoop.security.authentication", "Kerberos");
        config.setBoolean("hadoop.security.authorization", true);
        UserGroupInformation.setConfiguration(config);
        try {
            UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
            logger.error(
                    "Kerberos [{}] authentication success.",
                    UserGroupInformation.getLoginUser().getUserName());
            // TODO 设置定时任务刷新票据就完事了。 ugi是JVM级别的，一个线程执行了登录其他线程都能用
            long refreshInterval = TimeUnit.HOURS.toMillis(12);
            // 防止重复创建task
            if (lastTask == null){
                lastTask = scheduler.scheduleWithFixedDelay(() -> {
                    try {
                        logger.info("Refreshing Kerberos ticket.");
                        UserGroupInformation.loginUserFromKeytab(principal, keytabPath);
                    } catch (IOException e) {
                        logger.error("Error refreshing Kerberos ticket: " + e.getMessage());
                    }
                }, refreshInterval, refreshInterval, TimeUnit.MILLISECONDS);
            }
        } catch (IOException e) {
            logger.error("Kerberos authentication failed. ", e);
        }
    }
}
