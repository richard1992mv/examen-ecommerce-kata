package com.examen.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.InvalidCartException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Validación de negocio del carrito, independiente de Bean Validation: protege a PricingService cuando se invoca
 * sin pasar por HTTP. Prueba unitaria pura (sin Spring ni BD).
 */
@DisplayName("CartValidator · estructura del carrito")
class CartValidatorTest {

    private final CartValidator validator = new CartValidator();

    @Test
    @DisplayName("Un carrito válido no lanza excepción")
    void carritoValido() {
        assertDoesNotThrow(() -> validator.validate(List.of(new CartItemRequest(1, 2), new CartItemRequest(2, 1))));
    }

    @Test
    @DisplayName("Carrito null o vacío → 400")
    void carritoVacio() {
        assertMessage(ErrorMessages.CART_EMPTY, null);
        assertMessage(ErrorMessages.CART_EMPTY, List.of());
    }

    @Test
    @DisplayName("Línea null o sin productId → 400")
    void lineaSinProducto() {
        assertMessage(ErrorMessages.CART_INVALID_PRODUCT, Arrays.asList((CartItemRequest) null));
        assertMessage(ErrorMessages.CART_INVALID_PRODUCT, List.of(new CartItemRequest(null, 1)));
    }

    @Test
    @DisplayName("Cantidad null, 0 o negativa → 400")
    void cantidadInvalida() {
        String expected = ErrorMessages.CART_INVALID_QUANTITY.formatted(3);
        assertMessage(expected, List.of(new CartItemRequest(3, null)));
        assertMessage(expected, List.of(new CartItemRequest(3, 0)));
        assertMessage(expected, List.of(new CartItemRequest(3, -1)));
    }

    @Test
    @DisplayName("productId repetido → 400")
    void productoDuplicado() {
        assertMessage(ErrorMessages.CART_DUPLICATED_PRODUCT.formatted(1),
                List.of(new CartItemRequest(1, 1), new CartItemRequest(1, 2)));
    }

    private void assertMessage(String expected, List<CartItemRequest> items) {
        InvalidCartException ex = assertThrows(InvalidCartException.class, () -> validator.validate(items));
        assertEquals(expected, ex.getMessage());
    }
}
