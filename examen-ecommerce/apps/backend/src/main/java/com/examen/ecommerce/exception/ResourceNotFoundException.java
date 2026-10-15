package com.examen.ecommerce.exception;

/** 404: recurso inexistente. */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String resource, Object identifier) {
        super(ErrorMessages.RESOURCE_NOT_FOUND.formatted(resource, identifier));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
