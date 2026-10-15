package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.adapter.DiscountResultAdapter;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.event.CartEventPublisher;
import com.examen.ecommerce.event.CartQuotedEvent;
import com.examen.ecommerce.event.DiscountCapReachedEvent;
import com.examen.ecommerce.service.CartService;
import com.examen.ecommerce.service.PricingService;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import org.springframework.stereotype.Service;

@Service
public class CartServiceImpl implements CartService {

    private final PricingService pricingService;
    private final DiscountResultAdapter resultAdapter;
    private final CartEventPublisher publisher;

    public CartServiceImpl(PricingService pricingService, DiscountResultAdapter resultAdapter,
                           CartEventPublisher publisher) {
        this.pricingService = pricingService;
        this.resultAdapter = resultAdapter;
        this.publisher = publisher;
    }

    @Override
    public CartQuoteResponse quote(CartRequest request) {
        DiscountResult result = pricingService.price(request).result();
        CartQuoteResponse response = resultAdapter.toQuoteResponse(result);
        publishEvents(result);
        return response;
    }

    private void publishEvents(DiscountResult result) {
        publisher.publish(new CartQuotedEvent(result.lines().size(), result.discountTotal(), result.capApplied()));
        if (result.capApplied()) {
            publisher.publish(new DiscountCapReachedEvent(result.subtotalOriginal(),
                    result.cap().grossDiscount(), result.cap().adjustment()));
        }
    }
}
