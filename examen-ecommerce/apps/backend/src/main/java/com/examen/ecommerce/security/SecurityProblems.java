package com.examen.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Construye errores de seguridad con el mismo formato Problem Details (RFC 9457) que
 * GlobalExceptionHandler: title = reason phrase y type = "urn:problem-type:{tipo}".
 */
final class SecurityProblems {

    static final String MISSING_TOKEN = "Se requiere un token de acceso (Authorization: Bearer <token>)";
    static final String INVALID_TOKEN = "El token de acceso es inválido";
    static final String EXPIRED_TOKEN = "El token de acceso ha expirado";
    static final String ACCESS_DENIED = "No tiene permisos para acceder a este recurso";

    private static final String TYPE_PREFIX = "urn:problem-type:";

    private SecurityProblems() {
    }

    static ProblemDetail problem(HttpStatus status, String type, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create(TYPE_PREFIX + type));
        return problem;
    }

    static void write(HttpServletRequest request, HttpServletResponse response, ObjectMapper mapper,
                      ProblemDetail problem) throws IOException {
        problem.setInstance(URI.create(request.getRequestURI()));
        response.setStatus(problem.getStatus());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(), problem);
    }
}
