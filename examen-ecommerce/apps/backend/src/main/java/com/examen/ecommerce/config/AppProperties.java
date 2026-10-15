package com.examen.ecommerce.config;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propiedades generales de la aplicación (prefijo {@code app}).
 *
 * @param timeZone zona horaria de negocio (Clock, JDBC e Hibernate)
 * @param clock    reloj fijo opcional (solo pruebas)
 * @param cors     orígenes y métodos permitidos por perfil
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(@NotBlank String timeZone, ClockSettings clock, Cors cors) {

    public AppProperties {
        clock = clock == null ? new ClockSettings(null) : clock;
        cors = cors == null ? new Cors(List.of(), List.of()) : cors;
    }

    public record ClockSettings(String fixedInstant) {

        public Optional<String> fixed() {
            return Optional.ofNullable(fixedInstant).filter(value -> !value.isBlank());
        }
    }

    public record Cors(List<String> allowedOrigins, List<String> allowedMethods) {

        public Cors {
            allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
            allowedMethods = allowedMethods == null ? List.of() : List.copyOf(allowedMethods);
        }
    }
}
