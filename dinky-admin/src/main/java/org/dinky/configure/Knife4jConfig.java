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

import org.dinky.DinkyVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver;
import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import com.github.xiaoymin.knife4j.spring.extension.OpenApiExtensionResolver;

import lombok.RequiredArgsConstructor;
import springfox.bean.validators.configuration.BeanValidatorPluginsConfiguration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
// 自动生成包含所有final字段的构造函数，使得这些字段可以通过依赖注入的方式被Spring自动赋值
// 如果没有这个注解的话会实例化失败，因为 final 字段必须在对象构造时就被初始化，而如果没有合适的构造函数，Spring 就没有办法在创建对象时为这些字段提供值。
@RequiredArgsConstructor
// @Import注解用于导入其他配置类或组件到当前Spring应用的容器中
@Import(BeanValidatorPluginsConfiguration.class)
public class Knife4jConfig {

    private final String dinkyVersion = DinkyVersion.getVersion();

    private final OpenApiExtensionResolver openApiExtensionResolver;

    @Bean(value = "defaultApi2")
    public Docket defaultApi2() {

        // 定义一个Swagger的Docket Bean，用于配置Swagger文档生成的详细参数。
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .groupName(dinkyVersion)
                .select()
                // 指定扫描哪个包下的Controller
                .apis(RequestHandlerSelectors.basePackage("org.dinky.controller"))
                // 不基于URL对controller做任何过滤，所有的都加入文档
                .paths(PathSelectors.any())
                .build()
                .extensions(openApiExtensionResolver.buildSettingExtensions());
    }

    private ApiInfo apiInfo() {
        // 设置Api文档的基本信息如标题、描述、版本等
        return new ApiInfoBuilder()
                .title("Dinky Rest API Document")
                .description("Dinky Rest API Document")
                .version(dinkyVersion)
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
                .license("Apache 2.0")
                .contact(new Contact("Dinky 开源", "http://www.dinky.org.cn/", ""))
                .build();
    }
    /** 增加如下配置可解决Spring Boot 6.x 与Swagger 3.0.0 不兼容问题 */
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
            WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier,
            ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes,
            CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties,
            Environment environment) {
        List<ExposableEndpoint<?>> allEndpoints = new ArrayList<>();
        Collection<ExposableWebEndpoint> webEndpoints = webEndpointsSupplier.getEndpoints();
        allEndpoints.addAll(webEndpoints);
        allEndpoints.addAll(servletEndpointsSupplier.getEndpoints());
        allEndpoints.addAll(controllerEndpointsSupplier.getEndpoints());
        String basePath = webEndpointProperties.getBasePath();
        EndpointMapping endpointMapping = new EndpointMapping(basePath);
        boolean shouldRegisterLinksMapping =
                this.shouldRegisterLinksMapping(webEndpointProperties, environment, basePath);
        return new WebMvcEndpointHandlerMapping(
                endpointMapping,
                webEndpoints,
                endpointMediaTypes,
                corsProperties.toCorsConfiguration(),
                new EndpointLinksResolver(allEndpoints, basePath),
                shouldRegisterLinksMapping,
                null);
    }

    private boolean shouldRegisterLinksMapping(
            WebEndpointProperties webEndpointProperties, Environment environment, String basePath) {
        return webEndpointProperties.getDiscovery().isEnabled()
                && (StringUtils.hasText(basePath)
                        || ManagementPortType.get(environment).equals(ManagementPortType.DIFFERENT));
    }
}
