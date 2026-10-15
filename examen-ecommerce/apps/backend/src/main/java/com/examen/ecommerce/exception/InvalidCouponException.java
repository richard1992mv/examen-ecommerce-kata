package com.examen.ecommerce.exception;

/** 422: cupón inexistente, inactivo o fuera de vigencia. */
public class InvalidCouponException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String couponCode;

    public InvalidCouponException(String template, String couponCode) {
        super(template.formatted(couponCode));
        this.couponCode = couponCode;
    }

    public String getCouponCode() {
        return couponCode;
    }
}
