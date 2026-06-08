package com.novel.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Novel API")
                        .description("本地写作智能助手接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("novel")
                                .email("zhangwenpeng09@foxmail.com")));
    }
}