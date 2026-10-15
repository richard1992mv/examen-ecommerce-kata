package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import java.math.BigDecimal;

/**
 * STRATEGY. Contrato común (Liskov):
 * <ul>
 *   <li>monto ≥ 0; devuelve 0 si no aplica, sin lanzar excepciones;</li>
 *   <li>no muta el contexto;</li>
 *   <li>redondea HALF_UP a 2 decimales vía MoneyUtils.</li>
 * </ul>
 */
public interface DiscountRule {

    DiscountType type();

    /** Posición en la cascada (menor primero). */
    int order();

    DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal);
}
