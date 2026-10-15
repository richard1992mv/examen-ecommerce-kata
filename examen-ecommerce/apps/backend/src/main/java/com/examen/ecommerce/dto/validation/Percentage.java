package com.examen.ecommerce.dto.validation;

import com.examen.ecommerce.exception.ErrorMessages;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Porcentaje DECIMAL(5,2) entre 0.00 y 100.00 (reutilizable en categorías, cupones y propiedades). */
@Documented
@Constraint(validatedBy = {})
@DecimalMin("0.00")
@DecimalMax("100.00")
@Digits(integer = 3, fraction = 2)
@ReportAsSingleViolation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Percentage {

    String message() default ErrorMessages.PERCENTAGE_RANGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
