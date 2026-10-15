package com.examen.ecommerce.service;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import java.util.Map;
import java.util.Optional;

/** Resultado del cálculo compartido por /cart/quote y /orders. */
public record PricedCart(Map<Integer, Product> products, Optional<Coupon> coupon, DiscountResult result) {

    public PricedCart {
        products = Map.copyOf(products);
        coupon = coupon == null ? Optional.empty() : coupon;
    }
}
