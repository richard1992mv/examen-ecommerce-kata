package com.examen.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.config.OrderNumberProperties;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.OrderNumberGenerationException;
import com.examen.ecommerce.mapper.CheckoutMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.util.OrderNumberGenerator;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * HU3 · reintento acotado del checkout cuando dos órdenes generan el mismo order_number (UNIQUE
 * uk_orders_order_number). Prueba unitaria con Mockito: la colisión es difícil de provocar contra la BD real.
 */
@DisplayName("OrderService · reintento por colisión de order_number")
class OrderServiceRetryTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final CartRequest REQUEST = new CartRequest(List.of(new CartItemRequest(1, 1)), null);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-16T15:00:00Z"), ZoneId.of("America/Bogota"));

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CheckoutMapper mapper = mock(CheckoutMapper.class);
    private final Order order = mock(Order.class);
    private final OrderResponse response = mock(OrderResponse.class);
    private OrderService service;

    @BeforeEach
    void setUp() {
        PricingService pricingService = mock(PricingService.class);
        when(pricingService.price(REQUEST)).thenReturn(mock(PricedCart.class));
        when(orderRepository.findLastOrderNumber(anyString())).thenReturn(Optional.empty());
        when(mapper.toOrder(any(), anyString(), any(LocalDateTime.class))).thenReturn(order);
        when(mapper.toOrderResponse(order)).thenReturn(response);

        service = new OrderService(pricingService, mock(StockService.class), orderRepository,
                new OrderNumberGenerator(new OrderNumberProperties("ORD", MAX_ATTEMPTS)), mapper,
                mock(PlatformTransactionManager.class), new OrderNumberProperties("ORD", MAX_ATTEMPTS), CLOCK);
    }

    private static DataIntegrityViolationException violation(String constraintMessage) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException(constraintMessage));
    }

    @Test
    @DisplayName("Una colisión de order_number se reintenta en una transacción nueva y la orden se crea")
    void reintentaTrasColision() {
        when(orderRepository.saveAndFlush(order))
                .thenThrow(violation("Duplicate entry 'ORD-2026-000005' for key 'uk_orders_order_number'"))
                .thenReturn(order);

        assertSame(response, service.checkout(REQUEST));
        verify(orderRepository, times(2)).saveAndFlush(order);
    }

    @Test
    @DisplayName("Si todos los intentos colisionan → OrderNumberGenerationException (409)")
    void agotaLosIntentos() {
        when(orderRepository.saveAndFlush(order))
                .thenThrow(violation("Duplicate entry for key 'UK_ORDERS_ORDER_NUMBER'"));

        OrderNumberGenerationException ex =
                assertThrows(OrderNumberGenerationException.class, () -> service.checkout(REQUEST));
        assertEquals(ErrorMessages.ORDER_NUMBER_EXHAUSTED.formatted(MAX_ATTEMPTS), ex.getMessage());
        verify(orderRepository, times(MAX_ATTEMPTS)).saveAndFlush(order);
    }

    @Test
    @DisplayName("Otra violación de integridad no se reintenta: se propaga en el primer intento")
    void otraViolacionNoSeReintenta() {
        DataIntegrityViolationException other = violation("Cannot add or update a child row: fk_order_items_product");
        when(orderRepository.saveAndFlush(order)).thenThrow(other);

        assertSame(other, assertThrows(DataIntegrityViolationException.class, () -> service.checkout(REQUEST)));
        verify(orderRepository, times(1)).saveAndFlush(order);
    }
}
