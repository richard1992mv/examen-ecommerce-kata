package com.examen.ecommerce.service.discount;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examen.ecommerce.config.DiscountProperties;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regla 4 – Límite de Descuento Absoluto: el descuento total nunca supera el 35 % del subtotal
 * original. Si el acumulado lo supera, se trunca exactamente en el 35 % y el total se calcula
 * sobre ese límite. Prueba unitaria pura (sin Spring ni BD).
 * <p>
 * Con WELCOME2026 (15 %) el máximo posible es 27.33 %, así que para superar el tope se usan
 * cupones con porcentajes mayores (como TESTCAP30 del seed).
 */
@DisplayName("Regla 4 · Límite de descuento absoluto (tope 35 % del subtotal original)")
class DiscountCapTest {

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
    @DisplayName("Caso (d) del seed: bruto 125.16 > tope 112.00 → ajuste 13.16, total 208.00, 35 %")
    void casoDDelSeedTruncaEnElTope() {
        // 320.00 → cat 27.00 → 293.00 → vol 14.65 → 278.35 → cupón 30 % 83.51 → bruto 125.16 (39.11 %)
        DiscountLine monitor = line(4, "Monitor 24 pulgadas", TECNOLOGIA, "150.00", 1, TECNOLOGIA_PCT);
        DiscountLine smartwatch = line(12, "Smartwatch", TECNOLOGIA, "120.00", 1, TECNOLOGIA_PCT);
        DiscountLine balon = line(10, "Balón de fútbol", "Deportes", "25.00", 2, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(coupon("30.00"), monitor, smartwatch, balon);

        assertEquals(new BigDecimal("320.00"), result.subtotalOriginal());
        assertEquals(new BigDecimal("27.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("14.65"), result.volumeDiscount());
        assertEquals(new BigDecimal("83.51"), result.couponDiscount());
        assertEquals(new BigDecimal("13.16"), result.capAdjustment());
        assertEquals(new BigDecimal("112.00"), result.discountTotal());
        assertEquals(new BigDecimal("208.00"), result.total());
        assertEquals(new BigDecimal("35.00"), result.effectiveDiscountPct());
        assertEquals(new BigDecimal("35.00"), result.discountCapPct());
        assertTrue(result.capApplied());
    }

    @Test
    @DisplayName("El ajuste es exactamente bruto − tope y el total cuadra con el original")
    void ajusteYCuadre() {
        DiscountLine monitor = line(4, "Monitor 24 pulgadas", TECNOLOGIA, "150.00", 1, TECNOLOGIA_PCT);
        DiscountLine smartwatch = line(12, "Smartwatch", TECNOLOGIA, "120.00", 1, TECNOLOGIA_PCT);
        DiscountLine balon = line(10, "Balón de fútbol", "Deportes", "25.00", 2, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(coupon("30.00"), monitor, smartwatch, balon);

        BigDecimal bruto = result.categoryDiscount().add(result.volumeDiscount()).add(result.couponDiscount());
        assertEquals(new BigDecimal("125.16"), bruto);
        assertEquals(bruto.subtract(result.capAdjustment()), result.discountTotal());
        assertEquals(result.subtotalOriginal(), result.total().add(result.discountTotal()));
    }

    @Test
    @DisplayName("Borde: bruto exactamente igual al 35 % → no se ajusta (capApplied = false)")
    void exactamenteElTopeNoAjusta() {
        // 100.00 sin categoría ni volumen (no supera 100) → cupón 35 % = 35.00 = tope
        DiscountLine lampara = line(6, "Lámpara", "Hogar", "100.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(coupon("35.00"), lampara);

        assertEquals(new BigDecimal("35.00"), result.discountTotal());
        assertEquals(new BigDecimal("0.00"), result.capAdjustment());
        assertEquals(new BigDecimal("65.00"), result.total());
        assertFalse(result.capApplied());
    }

    @Test
    @DisplayName("Borde: un centavo por encima del 35 % → se trunca (ajuste 0.01)")
    void unCentavoPorEncimaTrunca() {
        DiscountLine lampara = line(6, "Lámpara", "Hogar", "100.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(coupon("35.01"), lampara);

        assertEquals(new BigDecimal("35.01"), result.couponDiscount());
        assertEquals(new BigDecimal("0.01"), result.capAdjustment());
        assertEquals(new BigDecimal("35.00"), result.discountTotal());
        assertEquals(new BigDecimal("65.00"), result.total());
        assertTrue(result.capApplied());
    }

    @Test
    @DisplayName("Descuento acumulado extremo (cupón 100 %) → nunca pasa del 35 %")
    void descuentoExtremoQuedaEnElTope() {
        // 200.00 → cat 20.00 → 180.00 → vol 9.00 → 171.00 → cupón 171.00 → bruto 200.00 (100 %)
        DiscountLine monitor = line(4, "Monitor", TECNOLOGIA, "200.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculate(coupon("100.00"), monitor);

        assertEquals(new BigDecimal("130.00"), result.capAdjustment());
        assertEquals(new BigDecimal("70.00"), result.discountTotal());
        assertEquals(new BigDecimal("130.00"), result.total());
        assertEquals(new BigDecimal("35.00"), result.effectiveDiscountPct());
        assertTrue(result.capApplied());
    }

    @Test
    @DisplayName("Por debajo del tope (caso (c) con WELCOME2026, 24.84 %) → sin ajuste")
    void porDebajoDelTopeNoAjusta() {
        DiscountLine audifonos = line(1, "Audífonos Bluetooth", TECNOLOGIA, "45.00", 2, TECNOLOGIA_PCT);
        DiscountLine libro = line(9, "Libro de programación", "Libros", "40.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculate(coupon("15.00"), audifonos, libro);

        assertEquals(new BigDecimal("0.00"), result.capAdjustment());
        assertEquals(new BigDecimal("32.29"), result.discountTotal());
        assertEquals(new BigDecimal("24.84"), result.effectiveDiscountPct());
        assertFalse(result.capApplied());
    }

    private DiscountResult calculate(Optional<BigDecimal> couponPct, DiscountLine... lines) {
        return calculator.calculate(List.of(lines), couponPct);
    }

    private static Optional<BigDecimal> coupon(String pct) {
        return Optional.of(new BigDecimal(pct));
    }

    private static DiscountLine line(int id, String name, String category, String unitPrice, int quantity,
                                     String categoryPct) {
        return new DiscountLine(id, name, category, new BigDecimal(unitPrice), quantity, new BigDecimal(categoryPct));
    }
}
