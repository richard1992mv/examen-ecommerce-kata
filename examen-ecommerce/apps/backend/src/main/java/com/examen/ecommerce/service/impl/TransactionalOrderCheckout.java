package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.adapter.DiscountResultAdapter;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.event.CartEventPublisher;
import com.examen.ecommerce.event.OrderConfirmedEvent;
import com.examen.ecommerce.mapper.OrderMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.service.OrderCheckout;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.PricingService;
import com.examen.ecommerce.service.StockService;
import com.examen.ecommerce.util.DateTimeUtils;
import com.examen.ecommerce.util.OrderNumberGenerator;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un intento de checkout dentro de UNA transacción: cálculo (mismo flujo que quote) →
 * UPDATE atómico de stock → order_number → persistir → publicar evento (se notifica AFTER_COMMIT).
 */
@Service
public class TransactionalOrderCheckout implements OrderCheckout {

    private final PricingService pricingService;
    private final StockService stockService;
    private final OrderRepository orderRepository;
    private final OrderNumberGenerator numberGenerator;
    private final DiscountResultAdapter resultAdapter;
    private final OrderMapper orderMapper;
    private final CartEventPublisher publisher;
    private final Clock clock;

    public TransactionalOrderCheckout(PricingService pricingService, StockService stockService,
                                      OrderRepository orderRepository, OrderNumberGenerator numberGenerator,
                                      DiscountResultAdapter resultAdapter, OrderMapper orderMapper,
                                      CartEventPublisher publisher, Clock clock) {
        this.pricingService = pricingService;
        this.stockService = stockService;
        this.orderRepository = orderRepository;
        this.numberGenerator = numberGenerator;
        this.resultAdapter = resultAdapter;
        this.orderMapper = orderMapper;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(CartRequest request) {
        PricedCart priced = pricingService.price(request);
        stockService.decrement(request.items());
        LocalDateTime now = DateTimeUtils.now(clock);
        Order order = resultAdapter.toOrder(priced, nextOrderNumber(now.getYear()), now);
        Order saved = orderRepository.saveAndFlush(order);
        publisher.publish(new OrderConfirmedEvent(saved.getId(), saved.getOrderNumber(), saved.getTotal()));
        return orderMapper.toResponse(saved);
    }

    private String nextOrderNumber(int year) {
        return numberGenerator.next(year, orderRepository.findLastOrderNumber(numberGenerator.yearPrefix(year)));
    }
}
