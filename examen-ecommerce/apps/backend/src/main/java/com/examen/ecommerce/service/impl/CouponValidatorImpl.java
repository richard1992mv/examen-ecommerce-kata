package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.repository.CouponRepository;
import com.examen.ecommerce.service.CouponValidator;
import com.examen.ecommerce.util.DateTimeUtils;
import java.time.Clock;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CouponValidatorImpl implements CouponValidator {

    private final CouponRepository couponRepository;
    private final Clock clock;

    public CouponValidatorImpl(CouponRepository couponRepository, Clock clock) {
        this.couponRepository = couponRepository;
        this.clock = clock;
    }

    @Override
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
        if (!coupon.isValidAt(DateTimeUtils.now(clock))) {
            throw new InvalidCouponException(ErrorMessages.COUPON_OUT_OF_VALIDITY, normalized);
        }
        return Optional.of(coupon);
    }
}
