package com.examen.ecommerce.repository;

import com.examen.ecommerce.entity.Order;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

/** Filtros opcionales de GET /orders: rango de fechas (inclusive) y código de cupón. */
public final class OrderSpecifications {

    private static final String CREATED_AT = "createdAt";

    private OrderSpecifications() {
    }

    public static Specification<Order> withFilters(LocalDateTime from, LocalDateTime to, String couponCode) {
        return Specification.where(createdFrom(from)).and(createdTo(to)).and(hasCouponCode(couponCode));
    }

    static Specification<Order> createdFrom(LocalDateTime from) {
        return (root, query, cb) -> from == null ? null : cb.greaterThanOrEqualTo(root.get(CREATED_AT), from);
    }

    static Specification<Order> createdTo(LocalDateTime to) {
        return (root, query, cb) -> to == null ? null : cb.lessThanOrEqualTo(root.get(CREATED_AT), to);
    }

    static Specification<Order> hasCouponCode(String couponCode) {
        return (root, query, cb) -> couponCode == null || couponCode.isBlank()
                ? null
                : cb.equal(root.get("couponCode"), couponCode.trim());
    }
}
