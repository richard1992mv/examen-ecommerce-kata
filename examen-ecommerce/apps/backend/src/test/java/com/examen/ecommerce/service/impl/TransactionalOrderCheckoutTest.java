package com.examen.ecommerce.service.impl;

import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.adapter.DiscountContextAdapter;
import com.examen.ecommerce.adapter.DiscountResultAdapter;
import com.examen.ecommerce.config.OrderNumberProperties;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.event.CartEventPublisher;
import com.examen.ecommerce.event.OrderConfirmedEvent;
import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.mapper.DiscountMessageMapper;
import com.examen.ecommerce.mapper.OrderMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.PricingService;
import com.examen.ecommerce.service.StockService;
import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import com.examen.ecommerce.support.TestData;
import com.examen.ecommerce.util.OrderNumberGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class TransactionalOrderCheckoutTest {

    private final PricingService pricingService = mock(PricingService.class);
    private final StockService stockService = mock(StockService.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CartEventPublisher publisher = mock(CartEventPublisher.class);
    private final DiscountMessageMapper messages = new DiscountMessageMapper();
    private final TransactionalOrderCheckout checkout = new TransactionalOrderCheckout(pricingService, stockService,
            orderRepository, new OrderNumberGenerator(new OrderNumberProperties("ORD", 3)),
            new DiscountResultAdapter(messages), new OrderMapper(messages), publisher, TestData.FIXED_CLOCK);

    private final CartRequest request = new CartRequest(
            List.of(new CartItemRequest(4, 1), new CartItemRequest(12, 1), new CartItemRequest(10, 2)), "TESTCAP30");

    @BeforeEach
    void setUp() {
        when(pricingService.price(request)).thenReturn(pricedCaseD());
        when(orderRepository.findLastOrderNumber("ORD-2026-")).thenReturn(Optional.of("ORD-2026-000004"));
        when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 5);
            return order;
        });
    }

    @Test
    void persistsOrderWithNextNumberSnapshotsAndPublishesEvent() {
        OrderResponse response = checkout.placeOrder(request);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).saveAndFlush(saved.capture());
        Order order = saved.getValue();
        assertThat(order.getOrderNumber()).isEqualTo("ORD-2026-000005");
        assertThat(order.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 10, 15, 12, 0));
        assertThat(order.getCouponCode()).isEqualTo("TESTCAP30");
        assertThat(order.getDiscountCapPct()).isEqualByComparingTo("35.00");
        assertThat(order.getItems()).extracting("productName", "categoryName")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Monitor 24 pulgadas", "Tecnología"),
                        org.assertj.core.groups.Tuple.tuple("Smartwatch", "Tecnología"),
                        org.assertj.core.groups.Tuple.tuple("Balón de fútbol", "Deportes"));
        assertThat(order.getItems().get(2).getUnitPrice()).isEqualByComparingTo("25.00");
        verify(stockService).decrement(request.items());
        verify(publisher).publish(new OrderConfirmedEvent(5, "ORD-2026-000005", money("208.00")));
        assertThat(response.message()).contains("35%");
        assertThat(response.total()).isEqualByComparingTo("208.00");
    }

    @Test
    void insufficientStockDoesNotPersistNorPublish() {
        doThrow(new InsufficientStockException(4, 1, 0)).when(stockService).decrement(request.items());

        assertThatThrownBy(() -> checkout.placeOrder(request)).isInstanceOf(InsufficientStockException.class);

        verify(orderRepository, never()).saveAndFlush(any());
        verify(publisher, never()).publish(any());
    }

    private static PricedCart pricedCaseD() {
        Map<Integer, Product> products = Map.of(4, product(4), 12, product(12), 10, product(10));
        Coupon coupon = TestData.testCap30();
        DiscountContextAdapter adapter = new DiscountContextAdapter();
        DiscountContext ctx = adapter.toContext(List.of(new CartItemRequest(4, 1), new CartItemRequest(12, 1),
                new CartItemRequest(10, 2)), products, Optional.of(coupon));
        DiscountRuleFactory factory = new DiscountRuleFactory(money("5.00"), money("100.00"));
        return new PricedCart(products, Optional.of(coupon), new DiscountEngine(new PercentageCapPolicy(
                money("35.00"))).calculate(ctx, factory.createRules(ctx.coupon())));
    }
}
