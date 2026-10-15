package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;

/** Una ejecución transaccional completa del checkout (un intento). */
public interface OrderCheckout {

    OrderResponse placeOrder(CartRequest request);
}
