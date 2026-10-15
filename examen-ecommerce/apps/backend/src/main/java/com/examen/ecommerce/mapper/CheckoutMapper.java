package com.examen.ecommerce.mapper;

import com.examen.ecommerce.dto.response.CartLineResponse;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.dto.response.OrderItemResponse;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.OrderItem;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.discount.DiscountLine;
import com.examen.ecommerce.service.discount.DiscountResult;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** DiscountResult → respuesta de /cart/quote, entidad Order (con snapshots) y respuesta de /orders. */
@Component
public class CheckoutMapper {

    /** HU4: se muestra solo cuando se aplicó el tope. */
    public static final String CAP_REACHED_TEMPLATE =
            "¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (%s%%)";

    public CartQuoteResponse toQuoteResponse(DiscountResult result, Optional<Coupon> coupon) {
        return new CartQuoteResponse(result.subtotalOriginal(), result.categoryDiscount(), result.volumeDiscount(),
                result.couponDiscount(), result.capAdjustment(), result.discountTotal(),
                result.effectiveDiscountPct(), result.discountCapPct(), result.capApplied(),
                capMessage(result.capApplied(), result.discountCapPct()), result.total(),
                MoneyUtils.DEFAULT_CURRENCY, coupon.map(Coupon::getCode).orElse(null),
                result.lines().stream().map(CheckoutMapper::toLineResponse).toList());
    }

    public Order toOrder(PricedCart priced, String orderNumber, LocalDateTime createdAt) {
        DiscountResult result = priced.result();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .coupon(priced.coupon().orElse(null), priced.coupon().map(Coupon::getCode).orElse(null))
                .subtotalOriginal(result.subtotalOriginal())
                .categoryDiscountAmount(result.categoryDiscount())
                .volumeDiscountAmount(result.volumeDiscount())
                .couponDiscountAmount(result.couponDiscount())
                .capAdjustmentAmount(result.capAdjustment())
                .discountTotal(result.discountTotal())
                .effectiveDiscountPct(result.effectiveDiscountPct())
                .discountCapPct(result.discountCapPct())
                .capApplied(result.capApplied())
                .total(result.total())
                .currency(MoneyUtils.DEFAULT_CURRENCY)
                .createdAt(createdAt)
                .build();
        result.lines().forEach(lineResult -> order.addItem(toOrderItem(priced, lineResult)));
        return order;
    }

    public OrderResponse toOrderResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCreatedAt(), order.getCouponCode(),
                order.getSubtotalOriginal(), order.getCategoryDiscountAmount(), order.getVolumeDiscountAmount(),
                order.getCouponDiscountAmount(), order.getCapAdjustmentAmount(), order.getDiscountTotal(),
                order.getEffectiveDiscountPct(), order.getDiscountCapPct(), order.isCapApplied(),
                capMessage(order.isCapApplied(), order.getDiscountCapPct()),
                order.getTotal(), order.getCurrency(),
                order.getItems().stream().map(CheckoutMapper::toItemResponse).toList());
    }

    /** @return mensaje HU4 si se aplicó el tope; null en otro caso */
    static String capMessage(boolean capApplied, BigDecimal capPct) {
        return capApplied ? CAP_REACHED_TEMPLATE.formatted(capPct.stripTrailingZeros().toPlainString()) : null;
    }

    private static CartLineResponse toLineResponse(DiscountResult.LineResult lineResult) {
        DiscountLine line = lineResult.line();
        return new CartLineResponse(line.productId(), line.productName(), line.categoryName(), line.unitPrice(),
                line.quantity(), line.lineSubtotal(), lineResult.categoryDiscount(), lineResult.lineTotal());
    }

    private static OrderItem toOrderItem(PricedCart priced, DiscountResult.LineResult lineResult) {
        DiscountLine line = lineResult.line();
        OrderItem.ItemSnapshot snapshot = new OrderItem.ItemSnapshot(line.productName(), line.categoryName(),
                line.unitPrice(), line.quantity(), line.lineSubtotal(), lineResult.categoryDiscount(),
                lineResult.lineTotal());
        return new OrderItem(priced.products().get(line.productId()), snapshot);
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProduct().getId(), item.getProductName(),
                item.getCategoryName(), item.getUnitPrice(), item.getQuantity(), item.getLineSubtotal(),
                item.getCategoryDiscountAmount(), item.getLineTotal());
    }
}
