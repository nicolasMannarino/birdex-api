package com.birdex.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "BirdEx", version = "v1", description = "APIs del proyecto (avistajes, etc.)"),
        servers = {
                @Server(url = "http://localhost:${server.port:8080}", description = "Local")
        }
)
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().components(new Components());
    }

}