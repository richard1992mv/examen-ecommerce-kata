package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.validation.UniqueProductIdsValidator;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.InvalidCartException;
import com.examen.ecommerce.service.CartValidator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Garantía de negocio independiente del canal HTTP (Bean Validation ya cubre la API REST;
 * esta clase protege a cualquier otro invocador del servicio).
 */
@Component
public class CartValidatorImpl implements CartValidator {

    @Override
    public void validate(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidCartException(ErrorMessages.CART_EMPTY);
        }
        items.forEach(CartValidatorImpl::validateItem);
        UniqueProductIdsValidator.firstDuplicate(items).ifPresent(productId -> {
            throw new InvalidCartException(ErrorMessages.CART_DUPLICATED_PRODUCT.formatted(productId));
        });
    }

    private static void validateItem(CartItemRequest item) {
        if (item == null || item.productId() == null) {
            throw new InvalidCartException(ErrorMessages.CART_INVALID_PRODUCT);
        }
        if (item.quantity() == null || item.quantity() <= 0) {
            throw new InvalidCartException(ErrorMessages.CART_INVALID_QUANTITY.formatted(item.productId()));
        }
    }
}
