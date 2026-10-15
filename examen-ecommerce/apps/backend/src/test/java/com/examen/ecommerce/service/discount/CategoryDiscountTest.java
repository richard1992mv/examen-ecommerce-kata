package com.examen.ecommerce.service.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.examen.ecommerce.config.DiscountProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regla 1 – Descuento de Categoría: 10 % solo sobre los productos de "Tecnología".
 * Prueba unitaria pura (sin Spring ni BD). Los carritos suman ≤ 100.00 y no llevan cupón
 * para aislar la regla 1 de las reglas 2 y 3.
 */
@DisplayName("Regla 1 · Descuento de Categoría (Tecnología 10 %)")
class CategoryDiscountTest {

    private static final String TECNOLOGIA = "Tecnología";
    private static final String TECNOLOGIA_PCT = "10.00";
    private static final String SIN_DESCUENTO_PCT = "0.00";

    private DiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        DiscountProperties properties = new DiscountProperties(
                new DiscountProperties.Volume(new BigDecimal("5.00"), new BigDecimal("100.00")),
                new BigDecimal("35.00"));
        calculator = new DiscountCalculator(properties);
    }

    @Test
    @DisplayName("Un producto de Tecnología recibe 10 % sobre su subtotal de línea")
    void aplicaDiezPorCientoAProductoDeTecnologia() {
        DiscountLine audifonos = line(1, "Audífonos Bluetooth", TECNOLOGIA, "45.00", 2, TECNOLOGIA_PCT);

        DiscountResult result = calculate(audifonos);

        assertEquals(new BigDecimal("90.00"), result.subtotalOriginal());
        assertEquals(new BigDecimal("9.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("9.00"), result.lines().get(0).categoryDiscount());
        assertEquals(new BigDecimal("81.00"), result.lines().get(0).lineTotal());
    }

    @Test
    @DisplayName("En un carrito mixto solo se descuentan las líneas de Tecnología")
    void soloDescuentaLasLineasDeTecnologia() {
        DiscountLine audifonos = line(1, "Audífonos Bluetooth", TECNOLOGIA, "45.00", 1, TECNOLOGIA_PCT);
        DiscountLine libro = line(9, "Libro de programación", "Libros", "40.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(audifonos, libro);

        assertEquals(new BigDecimal("4.50"), result.lines().get(0).categoryDiscount());
        assertEquals(new BigDecimal("0.00"), result.lines().get(1).categoryDiscount());
        assertEquals(new BigDecimal("40.00"), result.lines().get(1).lineTotal());
        assertEquals(new BigDecimal("4.50"), result.categoryDiscount());
    }

    @Test
    @DisplayName("Sin productos de Tecnología el descuento de categoría es 0.00")
    void sinTecnologiaNoHayDescuentoDeCategoria() {
        DiscountLine novela = line(8, "Novela", "Libros", "15.00", 2, SIN_DESCUENTO_PCT);
        DiscountLine balon = line(10, "Balón de fútbol", "Deportes", "25.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(novela, balon);

        assertEquals(new BigDecimal("0.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("55.00"), result.total());
    }

    @Test
    @DisplayName("El descuento se redondea HALF_UP por línea (no sobre la suma)")
    void redondeaPorLinea() {
        // 20.05 × 10 % = 2.005 → 2.01 por línea; 2 líneas = 4.02 (redondear sobre 40.10 daría 4.01)
        DiscountLine mouse = line(2, "Mouse", TECNOLOGIA, "20.05", 1, TECNOLOGIA_PCT);
        DiscountLine teclado = line(3, "Teclado", TECNOLOGIA, "20.05", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculate(mouse, teclado);

        assertEquals(new BigDecimal("2.01"), result.lines().get(0).categoryDiscount());
        assertEquals(new BigDecimal("2.01"), result.lines().get(1).categoryDiscount());
        assertEquals(new BigDecimal("4.02"), result.categoryDiscount());
    }

    @Test
    @DisplayName("Caso (a) del seed: Novela×2 + Mouse×1 → categoría 2.00, total 48.00")
    void casoADelSeed() {
        DiscountLine novela = line(8, "Novela", "Libros", "15.00", 2, SIN_DESCUENTO_PCT);
        DiscountLine mouse = line(2, "Mouse inalámbrico", TECNOLOGIA, "20.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculate(novela, mouse);

        assertEquals(new BigDecimal("50.00"), result.subtotalOriginal());
        assertEquals(new BigDecimal("2.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("48.00"), result.total());
    }

    private DiscountResult calculate(DiscountLine... lines) {
        return calculator.calculate(List.of(lines), Optional.empty());
    }

    private static DiscountLine line(int id, String name, String category, String unitPrice, int quantity,
                                     String categoryPct) {
        return new DiscountLine(id, name, category, new BigDecimal(unitPrice), quantity, new BigDecimal(categoryPct));
    }
}
