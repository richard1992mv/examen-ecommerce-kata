package com.examen.ecommerce.util;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Hora de negocio con la precisión de DATETIME (segundos), siempre desde el Clock inyectado. */
public final class DateTimeUtils {

    private DateTimeUtils() {
    }

    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
    }
}
