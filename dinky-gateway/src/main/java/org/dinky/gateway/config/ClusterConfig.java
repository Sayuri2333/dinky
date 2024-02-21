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

package org.dinky.gateway.config;

import org.dinky.gateway.model.CustomConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * ClusterConfig
 *
 * @since 2021/11/3 21:52
 */
@Getter
@Setter
@ApiModel(value = "ClusterConfig", description = "Configuration for a Flink cluster")
public class ClusterConfig {

    // flink-conf文件路径
    @ApiModelProperty(
            value = "Path to Flink configuration file",
            dataType = "String",
            example = "/opt/flink/conf/flink-conf.yaml",
            notes = "Path to the Flink configuration file")
    private String flinkConfigPath;

    // flink lib路径
    @ApiModelProperty(
            value = "Path to Flink library directory",
            dataType = "String",
            example = "/opt/flink/lib",
            notes = "Path to the Flink library directory")
    private String flinkLibPath;

    // hadoop conf路径，yarn-site之类的
    @ApiModelProperty(
            value = "Path to YARN configuration file",
            dataType = "String",
            example = "/etc/hadoop/conf/yarn-site.xml",
            notes = "Path to the YARN configuration file")
    private String hadoopConfigPath;

    // 其他的hadoop参数
    @ApiModelProperty(
            value = "Additional hadoop configuration properties",
            dataType = "List",
            example = "[{\"name\":\"key1\",\"value\":\"value1\"}, {\"name\":\"key2\",\"value\":\"value2\"}]",
            notes = "Additional hadoop configuration properties for the job on web page")
    private List<CustomConfig> hadoopConfigList = new ArrayList<>();

    @ApiModelProperty(
            value = "Additional hadoop configuration properties",
            dataType = "Map",
            example = "{\"key1\":\"value1\",\"key2\":\"value2\"}",
            notes = "Additional hadoop configuration properties for the job, invisible on web page",
            hidden = true)
    private Map<String, String> hadoopConfigMap = new HashMap<>();

    // yarn application id
    @ApiModelProperty(
            value = "YARN application ID",
            dataType = "String",
            example = "application_12345_67890",
            notes = "ID of the YARN application associated with the Flink cluster")
    private String appId;

    public ClusterConfig() {}

    public ClusterConfig(String flinkConfigPath) {
        this.flinkConfigPath = flinkConfigPath;
    }

    public ClusterConfig(String flinkConfigPath, String flinkLibPath, String hadoopConfigPath) {
        this.flinkConfigPath = flinkConfigPath;
        this.flinkLibPath = flinkLibPath;
        this.hadoopConfigPath = hadoopConfigPath;
    }

    public static ClusterConfig build(String flinkConfigPath) {
        return build(flinkConfigPath, null, null);
    }

    public static ClusterConfig build(String flinkConfigPath, String flinkLibPath, String yarnConfigPath) {
        return new ClusterConfig(flinkConfigPath, flinkLibPath, yarnConfigPath);
    }

    @Override
    public String toString() {
        return String.format(
                "ClusterConfig{flinkConfigPath='%s', flinkLibPath='%s', yarnConfigPath='%s', " + "appId='%s'}",
                flinkConfigPath, flinkLibPath, hadoopConfigPath, appId);
    }
}
