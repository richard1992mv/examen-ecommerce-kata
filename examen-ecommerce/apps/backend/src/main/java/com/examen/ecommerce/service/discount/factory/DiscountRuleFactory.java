package com.examen.ecommerce.service.discount.factory;

import com.examen.ecommerce.service.discount.model.CouponSnapshot;
import com.examen.ecommerce.service.discount.model.DiscountType;
import com.examen.ecommerce.service.discount.strategy.CategoryDiscountRule;
import com.examen.ecommerce.service.discount.strategy.CouponDiscountRule;
import com.examen.ecommerce.service.discount.strategy.DiscountRule;
import com.examen.ecommerce.service.discount.strategy.VolumeDiscountRule;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * FACTORY: único punto donde se hace {@code new} de reglas. Los porcentajes llegan de
 * DiscountProperties (vía config/DiscountEngineConfig). Las reglas registradas como
 * extensión se agregan sin tocar el motor (OCP).
 */
public final class DiscountRuleFactory {

    private final BigDecimal volumePct;
    private final BigDecimal volumeThreshold;
    private final List<DiscountRule> registeredRules;

    public DiscountRuleFactory(BigDecimal volumePct, BigDecimal volumeThreshold) {
        this(volumePct, volumeThreshold, List.of());
    }

    public DiscountRuleFactory(BigDecimal volumePct, BigDecimal volumeThreshold, List<DiscountRule> registeredRules) {
        this.volumePct = Objects.requireNonNull(volumePct, "volumePct");
        this.volumeThreshold = Objects.requireNonNull(volumeThreshold, "volumeThreshold");
        this.registeredRules = List.copyOf(registeredRules);
    }

    /** Reglas ordenadas; incluye la del cupón solo si hay un cupón válido. */
    public List<DiscountRule> createRules(Optional<CouponSnapshot> coupon) {
        List<DiscountRule> rules = new ArrayList<>();
        rules.add(create(DiscountType.CATEGORY, null));
        rules.add(create(DiscountType.VOLUME, null));
        coupon.ifPresent(snapshot -> rules.add(create(DiscountType.COUPON, snapshot)));
        rules.addAll(registeredRules);
        rules.sort(Comparator.comparingInt(DiscountRule::order));
        return List.copyOf(rules);
    }

    /** Switch exhaustivo sobre el enum; tipo nulo o sin datos requeridos → IllegalArgumentException. */
    public DiscountRule create(DiscountType type, CouponSnapshot coupon) {
        if (type == null) {
            throw new IllegalArgumentException("Tipo de descuento no soportado: null");
        }
        return switch (type) {
            case CATEGORY -> new CategoryDiscountRule();
            case VOLUME -> new VolumeDiscountRule(volumePct, volumeThreshold);
            case COUPON -> new CouponDiscountRule(requireCoupon(coupon));
        };
    }

    private static CouponSnapshot requireCoupon(CouponSnapshot coupon) {
        if (coupon == null) {
            throw new IllegalArgumentException("La regla COUPON requiere un CouponSnapshot");
        }
        return coupon;
    }
}
