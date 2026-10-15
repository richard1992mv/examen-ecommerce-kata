package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.validation.UniqueProductIdsValidator;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.InvalidCartException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Carrito no vacío, quantity &gt; 0 y sin productId duplicados (400). Garantía de negocio para
 * cualquier invocador del servicio; en HTTP lo cubre antes Bean Validation.
 */
@Component
public class CartValidator {

    public void validate(List<CartItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new InvalidCartException(ErrorMessages.CART_EMPTY);
        }
        items.forEach(CartValidator::validateItem);
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
