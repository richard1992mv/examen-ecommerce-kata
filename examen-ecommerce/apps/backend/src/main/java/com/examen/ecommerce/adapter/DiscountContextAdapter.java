package com.examen.ecommerce.adapter;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.entity.Category;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * ADAPTER de entrada: entidades JPA + request → DiscountContext (records sin JPA).
 * Un cambio de esquema solo toca esta clase, no el motor.
 */
@Component
public class DiscountContextAdapter {

    public DiscountContext toContext(List<CartItemRequest> items, Map<Integer, Product> productsById,
                                     Optional<Coupon> coupon) {
        List<DiscountLine> lines = items.stream()
                .map(item -> toLine(productsById.get(item.productId()), item.quantity()))
                .toList();
        return new DiscountContext(lines, coupon.map(this::toSnapshot));
    }

    public DiscountLine toLine(Product product, int quantity) {
        Category category = product.getCategory();
        return new DiscountLine(product.getId(), product.getName(), category.getName(), product.getUnitPrice(),
                quantity, category.getDiscountPct());
    }

    public CouponSnapshot toSnapshot(Coupon coupon) {
        return new CouponSnapshot(coupon.getId(), coupon.getCode(), coupon.getDiscountPct());
    }
}
