package com.examen.ecommerce.dto.validation;

import java.time.LocalDateTime;

/** Contrato de los DTO con vigencia validable por {@link ValidDateRange}. */
public interface DateRange {

    LocalDateTime validFrom();

    LocalDateTime validUntil();
}
