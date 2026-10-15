package com.examen.ecommerce.event.listener;

import com.examen.ecommerce.event.CartQuotedEvent;
import com.examen.ecommerce.event.DiscountCapReachedEvent;
import com.examen.ecommerce.event.OrderConfirmedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observador de auditoría: log estructurado (clave=valor) sin datos sensibles.
 * Solo efectos secundarios: no modifica cálculos, respuesta ni stock.
 */
@Component
public class CartAuditListener {

    private static final Logger LOG = LoggerFactory.getLogger("AUDIT");

    @EventListener
    public void onCartQuoted(CartQuotedEvent event) {
        safely(() -> LOG.info("event=cart.quoted items={} discountTotal={} capApplied={}",
                event.items(), event.discountTotal(), event.capApplied()));
    }

    @EventListener
    public void onDiscountCapReached(DiscountCapReachedEvent event) {
        safely(() -> LOG.info("event=discount.cap.reached subtotalOriginal={} grossDiscount={} capAdjustment={}",
                event.subtotalOriginal(), event.grossDiscount(), event.capAdjustment()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        safely(() -> LOG.info("event=order.confirmed orderId={} orderNumber={} total={}",
                event.orderId(), event.orderNumber(), event.total()));
    }

    private static void safely(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            LOG.warn("event=audit.failed reason={}", ex.getMessage());
        }
    }
}
