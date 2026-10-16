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
 * Regla 2 – Descuento por Volumen: si el subtotal DESPUÉS del descuento de categoría supera
 * 100.00 USD, se aplica 5 % adicional sobre todo el carrito. Prueba unitaria pura (sin Spring ni BD),
 * sin cupón para aislar la regla 2 de la regla 3.
 */
@DisplayName("Regla 2 · Descuento por Volumen (5 % si subtotal tras categoría > 100.00)")
class VolumeDiscountTest {

    private static final String TECNOLOGIA = "Tecnología";
    private static final String TECNOLOGIA_PCT = "10.00";
    private static final String SIN_DESCUENTO_PCT = "0.00";

    private DiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        DiscountProperties properties = new DiscountProperties(
                new DiscountProperties.Volume(new BigDecimal("5.00"), new BigDecimal("100.00")),
                new BigDecimal("35.00"));
        calculator = new DiscountCalculator(new DiscountRuleFactory(properties), properties);
    }

    @Test
    @DisplayName("Caso (b) del seed: 110.00 → categoría 8.00 → 102.00 → volumen 5.10, total 96.90")
    void aplicaCincoPorCientoSobreSubtotalTrasCategoria() {
        DiscountLine teclado = line(3, "Teclado mecánico", TECNOLOGIA, "80.00", 1, TECNOLOGIA_PCT);
        DiscountLine lampara = line(6, "Lámpara de escritorio", "Hogar", "30.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(teclado, lampara);

        assertEquals(new BigDecimal("110.00"), result.subtotalOriginal());
        assertEquals(new BigDecimal("8.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("5.10"), result.volumeDiscount());
        assertEquals(new BigDecimal("13.10"), result.discountTotal());
        assertEquals(new BigDecimal("96.90"), result.total());
        assertEquals(new BigDecimal("11.91"), result.effectiveDiscountPct());
    }

    @Test
    @DisplayName("Se evalúa el subtotal tras categoría, no el original: 110.00 → 99.00 no aplica")
    void evaluaElSubtotalDespuesDeCategoria() {
        // Original 110.00 > 100, pero tras el 10 % de Tecnología queda en 99.00 → sin volumen
        DiscountLine monitor = line(4, "Monitor", TECNOLOGIA, "110.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculate(monitor);

        assertEquals(new BigDecimal("11.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("0.00"), result.volumeDiscount());
        assertEquals(new BigDecimal("99.00"), result.total());
    }

    @Test
    @DisplayName("Borde: subtotal tras categoría exactamente 100.00 no aplica (debe superar)")
    void exactamenteCienNoAplica() {
        DiscountLine libros = line(9, "Libro de programación", "Libros", "50.00", 2, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(libros);

        assertEquals(new BigDecimal("0.00"), result.volumeDiscount());
        assertEquals(new BigDecimal("100.00"), result.total());
    }

    @Test
    @DisplayName("Borde: 100.01 sí aplica → 100.01 × 5 % = 5.0005 → 5.00")
    void cienPuntoCeroUnoSiAplica() {
        DiscountLine libro = line(9, "Libro de programación", "Libros", "100.01", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(libro);

        assertEquals(new BigDecimal("5.00"), result.volumeDiscount());
        assertEquals(new BigDecimal("95.01"), result.total());
    }

    @Test
    @DisplayName("Aplica sobre todo el carrito, aunque no haya productos de Tecnología")
    void aplicaSobreTodoElCarrito() {
        DiscountLine balon = line(10, "Balón de fútbol", "Deportes", "25.00", 2, SIN_DESCUENTO_PCT);
        DiscountLine lampara = line(6, "Lámpara de escritorio", "Hogar", "70.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(balon, lampara);

        assertEquals(new BigDecimal("0.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("6.00"), result.volumeDiscount());
        assertEquals(new BigDecimal("114.00"), result.total());
    }

    @Test
    @DisplayName("Redondeo HALF_UP: 102.50 × 5 % = 5.125 → 5.13")
    void redondeaHalfUp() {
        DiscountLine lampara = line(6, "Lámpara de escritorio", "Hogar", "102.50", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(lampara);

        assertEquals(new BigDecimal("5.13"), result.volumeDiscount());
        assertEquals(new BigDecimal("97.37"), result.total());
    }

    @Test
    @DisplayName("Subtotal menor a 100.00 no aplica (caso (a) del seed: 48.00)")
    void menorACienNoAplica() {
        DiscountLine novela = line(8, "Novela", "Libros", "15.00", 2, SIN_DESCUENTO_PCT);
        DiscountLine mouse = line(2, "Mouse inalámbrico", TECNOLOGIA, "20.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculate(novela, mouse);

        assertEquals(new BigDecimal("0.00"), result.volumeDiscount());
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
