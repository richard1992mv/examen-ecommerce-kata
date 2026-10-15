package com.examen.ecommerce.controller;

import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HU2 + HU4: cotización del carrito (no persiste ni toca stock). */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Carrito (HU2/HU4)")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/quote")
    @Operation(summary = "Cotizar carrito: categoría → volumen → cupón, con tope del 35 %",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(examples = {
                @ExampleObject(name = "Con tope (caso d)", value = ApiExamples.CART_REQUEST_CAP),
                @ExampleObject(name = "Con cupón (caso c)", value = ApiExamples.CART_REQUEST_COUPON)})))
    @ApiResponse(responseCode = "200", content = @Content(examples = @ExampleObject(ApiExamples.QUOTE_RESPONSE_CAP)))
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_VALIDATION)))
    @ApiResponse(responseCode = "404", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_NOT_FOUND)))
    @ApiResponse(responseCode = "409", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_STOCK)))
    @ApiResponse(responseCode = "422", content = @Content(examples = @ExampleObject(ApiExamples.PROBLEM_COUPON)))
    public CartQuoteResponse quote(@Valid @RequestBody CartRequest request) {
        return cartService.quote(request);
    }
}
