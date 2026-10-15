package com.examen.ecommerce.exception;

/** 409: el recurso está en uso (FK) o duplicaría un valor UNIQUE. */
public class ResourceConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceConflictException(String message) {
        super(message);
    }
}
