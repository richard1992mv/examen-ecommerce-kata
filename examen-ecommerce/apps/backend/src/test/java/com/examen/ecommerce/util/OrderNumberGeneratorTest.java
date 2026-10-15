package com.examen.ecommerce.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.config.OrderNumberProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderNumberGeneratorTest {

    private final OrderNumberGenerator generator = new OrderNumberGenerator(new OrderNumberProperties("ORD", 3));

    @Test
    void continuesTheSeedSequence() {
        assertThat(generator.next(2026, Optional.of("ORD-2026-000004"))).isEqualTo("ORD-2026-000005");
    }

    @Test
    void startsAtOneForANewYear() {
        assertThat(generator.next(2027, Optional.empty())).isEqualTo("ORD-2027-000001");
        assertThat(generator.yearPrefix(2027)).isEqualTo("ORD-2027-");
    }
}
