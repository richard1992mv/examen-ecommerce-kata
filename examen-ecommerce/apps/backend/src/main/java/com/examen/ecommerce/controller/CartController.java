package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Carrito (HU2/HU4)")
public class CartController {

    private final PricingService pricingService;

    public CartController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @PostMapping("/quote")
    @Operation(summary = "Cotizar carrito: categoría → volumen → cupón, con tope del 35 % (no persiste ni toca stock)")
    public CartQuoteResponse quote(@Valid @RequestBody CartRequest request) {
        return pricingService.quote(request);
    }
}
