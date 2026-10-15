package com.examen.ecommerce.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.service.CartService;
import com.examen.ecommerce.service.CategoryService;
import com.examen.ecommerce.service.CouponService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Slice web sin BD: validación Bean Validation y traducción a Problem Details. */
@WebMvcTest(controllers = {CartController.class, CategoryController.class, CouponController.class})
@ActiveProfiles("test")
class ApiErrorHandlingWebTest {

    private static final MediaType PROBLEM = MediaType.APPLICATION_PROBLEM_JSON;

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CartService cartService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private CouponService couponService;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "{\"items\": []}",
        "{\"items\": [{\"productId\": 1, \"quantity\": 0}]}",
        "{\"items\": [{\"productId\": 1, \"quantity\": -1}]}",
        "{\"items\": [{\"productId\": 1, \"quantity\": 1}, {\"productId\": 1, \"quantity\": 2}]}",
        "{\"items\": [{\"quantity\": 1}]}"})
    void invalidCartIs400WithFieldList(String body) throws Exception {
        mvc.perform(post("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM))
                .andExpect(jsonPath("$.detail").value(ErrorMessages.VALIDATION_FAILED))
                .andExpect(jsonPath("$.errors").isArray());
        verifyNoInteractions(cartService);
    }

    @Test
    void malformedJsonIs400() throws Exception {
        mvc.perform(post("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON).content("{items:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("urn:problem-type:malformed-request"));
    }

    @Test
    void insufficientStockIs409WithDetails() throws Exception {
        when(cartService.quote(any())).thenThrow(new InsufficientStockException(7, 3, 2));

        mvc.perform(post("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 7, \"quantity\": 3}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.productId").value(7))
                .andExpect(jsonPath("$.requested").value(3))
                .andExpect(jsonPath("$.available").value(2));
    }

    @Test
    void invalidCouponIs422() throws Exception {
        when(cartService.quote(any())).thenThrow(new InvalidCouponException(ErrorMessages.COUPON_INACTIVE, "INACTIVE10"));

        mvc.perform(post("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 1}], \"couponCode\": \"INACTIVE10\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.couponCode").value("INACTIVE10"));
    }

    @Test
    void unexpectedErrorIs500WithoutInternals() throws Exception {
        when(cartService.quote(any())).thenThrow(new IllegalStateException("SELECT * FROM secret"));

        mvc.perform(post("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": [{\"productId\": 1, \"quantity\": 1}]}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value(ErrorMessages.INTERNAL_ERROR))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void integrityViolationIs409WithoutSql() throws Exception {
        when(categoryService.update(any(), any())).thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        mvc.perform(put("/api/v1/categories/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hogar\", \"discountPct\": 0}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(ErrorMessages.INTEGRITY_VIOLATION));
    }

    @Test
    void categoryPercentageOutOfRangeIs400() throws Exception {
        mvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"X\", \"discountPct\": 150}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("discountPct"));
    }

    @Test
    void couponWithInvertedDatesIs400() throws Exception {
        mvc.perform(post("/api/v1/coupons").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\": \"X\", \"discountPct\": 10, \"validFrom\": \"2026-12-31T00:00:00\","
                                + " \"validUntil\": \"2026-01-01T00:00:00\", \"active\": true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message").value(ErrorMessages.DATE_RANGE));
    }

    @Test
    void typeMismatchIs400AndUnknownEndpointIs404() throws Exception {
        mvc.perform(get("/api/v1/categories/abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/no-existe")).andExpect(status().isNotFound());
    }

    @Test
    void unsupportedMethodIs405() throws Exception {
        mvc.perform(put("/api/v1/cart/quote").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed());
    }
}
