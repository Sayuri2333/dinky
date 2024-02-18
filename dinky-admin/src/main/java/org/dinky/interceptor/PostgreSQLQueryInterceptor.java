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

import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

// @Intercepts注解声明了这个拦截器要拦截的MyBatis操作。在这个例子中，它定义了两个@Signature，这意味着拦截器会拦截Executor类的query方法。
// 这个签名只会拦截select语句。如果想要拦截其他语句如update，配置差不多：
// @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})
@Intercepts({
        // 第一个@Signature拦截的是四参数的query方法版本
    @Signature(
            type = Executor.class,
            method = "query",
            args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        // 第二个拦截的是六参数的版本， 包含额外的两个参数
    @Signature(
            type = Executor.class,
            method = "query",
            args = {
                MappedStatement.class,
                Object.class,
                RowBounds.class,
                ResultHandler.class,
                CacheKey.class,
                BoundSql.class
            })
})
public class PostgreSQLQueryInterceptor implements Interceptor {

    // intercept方法是拦截器的核心，它在MyBatis执行拦截的方法（在此例中为query方法）时被调用。
    // 在intercept方法内，可以获取并修改MyBatis操作的各种参数。例如，可以修改查询语句或查询参数等。
    // 拦截器方法可以直接获得Invocation对象，类似spring中的环绕切入，可以控制方法本身是否执行（本质也是使用动态代理）
    @Override
    public Object intercept(final Invocation invocation) throws Throwable {
        // 在这个特定的实现中，intercept方法实际上没有修改任何参数或行为
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0];
        Object parameter = args[1];
        RowBounds rowBounds = (RowBounds) args[2];
        ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
        // 获得这个执行器对象本身
        Executor executor = (Executor) invocation.getTarget();
        CacheKey cacheKey;
        BoundSql boundSql;
        if (args.length == 4) {
            boundSql = ms.getBoundSql(parameter);
            cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        } else {
            cacheKey = (CacheKey) args[4];
            boundSql = (BoundSql) args[5];
        }
        return executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
    }

    @Override
    // plugin方法用于创建当前拦截器的代理，以便将其插入到MyBatis的执行链中。这是通过调用Plugin.wrap实现的。
    public Object plugin(final Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(final Properties properties) {}
}
