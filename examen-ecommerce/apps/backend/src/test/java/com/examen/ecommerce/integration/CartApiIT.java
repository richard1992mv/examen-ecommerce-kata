package com.examen.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examen.ecommerce.event.CartQuotedEvent;
import com.examen.ecommerce.event.DiscountCapReachedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** HU2 + HU4 de extremo a extremo. */
class CartApiIT extends AbstractIntegrationTest {

    private static final String QUOTE = "/api/v1/cart/quote";

    @Test
    void caseAWithoutCoupon() throws Exception {
        postJson(QUOTE, cart(null, 8, 2, 2, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotalOriginal").value(50.00))
                .andExpect(jsonPath("$.categoryDiscount").value(2.00))
                .andExpect(jsonPath("$.volumeDiscount").value(0.00))
                .andExpect(jsonPath("$.total").value(48.00))
                .andExpect(jsonPath("$.effectiveDiscountPct").value(4.00))
                .andExpect(jsonPath("$.capApplied").value(false))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @Test
    void caseBWithVolume() throws Exception {
        postJson(QUOTE, cart(null, 3, 1, 6, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volumeDiscount").value(5.10))
                .andExpect(jsonPath("$.discountTotal").value(13.10))
                .andExpect(jsonPath("$.total").value(96.90))
                .andExpect(jsonPath("$.effectiveDiscountPct").value(11.91));
    }

    @Test
    void caseCWithWelcomeCoupon() throws Exception {
        postJson(QUOTE, cart("WELCOME2026", 1, 2, 9, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponDiscount").value(17.24))
                .andExpect(jsonPath("$.discountTotal").value(32.29))
                .andExpect(jsonPath("$.total").value(97.71))
                .andExpect(jsonPath("$.effectiveDiscountPct").value(24.84))
                .andExpect(jsonPath("$.items[0].lineTotal").value(81.00));
        assertThat(events.stream(CartQuotedEvent.class)).hasSize(1);
        assertThat(events.stream(DiscountCapReachedEvent.class)).isEmpty();
    }

    @Test
    void caseDReachesTheCapAndShowsHu4Message() throws Exception {
        postJson(QUOTE, cart("TESTCAP30", 4, 1, 12, 1, 10, 2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.couponDiscount").value(83.51))
                .andExpect(jsonPath("$.capAdjustment").value(13.16))
                .andExpect(jsonPath("$.discountTotal").value(112.00))
                .andExpect(jsonPath("$.total").value(208.00))
                .andExpect(jsonPath("$.effectiveDiscountPct").value(35.00))
                .andExpect(jsonPath("$.capApplied").value(true))
                .andExpect(jsonPath("$.message")
                        .value("¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)"))
                .andExpect(jsonPath("$.currency").value("USD"));
        assertThat(events.stream(DiscountCapReachedEvent.class)).singleElement()
                .satisfies(event -> assertThat(event.capAdjustment()).isEqualByComparingTo("13.16"));
    }

    @Test
    void quoteDoesNotTouchStockNorPersist() throws Exception {
        postJson(QUOTE, cart("TESTCAP30", 4, 1, 12, 1, 10, 2)).andExpect(status().isOk());
        assertThat(stockOf(4)).isEqualTo(10);
        assertThat(orderCount()).isEqualTo(4);
    }

    @ParameterizedTest(name = "producto {0} × {1} → {2}")
    @CsvSource({"5, 1, 409", "7, 3, 409", "7, 2, 200", "999, 1, 404"})
    void stockAndExistenceRules(int productId, int quantity, int expectedStatus) throws Exception {
        postJson(QUOTE, cart(null, productId, quantity)).andExpect(status().is(expectedStatus));
    }

    @ParameterizedTest(name = "cupón {0} → 422")
    @CsvSource({"EXPIRED2025", "INACTIVE10", "NOEXISTE"})
    void invalidCouponsAre422(String code) throws Exception {
        postJson(QUOTE, cart(code, 1, 1))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.couponCode").value(code));
    }

    @Test
    void failingListenerDoesNotAffectTheResponse() throws Exception {
        doThrow(new IllegalStateException("listener roto")).when(auditListener).onCartQuoted(any());

        postJson(QUOTE, cart(null, 8, 2, 2, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(48.00));
    }

    @Test
    void metricsAreExposedThroughActuator() throws Exception {
        postJson(QUOTE, cart(null, 8, 1)).andExpect(status().isOk());
        mvc.perform(get("/actuator/metrics/cart.quotes")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
