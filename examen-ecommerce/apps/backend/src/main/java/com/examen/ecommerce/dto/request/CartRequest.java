package com.examen.ecommerce.dto.request;

import com.examen.ecommerce.dto.validation.UniqueProductIds;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Carrito enviado a /cart/quote y a /orders (mismo DTO: mismo cálculo).
 * No admite montos calculados: el servidor recalcula todo.
 */
public record CartRequest(
        @NotEmpty @UniqueProductIds List<@Valid @NotNull CartItemRequest> items,
        @Schema(example = "WELCOME2026", nullable = true) @Size(max = 50) String couponCode) {

    public CartRequest {
        items = items == null ? null : List.copyOf(items);
    }
}
