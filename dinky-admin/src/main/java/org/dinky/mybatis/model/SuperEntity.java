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

package org.dinky.mybatis.model;

import org.dinky.mybatis.annotation.Save;

import java.io.Serializable;
import java.time.LocalDateTime;

import javax.validation.constraints.NotNull;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * SuperEntity
 *
 * @since 2021/5/28
 */
@Setter
@Getter
@ApiModel(value = "SuperEntity", description = "Super Base Entity", parent = Model.class)
// 下面这个写法的意思时SuperEntity接收一个Model类的子类作为泛型传入，但SuperEntity本身并不是Model类的子类。
// Model类是mybatis-plus包中用来将数据库表的记录映射到对象上，使得对象的操作直接映射到数据库的操作的类。
// 下面包括了各个表中都公共包含的字段。
public class SuperEntity<T extends Model<?>> extends Model<T> {

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    @ApiModelProperty(value = "ID", required = true, dataType = "Integer", example = "1", notes = "Primary Key")
    private Integer id;

    @NotNull(
            message = "Name cannot be null",
            groups = {Save.class})
    @ApiModelProperty(value = "Name", required = true, dataType = "String", example = "Name")
    private String name;

    @NotNull(
            message = "Enabled cannot be null",
            groups = {Save.class})
    @ApiModelProperty(value = "Enabled", required = true, dataType = "Boolean", example = "true")
    private Boolean enabled;

    // mybatis-plus中的注解，表示在插入数据时自动填充该字段的值
    // 需要配置继承MetaObjectHandler来设置自动填充的策略，参照org.dinky.mybatis.handler.DateMetaObjectHandler类
    @TableField(fill = FieldFill.INSERT)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @ApiModelProperty(
            value = "Create Time",
            required = true,
            dataType = "LocalDateTime",
            example = "2021-05-28 00:00:00")
    private LocalDateTime createTime;

    // mybatis-plus中的注解，表示在插入、更新数据时自动填充该字段的值
    @TableField(fill = FieldFill.INSERT_UPDATE)
    // jackson包的注解，用于自定义 JSON 序列化和反序列化方法
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @ApiModelProperty(
            value = "Update Time",
            required = true,
            dataType = "LocalDateTime",
            example = "2021-05-28 00:00:00")
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    @ApiModelProperty(value = "Creator", required = true, dataType = "String", example = "Creator")
    private Integer creator;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    @ApiModelProperty(value = "Updater", required = true, dataType = "String", example = "Updater")
    private Integer updater;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}
