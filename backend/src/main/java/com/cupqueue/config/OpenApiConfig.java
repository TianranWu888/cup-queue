package com.cupqueue.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the generated OpenAPI document and its bearer-token security scheme.
 */
@Configuration(proxyBeanMethods = false)
@OpenAPIDefinition(
        info = @Info(
                title = "CupQueue API",
                version = "v1",
                description = "REST API for coffee ordering and pickup queue management."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste a JWT access token without the Bearer prefix."
)
public class OpenApiConfig {

    /**
     * Creates the OpenAPI configuration.
     */
    public OpenApiConfig() {
    }
}
