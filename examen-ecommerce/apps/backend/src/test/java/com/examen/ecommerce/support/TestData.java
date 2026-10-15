package com.examen.ecommerce.support;

import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.test.util.ReflectionTestUtils;

/** Datos de 02_seed_data.sql para pruebas unitarias (sin BD). */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");
    public static final Clock FIXED_CLOCK =
            Clock.fixed(OffsetDateTime.parse("2026-10-15T12:00:00-05:00").toInstant(), ZONE);

    public static final Category TECNOLOGIA = category(1, "Tecnología", "10.00");
    public static final Category HOGAR = category(2, "Hogar", "0.00");
    public static final Category LIBROS = category(3, "Libros", "0.00");
    public static final Category DEPORTES = category(4, "Deportes", "0.00");

    private TestData() {
    }

    public static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    public static Clock clockAt(LocalDateTime dateTime) {
        return Clock.fixed(dateTime.atZone(ZONE).toInstant(), ZONE);
    }

    public static Category category(int id, String name, String pct) {
        Category category = new Category(name, money(pct));
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    public static Product product(int id, String name, String price, Category category, int stock) {
        Product product = new Product(name, money(price), category, stock);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    public static Product product(int id) {
        return switch (id) {
            case 1 -> product(1, "Audífonos Bluetooth", "45.00", TECNOLOGIA, 25);
            case 2 -> product(2, "Mouse inalámbrico", "20.00", TECNOLOGIA, 40);
            case 3 -> product(3, "Teclado mecánico", "80.00", TECNOLOGIA, 15);
            case 4 -> product(4, "Monitor 24 pulgadas", "150.00", TECNOLOGIA, 10);
            case 5 -> product(5, "Cargador USB-C", "12.50", TECNOLOGIA, 0);
            case 6 -> product(6, "Lámpara de escritorio", "30.00", HOGAR, 20);
            case 7 -> product(7, "Juego de sartenes", "55.00", HOGAR, 2);
            case 8 -> product(8, "Novela clásica", "15.00", LIBROS, 50);
            case 9 -> product(9, "Libro de programación", "40.00", LIBROS, 30);
            case 10 -> product(10, "Balón de fútbol", "25.00", DEPORTES, 35);
            case 11 -> product(11, "Esterilla de yoga", "18.00", DEPORTES, 22);
            case 12 -> product(12, "Smartwatch", "120.00", TECNOLOGIA, 8);
            default -> throw new IllegalArgumentException("Producto no existe en el seed: " + id);
        };
    }

    public static Coupon coupon(int id, String code, String pct, LocalDateTime from, LocalDateTime until,
                                boolean active) {
        Coupon coupon = new Coupon(code, money(pct), from, until, active);
        ReflectionTestUtils.setField(coupon, "id", id);
        return coupon;
    }

    public static Coupon welcome2026() {
        return coupon(1, "WELCOME2026", "15.00", LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59), true);
    }

    public static Coupon expired2025() {
        return coupon(2, "EXPIRED2025", "20.00", LocalDateTime.of(2025, 1, 1, 0, 0),
                LocalDateTime.of(2025, 12, 31, 23, 59, 59), true);
    }

    public static Coupon inactive10() {
        return coupon(3, "INACTIVE10", "10.00", LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59), false);
    }

    public static Coupon testCap30() {
        return coupon(4, "TESTCAP30", "30.00", LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59, 59), true);
    }

    public static DiscountLine line(int productId, int quantity) {
        Product product = product(productId);
        return new DiscountLine(product.getId(), product.getName(), product.getCategory().getName(),
                product.getUnitPrice(), quantity, product.getCategory().getDiscountPct());
    }

    public static CouponSnapshot snapshot(Coupon coupon) {
        return new CouponSnapshot(coupon.getId(), coupon.getCode(), coupon.getDiscountPct());
    }
}
