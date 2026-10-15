package com.examen.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * HU2 (lado backend) con la base de datos real del perfil dev: al "Aplicar" un cupón,
 * POST /cart/quote devuelve el desglose (categoría, volumen, cupón), el % efectivo, el ahorro total
 * (discountTotal) y el valor final a pagar (total). Los cupones se leen de la tabla coupons del seed.
 * <p>
 * Solo lectura. Requiere MySQL en localhost:3306. WELCOME2026 es válido hasta 2026-12-31 23:59:59:
 * después de esa fecha los casos que lo usan pasarán a responder 422.
 */
@SpringBootTest(properties = "app.time-zone=America/Bogota")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("HU2 · Aplicar cupón y ver el desglose de descuentos (con base de datos)")
class CouponBreakdownTest {

    private static final String QUOTE_URL = "/api/v1/cart/quote";
    /** Caso (c) del seed: Audífonos Bluetooth ×2 (Tecnología) + Libro de programación ×1. */
    private static final String CART_C = "[{\"productId\": 1, \"quantity\": 2}, {\"productId\": 9, \"quantity\": 1}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- Cupón válido: desglose completo ----------

    @Test
    @DisplayName("WELCOME2026: desglose del caso (c) — 9.00 + 6.05 + 17.24 = 32.29, 24.84 %, total 97.71")
    void aplicaWelcome2026YMuestraElDesglose() throws Exception {
        JsonNode quote = quoteOk(CART_C, "WELCOME2026");

        assertMoney("130.00", quote, "subtotalOriginal");
        assertMoney("9.00", quote, "categoryDiscount");
        assertMoney("6.05", quote, "volumeDiscount");
        assertMoney("17.24", quote, "couponDiscount");
        assertMoney("0.00", quote, "capAdjustment");
        assertMoney("32.29", quote, "discountTotal");
        assertMoney("24.84", quote, "effectiveDiscountPct");
        assertMoney("97.71", quote, "total");
        assertEquals("WELCOME2026", quote.get("couponCode").asText());
        assertEquals("USD", quote.get("currency").asText());
        assertFalse(quote.get("capApplied").asBoolean());
        assertTrue(quote.get("message").isNull(), "sin tope no hay mensaje HU4");
    }

    @Test
    @DisplayName("El desglose por línea muestra el descuento de categoría de cada producto")
    void desglosePorLinea() throws Exception {
        JsonNode items = quoteOk(CART_C, "WELCOME2026").get("items");

        assertEquals(2, items.size());
        JsonNode audifonos = items.get(0);
        assertEquals(1, audifonos.get("productId").asInt());
        assertEquals("Audífonos Bluetooth", audifonos.get("productName").asText());
        assertEquals("Tecnología", audifonos.get("categoryName").asText());
        assertMoney("90.00", audifonos, "lineSubtotal");
        assertMoney("9.00", audifonos, "categoryDiscount");
        assertMoney("81.00", audifonos, "lineTotal");

        JsonNode libro = items.get(1);
        assertEquals("Libro de programación", libro.get("productName").asText());
        assertMoney("0.00", libro, "categoryDiscount");
        assertMoney("40.00", libro, "lineTotal");
    }

    @Test
    @DisplayName("Ahorro total + valor final = subtotal original, y ahorro = categoría + volumen + cupón")
    void ahorroYTotalCuadran() throws Exception {
        JsonNode quote = quoteOk(CART_C, "WELCOME2026");

        BigDecimal original = money(quote, "subtotalOriginal");
        BigDecimal ahorro = money(quote, "discountTotal");
        BigDecimal total = money(quote, "total");
        BigDecimal suma = money(quote, "categoryDiscount")
                .add(money(quote, "volumeDiscount"))
                .add(money(quote, "couponDiscount"));

        assertEquals(0, original.compareTo(ahorro.add(total)));
        assertEquals(0, ahorro.compareTo(suma));
    }

    @Test
    @DisplayName("Mismo carrito sin cupón → cupón 0.00, ahorro 15.05 (11.58 %), total 114.95")
    void sinCuponElDesgloseCambia() throws Exception {
        JsonNode quote = quoteOk(CART_C, null);

        assertMoney("9.00", quote, "categoryDiscount");
        assertMoney("6.05", quote, "volumeDiscount");
        assertMoney("0.00", quote, "couponDiscount");
        assertMoney("15.05", quote, "discountTotal");
        assertMoney("11.58", quote, "effectiveDiscountPct");
        assertMoney("114.95", quote, "total");
        assertTrue(quote.get("couponCode").isNull());
    }

    @Test
    @DisplayName("Carrito pequeño (caso (a)) + WELCOME2026 → sin volumen, cupón 7.20, total 40.80")
    void cuponEnCarritoSinVolumen() throws Exception {
        String cartA = "[{\"productId\": 8, \"quantity\": 2}, {\"productId\": 2, \"quantity\": 1}]";

        JsonNode quote = quoteOk(cartA, "WELCOME2026");

        assertMoney("50.00", quote, "subtotalOriginal");
        assertMoney("2.00", quote, "categoryDiscount");
        assertMoney("0.00", quote, "volumeDiscount");
        assertMoney("7.20", quote, "couponDiscount");
        assertMoney("9.20", quote, "discountTotal");
        assertMoney("18.40", quote, "effectiveDiscountPct");
        assertMoney("40.80", quote, "total");
    }

    @Test
    @DisplayName("El código se acepta con espacios alrededor (se normaliza)")
    void codigoConEspacios() throws Exception {
        JsonNode quote = quoteOk(CART_C, "  WELCOME2026  ");

        assertEquals("WELCOME2026", quote.get("couponCode").asText());
        assertMoney("17.24", quote, "couponDiscount");
    }

    // ---------- Cupones inválidos de la BD → 422 ----------

    @Test
    @DisplayName("EXPIRED2025 (vencido) → 422 'fuera de vigencia'")
    void cuponVencido() throws Exception {
        assertInvalidCoupon("EXPIRED2025", "El cupón 'EXPIRED2025' está fuera de vigencia");
    }

    @Test
    @DisplayName("INACTIVE10 (inactivo) → 422 'está inactivo'")
    void cuponInactivo() throws Exception {
        assertInvalidCoupon("INACTIVE10", "El cupón 'INACTIVE10' está inactivo");
    }

    @Test
    @DisplayName("Código inexistente → 422 'no existe'")
    void cuponInexistente() throws Exception {
        assertInvalidCoupon("NOEXISTE", "El cupón 'NOEXISTE' no existe");
    }

    @Test
    @DisplayName("Aplicar un cupón (cotizar) no modifica el stock en la base de datos")
    void aplicarCuponNoModificaStock() throws Exception {
        Integer audifonosBefore = stockOf(1);
        Integer libroBefore = stockOf(9);

        quoteOk(CART_C, "WELCOME2026");

        assertEquals(audifonosBefore, stockOf(1));
        assertEquals(libroBefore, stockOf(9));
    }

    // ---------- Utilidades ----------

    private JsonNode quoteOk(String itemsJson, String couponCode) throws Exception {
        String body = mockMvc.perform(post(QUOTE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(itemsJson, couponCode)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    private void assertInvalidCoupon(String couponCode, String expectedDetail) throws Exception {
        String body = mockMvc.perform(post(QUOTE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody(CART_C, couponCode)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode problem = objectMapper.readTree(body);

        assertEquals("urn:problem-type:invalid-coupon", problem.get("type").asText());
        assertEquals(422, problem.get("status").asInt());
        assertEquals(expectedDetail, problem.get("detail").asText());
        assertEquals(couponCode, problem.get("couponCode").asText());
    }

    private String requestBody(String itemsJson, String couponCode) throws Exception {
        String coupon = couponCode == null ? "null" : objectMapper.writeValueAsString(couponCode);
        return "{\"items\": " + itemsJson + ", \"couponCode\": " + coupon + "}";
    }

    private Integer stockOf(int productId) {
        return jdbcTemplate.queryForObject("select stock from products where id = ?", Integer.class, productId);
    }

    private static BigDecimal money(JsonNode node, String field) {
        return node.get(field).decimalValue();
    }

    /** Compara importes por valor (97.71 == 97.710), sin depender de la escala del JSON. */
    private static void assertMoney(String expected, JsonNode node, String field) {
        BigDecimal actual = money(node, field);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                field + ": esperado " + expected + " pero fue " + actual);
    }
}
