package com.examen.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.DiscountMessageMapper;
import com.examen.ecommerce.mapper.OrderMapper;
import com.examen.ecommerce.repository.OrderRepository;
import com.examen.ecommerce.util.MoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class OrderQueryServiceImplTest {

    private final OrderRepository repository = mock(OrderRepository.class);
    private final OrderQueryServiceImpl service = new OrderQueryServiceImpl(repository,
            new OrderMapper(new DiscountMessageMapper()));
    private final Order order = Order.builder().orderNumber("ORD-2026-000004").coupon(null, "TESTCAP30")
            .subtotalOriginal(new BigDecimal("320.00")).categoryDiscountAmount(new BigDecimal("27.00"))
            .volumeDiscountAmount(new BigDecimal("14.65")).couponDiscountAmount(new BigDecimal("83.51"))
            .capAdjustmentAmount(new BigDecimal("13.16")).discountTotal(new BigDecimal("112.00"))
            .effectiveDiscountPct(new BigDecimal("35.00")).discountCapPct(new BigDecimal("35.00")).capApplied(true)
            .total(new BigDecimal("208.00")).currency(MoneyUtils.DEFAULT_CURRENCY)
            .createdAt(LocalDateTime.of(2026, 10, 8, 19, 30)).build();

    @Test
    @SuppressWarnings("unchecked")
    void listsSummaries() {
        Pageable page = PageRequest.of(0, 20);
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order), page, 1));

        assertThat(service.findAll(null, null, "TESTCAP30", page).content()).singleElement()
                .satisfies(summary -> assertThat(summary.capApplied()).isTrue());
    }

    @Test
    void findsByIdAndNumberWithHu4Message() {
        when(repository.findWithItemsById(4)).thenReturn(Optional.of(order));
        when(repository.findWithItemsByOrderNumber("ORD-2026-000004")).thenReturn(Optional.of(order));

        assertThat(service.findById(4).message()).contains("35%");
        assertThat(service.findByOrderNumber("ORD-2026-000004").total()).isEqualByComparingTo("208.00");
    }

    @Test
    void missingOrderIs404() {
        when(repository.findWithItemsById(99)).thenReturn(Optional.empty());
        when(repository.findWithItemsByOrderNumber("X")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.findByOrderNumber("X")).isInstanceOf(ResourceNotFoundException.class);
    }
}
