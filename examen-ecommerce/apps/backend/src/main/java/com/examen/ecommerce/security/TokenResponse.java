package com.examen.ecommerce.security;

import io.swagger.v3.oas.annotations.media.Schema;

/** Respuesta del login: token JWT, tipo y vigencia en segundos. */
public record TokenResponse(
        String accessToken,
        @Schema(example = "Bearer") String tokenType,
        @Schema(description = "Segundos de vigencia del token", example = "3600") long expiresIn) {

    public static TokenResponse bearer(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, "Bearer", expiresInSeconds);
    }
}
