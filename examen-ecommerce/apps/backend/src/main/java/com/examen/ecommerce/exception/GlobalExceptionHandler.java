package com.examen.ecommerce.exception;

import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Traduce las excepciones de la API a Problem Details (RFC 9457), construidos con {@link ProblemDetails}.
 * Nunca expone stack trace ni SQL.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Stream<FieldViolation> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()));
        Stream<FieldViolation> globals = ex.getBindingResult().getGlobalErrors().stream()
                .map(error -> new FieldViolation(error.getObjectName(), error.getDefaultMessage()));
        List<FieldViolation> violations = Stream.concat(fields, globals).toList();
        ProblemDetail problem =
                ProblemDetails.of(HttpStatus.BAD_REQUEST, "validation-error", ErrorMessages.VALIDATION_FAILED);
        problem.setProperty("errors", violations);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleMalformed(HttpMessageNotReadableException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "malformed-request", ErrorMessages.MALFORMED_REQUEST);
    }

    @ExceptionHandler(InvalidCartException.class)
    public ProblemDetail handleInvalidCart(InvalidCartException ex) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "invalid-cart", ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "resource-not-found", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoEndpoint(NoResourceFoundException ex) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "endpoint-not-found", ErrorMessages.ENDPOINT_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ProblemDetails.of(HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed",
                ErrorMessages.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ProblemDetails.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                ErrorMessages.MALFORMED_REQUEST);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex) {
        ProblemDetail problem = ProblemDetails.of(HttpStatus.CONFLICT, "insufficient-stock", ex.getMessage());
        problem.setProperty("productId", ex.getProductId());
        problem.setProperty("requested", ex.getRequested());
        problem.setProperty("available", ex.getAvailable());
        return problem;
    }

    @ExceptionHandler(OrderNumberGenerationException.class)
    public ProblemDetail handleOrderNumber(OrderNumberGenerationException ex) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "order-number-conflict", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        LOG.warn("Violación de integridad: {}", ex.getClass().getSimpleName());
        return ProblemDetails.of(HttpStatus.CONFLICT, "integrity-violation", ErrorMessages.INTEGRITY_VIOLATION);
    }

    @ExceptionHandler(InvalidCouponException.class)
    public ProblemDetail handleInvalidCoupon(InvalidCouponException ex) {
        ProblemDetail problem =
                ProblemDetails.of(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-coupon", ex.getMessage());
        problem.setProperty("couponCode", ex.getCouponCode());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        LOG.error("Error no controlado", ex);
        return ProblemDetails.of(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error", ErrorMessages.INTERNAL_ERROR);
    }
}
