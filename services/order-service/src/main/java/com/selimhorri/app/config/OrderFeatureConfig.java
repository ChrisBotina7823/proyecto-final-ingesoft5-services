package com.selimhorri.app.config;

import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;

public enum OrderFeatureConfig implements Feature {
    
    @Label("Priority Field in Orders")
    @EnabledByDefault
    PRIORITY_FIELD;
    
    public boolean isActive() {
        return FeatureContext.getFeatureManager().isActive(this);
    }
    
}
