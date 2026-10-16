package com.examen.ecommerce.controller;

import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ProblemDetails;
import com.examen.ecommerce.security.JwtService;
import com.examen.ecommerce.security.LoginRequest;
import com.examen.ecommerce.security.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener un token JWT (Bearer)")
    @ApiResponse(responseCode = "200", description = "Credenciales válidas: devuelve el token")
    @ApiResponse(responseCode = "401", description = "Credenciales inválidas")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        String token = jwtService.generateToken(authentication.getName());
        return TokenResponse.bearer(token, jwtService.getExpirationSeconds());
    }

    /** Local al controlador: tiene prioridad sobre el manejador genérico (500) de GlobalExceptionHandler. */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleBadCredentials(AuthenticationException ex) {
        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "invalid-credentials", ErrorMessages.BAD_CREDENTIALS);
    }
}
