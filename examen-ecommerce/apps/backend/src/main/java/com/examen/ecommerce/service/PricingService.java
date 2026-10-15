package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartRequest;

/** Flujo único de cálculo (Adapter → Factory → Engine) reutilizado por quote y checkout (DRY). */
public interface PricingService {

    PricedCart price(CartRequest request);
}
