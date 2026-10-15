package com.examen.ecommerce.exception;

/** 404: recurso inexistente (productos del carrito). */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
