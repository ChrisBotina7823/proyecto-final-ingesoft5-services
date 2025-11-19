package com.selimhorri.app.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Collects and exposes business metrics for monitoring.
 * This component is optional and can be disabled in tests by setting:
 * management.metrics.enabled=false
 */
@Component
@ConditionalOnProperty(
    name = "management.metrics.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@Slf4j
@RequiredArgsConstructor
public class BusinessMetricsCollector {
    
    private final MeterRegistry meterRegistry;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    
    private final AtomicReference<Double> cartAbandonmentRate = new AtomicReference<>(0.0);
    
    @PostConstruct
    public void init() {
        // Register gauge for cart abandonment rate
        Gauge.builder("cart_abandonment_rate", cartAbandonmentRate, AtomicReference::get)
                .description("Cart abandonment rate (0.0 to 1.0)")
                .tag("application", "order-service")
                .register(meterRegistry);
        
        log.info("Business metrics collector initialized");
    }
    
    /**
     * Calculate cart abandonment rate every 5 minutes.
     * Abandonment rate = (total_carts - total_orders) / total_carts
     */
    @Scheduled(fixedRate = 300000, initialDelay = 10000) // 5 minutes, start after 10s
    public void calculateCartAbandonmentRate() {
        try {
            long totalCarts = cartRepository.count();
            long totalOrders = orderRepository.count();
            
            if (totalCarts > 0) {
                double abandonmentRate = (double) (totalCarts - totalOrders) / totalCarts;
                // Ensure rate is between 0 and 1
                abandonmentRate = Math.max(0.0, Math.min(1.0, abandonmentRate));
                cartAbandonmentRate.set(abandonmentRate);
                
                log.debug("Cart abandonment rate updated: {:.2%} (carts={}, orders={})",
                        abandonmentRate, totalCarts, totalOrders);
            } else {
                cartAbandonmentRate.set(0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating cart abandonment rate", e);
            // Keep previous value on error
        }
    }
}
