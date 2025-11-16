package com.selimhorri.app.config.feature;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

/**
 * Feature toggles for Proxy Client / API Gateway
 * Allows enabling/disabling features without code deployment
 */
public enum ProxyFeatures implements Feature {

    @Label("Enable API v2 endpoints")
    API_V2_ENABLED,

    @Label("Enable aggressive rate limiting")
    AGGRESSIVE_RATE_LIMITING,

    @Label("Enable circuit breaker for all services")
    @EnabledByDefault
    CIRCUIT_BREAKER_ENABLED,

    @Label("Enable request caching")
    REQUEST_CACHING_ENABLED,

    @Label("Enable verbose logging for debugging")
    VERBOSE_LOGGING;

    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
}
