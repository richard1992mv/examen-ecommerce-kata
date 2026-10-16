package com.examen.ecommerce.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examen.ecommerce.controller.AuthController;
import com.examen.ecommerce.controller.ProductController;
import com.examen.ecommerce.service.ProductService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Seguridad JWT de la API (sin base de datos: ProductService se simula).
 * Cubre: 401 sin token, 401 con token inválido/expirado, 200 en login, 401 con credenciales
 * inválidas y 200 en GET /products con token válido.
 */
@WebMvcTest(controllers = {AuthController.class, ProductController.class})
@Import({SecurityConfig.class, JwtService.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
@DisplayName("Seguridad JWT · login, 401 y acceso con token")
class SecurityIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String VALID_LOGIN = "{\"username\":\"admin\",\"password\":\"admin123\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private ProductService productService;

    @Test
    @DisplayName("GET /products sin token → 401 en formato Problem Details")
    void productsWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(header().exists(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"))
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.detail").value(SecurityProblems.MISSING_TOKEN));
    }

    @Test
    @DisplayName("GET /products con token inválido → 401")
    void productsWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:unauthorized"))
                .andExpect(jsonPath("$.detail").value(SecurityProblems.INVALID_TOKEN));
    }

    @Test
    @DisplayName("GET /products con token expirado → 401")
    void productsWithExpiredTokenReturns401() throws Exception {
        JwtService expiredIssuer = new JwtService(
                "sQMXlSLkSLGE8RVJOxKTPG+Omna01r2jPqtCUbzpgvQ=", -60_000);
        String expired = expiredIssuer.generateToken("admin");

        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value(SecurityProblems.EXPIRED_TOKEN));
    }

    @Test
    @DisplayName("POST /auth/login con credenciales válidas → 200 con accessToken Bearer")
    void loginReturns200WithToken() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    @DisplayName("POST /auth/login con credenciales inválidas → 401")
    void loginWithBadCredentialsReturns401() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("urn:problem-type:invalid-credentials"));
    }

    @Test
    @DisplayName("GET /products con token del login → 200")
    void productsWithTokenReturns200() throws Exception {
        when(productService.findAll()).thenReturn(List.of());

        String body = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_LOGIN))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        String token = json.get("accessToken").asText();

        mockMvc.perform(get(PRODUCTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("El token emitido identifica al usuario y dura app.jwt.expiration-ms")
    void issuedTokenCarriesSubject() {
        String token = jwtService.generateToken("admin");
        assertThat(jwtService.validateAndGetUsername(token)).isEqualTo("admin");
        assertThat(jwtService.getExpirationMs()).isEqualTo(3_600_000L);
    }
}
