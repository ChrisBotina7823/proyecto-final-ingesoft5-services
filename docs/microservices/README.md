# Microservices

E-commerce microservices architecture with fixes, resilience patterns, and observability.

## Services

- `service-discovery` - Eureka server for service registration
- `cloud-config` - Centralized configuration management
- `api-gateway` - Entry point with routing and load balancing
- `user-service` - User management and authentication
- `product-service` - Product catalog
- `order-service` - Order processing
- `payment-service` - Payment processing with external integration
- `shipping-service` - Shipping and delivery management
- `favourite-service` - User favorites and wishlist
- `proxy-client` - Client proxy for service communication

## Fixes Applied

**Mapper Issues**: Original implementation attempted to persist ID fields incorrectly during entity mapping.

Problem: Mappers tried to populate ID fields before persistence, causing database constraint violations.

Solution: Corrected mapper direction to respect JPA relationship ownership. Parent entities now properly cascade to children without manual ID assignment.

**N+1 Query Problem**: Fetching lists of entities with relationships triggered individual queries per item.

Problem: Loading orders with associated users caused one query for orders plus N queries for users (one per order).

Current state: Issue identified and documented. Lazy loading with fetch joins recommended for future optimization.

**Spring Boot Relationship Mapping**: Bidirectional relationships not properly configured.

Solution: Added `@JsonManagedReference` and `@JsonBackReference` to prevent serialization loops. Configured proper cascade types and fetch strategies.

## Resilience Patterns

Implemented in `order-service` for user service communication.

**Retry**: Automatic retry on transient failures.
```java
@Retry(name = "userService", fallbackMethod = "getUserFallback")
public User getUser(Long userId) { ... }
```

Configuration:
- Max attempts: 3
- Wait duration: 1 second
- Exponential backoff

**Circuit Breaker**: Prevents cascading failures.
```java
@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
```

States:
- Closed: Normal operation
- Open: Reject requests immediately (50% failure threshold)
- Half-Open: Test if service recovered

Configuration:
- Failure rate threshold: 50%
- Wait duration in open state: 10 seconds
- Permitted calls in half-open: 3

**Bulkhead**: Resource isolation.
```java
@Bulkhead(name = "userService", fallbackMethod = "getUserFallback")
```

Limits:
- Max concurrent calls: 10
- Max wait duration: 0ms (fail fast)

Prevents resource exhaustion from affecting entire application.

**Combined Pattern**:
```java
@Retry(name = "userService")
@CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
@Bulkhead(name = "userService")
public User getUserById(Long userId) {
    return restTemplate.getForObject(userServiceUrl + "/" + userId, User.class);
}

private User getUserFallback(Long userId, Exception e) {
    return User.builder()
        .id(userId)
        .username("unavailable")
        .email("service-unavailable@system.local")
        .build();
}
```

## Health Checks

All services implement Spring Boot Actuator health endpoints.

**Liveness**: `/actuator/health/liveness`
- Indicates if application is running
- Used by Kubernetes to restart unhealthy pods

**Readiness**: `/actuator/health/readiness`
- Indicates if application can serve traffic
- Checks database connectivity and external service availability
- Used by Kubernetes to route traffic

**Full Health**: `/actuator/health`
- Detailed health information including database status and disk space

Configuration:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  health:
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

## Business Metrics

Custom metrics exposed via Micrometer for business intelligence.

**User Service**:
- `users_registered_total` - Counter of user registrations
- `users_active_count` - Gauge of active users

**Order Service**:
- `orders_created_total` - Counter of orders placed
- `cart_abandonment_rate` - Gauge of abandoned carts percentage

**Payment Service**:
- `payments_processed_total` - Counter of successful payments
- `payments_failed_total` - Counter of failed payments

**Implementation**:
```java
@Configuration
public class BusinessMetricsConfig {
    private final MeterRegistry meterRegistry;
    
    @Bean
    public Counter userRegistrationCounter() {
        return Counter.builder("users_registered_total")
            .description("Total number of user registrations")
            .register(meterRegistry);
    }
}
```

**Usage in service**:
```java
@Service
public class UserServiceImpl {
    private final Counter registrationCounter;
    
    public User createUser(UserRequest request) {
        User user = // ... create user
        registrationCounter.increment();
        return user;
    }
}
```

Metrics available at `/actuator/prometheus` for Prometheus scraping.

## Monitoring Integration

**Prometheus**: Scrapes metrics from `/actuator/prometheus` endpoint.

**Grafana**: Visualizes metrics in custom dashboards.

**Loki**: Aggregates logs from all services.

**Service Discovery**: ServiceMonitor automatically configures Prometheus targets.

## Configuration

**External Configuration**: Cloud Config Server provides centralized configuration.

**Environment-Specific**: Profiles for dev and prod with different database URLs and API keys.

**Secrets**: Kubernetes Secrets for sensitive data (database passwords, API keys).

## Database Schema

Each service has isolated database:
- `user_db`
- `product_db`
- `order_db`
- `payment_db`
- `shipping_db`
- `favourite_db`

No shared database access ensuring service independence.