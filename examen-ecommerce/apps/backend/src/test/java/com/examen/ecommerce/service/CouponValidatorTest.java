package com.examen.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.repository.CouponRepository;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Regla 3 – el cupón solo aplica si el código existe, está activo y vigente (si no, 422).
 * Prueba unitaria solo con JUnit 5 (sin Mockito, sin Spring ni BD):
 * <ul>
 *   <li>El repositorio es un doble en memoria creado con {@link Proxy} del JDK.</li>
 *   <li>Los cupones son subclases anónimas de {@link Coupon} que fijan activo/vigente.</li>
 * </ul>
 */
@DisplayName("Regla 3 · Validación del cupón WELCOME2026")
class CouponValidatorTest {

    private static final String WELCOME2026 = "WELCOME2026";

    private final Map<String, Coupon> couponsByCode = new HashMap<>();
    private int repositoryCalls;
    private CouponValidator validator;

    @BeforeEach
    void setUp() {
        couponsByCode.clear();
        repositoryCalls = 0;
        validator = new CouponValidator(inMemoryRepository());
    }

    @Test
    @DisplayName("WELCOME2026 activo y vigente → se acepta con su 15 %")
    void aceptaCuponActivoYVigente() {
        save(coupon(WELCOME2026, "15.00", true, true));

        Optional<Coupon> result = validator.validate(WELCOME2026);

        assertTrue(result.isPresent());
        assertEquals(WELCOME2026, result.get().getCode());
        assertEquals(new BigDecimal("15.00"), result.get().getDiscountPct());
    }

    @Test
    @DisplayName("El código se normaliza quitando espacios")
    void normalizaEspacios() {
        save(coupon(WELCOME2026, "15.00", true, true));

        assertTrue(validator.validate("  WELCOME2026 ").isPresent());
    }

    @Test
    @DisplayName("Sin código (null o vacío) no hay cupón y no se consulta la BD")
    void sinCodigoNoHayCupon() {
        assertTrue(validator.validate(null).isEmpty());
        assertTrue(validator.validate("   ").isEmpty());
        assertEquals(0, repositoryCalls);
    }

    @Test
    @DisplayName("Código inexistente → InvalidCouponException (422)")
    void codigoInexistente() {
        InvalidCouponException ex = assertThrows(InvalidCouponException.class, () -> validator.validate("NOEXISTE"));

        assertEquals("El cupón 'NOEXISTE' no existe", ex.getMessage());
        assertEquals("NOEXISTE", ex.getCouponCode());
    }

    @Test
    @DisplayName("Cupón inactivo → InvalidCouponException (422)")
    void cuponInactivo() {
        save(coupon(WELCOME2026, "15.00", false, true));

        InvalidCouponException ex = assertThrows(InvalidCouponException.class, () -> validator.validate(WELCOME2026));

        assertEquals("El cupón 'WELCOME2026' está inactivo", ex.getMessage());
    }

    @Test
    @DisplayName("Cupón fuera de vigencia → InvalidCouponException (422)")
    void cuponFueraDeVigencia() {
        save(coupon(WELCOME2026, "15.00", true, false));

        InvalidCouponException ex = assertThrows(InvalidCouponException.class, () -> validator.validate(WELCOME2026));

        assertEquals("El cupón 'WELCOME2026' está fuera de vigencia", ex.getMessage());
    }

    private void save(Coupon coupon) {
        couponsByCode.put(coupon.getCode(), coupon);
    }

    /** Doble de CouponRepository: solo implementa findByCode; cualquier otro método falla. */
    private CouponRepository inMemoryRepository() {
        return (CouponRepository) Proxy.newProxyInstance(
                CouponRepository.class.getClassLoader(),
                new Class<?>[] {CouponRepository.class},
                (proxy, method, args) -> {
                    if ("findByCode".equals(method.getName())) {
                        repositoryCalls++;
                        return Optional.ofNullable(couponsByCode.get((String) args[0]));
                    }
                    if ("toString".equals(method.getName())) {
                        return "InMemoryCouponRepository";
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** Coupon tiene constructor protegido: una subclase anónima permite fijar su estado sin BD. */
    private static Coupon coupon(String code, String discountPct, boolean active, boolean validNow) {
        return new Coupon() {
            @Override
            public String getCode() {
                return code;
            }

            @Override
            public BigDecimal getDiscountPct() {
                return new BigDecimal(discountPct);
            }

            @Override
            public boolean isActive() {
                return active;
            }

            @Override
            public boolean isValidAt(LocalDateTime instant) {
                return validNow;
            }
        };
    }
}
