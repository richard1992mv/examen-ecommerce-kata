package com.examen.ecommerce.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Categorías, productos (HU1) y cupones: casos felices y de error. */
class CatalogApiIT extends AbstractIntegrationTest {

    private static final String CATEGORIES = "/api/v1/categories";
    private static final String PRODUCTS = "/api/v1/products";
    private static final String COUPONS = "/api/v1/coupons";

    @Test
    void categoriesCrud() throws Exception {
        mvc.perform(get(CATEGORIES)).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get(CATEGORIES + "/1")).andExpect(jsonPath("$.name").value("Tecnología"));
        mvc.perform(get(CATEGORIES + "/99")).andExpect(status().isNotFound());

        String body = postJson(CATEGORIES, "{\"name\":\"Oficina\",\"discountPct\":5}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString(CATEGORIES + "/")))
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mvc.perform(put(CATEGORIES + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Oficina y papelería\",\"discountPct\":7.5}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.discountPct").value(7.5));
        postJson(CATEGORIES, "{\"name\":\"hogar\",\"discountPct\":0}").andExpect(status().isConflict());
        mvc.perform(delete(CATEGORIES + "/1")).andExpect(status().isConflict());
        mvc.perform(delete(CATEGORIES + "/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void productsCrudAndFilters() throws Exception {
        mvc.perform(get(PRODUCTS).param("categoryId", "1")).andExpect(jsonPath("$.totalElements").value(6));
        mvc.perform(get(PRODUCTS).param("name", "libro"))
                .andExpect(jsonPath("$.content[0].name").value("Libro de programación"));
        mvc.perform(get(PRODUCTS).param("size", "5").param("page", "1").param("sort", "unitPrice,desc"))
                .andExpect(jsonPath("$.content.length()").value(5));
        mvc.perform(get(PRODUCTS + "/5")).andExpect(jsonPath("$.stock").value(0));
        mvc.perform(get(PRODUCTS + "/999")).andExpect(status().isNotFound());

        postJson(PRODUCTS, "{\"name\":\"Gratis\",\"unitPrice\":0,\"categoryId\":1,\"stock\":1}")
                .andExpect(status().isBadRequest());
        postJson(PRODUCTS, "{\"name\":\"Negativo\",\"unitPrice\":10,\"categoryId\":1,\"stock\":-1}")
                .andExpect(status().isBadRequest());
        postJson(PRODUCTS, "{\"name\":\"Huérfano\",\"unitPrice\":10,\"categoryId\":99,\"stock\":1}")
                .andExpect(status().isNotFound());

        String body = postJson(PRODUCTS, "{\"name\":\"Webcam HD\",\"unitPrice\":35.00,\"categoryId\":1,\"stock\":5}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        mvc.perform(put(PRODUCTS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Webcam 4K\",\"unitPrice\":60.00,\"categoryId\":1,\"stock\":3}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unitPrice").value(60.00));

        mvc.perform(delete(PRODUCTS + "/2")).andExpect(status().isConflict());
        mvc.perform(delete(PRODUCTS + "/" + id)).andExpect(status().isNoContent());
    }

    @Test
    void couponsCrud() throws Exception {
        mvc.perform(get(COUPONS)).andExpect(jsonPath("$.totalElements").value(4));
        mvc.perform(get(COUPONS + "/WELCOME2026")).andExpect(jsonPath("$.discountPct").value(15.00))
                .andExpect(jsonPath("$.validUntil").value("2026-12-31T23:59:59"));
        mvc.perform(get(COUPONS + "/NOEXISTE")).andExpect(status().isNotFound());

        String coupon = "{\"code\":\"BLACKFRIDAY\",\"discountPct\":20,\"validFrom\":\"2026-11-27T00:00:00\","
                + "\"validUntil\":\"2026-11-30T23:59:59\",\"active\":true}";
        String body = postJson(COUPONS, coupon).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");
        postJson(COUPONS, coupon).andExpect(status().isConflict());
        postJson(COUPONS, coupon.replace("\"discountPct\":20", "\"discountPct\":120")).andExpect(status().isBadRequest());

        mvc.perform(put(COUPONS + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(coupon.replace("true", "false")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete(COUPONS + "/1")).andExpect(status().isConflict());
        mvc.perform(delete(COUPONS + "/2")).andExpect(status().isNoContent());
        Integer remaining = jdbc.queryForObject("SELECT COUNT(*) FROM coupons", Integer.class);
        assertThat(remaining).isEqualTo(4);
    }

    @Test
    void openApiIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/cart/quote']").exists());
    }
}
