package com.examen.ecommerce.dto.validation;

import com.examen.ecommerce.exception.ErrorMessages;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Importe DECIMAL(12,2) estrictamente mayor que 0. */
@Documented
@Constraint(validatedBy = {})
@DecimalMin(value = "0.00", inclusive = false)
@Digits(integer = 10, fraction = 2)
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Money {

    String message() default ErrorMessages.MONEY_POSITIVE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
