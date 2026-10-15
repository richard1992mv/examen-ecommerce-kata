package com.examen.ecommerce.mapper;

import com.examen.ecommerce.dto.response.OrderItemResponse;
import com.examen.ecommerce.dto.response.OrderResponse;
import com.examen.ecommerce.dto.response.OrderSummaryResponse;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final DiscountMessageMapper messageMapper;

    public OrderMapper(DiscountMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(order.getId(), order.getOrderNumber(), order.getCreatedAt(), order.getCouponCode(),
                order.getSubtotalOriginal(), order.getCategoryDiscountAmount(), order.getVolumeDiscountAmount(),
                order.getCouponDiscountAmount(), order.getCapAdjustmentAmount(), order.getDiscountTotal(),
                order.getEffectiveDiscountPct(), order.getDiscountCapPct(), order.isCapApplied(),
                messageMapper.messageFor(order.isCapApplied(), order.getDiscountCapPct()),
                order.getTotal(), order.getCurrency(),
                order.getItems().stream().map(this::toItemResponse).toList());
    }

    public OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getOrderNumber(), order.getCreatedAt(),
                order.getCouponCode(), order.getSubtotalOriginal(), order.getDiscountTotal(), order.getTotal(),
                order.isCapApplied(), order.getCurrency());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getId(), item.getProduct().getId(), item.getProductName(),
                item.getCategoryName(), item.getUnitPrice(), item.getQuantity(), item.getLineSubtotal(),
                item.getCategoryDiscountAmount(), item.getLineTotal());
    }
}
