package com.examen.ecommerce.service.discount.strategy;

import com.examen.ecommerce.service.discount.model.CapResult;
import java.math.BigDecimal;

/** El tope NO es un descuento: limita la suma de descuentos. Va separado de las reglas. */
public interface DiscountCapPolicy {

    CapResult apply(BigDecimal original, BigDecimal grossDiscount);
}
