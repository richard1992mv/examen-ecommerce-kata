package com.examen.ecommerce.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Único punto donde se obtiene la hora. Los servicios inyectan {@link Clock}
 * (nunca LocalDateTime.now() directo). En pruebas se fija con app.clock.fixed-instant.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(AppProperties properties) {
        ZoneId zone = ZoneId.of(properties.timeZone());
        return properties.clock().fixed()
                .map(instant -> Clock.fixed(OffsetDateTime.parse(instant).toInstant(), zone))
                .orElseGet(() -> Clock.system(zone));
    }
}
