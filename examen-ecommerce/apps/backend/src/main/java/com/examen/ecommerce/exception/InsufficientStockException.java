package com.examen.ecommerce.exception;

/** 409: stock insuficiente. Provoca rollback total del checkout. */
public class InsufficientStockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Integer productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(Integer productId, int requested, int available) {
        super(ErrorMessages.INSUFFICIENT_STOCK.formatted(productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public Integer getProductId() {
        return productId;
    }

    public int getRequested() {
        return requested;
    }

    public int getAvailable() {
        return available;
    }
}
