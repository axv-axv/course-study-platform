package com.courseplatform.backend.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI coursePlatformOpenApi() {
        return new OpenAPI().info(new Info()
                .title("课程资料与智能学习平台 API")
                .description("M0 工程底座与后续业务接口契约")
                .version("v1"));
    }
}
