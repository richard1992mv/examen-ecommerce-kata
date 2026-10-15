package com.examen.ecommerce.service.impl;

import com.examen.ecommerce.adapter.DiscountContextAdapter;
import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.service.CartValidator;
import com.examen.ecommerce.service.CouponValidator;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.PricingService;
import com.examen.ecommerce.service.StockService;
import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo único: validar carrito → cargar productos (1 consulta) → validar cupón →
 * verificar stock → Adapter → Factory → Engine. Siempre con precios y % de la BD.
 */
@Service
public class PricingServiceImpl implements PricingService {

    private final CartValidator cartValidator;
    private final CouponValidator couponValidator;
    private final StockService stockService;
    private final ProductRepository productRepository;
    private final DiscountContextAdapter contextAdapter;
    private final DiscountRuleFactory ruleFactory;
    private final DiscountEngine engine;

    public PricingServiceImpl(CartValidator cartValidator, CouponValidator couponValidator,
                              StockService stockService, ProductRepository productRepository,
                              DiscountContextAdapter contextAdapter, DiscountRuleFactory ruleFactory,
                              DiscountEngine engine) {
        this.cartValidator = cartValidator;
        this.couponValidator = couponValidator;
        this.stockService = stockService;
        this.productRepository = productRepository;
        this.contextAdapter = contextAdapter;
        this.ruleFactory = ruleFactory;
        this.engine = engine;
    }

    @Override
    @Transactional(readOnly = true)
    public PricedCart price(CartRequest request) {
        List<CartItemRequest> items = request.items();
        cartValidator.validate(items);
        Map<Integer, Product> products = loadProducts(items);
        Optional<Coupon> coupon = couponValidator.validate(request.couponCode());
        stockService.assertAvailable(items, products);
        DiscountContext context = contextAdapter.toContext(items, products, coupon);
        DiscountResult result = engine.calculate(context, ruleFactory.createRules(context.coupon()));
        return new PricedCart(products, coupon, result);
    }

    private Map<Integer, Product> loadProducts(List<CartItemRequest> items) {
        List<Integer> ids = items.stream().map(CartItemRequest::productId).toList();
        Map<Integer, Product> byId = productRepository.findAllWithCategoryByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        List<Integer> missing = ids.stream().filter(id -> !byId.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException(ErrorMessages.PRODUCTS_NOT_FOUND.formatted(missing));
        }
        return byId;
    }
}
