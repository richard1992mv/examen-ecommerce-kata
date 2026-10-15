package com.examen.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.repository.CouponRepository;
import com.examen.ecommerce.support.TestData;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CouponValidatorImplTest {

    private final CouponRepository repository = mock(CouponRepository.class);
    private final CouponValidatorImpl validator = new CouponValidatorImpl(repository, TestData.FIXED_CLOCK);

    @BeforeEach
    void seed() {
        stub(TestData.welcome2026());
        stub(TestData.expired2025());
        stub(TestData.inactive10());
        when(repository.findByCode("NOEXISTE")).thenReturn(Optional.empty());
    }

    @Test
    void expiredCouponIs422() {
        assertThatThrownBy(() -> validator.validate("EXPIRED2025")).isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("fuera de vigencia");
    }

    @Test
    void inactiveCouponIs422() {
        assertThatThrownBy(() -> validator.validate("INACTIVE10")).isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void unknownCouponIs422() {
        assertThatThrownBy(() -> validator.validate("NOEXISTE")).isInstanceOf(InvalidCouponException.class)
                .hasMessageContaining("no existe")
                .extracting(ex -> ((InvalidCouponException) ex).getCouponCode()).isEqualTo("NOEXISTE");
    }

    @Test
    void validCouponIsReturned() {
        assertThat(validator.validate(" WELCOME2026 ")).map(Coupon::getCode).contains("WELCOME2026");
    }

    @ParameterizedTest
    @ValueSource(strings = {"2026-01-01T00:00:00", "2026-12-31T23:59:59"})
    void welcomeIsValidAtTheExactBoundaries(String instant) {
        CouponValidatorImpl atBoundary = new CouponValidatorImpl(repository,
                TestData.clockAt(LocalDateTime.parse(instant)));
        assertThat(atBoundary.validate("WELCOME2026")).isPresent();
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025-12-31T23:59:59", "2027-01-01T00:00:00"})
    void welcomeIsInvalidJustOutsideTheBoundaries(String instant) {
        CouponValidatorImpl outside = new CouponValidatorImpl(repository,
                TestData.clockAt(LocalDateTime.parse(instant)));
        assertThatThrownBy(() -> outside.validate("WELCOME2026")).isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void blankCodeMeansNoCoupon() {
        assertThat(validator.validate(null)).isEmpty();
        assertThat(validator.validate("  ")).isEmpty();
    }

    private void stub(Coupon coupon) {
        when(repository.findByCode(coupon.getCode())).thenReturn(Optional.of(coupon));
    }
}
