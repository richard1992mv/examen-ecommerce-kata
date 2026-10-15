package com.examen.ecommerce.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Los nulos los reporta @NotNull; aquí solo se compara el orden de las fechas. */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, DateRange> {

    @Override
    public boolean isValid(DateRange value, ConstraintValidatorContext context) {
        if (value == null || value.validFrom() == null || value.validUntil() == null) {
            return true;
        }
        return !value.validFrom().isAfter(value.validUntil());
    }
}
