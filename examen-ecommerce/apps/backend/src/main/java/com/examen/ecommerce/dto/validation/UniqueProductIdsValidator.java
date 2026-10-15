package com.examen.ecommerce.dto.validation;

import com.examen.ecommerce.dto.request.CartItemRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Implementación única de la detección de productId duplicados: la usan tanto
 * Bean Validation (HTTP) como {@code CartValidator} (servicio).
 */
public class UniqueProductIdsValidator implements ConstraintValidator<UniqueProductIds, List<CartItemRequest>> {

    @Override
    public boolean isValid(List<CartItemRequest> items, ConstraintValidatorContext context) {
        return items == null || firstDuplicate(items).isEmpty();
    }

    public static Optional<Integer> firstDuplicate(List<CartItemRequest> items) {
        Set<Integer> seen = new HashSet<>();
        return items.stream()
                .filter(Objects::nonNull)
                .map(CartItemRequest::productId)
                .filter(Objects::nonNull)
                .filter(id -> !seen.add(id))
                .findFirst();
    }
}
