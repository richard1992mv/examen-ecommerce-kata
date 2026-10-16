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
 * Regla 3 – Descuento por Cupón: WELCOME2026 aplica 15 % adicional sobre el total obtenido tras la
 * regla 2 (subtotal_2), en cascada. Prueba unitaria pura (sin Spring ni BD). Que el cupón exista,
 * esté activo y vigente lo prueba {@code CouponValidatorTest}.
 */
@DisplayName("Regla 3 · Descuento por Cupón (WELCOME2026 15 % sobre el subtotal tras la regla 2)")
class CouponDiscountTest {

    private static final String TECNOLOGIA = "Tecnología";
    private static final String TECNOLOGIA_PCT = "10.00";
    private static final String SIN_DESCUENTO_PCT = "0.00";
    private static final Optional<BigDecimal> WELCOME2026 = Optional.of(new BigDecimal("15.00"));

    private DiscountCalculator calculator;

    @BeforeEach
    void setUp() {
        DiscountProperties properties = new DiscountProperties(
                new DiscountProperties.Volume(new BigDecimal("5.00"), new BigDecimal("100.00")),
                new BigDecimal("35.00"));
        calculator = new DiscountCalculator(new DiscountRuleFactory(properties), properties);
    }

    @Test
    @DisplayName("Caso (c) del seed: 130.00 → 121.00 → 114.95 → cupón 17.24, total 97.71")
    void casoCDelSeed() {
        DiscountLine audifonos = line(1, "Audífonos Bluetooth", TECNOLOGIA, "45.00", 2, TECNOLOGIA_PCT);
        DiscountLine libro = line(9, "Libro de programación", "Libros", "40.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculator.calculate(List.of(audifonos, libro), WELCOME2026);

        assertEquals(new BigDecimal("130.00"), result.subtotalOriginal());
        assertEquals(new BigDecimal("9.00"), result.categoryDiscount());
        assertEquals(new BigDecimal("6.05"), result.volumeDiscount());
        assertEquals(new BigDecimal("17.24"), result.couponDiscount());
        assertEquals(new BigDecimal("32.29"), result.discountTotal());
        assertEquals(new BigDecimal("97.71"), result.total());
        assertEquals(new BigDecimal("24.84"), result.effectiveDiscountPct());
    }

    @Test
    @DisplayName("Se calcula sobre el subtotal tras la regla 2, no sobre el original (17.24, no 19.50)")
    void seCalculaSobreSubtotalTrasRegla2() {
        // 130.00 × 15 % = 19.50 sería sumar porcentajes; en cascada es 114.95 × 15 % = 17.2425 → 17.24
        DiscountLine audifonos = line(1, "Audífonos Bluetooth", TECNOLOGIA, "45.00", 2, TECNOLOGIA_PCT);
        DiscountLine libro = line(9, "Libro de programación", "Libros", "40.00", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculator.calculate(List.of(audifonos, libro), WELCOME2026);

        BigDecimal subtotalTrasRegla2 = result.subtotalOriginal()
                .subtract(result.categoryDiscount())
                .subtract(result.volumeDiscount());
        assertEquals(new BigDecimal("114.95"), subtotalTrasRegla2);
        assertEquals(new BigDecimal("17.24"), result.couponDiscount());
    }

    @Test
    @DisplayName("Sin volumen: 50.00 → categoría 2.00 → 48.00 → cupón 7.20, total 40.80")
    void aplicaAunqueNoHayaVolumen() {
        DiscountLine novela = line(8, "Novela", "Libros", "15.00", 2, SIN_DESCUENTO_PCT);
        DiscountLine mouse = line(2, "Mouse inalámbrico", TECNOLOGIA, "20.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculator.calculate(List.of(novela, mouse), WELCOME2026);

        assertEquals(new BigDecimal("0.00"), result.volumeDiscount());
        assertEquals(new BigDecimal("7.20"), result.couponDiscount());
        assertEquals(new BigDecimal("40.80"), result.total());
    }

    @Test
    @DisplayName("Sin cupón el descuento por cupón es 0.00")
    void sinCuponNoHayDescuento() {
        DiscountLine novela = line(8, "Novela", "Libros", "15.00", 2, SIN_DESCUENTO_PCT);

        DiscountResult result = calculator.calculate(List.of(novela), Optional.empty());

        assertEquals(new BigDecimal("0.00"), result.couponDiscount());
        assertEquals(new BigDecimal("30.00"), result.total());
    }

    @Test
    @DisplayName("Redondeo HALF_UP: 10.10 × 15 % = 1.515 → 1.52")
    void redondeaHalfUp() {
        DiscountLine libro = line(9, "Libro", "Libros", "10.10", 1, SIN_DESCUENTO_PCT);

        DiscountResult result = calculator.calculate(List.of(libro), WELCOME2026);

        assertEquals(new BigDecimal("1.52"), result.couponDiscount());
        assertEquals(new BigDecimal("8.58"), result.total());
    }

    @Test
    @DisplayName("Las 3 reglas al máximo dan 27.33 % (< 35 %): el tope no se activa")
    void tresReglasNoAlcanzanElTope() {
        // 200.00 → cat 20.00 → 180.00 → vol 9.00 → 171.00 → cupón 25.65 → dto 54.65 (27.325 % → 27.33)
        DiscountLine monitor = line(4, "Monitor", TECNOLOGIA, "200.00", 1, TECNOLOGIA_PCT);

        DiscountResult result = calculator.calculate(List.of(monitor), WELCOME2026);

        assertEquals(new BigDecimal("25.65"), result.couponDiscount());
        assertEquals(new BigDecimal("54.65"), result.discountTotal());
        assertEquals(new BigDecimal("27.33"), result.effectiveDiscountPct());
        assertEquals(false, result.capApplied());
        assertEquals(new BigDecimal("145.35"), result.total());
    }

    private static DiscountLine line(int id, String name, String category, String unitPrice, int quantity,
                                     String categoryPct) {
        return new DiscountLine(id, name, category, new BigDecimal(unitPrice), quantity, new BigDecimal(categoryPct));
    }
}
