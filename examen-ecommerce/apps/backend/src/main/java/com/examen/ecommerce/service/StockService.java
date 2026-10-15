package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.entity.Product;
import java.util.List;
import java.util.Map;

/** Gestión del stock: verificación de lectura y descuento atómico dentro del checkout. */
public interface StockService {

    void assertAvailable(List<CartItemRequest> items, Map<Integer, Product> productsById);

    /** Requiere una transacción activa: si una línea falla se hace rollback de todo. */
    void decrement(List<CartItemRequest> items);
}
