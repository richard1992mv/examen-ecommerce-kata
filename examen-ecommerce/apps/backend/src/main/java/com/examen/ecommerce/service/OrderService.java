package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;

/** HU3 (escritura): confirmar la compra. */
public interface OrderService {

    OrderResponse checkout(CartRequest request);
}
