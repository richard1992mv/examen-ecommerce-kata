package com.examen.ecommerce.mapper;

import com.examen.ecommerce.dto.request.CouponRequest;
import com.examen.ecommerce.dto.response.CouponResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.util.MoneyUtils;
import org.springframework.stereotype.Component;

@Component
public class CouponMapper {

    public CouponResponse toResponse(Coupon coupon) {
        return new CouponResponse(coupon.getId(), coupon.getCode(), coupon.getDiscountPct(), coupon.getValidFrom(),
                coupon.getValidUntil(), coupon.isActive());
    }

    public Coupon toEntity(CouponRequest request) {
        return new Coupon(request.code().trim(), MoneyUtils.round(request.discountPct()), request.validFrom(),
                request.validUntil(), request.active());
    }

    public void updateEntity(Coupon coupon, CouponRequest request) {
        coupon.update(request.code().trim(), MoneyUtils.round(request.discountPct()), request.validFrom(),
                request.validUntil(), request.active());
    }
}
