package com.selimhorri.app.config.feature;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.spi.FeatureProvider;

/**
 * Togglz configuration for Payment Service
 */
@Configuration
public class TogglzConfiguration {

    @Bean
    public FeatureProvider featureProvider() {
        return new EnumBasedFeatureProvider(PaymentFeatures.class);
    }
}
