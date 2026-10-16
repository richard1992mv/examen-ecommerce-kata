package com.examen.ecommerce.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/**
 * Escribe en la respuesta servlet los errores 401/403, que ocurren en la cadena de filtros y por eso no pasan
 * por GlobalExceptionHandler. El Problem Details se construye con
 * {@link com.examen.ecommerce.exception.ProblemDetails} y los mensajes vienen de
 * {@link com.examen.ecommerce.exception.ErrorMessages}.
 */
final class SecurityProblems {

    private SecurityProblems() {
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
