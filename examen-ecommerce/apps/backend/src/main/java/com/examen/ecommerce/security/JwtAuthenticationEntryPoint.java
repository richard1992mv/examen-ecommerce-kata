package com.examen.ecommerce.security;

import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ProblemDetails;
import com.examen.ecommerce.security.JwtAuthenticationFilter.TokenError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Responde 401 (no 403) en JSON Problem Details cuando el token falta, es inválido o expiró. */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // getAttribute devuelve Object (API de Servlet): se acota a TokenError; null = no se envió token
        TokenError error = request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE)
                instanceof TokenError tokenError ? tokenError : null;
        String detail;
        String bearerError;
        if (error == TokenError.EXPIRED) {
            detail = ErrorMessages.EXPIRED_TOKEN;
            bearerError = "Bearer error=\"invalid_token\", error_description=\"token expirado\"";
        } else if (error == TokenError.INVALID) {
            detail = ErrorMessages.INVALID_TOKEN;
            bearerError = "Bearer error=\"invalid_token\"";
        } else {
            detail = ErrorMessages.MISSING_TOKEN;
            bearerError = "Bearer";
        }
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, bearerError);
        SecurityProblems.write(request, response, objectMapper,
                ProblemDetails.of(HttpStatus.UNAUTHORIZED, "unauthorized", detail));
    }
}
