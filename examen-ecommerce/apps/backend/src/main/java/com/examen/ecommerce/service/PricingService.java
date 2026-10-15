package com.examen.ecommerce.service;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.request.CartRequest;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.exception.ErrorMessages;
import com.examen.ecommerce.exception.ResourceNotFoundException;
import com.examen.ecommerce.mapper.CheckoutMapper;
import com.examen.ecommerce.repository.ProductRepository;
import com.examen.ecommerce.service.discount.DiscountCalculator;
import com.examen.ecommerce.service.discount.DiscountLine;
import com.examen.ecommerce.service.discount.DiscountResult;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flujo único de cálculo (HU2 y HU3): validar carrito → cargar productos (1 consulta) → validar cupón →
 * verificar stock → calcular. Siempre con precios, porcentajes y stock de la BD.
 */
@Service
public class PricingService {

    private final CartValidator cartValidator;
    private final CouponValidator couponValidator;
    private final StockService stockService;
    private final ProductRepository productRepository;
    private final DiscountCalculator calculator;
    private final CheckoutMapper mapper;

    public PricingService(CartValidator cartValidator, CouponValidator couponValidator, StockService stockService,
                          ProductRepository productRepository, DiscountCalculator calculator,
                          CheckoutMapper mapper) {
        this.cartValidator = cartValidator;
        this.couponValidator = couponValidator;
        this.stockService = stockService;
        this.productRepository = productRepository;
        this.calculator = calculator;
        this.mapper = mapper;
    }

    /** HU2: cotización sin persistir ni tocar stock. */
    @Transactional(readOnly = true)
    public CartQuoteResponse quote(CartRequest request) {
        PricedCart priced = price(request);
        return mapper.toQuoteResponse(priced.result(), priced.coupon());
    }

    /** Se ejecuta dentro de la transacción del checkout cuando lo invoca OrderService. */
    @Transactional(readOnly = true)
    public PricedCart price(CartRequest request) {
        List<CartItemRequest> items = request.items();
        cartValidator.validate(items);
        Map<Integer, Product> products = loadProducts(items);
        Optional<Coupon> coupon = couponValidator.validate(request.couponCode());
        stockService.assertAvailable(items, products);
        List<DiscountLine> lines = items.stream()
                .map(item -> toLine(products.get(item.productId()), item.quantity()))
                .toList();
        DiscountResult result = calculator.calculate(lines, coupon.map(Coupon::getDiscountPct));
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

    private static DiscountLine toLine(Product product, int quantity) {
        return new DiscountLine(product.getId(), product.getName(), product.getCategory().getName(),
                product.getUnitPrice(), quantity, product.getCategory().getDiscountPct());
    }
}
