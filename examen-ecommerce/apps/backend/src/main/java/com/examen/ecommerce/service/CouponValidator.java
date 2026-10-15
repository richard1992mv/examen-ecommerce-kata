package com.examen.ecommerce.service;

import com.examen.ecommerce.entity.Coupon;
import java.util.Optional;

/** Valida cupones: existe, is_active = 1 y valid_from ≤ ahora ≤ valid_until. */
public interface CouponValidator {

    /** @return vacío si no se envió código; el cupón si es válido; 422 en otro caso */
    Optional<Coupon> validate(String code);
}
