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

package org.dinky.mybatis.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * MybatisPlusFillProperties
 *
 * @since 2021/5/25
 */
@Setter
@Getter
// ConfigurationProperties注解可以将配置文件中的属性值映射到bean上。
// 配置prefix表示只有以指定prefix为前缀的属性才会被绑定。
// 使用@ConfigurationProperties可以创建类型安全的配置。这意味着如果配置值的格式不正确或与Java对象的字段类型不兼容，应用在启动时会报错。
// @ConfigurationProperties支持松散绑定，fieldName对应配置文件中的field_name。
// 使用时可以在这个类上加上component注解来启用，也可以在别的地方使用@EnableConfigurationProperties来启用
// 对应的这个POJO类需要有getter、setter方法才行
@ConfigurationProperties(prefix = "dinky.mybatis-plus.fill")
public class MybatisPlusFillProperties {

    private Boolean enabled = true;

    private Boolean enableInsertFill = true;

    private Boolean enableUpdateFill = true;

    private String createTimeField = "createTime";

    private String updateTimeField = "updateTime";

    private String creatorField = "creator";

    private String updaterField = "updater";

    private String operatorField = "operator";

    private String name = "name";
}
