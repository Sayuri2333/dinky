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

package org.dinky.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.Properties;

// 拦截StatementHandler类的prepare方法。这个方法在SQL语句被实际执行之前被调用。
@Intercepts({
    @Signature(
            type = StatementHandler.class,
            method = "prepare",
            args = {Connection.class, Integer.class})
})
public class PostgreSQLPrepareInterceptor implements Interceptor {
    @Override
    public Object intercept(final Invocation invocation) throws Throwable {
        // 获取到被拦截的对象
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        // 获取到BoundSql实例，包含了即将执行的SQL语句。
        BoundSql boundSql = statementHandler.getBoundSql();
        // 利用反射技术获取到BoundSql对象中的sql字段，这个字段保存了即将执行的SQL语句。
        Field field = boundSql.getClass().getDeclaredField("sql");
        // 通过field.setAccessible(true)使得这个私有字段可以被访问和修改。
        field.setAccessible(true);
        // 将SQL语句中所有的反引号（`）替换为双引号（"），并将SQL语句转换为小写。
        // 这种修改可能是为了适应PostgreSQL数据库的特定要求，因为PostgreSQL通常使用双引号来标识数据库对象（如表名、列名等），
        // 而MySQL等数据库可能使用反引号。
        field.set(boundSql, boundSql.getSql().replace("`", "\"").toLowerCase());
        // 继续执行
        return invocation.proceed();
    }

    @Override
    // plugin方法通过调用Plugin.wrap来创建拦截器的代理，这是MyBatis拦截器机制的一部分，用于将拦截器应用到目标对象上。
    public Object plugin(final Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(final Properties properties) {}
}
