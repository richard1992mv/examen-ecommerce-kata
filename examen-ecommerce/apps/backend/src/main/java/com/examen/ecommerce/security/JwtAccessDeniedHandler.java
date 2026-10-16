package com.examen.ecommerce.security;

import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ProblemDetails;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 403 en formato Problem Details para usuarios autenticados sin permisos. */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        SecurityProblems.write(request, response, objectMapper,
                ProblemDetails.of(HttpStatus.FORBIDDEN, "forbidden", ErrorMessages.ACCESS_DENIED));
    }
}
