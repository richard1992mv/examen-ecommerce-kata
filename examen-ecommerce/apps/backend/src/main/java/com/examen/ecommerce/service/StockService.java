package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.repository.ProductRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Verificación de stock (quote) y descuento atómico (checkout). */
@Service
public class StockService {

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Verificación informativa con el stock leído; la autoritativa es {@link #decrement}. */
    public void assertAvailable(List<CartItemRequest> items, Map<Integer, Product> productsById) {
        for (CartItemRequest item : items) {
            int available = productsById.get(item.productId()).getStock();
            if (available < item.quantity()) {
                throw new InsufficientStockException(item.productId(), item.quantity(), available);
            }
        }
    }

    /**
     * UPDATE atómico por producto, en orden de id (evita deadlocks entre checkouts concurrentes).
     * 0 filas → 409 y rollback de toda la transacción del checkout.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrement(List<CartItemRequest> items) {
        items.stream()
                .sorted(Comparator.comparing(CartItemRequest::productId))
                .forEach(this::decrementOne);
    }

    private void decrementOne(CartItemRequest item) {
        if (productRepository.decrementStock(item.productId(), item.quantity()) == 0) {
            int available = productRepository.findStockById(item.productId()).orElse(0);
            throw new InsufficientStockException(item.productId(), item.quantity(), available);
        }
    }
}
