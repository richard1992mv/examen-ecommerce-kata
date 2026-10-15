package com.examen.ecommerce.service.discount;

import static com.examen.ecommerce.support.TestData.line;
import static com.examen.ecommerce.support.TestData.money;
import static org.assertj.core.api.Assertions.assertThat;

import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.model.DiscountContext;
import com.examen.ecommerce.service.discount.model.DiscountResult;
import com.examen.ecommerce.service.discount.model.DiscountStepResult;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.DiscountRule;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** OCP: una regla nueva registrada en la Factory se aplica sin modificar DiscountEngine. */
class OpenClosedPrincipleTest {

    /** Regla falsa: 1.00 fijo, después del volumen y antes del cupón. */
    private static final class FixedLoyaltyRule implements DiscountRule {

        @Override
        public DiscountType type() {
            return DiscountType.VOLUME;
        }

        @Override
        public int order() {
            return 2;
        }

        @Override
        public DiscountStepResult apply(DiscountContext ctx, BigDecimal currentSubtotal) {
            return DiscountStepResult.of(type(), money("1.00"));
        }
    }

    @Test
    void registeredRuleIsAppliedByTheUnchangedEngine() {
        DiscountRuleFactory extended = new DiscountRuleFactory(money("5.00"), money("100.00"),
                List.of(new FixedLoyaltyRule()));
        DiscountEngine engine = new DiscountEngine(new PercentageCapPolicy(money("35.00")));
        DiscountContext ctx = new DiscountContext(List.of(line(8, 2), line(2, 1)), Optional.empty());

        DiscountResult result = engine.calculate(ctx, extended.createRules(ctx.coupon()));

        assertThat(result.steps()).hasSize(3);
        assertThat(result.discountTotal()).isEqualByComparingTo("3.00");
        assertThat(result.total()).isEqualByComparingTo("47.00");
    }
}
