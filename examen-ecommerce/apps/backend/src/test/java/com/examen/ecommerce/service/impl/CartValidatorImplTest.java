package com.examen.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.exception.InvalidCartException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartValidatorImplTest {

    private final CartValidatorImpl validator = new CartValidatorImpl();

    @Test
    void emptyOrNullCartIsRejected() {
        assertThatThrownBy(() -> validator.validate(List.of())).isInstanceOf(InvalidCartException.class)
                .hasMessage("El carrito no puede estar vacío");
        assertThatThrownBy(() -> validator.validate(null)).isInstanceOf(InvalidCartException.class);
    }

    @Test
    void quantityMustBePositive() {
        assertThatThrownBy(() -> validator.validate(List.of(new CartItemRequest(1, 0))))
                .isInstanceOf(InvalidCartException.class).hasMessageContaining("mayor que 0");
        assertThatThrownBy(() -> validator.validate(List.of(new CartItemRequest(1, -2))))
                .isInstanceOf(InvalidCartException.class);
        assertThatThrownBy(() -> validator.validate(List.of(new CartItemRequest(1, null))))
                .isInstanceOf(InvalidCartException.class);
    }

    @Test
    void lineWithoutProductIsRejected() {
        List<CartItemRequest> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> validator.validate(withNull)).isInstanceOf(InvalidCartException.class);
        assertThatThrownBy(() -> validator.validate(List.of(new CartItemRequest(null, 1))))
                .isInstanceOf(InvalidCartException.class);
    }

    @Test
    void duplicatedProductIdsAreRejected() {
        assertThatThrownBy(() -> validator.validate(List.of(new CartItemRequest(1, 1), new CartItemRequest(1, 2))))
                .isInstanceOf(InvalidCartException.class).hasMessageContaining("duplicado");
    }

    @Test
    void validCartPasses() {
        assertThatCode(() -> validator.validate(List.of(new CartItemRequest(1, 2), new CartItemRequest(9, 1))))
                .doesNotThrowAnyException();
    }
}
