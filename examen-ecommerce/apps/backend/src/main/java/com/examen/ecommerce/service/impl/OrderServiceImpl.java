package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.config.OrderNumberProperties;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.exception.OrderNumberGenerationException;
import com.examen.ecommerce.service.OrderCheckout;
import com.examen.ecommerce.service.OrderService;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/**
 * Reintento ACOTADO ante colisión de uk_orders_order_number. No es transaccional: cada
 * intento es una transacción nueva y completa (la anterior ya hizo rollback).
 */
@Service
public class OrderServiceImpl implements OrderService {

    static final String ORDER_NUMBER_CONSTRAINT = "uk_orders_order_number";

    private static final Logger LOG = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderCheckout orderCheckout;
    private final int maxAttempts;

    public OrderServiceImpl(OrderCheckout orderCheckout, OrderNumberProperties properties) {
        this.orderCheckout = orderCheckout;
        this.maxAttempts = properties.maxAttempts();
    }

    @Override
    public OrderResponse checkout(CartRequest request) {
        DataIntegrityViolationException lastCollision = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return orderCheckout.placeOrder(request);
            } catch (DataIntegrityViolationException ex) {
                if (!isOrderNumberCollision(ex)) {
                    throw ex;
                }
                lastCollision = ex;
                LOG.warn("Colisión de order_number en el intento {}/{}; reintentando", attempt, maxAttempts);
            }
        }
        throw new OrderNumberGenerationException(maxAttempts, lastCollision);
    }

    private static boolean isOrderNumberCollision(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase(Locale.ROOT).contains(ORDER_NUMBER_CONSTRAINT);
    }
}
