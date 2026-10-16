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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

/**
 * HU3 con la base de datos MySQL real (perfil dev): POST /orders valida stock, recalcula el motor
 * (categoría → volumen → cupón → tope 35 %), descuenta stock, persiste orders + order_items y responde
 * con el desglose exacto.
 * <p>
 * <b>No deja datos en la BD:</b> la clase es {@code @Transactional}, así que cada prueba corre en una
 * transacción que se revierte al terminar (el checkout se une a ella). Solo los contadores
 * AUTO_INCREMENT de MySQL avanzan, porque no se revierten.
 * <p>
 * Requisitos: MySQL en localhost:3306 con 01 y 02 cargados; stock suficiente en los productos
 * 1, 4, 9, 10 y 12; WELCOME2026 y TESTCAP30 vigentes (hasta 2026-12-31).
 */
@SpringBootTest(properties = "app.time-zone=America/Bogota")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@WithMockUser
@Transactional
@DisplayName("HU3 · Checkout consistente: stock, cálculo, persistencia y confirmación (con base de datos)")
class CheckoutOrderTest {

    private static final String ORDERS_URL = "/api/v1/orders";
    private static final String CAP_MESSAGE = "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)";
    /** Caso (c): Audífonos Bluetooth ×2 (Tecnología) + Libro de programación ×1 + WELCOME2026. */
    private static final String CART_C =
            "{\"items\": [{\"productId\": 1, \"quantity\": 2}, {\"productId\": 9, \"quantity\": 1}],"
            + " \"couponCode\": \"WELCOME2026\"}";
    /** Caso (d): Monitor ×1 + Smartwatch ×1 + Balón ×2 + TESTCAP30 (supera el tope). */
    private static final String CART_D =
            "{\"items\": [{\"productId\": 4, \"quantity\": 1}, {\"productId\": 12, \"quantity\": 1},"
            + " {\"productId\": 10, \"quantity\": 2}], \"couponCode\": \"TESTCAP30\"}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- Checkout exitoso ----------

    @Test
    @DisplayName("Caso (c) → 201 con el desglose exacto: 130.00 − 32.29 = 97.71 (24.84 %)")
    void checkoutRetornaConfirmacionConDesglose() throws Exception {
        JsonNode order = checkout(CART_C, status().isCreated());

        assertTrue(order.get("id").asInt() > 0);
        assertEquals("WELCOME2026", order.get("couponCode").asText());
        assertMoney("130.00", order, "subtotalOriginal");
        assertMoney("9.00", order, "categoryDiscount");
        assertMoney("6.05", order, "volumeDiscount");
        assertMoney("17.24", order, "couponDiscount");
        assertMoney("0.00", order, "capAdjustment");
        assertMoney("32.29", order, "discountTotal");
        assertMoney("24.84", order, "effectiveDiscountPct");
        assertMoney("35.00", order, "discountCapPct");
        assertMoney("97.71", order, "total");
        assertEquals("USD", order.get("currency").asText());
        assertFalse(order.get("capApplied").asBoolean());
        assertTrue(order.get("message").isNull());
        assertEquals(2, order.get("items").size());
    }

    @Test
    @DisplayName("Genera el siguiente order_number del año (ORD-yyyy-NNNNNN)")
    void generaSiguienteNumeroDeOrden() throws Exception {
        String expected = nextOrderNumber();

        JsonNode order = checkout(CART_C, status().isCreated());

        assertEquals(expected, order.get("orderNumber").asText());
    }

    @Test
    @DisplayName("Persiste la orden en MySQL con los mismos montos y el snapshot del cupón")
    void persisteLaOrden() throws Exception {
        int ordersBefore = countOrders();

        JsonNode order = checkout(CART_C, status().isCreated());

        assertEquals(ordersBefore + 1, countOrders());
        Map<String, Object> row = jdbcTemplate.queryForMap("select * from orders where id = ?",
                order.get("id").asInt());
        assertEquals(order.get("orderNumber").asText(), row.get("order_number"));
        assertEquals(couponId("WELCOME2026"), ((Number) row.get("coupon_id")).intValue());
        assertEquals("WELCOME2026", row.get("coupon_code"));
        assertDbMoney("130.00", row, "subtotal_original");
        assertDbMoney("9.00", row, "category_discount_amount");
        assertDbMoney("6.05", row, "volume_discount_amount");
        assertDbMoney("17.24", row, "coupon_discount_amount");
        assertDbMoney("0.00", row, "cap_adjustment_amount");
        assertDbMoney("32.29", row, "discount_total");
        assertDbMoney("24.84", row, "effective_discount_pct");
        assertDbMoney("35.00", row, "discount_cap_pct");
        assertDbMoney("97.71", row, "total");
        assertFalse(flag(row.get("cap_applied")));
        assertEquals("USD", row.get("currency"));
    }

    @Test
    @DisplayName("Persiste order_items con snapshot de nombre, categoría y precio de cada línea")
    void persisteLasLineasConSnapshot() throws Exception {
        JsonNode order = checkout(CART_C, status().isCreated());

        List<Map<String, Object>> items = jdbcTemplate.queryForList(
                "select * from order_items where order_id = ? order by id", order.get("id").asInt());

        assertEquals(2, items.size());
        Map<String, Object> audifonos = items.get(0);
        assertEquals(1, ((Number) audifonos.get("product_id")).intValue());
        assertEquals("Audífonos Bluetooth", audifonos.get("product_name"));
        assertEquals("Tecnología", audifonos.get("category_name"));
        assertDbMoney("45.00", audifonos, "unit_price");
        assertEquals(2, ((Number) audifonos.get("quantity")).intValue());
        assertDbMoney("90.00", audifonos, "line_subtotal");
        assertDbMoney("9.00", audifonos, "category_discount_amount");
        assertDbMoney("81.00", audifonos, "line_total");

        Map<String, Object> libro = items.get(1);
        assertEquals("Libro de programación", libro.get("product_name"));
        assertEquals("Libros", libro.get("category_name"));
        assertDbMoney("40.00", libro, "line_total");
    }

    @Test
    @DisplayName("Decrementa el stock exactamente en las cantidades compradas")
    void decrementaElStock() throws Exception {
        int audifonosBefore = stockOf(1);
        int libroBefore = stockOf(9);

        checkout(CART_C, status().isCreated());

        assertEquals(audifonosBefore - 2, stockOf(1));
        assertEquals(libroBefore - 1, stockOf(9));
    }

    @Test
    @DisplayName("Caso (d) con TESTCAP30 → tope 35 %: ajuste 13.16, total 208.00, mensaje HU4 y cap_applied = 1")
    void aplicaElTopeYLoPersiste() throws Exception {
        JsonNode order = checkout(CART_D, status().isCreated());

        assertMoney("320.00", order, "subtotalOriginal");
        assertMoney("27.00", order, "categoryDiscount");
        assertMoney("14.65", order, "volumeDiscount");
        assertMoney("83.51", order, "couponDiscount");
        assertMoney("13.16", order, "capAdjustment");
        assertMoney("112.00", order, "discountTotal");
        assertMoney("35.00", order, "effectiveDiscountPct");
        assertMoney("208.00", order, "total");
        assertTrue(order.get("capApplied").asBoolean());
        assertEquals(CAP_MESSAGE, order.get("message").asText());

        Map<String, Object> row = jdbcTemplate.queryForMap("select * from orders where id = ?",
                order.get("id").asInt());
        assertTrue(flag(row.get("cap_applied")));
        assertDbMoney("13.16", row, "cap_adjustment_amount");
        assertDbMoney("208.00", row, "total");
    }

    @Test
    @DisplayName("Recalcula con los precios de la BD e ignora montos enviados por el cliente")
    void ignoraMontosDelCliente() throws Exception {
        String cartWithFakeAmounts =
                "{\"items\": [{\"productId\": 1, \"quantity\": 2, \"unitPrice\": 1.00},"
                + " {\"productId\": 9, \"quantity\": 1}], \"couponCode\": \"WELCOME2026\","
                + " \"total\": 1.00, \"discountTotal\": 999.00}";

        JsonNode order = checkout(cartWithFakeAmounts, status().isCreated());

        assertMoney("130.00", order, "subtotalOriginal");
        assertMoney("97.71", order, "total");
    }

    // ---------- Checkout rechazado: no persiste ni toca stock ----------

    @Test
    @DisplayName("Stock insuficiente (Juego de sartenes, pide más de lo disponible) → 409 sin orden ni cambio de stock")
    void stockInsuficiente() throws Exception {
        int available = stockOf(7);
        int requested = available + 1;
        int ordersBefore = countOrders();

        JsonNode problem = checkout("{\"items\": [{\"productId\": 7, \"quantity\": " + requested + "}]}",
                status().isConflict());

        assertEquals("urn:problem-type:insufficient-stock", problem.get("type").asText());
        assertEquals(7, problem.get("productId").asInt());
        assertEquals(requested, problem.get("requested").asInt());
        assertEquals(available, problem.get("available").asInt());
        assertEquals(ordersBefore, countOrders());
        assertEquals(available, stockOf(7));
    }

    @Test
    @DisplayName("Si una línea no tiene stock, no se descuenta ninguna (todo o nada)")
    void sinStockEnUnaLineaNoDescuentaNinguna() throws Exception {
        int audifonosBefore = stockOf(1);
        int sartenesBefore = stockOf(7);
        int ordersBefore = countOrders();

        checkout("{\"items\": [{\"productId\": 1, \"quantity\": 1}, {\"productId\": 7, \"quantity\": "
                + (sartenesBefore + 1) + "}]}", status().isConflict());

        assertEquals(audifonosBefore, stockOf(1));
        assertEquals(sartenesBefore, stockOf(7));
        assertEquals(ordersBefore, countOrders());
    }

    @Test
    @DisplayName("Cupón vencido (EXPIRED2025) → 422 sin orden ni cambio de stock")
    void cuponInvalido() throws Exception {
        int audifonosBefore = stockOf(1);
        int ordersBefore = countOrders();

        JsonNode problem = checkout(
                "{\"items\": [{\"productId\": 1, \"quantity\": 1}], \"couponCode\": \"EXPIRED2025\"}",
                status().isUnprocessableEntity());

        assertEquals("urn:problem-type:invalid-coupon", problem.get("type").asText());
        assertEquals(ordersBefore, countOrders());
        assertEquals(audifonosBefore, stockOf(1));
    }

    @Test
    @DisplayName("Producto inexistente → 404 sin orden")
    void productoInexistente() throws Exception {
        int ordersBefore = countOrders();

        JsonNode problem = checkout("{\"items\": [{\"productId\": 999, \"quantity\": 1}]}", status().isNotFound());

        assertEquals("urn:problem-type:resource-not-found", problem.get("type").asText());
        assertEquals(ordersBefore, countOrders());
    }

    @Test
    @DisplayName("Carrito vacío, cantidad 0 o productos duplicados → 400")
    void carritoInvalido() throws Exception {
        int ordersBefore = countOrders();

        checkout("{\"items\": []}", status().isBadRequest());
        checkout("{\"items\": [{\"productId\": 1, \"quantity\": 0}]}", status().isBadRequest());
        checkout("{\"items\": [{\"productId\": 1, \"quantity\": 1}, {\"productId\": 1, \"quantity\": 2}]}",
                status().isBadRequest());

        assertEquals(ordersBefore, countOrders());
    }

    // ---------- Utilidades ----------

    private JsonNode checkout(String body, ResultMatcher expectedStatus) throws Exception {
        String response = mockMvc.perform(post(ORDERS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(expectedStatus)
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response);
    }

    /** Mismo criterio que OrderNumberGenerator: último número del año + 1, o 000001 si no hay. */
    private String nextOrderNumber() {
        String prefix = "ORD-" + LocalDate.now().getYear() + "-";
        String last = jdbcTemplate.queryForObject(
                "select max(order_number) from orders where order_number like ?", String.class, prefix + "%");
        int next = last == null ? 1 : Integer.parseInt(last.substring(prefix.length())) + 1;
        return prefix + String.format("%06d", next);
    }

    private int countOrders() {
        return jdbcTemplate.queryForObject("select count(*) from orders", Integer.class);
    }

    private int stockOf(int productId) {
        return jdbcTemplate.queryForObject("select stock from products where id = ?", Integer.class, productId);
    }

    private int couponId(String code) {
        return jdbcTemplate.queryForObject("select id from coupons where code = ?", Integer.class, code);
    }

    /** TINYINT(1) puede llegar como Boolean (tinyInt1isBit=true, valor por defecto del driver) o como número. */
    private static boolean flag(Object value) {
        return value instanceof Boolean bool ? bool : ((Number) value).intValue() == 1;
    }

    /** Compara importes de la respuesta por valor (97.71 == 97.710). */
    private static void assertMoney(String expected, JsonNode node, String field) {
        BigDecimal actual = node.get(field).decimalValue();
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                field + ": esperado " + expected + " pero fue " + actual);
    }

    /** Compara columnas DECIMAL de MySQL por valor. */
    private static void assertDbMoney(String expected, Map<String, Object> row, String column) {
        BigDecimal actual = (BigDecimal) row.get(column);
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                column + ": esperado " + expected + " pero fue " + actual);
    }
}
