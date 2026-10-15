package com.examen.ecommerce.event.listener;

import com.examen.ecommerce.event.CartQuotedEvent;
import com.examen.ecommerce.event.DiscountCapReachedEvent;
import com.examen.ecommerce.event.OrderConfirmedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Observador de métricas Micrometer (visibles en /actuator/metrics). */
@Component
public class DiscountMetricsListener {

    public static final String CART_QUOTES = "cart.quotes";
    public static final String CAP_REACHED = "discount.cap.reached";
    public static final String ORDERS_CONFIRMED = "orders.confirmed";

    private static final Logger LOG = LoggerFactory.getLogger(DiscountMetricsListener.class);

    private final Counter quotes;
    private final Counter capReached;
    private final Counter ordersConfirmed;

    public DiscountMetricsListener(MeterRegistry registry) {
        this.quotes = Counter.builder(CART_QUOTES).description("Cotizaciones de carrito").register(registry);
        this.capReached = Counter.builder(CAP_REACHED).description("Veces que se aplicó el tope").register(registry);
        this.ordersConfirmed = Counter.builder(ORDERS_CONFIRMED).description("Órdenes confirmadas").register(registry);
    }

    @EventListener
    public void onCartQuoted(CartQuotedEvent event) {
        increment(quotes);
    }

    @EventListener
    public void onDiscountCapReached(DiscountCapReachedEvent event) {
        increment(capReached);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        increment(ordersConfirmed);
    }

    private static void increment(Counter counter) {
        try {
            counter.increment();
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo actualizar la métrica {}: {}", counter.getId().getName(), ex.getMessage());
        }
    }
}
