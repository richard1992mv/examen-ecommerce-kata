package com.examen.ecommerce.exception;

/** Campo inválido reportado en el Problem Details 400. */
public record FieldViolation(String field, String message) {
}
