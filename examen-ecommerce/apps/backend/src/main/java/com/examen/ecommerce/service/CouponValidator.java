package com.examen.ecommerce.service;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.repository.CouponRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Cupón válido = existe, is_active = 1 y valid_from ≤ ahora ≤ valid_until. Si no, 422. */
@Component
public class CouponValidator {

    private final CouponRepository couponRepository;

    public CouponValidator(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    /** @return vacío si no se envió código; el cupón si es válido */
    public Optional<Coupon> validate(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim();
        Coupon coupon = couponRepository.findByCode(normalized)
                .orElseThrow(() -> new InvalidCouponException(ErrorMessages.COUPON_NOT_FOUND, normalized));
        if (!coupon.isActive()) {
            throw new InvalidCouponException(ErrorMessages.COUPON_INACTIVE, normalized);
        }
        if (!coupon.isValidAt(LocalDateTime.now())) {
            throw new InvalidCouponException(ErrorMessages.COUPON_OUT_OF_VALIDITY, normalized);
        }
        return Optional.of(coupon);
    }
}
