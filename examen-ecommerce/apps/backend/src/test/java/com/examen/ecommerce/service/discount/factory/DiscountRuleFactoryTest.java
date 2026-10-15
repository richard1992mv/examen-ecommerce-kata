package com.examen.ecommerce.service.discount.factory;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.examen.ecommerce.config.DiscountEngineConfig;
import com.examen.ecommerce.config.DiscountProperties;
import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.DiscountRule;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DiscountRuleFactoryTest {

    private static final CouponSnapshot COUPON = new CouponSnapshot(1, "WELCOME2026", money("15.00"));

    private final DiscountRuleFactory factory = new DiscountRuleFactory(money("5.00"), money("100.00"));

    @Test
    void withoutCouponCreatesTwoRules() {
        List<DiscountRule> rules = factory.createRules(Optional.empty());
        assertThat(rules).extracting(DiscountRule::type).containsExactly(DiscountType.CATEGORY, DiscountType.VOLUME);
    }

    @Test
    void withCouponCreatesThreeRulesInOrder() {
        List<DiscountRule> rules = factory.createRules(Optional.of(COUPON));
        assertThat(rules).extracting(DiscountRule::order).containsExactly(1, 2, 3);
        assertThat(rules).extracting(DiscountRule::type)
                .containsExactly(DiscountType.CATEGORY, DiscountType.VOLUME, DiscountType.COUPON);
    }

    @Test
    void unsupportedTypeThrowsIllegalArgumentException() {
        assertThatThrownBy(() -> factory.create(null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> factory.create(DiscountType.COUPON, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createsEveryEnumType() {
        for (DiscountType type : DiscountType.values()) {
            assertThat(factory.create(type, COUPON).type()).isEqualTo(type);
        }
    }

    @Test
    void percentagesComeFromDiscountProperties() {
        DiscountProperties properties = new DiscountProperties(
                new DiscountProperties.Volume(money("10.00"), money("50.00")), money("35.00"));
        DiscountRuleFactory configured = new DiscountEngineConfig().discountRuleFactory(properties);
        DiscountRule volume = configured.create(DiscountType.VOLUME, null);
        DiscountContext ctx = new DiscountContext(List.of(line(9, 2)), Optional.empty());

        assertThat(volume.apply(ctx, money("80.00")).amount()).isEqualByComparingTo("8.00");
        assertThat(volume.apply(ctx, money("50.00")).amount()).isEqualByComparingTo("0.00");
    }
}
