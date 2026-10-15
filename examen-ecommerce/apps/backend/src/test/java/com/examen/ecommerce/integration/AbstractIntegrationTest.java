package com.examen.ecommerce.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.examen.ecommerce.event.listener.CartAuditListener;
import com.examen.ecommerce.event.listener.DiscountMetricsListener;
import com.examen.ecommerce.mapper.OrderMapper;
import com.examen.ecommerce.support.MySqlContainerSupport;
import com.examen.ecommerce.util.OrderNumberGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base de integración: contexto completo + MockMvc + MySQL 5.7.44 (Testcontainers).
 * Antes de CADA prueba se recrea el esquema y el seed (01 + 02) SOLO en el contenedor.
 * No es @Transactional: los commits reales permiten probar @TransactionalEventListener(AFTER_COMMIT).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@RecordApplicationEvents
@Sql(scripts = {MySqlContainerSupport.SCHEMA_SCRIPT, MySqlContainerSupport.SEED_SCRIPT},
        config = @SqlConfig(encoding = "UTF-8", transactionMode = SqlConfig.TransactionMode.ISOLATED),
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
abstract class AbstractIntegrationTest extends MySqlContainerSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    protected ApplicationEvents events;

    @SpyBean
    protected CartAuditListener auditListener;

    @SpyBean
    protected DiscountMetricsListener metricsListener;

    @SpyBean
    protected OrderNumberGenerator orderNumberGenerator;

    @SpyBean
    protected OrderMapper orderMapper;

    protected ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected int stockOf(int productId) {
        Integer stock = jdbc.queryForObject("SELECT stock FROM products WHERE id = ?", Integer.class, productId);
        return stock == null ? -1 : stock;
    }

    protected int orderCount() {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        return count == null ? -1 : count;
    }

    protected static String cart(String couponCode, int... productAndQuantity) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < productAndQuantity.length; i += 2) {
            items.append(i == 0 ? "" : ",")
                    .append("{\"productId\":").append(productAndQuantity[i])
                    .append(",\"quantity\":").append(productAndQuantity[i + 1]).append('}');
        }
        String coupon = couponCode == null ? "null" : "\"" + couponCode + "\"";
        return "{\"items\":[" + items + "],\"couponCode\":" + coupon + "}";
    }
}
