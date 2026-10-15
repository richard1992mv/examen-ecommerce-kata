package com.examen.ecommerce.event;

import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.examen.ecommerce.event.listener.CartAuditListener;
import com.examen.ecommerce.event.listener.DiscountMetricsListener;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class ObserverUnitTest {

    @Test
    void publisherSwallowsListenerFailures() {
        ApplicationEventPublisher delegate = mock(ApplicationEventPublisher.class);
        doThrow(new IllegalStateException("boom")).when(delegate).publishEvent(any(Object.class));

        assertThatCode(() -> new CartEventPublisher(delegate)
                .publish(new CartQuotedEvent(1, money("0.00"), false))).doesNotThrowAnyException();
    }

    @Test
    void metricsListenerCountsEachEvent() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DiscountMetricsListener listener = new DiscountMetricsListener(registry);

        listener.onCartQuoted(new CartQuotedEvent(3, money("112.00"), true));
        listener.onDiscountCapReached(new DiscountCapReachedEvent(money("320.00"), money("125.16"), money("13.16")));
        listener.onOrderConfirmed(new OrderConfirmedEvent(5, "ORD-2026-000005", money("208.00")));

        assertThat(registry.counter(DiscountMetricsListener.CART_QUOTES).count()).isEqualTo(1.0);
        assertThat(registry.counter(DiscountMetricsListener.CAP_REACHED).count()).isEqualTo(1.0);
        assertThat(registry.counter(DiscountMetricsListener.ORDERS_CONFIRMED).count()).isEqualTo(1.0);
    }

    @Test
    void auditListenerNeverThrows() {
        CartAuditListener listener = new CartAuditListener();
        assertThatCode(() -> {
            listener.onCartQuoted(new CartQuotedEvent(1, money("2.00"), false));
            listener.onDiscountCapReached(new DiscountCapReachedEvent(money("1"), money("1"), money("0")));
            listener.onOrderConfirmed(new OrderConfirmedEvent(1, "ORD-2026-000001", money("48.00")));
        }).doesNotThrowAnyException();
    }
}
