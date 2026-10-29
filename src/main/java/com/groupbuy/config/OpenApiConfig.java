package com.groupbuy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 / Swagger documentation configuration.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI groupBuyOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("GroupBuy E-Commerce Platform API")
                        .description("Production-style backend for GroupBuy: a smart group-buying e-commerce platform featuring dynamic, volume-tiered campaign pricing.")
                        .version("v1.0.0-phase1")
                        .contact(new Contact()
                                .name("GroupBuy Engineering Team")
                                .email("dev@groupbuy.internal"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("/").description("Current Server Context")
                ));
    }
}
