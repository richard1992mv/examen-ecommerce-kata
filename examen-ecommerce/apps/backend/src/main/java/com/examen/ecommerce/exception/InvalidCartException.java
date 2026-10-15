package com.examen.ecommerce.exception;

/** 400: carrito vacío, cantidades no positivas o productos duplicados. */
public class InvalidCartException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidCartException(String message) {
        super(message);
    }
}
