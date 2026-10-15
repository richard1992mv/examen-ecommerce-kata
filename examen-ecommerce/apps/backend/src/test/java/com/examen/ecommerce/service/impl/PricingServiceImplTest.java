package com.examen.ecommerce.service.impl;

import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.examen.ecommerce.adapter.DiscountContextAdapter;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.exception.InsufficientStockException;
import com.examen.ecommerce.exception.InvalidCartException;
import com.examen.ecommerce.exception.InvalidCouponException;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.repository.CouponRepository;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import com.examen.ecommerce.support.TestData;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Flujo real (validadores + Adapter + Factory + Engine) con repositorios simulados. */
class PricingServiceImplTest {

    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final CouponRepository couponRepository = mock(CouponRepository.class);
    private PricingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PricingServiceImpl(new CartValidatorImpl(),
                new CouponValidatorImpl(couponRepository, TestData.FIXED_CLOCK),
                new StockServiceImpl(productRepository), productRepository, new DiscountContextAdapter(),
                new DiscountRuleFactory(money("5.00"), money("100.00")),
                new DiscountEngine(new PercentageCapPolicy(money("35.00"))));
        when(couponRepository.findByCode(anyString())).thenAnswer(invocation ->
                switch (invocation.<String>getArgument(0)) {
                    case "WELCOME2026" -> Optional.of(TestData.welcome2026());
                    case "EXPIRED2025" -> Optional.of(TestData.expired2025());
                    case "INACTIVE10" -> Optional.of(TestData.inactive10());
                    default -> Optional.empty();
                });
    }

    @Test
    void calculatesCaseCFromDatabasePrices() {
        when(productRepository.findAllWithCategoryByIdIn(anyCollection())).thenReturn(List.of(product(1), product(9)));

        PricedCart priced = service.price(cart("WELCOME2026", item(1, 2), item(9, 1)));

        assertThat(priced.result().amountOf(DiscountType.COUPON)).isEqualByComparingTo("17.24");
        assertThat(priced.result().total()).isEqualByComparingTo("97.71");
        assertThat(priced.coupon()).isPresent();
        assertThat(priced.products()).containsKeys(1, 9);
    }

    @Test
    void missingProductIs404() {
        when(productRepository.findAllWithCategoryByIdIn(anyCollection())).thenReturn(List.of(product(1)));

        assertThatThrownBy(() -> service.price(cart(null, item(1, 1), item(999, 1))))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining("999");
    }

    @Test
    void productFiveWithoutStockIs409() {
        when(productRepository.findAllWithCategoryByIdIn(anyCollection())).thenReturn(List.of(product(5)));

        assertThatThrownBy(() -> service.price(cart(null, item(5, 1)))).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void productSevenThreeUnitsIs409AndTwoIsOk() {
        when(productRepository.findAllWithCategoryByIdIn(anyCollection())).thenReturn(List.of(product(7)));

        assertThatThrownBy(() -> service.price(cart(null, item(7, 3)))).isInstanceOf(InsufficientStockException.class);
        assertThat(service.price(cart(null, item(7, 2))).result().total()).isEqualByComparingTo("104.50");
    }

    @Test
    void invalidCouponsAre422() {
        when(productRepository.findAllWithCategoryByIdIn(anyCollection())).thenReturn(List.of(product(1)));

        assertThatThrownBy(() -> service.price(cart("EXPIRED2025", item(1, 1))))
                .isInstanceOf(InvalidCouponException.class);
        assertThatThrownBy(() -> service.price(cart("INACTIVE10", item(1, 1))))
                .isInstanceOf(InvalidCouponException.class);
        assertThatThrownBy(() -> service.price(cart("FAKE", item(1, 1))))
                .isInstanceOf(InvalidCouponException.class);
    }

    @Test
    void invalidCartsAre400() {
        assertThatThrownBy(() -> service.price(new CartRequest(List.of(), null)))
                .isInstanceOf(InvalidCartException.class);
        assertThatThrownBy(() -> service.price(cart(null, item(1, 0)))).isInstanceOf(InvalidCartException.class);
        assertThatThrownBy(() -> service.price(cart(null, item(1, 1), item(1, 1))))
                .isInstanceOf(InvalidCartException.class);
    }

    static CartItemRequest item(int productId, int quantity) {
        return new CartItemRequest(productId, quantity);
    }

    static CartRequest cart(String coupon, CartItemRequest... items) {
        return new CartRequest(List.of(items), coupon);
    }
}
