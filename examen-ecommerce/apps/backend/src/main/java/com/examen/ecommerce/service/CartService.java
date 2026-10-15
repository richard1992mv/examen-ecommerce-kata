package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;

/** HU2: cotización del carrito sin persistir ni tocar stock. */
public interface CartService {

    CartQuoteResponse quote(CartRequest request);
}
