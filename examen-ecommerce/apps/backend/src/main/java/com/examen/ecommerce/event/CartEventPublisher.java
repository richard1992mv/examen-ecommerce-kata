package com.examen.ecommerce.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Sujeto del patrón OBSERVER. Los servicios publican aquí y no conocen a los listeners.
 * Un fallo de un observador síncrono se captura y registra: nunca rompe la operación.
 */
@Component
public class CartEventPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(CartEventPublisher.class);

    private final ApplicationEventPublisher delegate;

    public CartEventPublisher(ApplicationEventPublisher delegate) {
        this.delegate = delegate;
    }

    public void publish(CartEvent event) {
        try {
            delegate.publishEvent(event);
        } catch (RuntimeException ex) {
            LOG.warn("Observador falló procesando {}: {}", event.getClass().getSimpleName(), ex.getMessage());
        }
    }
}
