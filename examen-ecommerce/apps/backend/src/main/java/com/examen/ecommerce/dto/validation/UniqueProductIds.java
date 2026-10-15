package com.examen.ecommerce.dto.validation;

import com.examen.ecommerce.exception.ErrorMessages;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** La lista de líneas del carrito no puede repetir productId. */
@Documented
@Constraint(validatedBy = UniqueProductIdsValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueProductIds {

    String message() default ErrorMessages.UNIQUE_PRODUCTS;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
