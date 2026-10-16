package com.examen.ecommerce.exception;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Único lugar donde se construye un Problem Details (RFC 9457): title = reason phrase del status y
 * type = "urn:problem-type:{tipo}". Lo usan GlobalExceptionHandler, AuthController y los manejadores 401/403.
 */
public final class ProblemDetails {

    private static final String TYPE_PREFIX = "urn:problem-type:";

    private ProblemDetails() {
    }

    public static ProblemDetail of(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create(TYPE_PREFIX + type));
        return problem;
    }
}
