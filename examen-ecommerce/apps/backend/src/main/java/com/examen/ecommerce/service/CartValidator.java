package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartItemRequest;
import java.util.List;

/** Valida el carrito: no vacío, quantity > 0 y sin productId duplicados (400). */
public interface CartValidator {

    void validate(List<CartItemRequest> items);
}
