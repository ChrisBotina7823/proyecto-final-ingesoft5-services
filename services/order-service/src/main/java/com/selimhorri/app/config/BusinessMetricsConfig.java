package com.selimhorri.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Configuration
@EnableScheduling
@Slf4j
public class BusinessMetricsConfig {
    
    private final MeterRegistry meterRegistry;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    
    private final AtomicReference<Double> cartAbandonmentRate = new AtomicReference<>(0.0);
    
    public BusinessMetricsConfig(MeterRegistry meterRegistry, CartRepository cartRepository, OrderRepository orderRepository) {
        this.meterRegistry = meterRegistry;
        this.cartRepository = cartRepository;
        this.orderRepository = orderRepository;
        
        // Register gauge for cart abandonment rate
        Gauge.builder("cart_abandonment_rate", cartAbandonmentRate, AtomicReference::get)
                .description("Cart abandonment rate (carts without orders / total carts)")
                .tag("application", "order-service")
                .register(meterRegistry);
    }
    
    /**
     * Calculate cart abandonment rate every 5 minutes
     * Cart Abandonment Rate = (Carts Created - Orders Created) / Carts Created
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void calculateCartAbandonmentRate() {
        try {
            long totalCarts = cartRepository.count();
            long totalOrders = orderRepository.count();
            
            if (totalCarts > 0) {
                double abandonmentRate = (double) (totalCarts - totalOrders) / totalCarts;
                cartAbandonmentRate.set(Math.max(0.0, Math.min(1.0, abandonmentRate))); // Clamp between 0 and 1
                log.debug("Cart abandonment rate calculated: {}", abandonmentRate);
            } else {
                cartAbandonmentRate.set(0.0);
            }
        } catch (Exception e) {
            log.error("Error calculating cart abandonment rate", e);
        }
    }
}
