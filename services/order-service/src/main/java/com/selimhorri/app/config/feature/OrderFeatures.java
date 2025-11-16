package com.selimhorri.app.config.feature;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature toggles for Order Service
 * Allows enabling/disabling features without code deployment
 */
public enum OrderFeatures implements Feature {

    @Label("Enable two-step checkout process")
    TWO_STEP_CHECKOUT,

    @Label("Enable SAGA orchestrated pattern")
    SAGA_ORCHESTRATED,

    @Label("Enable order validation with external service")
    @EnabledByDefault
    ORDER_VALIDATION_ENABLED,

    @Label("Enable automatic inventory reservation")
    @EnabledByDefault
    AUTO_INVENTORY_RESERVATION;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
