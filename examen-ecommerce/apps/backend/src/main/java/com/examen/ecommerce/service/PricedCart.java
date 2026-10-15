package com.examen.ecommerce.service;

import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.service.discount.DiscountResult;
import java.util.Map;
import java.util.Optional;

/** Resultado del cálculo compartido por /cart/quote y /orders (entidades + desglose). */
public record PricedCart(Map<Integer, Product> products, Optional<Coupon> coupon, DiscountResult result) {
}
