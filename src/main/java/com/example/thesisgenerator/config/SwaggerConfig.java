package com.example.thesisgenerator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("论文生成系统 API 文档")
                        .version("1.0")
                        .description("在线论文文档自动生成工具 - 后端接口文档"));
    }
}
