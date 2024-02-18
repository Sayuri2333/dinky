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

package org.dinky.configure;

import org.dinky.context.TenantContextHolder;
import org.dinky.interceptor.PostgreSQLPrepareInterceptor;
import org.dinky.interceptor.PostgreSQLQueryInterceptor;
import org.dinky.mybatis.handler.DateMetaObjectHandler;
import org.dinky.mybatis.properties.MybatisPlusFillProperties;

import java.util.Set;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.google.common.collect.ImmutableSet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;

/** mybatisPlus config class */
// my-batis配置类
@Configuration
// 指定my-batis需要扫描哪个包来寻找@Mapper注解的接口，这些节后会被Spring自动实现并注入。
@MapperScan("org.dinky.mapper")
// 显式地启用对MybatisPlusFillProperties类的配置属性支持
@EnableConfigurationProperties(MybatisPlusFillProperties.class)
@Slf4j
// 启用对MybatisPlusFillProperties类的配置属性支持
@RequiredArgsConstructor
public class MybatisPlusConfig {

    // 获取到配置文件中的配置
    private final MybatisPlusFillProperties autoFillProperties;

    // 忽略掉的表
    private static final Set<String> IGNORE_TABLE_NAMES = ImmutableSet.of(
            "dinky_namespace",
            "dinky_alert_group",
            "dinky_alert_history",
            "dinky_alert_instance",
            "dinky_catalogue",
            "dinky_cluster",
            "dinky_cluster_configuration",
            "dinky_database",
            "dinky_fragment",
            "dinky_history",
            "dinky_jar",
            "dinky_job_history",
            "dinky_job_instance",
            "dinky_role",
            "dinky_savepoints",
            "dinky_task",
            "dinky_task_statement",
            "dinky_git_project",
            "dinky_task_version");

    @Bean
    // 使用Profile注解控制配置类中的功能在什么profile下才会生效。这里是只会在pgsql这个profile被激活时才会被创建
    @Profile("pgsql")
    public PostgreSQLQueryInterceptor postgreSQLQueryInterceptor() {
        return new PostgreSQLQueryInterceptor();
    }

    /**
     * Add the plugin to the MyBatis plugin interceptor chain.
     * 把这个插件加入mybatis的拦截器链。
     *
     * @return {@linkplain PostgreSQLPrepareInterceptor}
     */
    // 一样也是只有在pgsql的profile启用时才会有
    @Bean
    @Profile("pgsql")
    public PostgreSQLPrepareInterceptor postgreSQLPrepareInterceptor() {
        return new PostgreSQLPrepareInterceptor();
    }

    // 上面是mybatis的拦截器，下面是mybatis-plus的拦截器
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("mybatis plus interceptor execute");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 多租户拦截器，用于实现多租户功能。多租户是一种常见的架构模式，允许多个租户（用户、组织等）在共享的系统中隔离地存储数据。
        // 在实际使用中拦截器会介入SQL执行过程。它修改SQL语句，加入租户ID作为查询条件。
        // 如果原始SQL是SELECT * FROM user，租户ID是123，则修改后的SQL可能是SELECT * FROM user WHERE tenant_id = 123。
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {

            @Override
            public Expression getTenantId() {
                // 使用租户ContextHolder来获取当前线程对应的租户id
                Integer tenantId = (Integer) TenantContextHolder.get();
                if (tenantId == null) {
                    return new NullValue();
                }
                // 返回一下
                return new LongValue(tenantId);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 配置什么时候忽略多租户的逻辑。要么是在TenantContextHolder配置了忽略，要么是这些表名在忽略的列表中
                if (TenantContextHolder.isIgnoreTenant()) {
                    return true;
                }
                return !IGNORE_TABLE_NAMES.contains(tableName);
            }
        }));
        // 分页拦截器。处理Mapper接口方法中Page对象的分页功能。
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    // ConditionalOnMissingBean表示只有当容器中不存在指定类型的Bean时，才会创建这个Bean。
    @ConditionalOnMissingBean
    // 这个注解用于条件化地创建Bean，基于配置文件中的属性值。
    // prefix = "dinky.mybatis-plus.fill"和name = "enabled"指定了要检查的属性，即dinky.mybatis-plus.fill.enabled。
    // havingValue = "true"表示只有当dinky.mybatis-plus.fill.enabled的值为true时，这个Bean才会被创建。
    // matchIfMissing = true意味着如果dinky.mybatis-plus.fill.enabled属性缺失，那么就当作它的值为true，也就是说在属性缺失的情况下依然会创建这个Bean。
    @ConditionalOnProperty(
            prefix = "dinky.mybatis-plus.fill",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true)
    public MetaObjectHandler metaObjectHandler() {
        return new DateMetaObjectHandler(autoFillProperties);
    }
}
