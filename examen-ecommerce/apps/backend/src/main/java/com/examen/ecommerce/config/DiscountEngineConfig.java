package com.examen.ecommerce.config;

import com.examen.ecommerce.service.discount.DiscountEngine;
import com.examen.ecommerce.service.discount.factory.DiscountRuleFactory;
import com.examen.ecommerce.service.discount.strategy.DiscountCapPolicy;
import com.examen.ecommerce.service.discount.strategy.PercentageCapPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra como beans las piezas del motor de descuentos. Se hace aquí (y no con
 * {@code @Component} dentro de service/discount) para que ese paquete siga siendo Java puro.
 */
@Configuration
public class DiscountEngineConfig {

    @Bean
    public DiscountRuleFactory discountRuleFactory(DiscountProperties properties) {
        return new DiscountRuleFactory(properties.volume().pct(), properties.volume().threshold());
    }

    @Bean
    public DiscountCapPolicy discountCapPolicy(DiscountProperties properties) {
        return new PercentageCapPolicy(properties.capPct());
    }

    @Bean
    public DiscountEngine discountEngine(DiscountCapPolicy capPolicy) {
        return new DiscountEngine(capPolicy);
    }
}
