package com.examen.ecommerce.service.impl;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.snapshot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.adapter.DiscountResultAdapter;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.event.CartEvent;
import com.examen.ecommerce.event.CartEventPublisher;
import com.examen.ecommerce.event.CartQuotedEvent;
import com.examen.ecommerce.event.DiscountCapReachedEvent;
import com.examen.ecommerce.mapper.DiscountMessageMapper;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.PricingService;
import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import com.examen.ecommerce.support.TestData;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CartServiceImplTest {

    private final PricingService pricingService = mock(PricingService.class);
    private final CartEventPublisher publisher = mock(CartEventPublisher.class);
    private final CartServiceImpl service = new CartServiceImpl(pricingService,
            new DiscountResultAdapter(new DiscountMessageMapper()), publisher);
    private final CartRequest request = new CartRequest(List.of(new CartItemRequest(1, 1)), null);

    @Test
    void quotePublishesCartQuotedEventOnly() {
        when(pricingService.price(any())).thenReturn(priced(List.of(line(8, 2), line(2, 1)), null));

        CartQuoteResponse response = service.quote(request);

        assertThat(response.total()).isEqualByComparingTo("48.00");
        assertThat(publishedEvents()).singleElement().isInstanceOf(CartQuotedEvent.class);
    }

    @Test
    void caseDPublishesDiscountCapReachedEvent() {
        when(pricingService.price(any())).thenReturn(priced(List.of(line(4, 1), line(12, 1), line(10, 2)),
                snapshot(TestData.testCap30())));

        CartQuoteResponse response = service.quote(request);

        assertThat(response.capApplied()).isTrue();
        assertThat(publishedEvents()).hasSize(2)
                .anySatisfy(event -> assertThat(event).isEqualTo(new CartQuotedEvent(3, money("112.00"), true)))
                .anySatisfy(event -> assertThat(event).isInstanceOfSatisfying(DiscountCapReachedEvent.class,
                        cap -> assertThat(cap.capAdjustment()).isEqualByComparingTo("13.16")));
    }

    private List<CartEvent> publishedEvents() {
        ArgumentCaptor<CartEvent> captor = ArgumentCaptor.forClass(CartEvent.class);
        verify(publisher, org.mockito.Mockito.atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    private static PricedCart priced(List<DiscountLine> lines, CouponSnapshot coupon) {
        DiscountRuleFactory factory = new DiscountRuleFactory(money("5.00"), money("100.00"));
        DiscountContext ctx = new DiscountContext(lines, Optional.ofNullable(coupon));
        DiscountResult result = new DiscountEngine(new PercentageCapPolicy(money("35.00")))
                .calculate(ctx, factory.createRules(ctx.coupon()));
        return new PricedCart(Map.of(), Optional.empty(), result);
    }
}
