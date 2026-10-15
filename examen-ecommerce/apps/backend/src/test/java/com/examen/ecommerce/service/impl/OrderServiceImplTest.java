package com.examen.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.config.OrderNumberProperties;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.exception.OrderNumberGenerationException;
import com.examen.ecommerce.service.OrderCheckout;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class OrderServiceImplTest {

    private final OrderCheckout checkout = mock(OrderCheckout.class);
    private final OrderServiceImpl service = new OrderServiceImpl(checkout, new OrderNumberProperties("ORD", 3));
    private final CartRequest request = new CartRequest(List.of(new CartItemRequest(1, 1)), null);
    private final OrderResponse response = mock(OrderResponse.class);

    @Test
    void retriesOnOrderNumberCollisionThenSucceeds() {
        when(checkout.placeOrder(any())).thenThrow(collision()).thenReturn(response);

        assertThat(service.checkout(request)).isSameAs(response);
        verify(checkout, times(2)).placeOrder(request);
    }

    @Test
    void retriesAreBounded() {
        when(checkout.placeOrder(any())).thenThrow(collision());

        assertThatThrownBy(() -> service.checkout(request)).isInstanceOf(OrderNumberGenerationException.class);
        verify(checkout, times(3)).placeOrder(request);
    }

    @Test
    void otherIntegrityViolationsAreNotRetried() {
        when(checkout.placeOrder(any())).thenThrow(new DataIntegrityViolationException("fk_order_items_product"));

        assertThatThrownBy(() -> service.checkout(request)).isInstanceOf(DataIntegrityViolationException.class);
        verify(checkout, times(1)).placeOrder(request);
    }

    private static DataIntegrityViolationException collision() {
        return new DataIntegrityViolationException("duplicado", new SQLIntegrityConstraintViolationException(
                "Duplicate entry 'ORD-2026-000005' for key 'uk_orders_order_number'"));
    }
}
