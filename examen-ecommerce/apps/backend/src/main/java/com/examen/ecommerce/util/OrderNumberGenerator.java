package com.examen.ecommerce.util;

import com.examen.ecommerce.config.OrderNumberProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Genera order_number con formato ORD-{yyyy}-{6 dígitos} continuando la secuencia
 * del año (ORD-2026-000004 → ORD-2026-000005). La unicidad final la garantiza el
 * UNIQUE uk_orders_order_number + reintento acotado en el servicio de órdenes.
 */
@Component
public class OrderNumberGenerator {

    private static final String SEPARATOR = "-";
    private static final String FORMAT = "%s%06d";
    private static final int FIRST_SEQUENCE = 1;

    private final String prefix;

    public OrderNumberGenerator(OrderNumberProperties properties) {
        this.prefix = properties.prefix();
    }

    /** Prefijo del año usado para buscar el último número: "ORD-2026-". */
    public String yearPrefix(int year) {
        return prefix + SEPARATOR + year + SEPARATOR;
    }

    public String next(int year, Optional<String> lastNumberOfYear) {
        int sequence = lastNumberOfYear.map(this::sequenceOf).map(value -> value + 1).orElse(FIRST_SEQUENCE);
        return FORMAT.formatted(yearPrefix(year), sequence);
    }

    private int sequenceOf(String orderNumber) {
        return Integer.parseInt(orderNumber.substring(orderNumber.lastIndexOf(SEPARATOR) + 1));
    }
}
