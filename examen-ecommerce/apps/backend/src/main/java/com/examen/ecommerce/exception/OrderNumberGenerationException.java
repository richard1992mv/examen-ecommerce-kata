package com.examen.ecommerce.exception;

/** Se agotaron los reintentos acotados para generar un order_number único. */
public class OrderNumberGenerationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public OrderNumberGenerationException(int attempts, Throwable cause) {
        super(ErrorMessages.ORDER_NUMBER_EXHAUSTED.formatted(attempts), cause);
    }
}
