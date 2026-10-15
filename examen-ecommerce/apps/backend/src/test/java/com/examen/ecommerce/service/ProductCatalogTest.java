package com.examen.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.examen.ecommerce.dto.response.ProductResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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
 * HU1 (lado backend) con la base de datos real del perfil dev (kata_ecommerce ya cargada con 01 y 02).
 * <ul>
 *   <li>GET /products devuelve los 12 productos pre-configurados con id, nombre, precio unitario,
 *       categoría y stock, tal como están en MySQL.</li>
 *   <li>POST /cart/quote recalcula el subtotal original al agregar, cambiar o remover productos,
 *       sin modificar el stock.</li>
 * </ul>
 * Solo lectura: no inserta, actualiza ni borra datos. Requiere MySQL levantado en localhost:3306.
 */
@SpringBootTest(properties = "app.time-zone=America/Bogota")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@DisplayName("HU1 · Catálogo de productos y subtotal original (con base de datos)")
class ProductCatalogTest {

    private static final String PRODUCTS_URL = "/api/v1/products";
    private static final String QUOTE_URL = "/api/v1/cart/quote";
    private static final String PRODUCTS_SQL = """
            select p.id, p.name, p.unit_price, p.stock, c.id as category_id, c.name as category_name
            from products p join categories c on c.id = p.category_id
            order by p.id""";

    /** Datos fijos de 02_seed_data.sql: id, nombre, precio unitario, id de categoría, categoría. */
    private static final Object[][] SEED_PRODUCTS = {
        {1, "Audífonos Bluetooth", "45.00", 1, "Tecnología"},
        {2, "Mouse inalámbrico", "20.00", 1, "Tecnología"},
        {3, "Teclado mecánico", "80.00", 1, "Tecnología"},
        {4, "Monitor 24 pulgadas", "150.00", 1, "Tecnología"},
        {5, "Cargador USB-C", "12.50", 1, "Tecnología"},
        {6, "Lámpara de escritorio", "30.00", 2, "Hogar"},
        {7, "Juego de sartenes", "55.00", 2, "Hogar"},
        {8, "Novela clásica", "15.00", 3, "Libros"},
        {9, "Libro de programación", "40.00", 3, "Libros"},
        {10, "Balón de fútbol", "25.00", 4, "Deportes"},
        {11, "Esterilla de yoga", "18.00", 4, "Deportes"},
        {12, "Smartwatch", "120.00", 1, "Tecnología"},
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---------- Catálogo (GET /products) ----------

    @Test
    @DisplayName("GET /products devuelve los 12 productos del seed con id, nombre, precio y categoría")
    void listaLosProductosPreConfigurados() throws Exception {
        List<ProductResponse> products = getProducts();

        assertEquals(SEED_PRODUCTS.length, products.size());
        for (int i = 0; i < SEED_PRODUCTS.length; i++) {
            Object[] expected = SEED_PRODUCTS[i];
            ProductResponse actual = products.get(i);
            String context = "producto " + expected[0];
            assertEquals(expected[0], actual.id(), context);
            assertEquals(expected[1], actual.name(), context);
            assertMoney((String) expected[2], actual.unitPrice(), context);
            assertEquals(expected[3], actual.categoryId(), context);
            assertEquals(expected[4], actual.categoryName(), context);
        }
    }

    @Test
    @DisplayName("Cada producto de la API coincide con su fila en MySQL (incluido el stock actual)")
    void coincideConLaBaseDeDatos() throws Exception {
        List<ProductResponse> products = getProducts();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(PRODUCTS_SQL);

        assertEquals(rows.size(), products.size());
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            ProductResponse actual = products.get(i);
            String context = "producto " + row.get("id");
            assertEquals(((Number) row.get("id")).intValue(), actual.id(), context);
            assertEquals(row.get("name"), actual.name(), context);
            assertMoney(((BigDecimal) row.get("unit_price")).toPlainString(), actual.unitPrice(), context);
            assertEquals(((Number) row.get("stock")).intValue(), actual.stock(), context);
            assertEquals(((Number) row.get("category_id")).intValue(), actual.categoryId(), context);
            assertEquals(row.get("category_name"), actual.categoryName(), context);
        }
    }

    @Test
    @DisplayName("Los productos vienen ordenados por id y con stock ≥ 0 (el 5 se lista con stock 0)")
    void ordenadosYConStockValido() throws Exception {
        List<ProductResponse> products = getProducts();

        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12),
                products.stream().map(ProductResponse::id).toList());
        assertTrue(products.stream().allMatch(product -> product.stock() >= 0));
        assertEquals(0, products.get(4).stock(), "Cargador USB-C (id 5) no tiene stock en el seed");
    }

    // ---------- Subtotal original del carrito (POST /cart/quote) ----------

    @Test
    @DisplayName("Agregar productos suma precio × cantidad: 45.00 → 85.00")
    void agregarProductoActualizaSubtotal() throws Exception {
        assertMoney("45.00", subtotalOriginal("[{\"productId\": 1, \"quantity\": 1}]"), "solo audífonos");
        assertMoney("85.00", subtotalOriginal(
                "[{\"productId\": 1, \"quantity\": 1}, {\"productId\": 9, \"quantity\": 1}]"), "+ libro");
    }

    @Test
    @DisplayName("Cambiar la cantidad actualiza el subtotal: audífonos ×2 + libro = 130.00")
    void cambiarCantidadActualizaSubtotal() throws Exception {
        assertMoney("130.00", subtotalOriginal(
                "[{\"productId\": 1, \"quantity\": 2}, {\"productId\": 9, \"quantity\": 1}]"), "audífonos x2");
    }

    @Test
    @DisplayName("Remover un producto resta su importe: 130.00 → 90.00")
    void removerProductoActualizaSubtotal() throws Exception {
        assertMoney("90.00", subtotalOriginal("[{\"productId\": 1, \"quantity\": 2}]"), "sin el libro");
    }

    @Test
    @DisplayName("El subtotal original es sin descuentos (Tecnología): 90.00 original, 81.00 total")
    void subtotalOriginalSinDescuentos() throws Exception {
        String body = quote("[{\"productId\": 1, \"quantity\": 2}]");

        assertMoney("90.00", objectMapper.readTree(body).get("subtotalOriginal").decimalValue(), "original");
        assertMoney("81.00", objectMapper.readTree(body).get("total").decimalValue(), "total");
    }

    @Test
    @DisplayName("Cotizar el carrito no modifica el stock en la base de datos")
    void cotizarNoModificaStock() throws Exception {
        Integer before = stockOf(1);

        quote("[{\"productId\": 1, \"quantity\": 2}]");

        assertEquals(before, stockOf(1));
    }

    // ---------- Utilidades ----------

    private List<ProductResponse> getProducts() throws Exception {
        String body = mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readValue(body, new TypeReference<List<ProductResponse>>() { });
    }

    private BigDecimal subtotalOriginal(String itemsJson) throws Exception {
        return objectMapper.readTree(quote(itemsJson)).get("subtotalOriginal").decimalValue();
    }

    private String quote(String itemsJson) throws Exception {
        return mockMvc.perform(post(QUOTE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\": " + itemsJson + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private Integer stockOf(int productId) {
        return jdbcTemplate.queryForObject("select stock from products where id = ?", Integer.class, productId);
    }

    /** Compara importes por valor (80.0 == 80.00), sin depender de la escala del JSON. */
    private static void assertMoney(String expected, BigDecimal actual, String context) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                context + ": esperado " + expected + " pero fue " + actual);
    }
}
