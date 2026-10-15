package com.examen.ecommerce.adapter;

import static com.examen.ecommerce.support.TestData.money;
import static com.examen.ecommerce.support.TestData.product;
import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.dto.request.CartItemRequest;
import com.examen.ecommerce.dto.response.CartLineResponse;
import com.examen.ecommerce.dto.response.CartQuoteResponse;
import com.examen.ecommerce.entity.Coupon;
import com.examen.ecommerce.entity.Order;
import com.examen.ecommerce.entity.OrderItem;
import com.examen.ecommerce.entity.Product;
import com.examen.ecommerce.mapper.DiscountMessageMapper;
import com.examen.ecommerce.service.PricedCart;
import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountLine;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import com.examen.ecommerce.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscountAdaptersTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 12, 0);

    private final DiscountContextAdapter contextAdapter = new DiscountContextAdapter();
    private final DiscountResultAdapter resultAdapter = new DiscountResultAdapter(new DiscountMessageMapper());
    private final DiscountRuleFactory factory = new DiscountRuleFactory(money("5.00"), money("100.00"));
    private final DiscountEngine engine = new DiscountEngine(new PercentageCapPolicy(money("35.00")));

    private final Map<Integer, Product> products = Map.of(4, product(4), 12, product(12), 10, product(10));
    private final List<CartItemRequest> items = List.of(new CartItemRequest(4, 1), new CartItemRequest(12, 1),
            new CartItemRequest(10, 2));
    private final Coupon coupon = TestData.testCap30();

    @Test
    void entitiesToExactDiscountContext() {
        DiscountContext ctx = contextAdapter.toContext(items, products, Optional.of(coupon));

        assertThat(ctx.lines()).containsExactly(
                new DiscountLine(4, "Monitor 24 pulgadas", "Tecnología", money("150.00"), 1, money("10.00")),
                new DiscountLine(12, "Smartwatch", "Tecnología", money("120.00"), 1, money("10.00")),
                new DiscountLine(10, "Balón de fútbol", "Deportes", money("25.00"), 2, money("0.00")));
        assertThat(ctx.coupon()).contains(new CouponSnapshot(4, "TESTCAP30", money("30.00")));
        assertThat(ctx.subtotalOriginal()).isEqualByComparingTo("320.00");
    }

    @Test
    void resultToOrderWithSnapshotsAndAmountsOfCaseD() {
        DiscountResult result = calculateCaseD();

        Order order = resultAdapter.toOrder(new PricedCart(products, Optional.of(coupon), result),
                "ORD-2026-000005", NOW);

        assertThat(order.getOrderNumber()).isEqualTo("ORD-2026-000005");
        assertThat(order.getCoupon()).isSameAs(coupon);
        assertThat(order.getCouponCode()).isEqualTo("TESTCAP30");
        assertAmounts(order);
        assertThat(order.getCreatedAt()).isEqualTo(NOW);
        assertThat(order.getItems()).hasSize(3).allSatisfy(item -> assertThat(item.getOrder()).isSameAs(order));
        OrderItem monitor = order.getItems().get(0);
        assertThat(monitor.getProduct()).isSameAs(products.get(4));
        assertThat(monitor.getProductName()).isEqualTo("Monitor 24 pulgadas");
        assertThat(monitor.getCategoryName()).isEqualTo("Tecnología");
        assertThat(monitor.getUnitPrice()).isEqualByComparingTo("150.00");
        assertThat(monitor.getLineSubtotal()).isEqualByComparingTo("150.00");
        assertThat(monitor.getCategoryDiscountAmount()).isEqualByComparingTo("15.00");
        assertThat(monitor.getLineTotal()).isEqualByComparingTo("135.00");
        assertThat(order.getItems().get(2).getQuantity()).isEqualTo(2);
    }

    @Test
    void resultToQuoteResponseWithHu4Message() {
        CartQuoteResponse response = resultAdapter.toQuoteResponse(calculateCaseD());

        assertThat(response.capApplied()).isTrue();
        assertThat(response.message())
                .isEqualTo("¡Enhorabuena! Has alcanzado el límite máximo de ahorro permitido (35%)");
        assertThat(response.capAdjustment()).isEqualByComparingTo("13.16");
        assertThat(response.total()).isEqualByComparingTo("208.00");
        assertThat(response.currency()).isEqualTo("USD");
        assertThat(response.couponCode()).isEqualTo("TESTCAP30");
        assertThat(response.items()).extracting(CartLineResponse::lineTotal)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(money("135.00"), money("108.00"), money("50.00"));
    }

    @Test
    void orderWithoutCouponHasNullSnapshot() {
        Map<Integer, Product> one = Map.of(8, product(8));
        DiscountContext ctx = contextAdapter.toContext(List.of(new CartItemRequest(8, 2)), one, Optional.empty());
        DiscountResult result = engine.calculate(ctx, factory.createRules(ctx.coupon()));

        Order order = resultAdapter.toOrder(new PricedCart(one, Optional.empty(), result), "ORD-2026-000006", NOW);

        assertThat(order.getCoupon()).isNull();
        assertThat(order.getCouponCode()).isNull();
        assertThat(order.isCapApplied()).isFalse();
        assertThat(resultAdapter.toQuoteResponse(result).message()).isNull();
    }

    private DiscountResult calculateCaseD() {
        DiscountContext ctx = contextAdapter.toContext(items, products, Optional.of(coupon));
        return engine.calculate(ctx, factory.createRules(ctx.coupon()));
    }

    private static void assertAmounts(Order order) {
        assertThat(order.getSubtotalOriginal()).isEqualByComparingTo("320.00");
        assertThat(order.getCategoryDiscountAmount()).isEqualByComparingTo("27.00");
        assertThat(order.getVolumeDiscountAmount()).isEqualByComparingTo("14.65");
        assertThat(order.getCouponDiscountAmount()).isEqualByComparingTo("83.51");
        assertThat(order.getCapAdjustmentAmount()).isEqualByComparingTo("13.16");
        assertThat(order.getDiscountTotal()).isEqualByComparingTo("112.00");
        assertThat(order.getEffectiveDiscountPct()).isEqualByComparingTo("35.00");
        assertThat(order.getDiscountCapPct()).isEqualByComparingTo("35.00");
        assertThat(order.isCapApplied()).isTrue();
        assertThat(order.getTotal()).isEqualByComparingTo("208.00");
        assertThat(order.getCurrency()).isEqualTo("USD");
    }
}
