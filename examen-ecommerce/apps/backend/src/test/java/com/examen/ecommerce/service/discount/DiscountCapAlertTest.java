package com.examen.ecommerce.service.discount;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU4 (lado backend) con la base de datos MySQL real (perfil dev): cuando productos + cupón superan
 * el 35 %, la API devuelve {@code capApplied = true} y el mensaje exacto que el frontend debe mostrar
 * como notificación persistente. Si no se alcanza el tope, {@code message} es null.
 * <p>
 * Usa TESTCAP30 (30 %) del seed: con WELCOME2026 (15 %) el máximo posible es 27.33 % y el tope
 * nunca se alcanza. La clase es {@code @Transactional}: el checkout se revierte y no deja datos.
 * Requiere MySQL en localhost:3306 y cupones vigentes (hasta 2026-12-31).
 */
@SpringBootTest(properties = "app.time-zone=America/Bogota")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser
@Transactional
@DisplayName("HU4 · Alerta de límite de descuento alcanzado (con base de datos)")
class DiscountCapAlertTest {

    private static final String QUOTE_URL = "/api/v1/cart/quote";
    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String CAP_MESSAGE =
            "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)";
    /** Caso (d): Monitor ×1 + Smartwatch ×1 + Balón ×2. */
    private static final String ITEMS_D = "[{\"productId\": 4, \"quantity\": 1}, {\"productId\": 12, \"quantity\": 1},"
            + " {\"productId\": 10, \"quantity\": 2}]";
    /** Caso (c): Audífonos ×2 + Libro de programación ×1. */
    private static final String ITEMS_C = "[{\"productId\": 1, \"quantity\": 2}, {\"productId\": 9, \"quantity\": 1}]";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ---------- Se supera el tope → mensaje ----------

    @Test
    @DisplayName("Caso (d) + TESTCAP30 (bruto 39.11 %) → capApplied = true y el mensaje exacto de la HU4")
    void superarElTopeMuestraElMensaje() throws Exception {
        JsonNode quote = call(QUOTE_URL, ITEMS_D, "TESTCAP30");

        assertTrue(quote.get("capApplied").asBoolean());
        assertEquals(CAP_MESSAGE, quote.get("message").asText());
        assertMoney("35.00", quote, "effectiveDiscountPct");
        assertMoney("13.16", quote, "capAdjustment");
        assertMoney("208.00", quote, "total");
    }

    @Test
    @DisplayName("El mensaje incluye el porcentaje del tope configurado (35%)")
    void elMensajeIncluyeElTope() throws Exception {
        JsonNode quote = call(QUOTE_URL, ITEMS_D, "TESTCAP30");

        String message = quote.get("message").asText();
        assertTrue(message.startsWith("¡Enhorabuena!"), message);
        assertTrue(message.contains("(35%)"), message);
        assertMoney("35.00", quote, "discountCapPct");
    }

    @Test
    @DisplayName("Notificación persistente: cotizar de nuevo el mismo carrito devuelve el mismo mensaje")
    void elMensajeSeMantieneEnCadaCotizacion() throws Exception {
        JsonNode first = call(QUOTE_URL, ITEMS_D, "TESTCAP30");
        JsonNode second = call(QUOTE_URL, ITEMS_D, "TESTCAP30");

        assertEquals(CAP_MESSAGE, first.get("message").asText());
        assertEquals(first.get("message").asText(), second.get("message").asText());
        assertEquals(first.get("total").decimalValue(), second.get("total").decimalValue());
    }

    @Test
    @DisplayName("El checkout confirma la orden con el mismo mensaje y capApplied = true (201)")
    void elCheckoutTambienDevuelveElMensaje() throws Exception {
        JsonNode quote = call(QUOTE_URL, ITEMS_D, "TESTCAP30");
        JsonNode order = postExpecting(ORDERS_URL, ITEMS_D, "TESTCAP30", 201);

        assertTrue(order.get("capApplied").asBoolean());
        assertEquals(CAP_MESSAGE, order.get("message").asText());
        assertEquals(quote.get("message").asText(), order.get("message").asText());
        assertMoney("208.00", order, "total");
    }

    // ---------- No se supera el tope → sin mensaje ----------

    @Test
    @DisplayName("Caso (c) + WELCOME2026 (24.84 %) → capApplied = false y message = null")
    void sinTopeNoHayMensaje() throws Exception {
        JsonNode quote = call(QUOTE_URL, ITEMS_C, "WELCOME2026");

        assertFalse(quote.get("capApplied").asBoolean());
        assertTrue(quote.get("message").isNull());
        assertMoney("0.00", quote, "capAdjustment");
    }

    @Test
    @DisplayName("Máximo posible con WELCOME2026 (27.33 %) → todavía sin mensaje")
    void maximoConWelcomeNoAlcanzaElTope() throws Exception {
        // 270.00 → cat 27.00 → 243.00 → vol 12.15 → 230.85 → cupón 34.63 → dto 73.78 (27.33 %)
        String tecnologia = "[{\"productId\": 4, \"quantity\": 1}, {\"productId\": 12, \"quantity\": 1}]";

        JsonNode quote = call(QUOTE_URL, tecnologia, "WELCOME2026");

        assertMoney("27.33", quote, "effectiveDiscountPct");
        assertFalse(quote.get("capApplied").asBoolean());
        assertTrue(quote.get("message").isNull());
    }

    @Test
    @DisplayName("Al quitar el cupón el tope deja de alcanzarse y el mensaje desaparece")
    void quitarElCuponQuitaElMensaje() throws Exception {
        assertEquals(CAP_MESSAGE, call(QUOTE_URL, ITEMS_D, "TESTCAP30").get("message").asText());

        JsonNode withoutCoupon = call(QUOTE_URL, ITEMS_D, null);

        assertFalse(withoutCoupon.get("capApplied").asBoolean());
        assertTrue(withoutCoupon.get("message").isNull());
        assertMoney("278.35", withoutCoupon, "total");
    }

    @Test
    @DisplayName("Sin tope, el checkout confirma la orden con message = null")
    void checkoutSinTopeNoTraeMensaje() throws Exception {
        JsonNode order = postExpecting(ORDERS_URL, ITEMS_C, "WELCOME2026", 201);

        assertFalse(order.get("capApplied").asBoolean());
        assertTrue(order.get("message").isNull());
    }

    // ---------- Utilidades ----------

    private JsonNode call(String url, String itemsJson, String couponCode) throws Exception {
        return postExpecting(url, itemsJson, couponCode, 200);
    }

    private JsonNode postExpecting(String url, String itemsJson, String couponCode, int expectedStatus)
            throws Exception {
        String coupon = couponCode == null ? "null" : objectMapper.writeValueAsString(couponCode);
        String body = mockMvc.perform(post(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": " + itemsJson + ", \"couponCode\": " + coupon + "}"))
                .andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body);
    }

    /** Compara importes por valor (208.0 == 208.00). */
    private static void assertMoney(String expected, JsonNode node, String field) {
        BigDecimal actual = node.get(field).decimalValue();
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                field + ": esperado " + expected + " pero fue " + actual);
    }
}
