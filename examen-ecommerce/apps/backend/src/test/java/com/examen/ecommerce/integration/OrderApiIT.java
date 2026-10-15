package com.examen.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examen.ecommerce.event.OrderConfirmedEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** HU3: checkout transaccional de extremo a extremo con verificación en BD. */
class OrderApiIT extends AbstractIntegrationTest {

    private static final String ORDERS = "/api/v1/orders";

    @Test
    void checkoutCaseCPersistsOrderItemsAndDecrementsStock() throws Exception {
        postJson(ORDERS, cart("WELCOME2026", 1, 2, 9, 1))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/orders/5")))
                .andExpect(jsonPath("$.orderNumber").value("ORD-2026-000005"))
                .andExpect(jsonPath("$.createdAt").value("2026-10-15T12:00:00"))
                .andExpect(jsonPath("$.total").value(97.71));

        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM orders WHERE order_number = 'ORD-2026-000005'");
        assertThat((BigDecimal) order.get("subtotal_original")).isEqualByComparingTo("130.00");
        assertThat((BigDecimal) order.get("category_discount_amount")).isEqualByComparingTo("9.00");
        assertThat((BigDecimal) order.get("volume_discount_amount")).isEqualByComparingTo("6.05");
        assertThat((BigDecimal) order.get("coupon_discount_amount")).isEqualByComparingTo("17.24");
        assertThat((BigDecimal) order.get("discount_total")).isEqualByComparingTo("32.29");
        assertThat((BigDecimal) order.get("effective_discount_pct")).isEqualByComparingTo("24.84");
        assertThat((BigDecimal) order.get("total")).isEqualByComparingTo("97.71");
        assertThat(order.get("coupon_code")).isEqualTo("WELCOME2026");
        assertThat(((Number) order.get("coupon_id")).intValue()).isEqualTo(1);
        assertThat(((Number) order.get("cap_applied")).intValue()).isZero();

        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT * FROM order_items WHERE order_id = ? ORDER BY id", order.get("id"));
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("product_name")).isEqualTo("Audífonos Bluetooth");
        assertThat(items.get(0).get("category_name")).isEqualTo("Tecnología");
        assertThat((BigDecimal) items.get(0).get("category_discount_amount")).isEqualByComparingTo("9.00");
        assertThat((BigDecimal) items.get(0).get("line_total")).isEqualByComparingTo("81.00");
        assertThat(stockOf(1)).isEqualTo(23);
        assertThat(stockOf(9)).isEqualTo(29);

        assertThat(events.stream(OrderConfirmedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.orderNumber()).isEqualTo("ORD-2026-000005"));
        verify(metricsListener).onOrderConfirmed(any());
    }

    @Test
    void checkoutCaseDAppliesTheCap() throws Exception {
        postJson(ORDERS, cart("TESTCAP30", 4, 1, 12, 1, 10, 2))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.capApplied").value(true))
                .andExpect(jsonPath("$.capAdjustment").value(13.16))
                .andExpect(jsonPath("$.discountCapPct").value(35.00))
                .andExpect(jsonPath("$.message")
                        .value("¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)"))
                .andExpect(jsonPath("$.items.length()").value(3));

        Map<String, Object> order = jdbc.queryForMap("SELECT * FROM orders WHERE order_number = 'ORD-2026-000005'");
        assertThat((BigDecimal) order.get("cap_adjustment_amount")).isEqualByComparingTo("13.16");
        assertThat((BigDecimal) order.get("discount_total")).isEqualByComparingTo("112.00");
        assertThat((BigDecimal) order.get("discount_cap_pct")).isEqualByComparingTo("35.00");
        assertThat((BigDecimal) order.get("total")).isEqualByComparingTo("208.00");
        assertThat(((Number) order.get("cap_applied")).intValue()).isEqualTo(1);
        assertThat(stockOf(4)).isEqualTo(9);
        assertThat(stockOf(12)).isEqualTo(7);
        assertThat(stockOf(10)).isEqualTo(33);
    }

    @Test
    void insufficientStockRollsBackAndIsNotNotified() throws Exception {
        postJson(ORDERS, cart(null, 1, 1, 7, 3))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.productId").value(7))
                .andExpect(jsonPath("$.available").value(2));

        assertThat(orderCount()).isEqualTo(4);
        assertThat(stockOf(1)).isEqualTo(25);
        assertThat(events.stream(OrderConfirmedEvent.class)).isEmpty();
        verify(metricsListener, never()).onOrderConfirmed(any());
    }

    @Test
    void failureAfterPublishingRollsBackEverythingAndAfterCommitIsNotNotified() throws Exception {
        doThrow(new IllegalStateException("fallo tardío")).when(orderMapper).toResponse(any());

        postJson(ORDERS, cart("WELCOME2026", 1, 2, 9, 1)).andExpect(status().isInternalServerError());

        assertThat(orderCount()).isEqualTo(4);
        assertThat(stockOf(1)).isEqualTo(25);
        assertThat(stockOf(9)).isEqualTo(30);
        assertThat(events.stream(OrderConfirmedEvent.class)).hasSize(1);
        verify(metricsListener, never()).onOrderConfirmed(any());
        verify(auditListener, never()).onOrderConfirmed(any());
    }

    @Test
    void duplicatedOrderNumberIsRetriedBoundedAndRolledBack() throws Exception {
        doReturn("ORD-2026-000001").when(orderNumberGenerator).next(anyInt(), any());

        postJson(ORDERS, cart(null, 2, 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:problem-type:order-number-conflict"));

        verify(orderNumberGenerator, times(3)).next(anyInt(), any());
        assertThat(orderCount()).isEqualTo(4);
        assertThat(stockOf(2)).isEqualTo(40);
    }

    @Test
    void failingAfterCommitListenerDoesNotBreakTheCheckout() throws Exception {
        doThrow(new IllegalStateException("auditoría caída")).when(auditListener).onOrderConfirmed(any());

        postJson(ORDERS, cart(null, 2, 1)).andExpect(status().isCreated());

        assertThat(orderCount()).isEqualTo(5);
    }

    @Test
    void invalidCheckoutRequestsAreRejected() throws Exception {
        postJson(ORDERS, "{\"items\":[]}").andExpect(status().isBadRequest());
        postJson(ORDERS, cart("EXPIRED2025", 1, 1)).andExpect(status().isUnprocessableEntity());
        postJson(ORDERS, cart(null, 999, 1)).andExpect(status().isNotFound());
        postJson(ORDERS, cart(null, 5, 1)).andExpect(status().isConflict());
        assertThat(orderCount()).isEqualTo(4);
    }

    @Test
    void queriesSeedOrders() throws Exception {
        mvc.perform(get(ORDERS)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-2026-000004"));
        mvc.perform(get(ORDERS).param("couponCode", "WELCOME2026"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get(ORDERS).param("from", "2026-10-02T00:00:00").param("to", "2026-10-06T00:00:00"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mvc.perform(get(ORDERS + "/4")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(get(ORDERS + "/number/ORD-2026-000003")).andExpect(status().isOk())
                .andExpect(jsonPath("$.couponCode").value("WELCOME2026"))
                .andExpect(jsonPath("$.items[0].productName").value("Audífonos Bluetooth"));
        mvc.perform(get(ORDERS + "/999")).andExpect(status().isNotFound());
        mvc.perform(get(ORDERS + "/number/ORD-2099-000001")).andExpect(status().isNotFound());
    }
}
