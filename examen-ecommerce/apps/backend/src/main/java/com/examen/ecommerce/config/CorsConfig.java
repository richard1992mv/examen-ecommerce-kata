package com.examen.ecommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configurable por perfil (app.cors.*). La autenticación/autorización
 * queda fuera de alcance en esta fase (ver docs/arquitectura.md, "Seguridad pendiente").
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private static final String API_PATTERN = "/api/**";

    private final AppProperties properties;

    public CorsConfig(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        AppProperties.Cors cors = properties.cors();
        registry.addMapping(API_PATTERN)
                .allowedOrigins(cors.allowedOrigins().toArray(String[]::new))
                .allowedMethods(cors.allowedMethods().toArray(String[]::new))
                .exposedHeaders("Location");
    }
}
