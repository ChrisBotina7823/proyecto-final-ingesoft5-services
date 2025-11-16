package com.selimhorri.app.config.feature;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature toggles for Payment Service
 * Allows enabling/disabling features without code deployment
 */
public enum PaymentFeatures implements Feature {

    @Label("Enable new payment gateway")
    @EnabledByDefault
    NEW_PAYMENT_GATEWAY,

    @Label("Enable strict fraud checks")
    STRICT_FRAUD_CHECKS,

    @Label("Enable payment retry mechanism")
    @EnabledByDefault
    PAYMENT_RETRY_ENABLED,

    @Label("Enable idempotency validation")
    @EnabledByDefault
    IDEMPOTENCY_ENABLED;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
