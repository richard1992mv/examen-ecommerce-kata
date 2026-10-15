package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.service.StockService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockServiceImpl implements StockService {

    private static final int NO_STOCK = 0;

    private final ProductRepository productRepository;

    public StockServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void assertAvailable(List<CartItemRequest> items, Map<Integer, Product> productsById) {
        for (CartItemRequest item : items) {
            int available = productsById.get(item.productId()).getStock();
            if (available < item.quantity()) {
                throw new InsufficientStockException(item.productId(), item.quantity(), available);
            }
        }
    }

    /**
     * UPDATE atómico por producto, en orden de id para que transacciones concurrentes
     * bloqueen filas en el mismo orden (evita deadlocks). 0 filas → 409 + rollback total.
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void decrement(List<CartItemRequest> items) {
        items.stream()
                .sorted(Comparator.comparing(CartItemRequest::productId))
                .forEach(this::decrementOne);
    }

    private void decrementOne(CartItemRequest item) {
        int updatedRows = productRepository.decrementStock(item.productId(), item.quantity());
        if (updatedRows == 0) {
            int available = productRepository.findStockById(item.productId()).orElse(NO_STOCK);
            throw new InsufficientStockException(item.productId(), item.quantity(), available);
        }
    }
}
