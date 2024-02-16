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

package org.dinky.data.model;

import org.dinky.mybatis.model.SuperEntity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ClusterInstance
 * 集群实例配置类
 *
 * @since 2021/5/28 13:53
 */
@Data
// 重写equals和hashcode方法，根据类中所有非瞬时（transient）字段以及非静态字段来判断和生成
@EqualsAndHashCode(callSuper = false)
// 表名
@TableName("dinky_cluster")
@ApiModel(value = "ClusterInstance", description = "ClusterInstance")
// extends superenetiy类来继承那些通用的字段
public class ClusterInstance extends SuperEntity<ClusterInstance> {

    private static final long serialVersionUID = 3104721227014487321L;

    // 租户id
    @ApiModelProperty(value = "name", required = true, dataType = "String", example = "test")
    private Integer tenantId;

    // 别名，自动注册的集群会自动生成别名
    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(
            value = "alias",
            required = true,
            dataType = "String",
            example = "test",
            notes = "cluster alias, if this is auto register, it will be has value, and can not modify it")
    private String alias;

    // 集群类型
    @ApiModelProperty(
            value = "type",
            required = true,
            dataType = "String",
            example = "test",
            notes = "cluster type, such as: standalone ,yarn-session")
    private String type;

    // 集群地址
    @ApiModelProperty(value = "hosts", required = true, dataType = "String", example = "test", notes = "cluster hosts")
    private String hosts;

    // 集群jm地址
    @ApiModelProperty(
            value = "jobManagerHost",
            required = true,
            dataType = "String",
            example = "test",
            notes = "job manager host")
    private String jobManagerHost;

    // 集群flink版本
    @ApiModelProperty(
            value = "version",
            required = true,
            dataType = "String",
            example = "test",
            notes = "Flink cluster version")
    private String version;

    // 是否启用
    @ApiModelProperty(
            value = "status",
            required = true,
            dataType = "Integer",
            example = "test",
            notes = "0:unavailable, 1:available")
    private Integer status;

    // 注释
    @ApiModelProperty(value = "note", dataType = "String", example = "test")
    private String note;

    // 是否自动生成
    @ApiModelProperty(
            value = "autoRegisters",
            required = true,
            dataType = "Boolean",
            example = "test",
            notes = "is auto registers, if this record from projob/application mode , it will be true")
    private Boolean autoRegisters;

    // 集群配置id
    @ApiModelProperty(
            value = "clusterConfigurationId",
            required = true,
            dataType = "Integer",
            example = "test",
            notes = "cluster configuration id")
    private Integer clusterConfigurationId;

    // 关联任务id
    @ApiModelProperty(value = "taskId", required = true, dataType = "Integer", example = "test", notes = "task id")
    private Integer taskId;
}
