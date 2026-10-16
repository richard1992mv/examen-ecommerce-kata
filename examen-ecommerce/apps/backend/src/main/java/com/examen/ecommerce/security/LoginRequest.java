package com.examen.ecommerce.security;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Credenciales de acceso para POST /api/v1/auth/login. */
public record LoginRequest(
        @Schema(example = "admin") @NotBlank String username,
        @Schema(example = "admin123") @NotBlank String password) {
}
