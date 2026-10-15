package com.examen.ecommerce.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DiscountMessageMapperTest {

    private final DiscountMessageMapper mapper = new DiscountMessageMapper();

    @Test
    void buildsHu4MessageOnlyWhenCapApplied() {
        assertThat(mapper.messageFor(true, new BigDecimal("35.00")))
                .isEqualTo("¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)");
        assertThat(mapper.messageFor(false, new BigDecimal("35.00"))).isNull();
    }
}
