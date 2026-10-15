package com.examen.ecommerce.dto.validation;

import com.examen.ecommerce.exception.ErrorMessages;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Valida a nivel de clase que validFrom ≤ validUntil. */
@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDateRange {

    String message() default ErrorMessages.DATE_RANGE;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
