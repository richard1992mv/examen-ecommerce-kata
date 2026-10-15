package com.examen.ecommerce.mapper;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** HU4: el mensaje se arma de forma determinista a partir de capApplied (no en un listener). */
@Component
public class DiscountMessageMapper {

    public static final String CAP_REACHED_TEMPLATE =
            "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (%s%%)";

    /** @return mensaje HU4 si se aplicó el tope; null en otro caso */
    public String messageFor(boolean capApplied, BigDecimal capPct) {
        if (!capApplied) {
            return null;
        }
        return CAP_REACHED_TEMPLATE.formatted(capPct.stripTrailingZeros().toPlainString());
    }
}
