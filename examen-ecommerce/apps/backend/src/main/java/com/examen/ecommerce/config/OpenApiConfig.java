package com.examen.ecommerce.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Metadatos OpenAPI. UI disponible en /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ecommerceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("KATA Core E-Commerce API")
                .version("v1")
                .description("Catálogo, carrito con descuentos acumulativos en cascada "
                        + "(categoría → volumen → cupón) con tope del 35 % y checkout transaccional.")
                .license(new License().name("Uso académico")));
    }
}
