package com.examen.ecommerce.adapter;

import com.examen.ecommerce.dto.response.CartLineResponse;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.OrderItem;
import com.examen.ecommerce.mapper.DiscountMessageMapper;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.util.MoneyUtils;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * ADAPTER de salida: DiscountResult → CartQuoteResponse (/cart/quote) o Order + OrderItem
 * con snapshots (/orders). Ambas salidas parten del MISMO DiscountResult.
 */
@Component
public class DiscountResultAdapter {

    private final DiscountMessageMapper messageMapper;

    public DiscountResultAdapter(DiscountMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public CartQuoteResponse toQuoteResponse(DiscountResult result) {
        return new CartQuoteResponse(result.subtotalOriginal(), result.amountOf(DiscountType.CATEGORY),
                result.amountOf(DiscountType.VOLUME), result.amountOf(DiscountType.COUPON),
                result.cap().adjustment(), result.discountTotal(), result.effectiveDiscountPct(),
                result.cap().capPct(), result.capApplied(),
                messageMapper.messageFor(result.capApplied(), result.cap().capPct()),
                result.total(), MoneyUtils.DEFAULT_CURRENCY,
                result.coupon().map(CouponSnapshot::code).orElse(null),
                result.lines().stream().map(line -> toLineResponse(result, line)).toList());
    }

    public Order toOrder(PricedCart priced, String orderNumber, LocalDateTime createdAt) {
        DiscountResult result = priced.result();
        Order order = Order.builder()
                .orderNumber(orderNumber)
                .coupon(priced.coupon().orElse(null), result.coupon().map(CouponSnapshot::code).orElse(null))
                .subtotalOriginal(result.subtotalOriginal())
                .categoryDiscountAmount(result.amountOf(DiscountType.CATEGORY))
                .volumeDiscountAmount(result.amountOf(DiscountType.VOLUME))
                .couponDiscountAmount(result.amountOf(DiscountType.COUPON))
                .capAdjustmentAmount(result.cap().adjustment())
                .discountTotal(result.discountTotal())
                .effectiveDiscountPct(result.effectiveDiscountPct())
                .discountCapPct(result.cap().capPct())
                .capApplied(result.capApplied())
                .total(result.total())
                .currency(MoneyUtils.DEFAULT_CURRENCY)
                .createdAt(createdAt)
                .build();
        result.lines().forEach(line -> order.addItem(toOrderItem(priced, result, line)));
        return order;
    }

    private CartLineResponse toLineResponse(DiscountResult result, DiscountLine line) {
        return new CartLineResponse(line.productId(), line.productName(), line.categoryName(), line.unitPrice(),
                line.quantity(), line.lineSubtotal(), result.lineDiscount(line.productId()), result.lineTotal(line));
    }

    private OrderItem toOrderItem(PricedCart priced, DiscountResult result, DiscountLine line) {
        OrderItem.ItemSnapshot snapshot = new OrderItem.ItemSnapshot(line.productName(), line.categoryName(),
                line.unitPrice(), line.quantity(), line.lineSubtotal(), result.lineDiscount(line.productId()),
                result.lineTotal(line));
        return new OrderItem(priced.products().get(line.productId()), snapshot);
    }
}
