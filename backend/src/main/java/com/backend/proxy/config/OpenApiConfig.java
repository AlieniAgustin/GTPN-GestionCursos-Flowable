package com.backend.proxy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Gestión de Cursos - Flowable")
                        .version("1.0")
                        .description("Documentación OpenAPI de los controladores REST del sistema de Gestión de Cursos."));
    }
}
