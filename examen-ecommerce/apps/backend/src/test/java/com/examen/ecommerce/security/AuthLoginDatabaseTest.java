package com.examen.ecommerce.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Login contra la tabla app_users real (perfil dev). Solo lectura.
 * Requiere MySQL en localhost:3306 con 04_auth_users.sql (o 01 + 02) ejecutado.
 */
@SpringBootTest(properties = "app.time-zone=America/Bogota")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("Login JWT con usuarios de MySQL (con base de datos)")
class AuthLoginDatabaseTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("admin / admin123 → 200 y el token permite GET /products")
    void adminObtieneTokenYConsultaProductos() throws Exception {
        String token = login("admin", "admin123", 200).get("accessToken").asText();
        assertFalse(token.isBlank());

        mockMvc.perform(get("/api/v1/products").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("cliente / cliente123 → 200 con tokenType Bearer")
    void clienteObtieneToken() throws Exception {
        JsonNode body = login("cliente", "cliente123", 200);

        assertEquals("Bearer", body.get("tokenType").asText());
        assertEquals(3600, body.get("expiresIn").asLong());
    }

    @Test
    @DisplayName("Contraseña incorrecta → 401")
    void contrasenaIncorrecta() throws Exception {
        login("admin", "incorrecta", 401);
    }

    @Test
    @DisplayName("Usuario inactivo (bloqueado) → 401")
    void usuarioInactivo() throws Exception {
        login("bloqueado", "bloqueado123", 401);
    }

    private JsonNode login(String username, String password, int expectedStatus) throws Exception {
        String json = objectMapper.writeValueAsString(new LoginRequest(username, password));
        String body = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }
}
