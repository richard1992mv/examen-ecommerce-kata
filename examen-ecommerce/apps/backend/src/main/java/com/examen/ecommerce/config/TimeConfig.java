package com.examen.ecommerce.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fuente única de la hora actual (inversión de dependencias): los servicios reciben un {@link Clock} en lugar
 * de llamar a {@code LocalDateTime.now()}, así las pruebas pueden fijar la fecha con {@code Clock.fixed(...)}.
 * Usa la zona por defecto de la JVM, que el pom fija en {@code America/Bogota} ({@code -Duser.timezone}).
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
