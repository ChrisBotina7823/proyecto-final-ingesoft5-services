package com.selimhorri.app.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.UserDto;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Small client bean that centralizes calls to USER-SERVICE.
 *
 * It applies a Bulkhead to limit concurrent calls and a Retry to handle transient errors.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserServiceClient {

    private final RestTemplate restTemplate;

    /**
     * Fetches user from USER-SERVICE with resilience patterns applied
     */
    @Retry(name = "orderServiceRetry")
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "getUserByIdFallback")
    @Bulkhead(name = "orderServiceBulkhead", type = Bulkhead.Type.THREADPOOL)
    public UserDto getUserById(final Integer userId) {
        log.debug("Fetching user id={} from USER-SERVICE (via UserServiceClient)", userId);
        return this.restTemplate.getForObject(
                AppConstant.DiscoveredDomainsApi.USER_SERVICE_API_URL + "/" + userId,
                UserDto.class);
    }

    /**
     * Fallback method when USER-SERVICE is unavailable or circuit is open
     * Returns a default UserDto with minimal information
     */
    private UserDto getUserByIdFallback(final Integer userId, Exception e) {
        log.warn("Fallback triggered for getUserById(userId={}). Reason: {}", userId, e.getMessage());
        
        // Return a default UserDto with minimal info
        UserDto fallbackUser = new UserDto();
        fallbackUser.setUserId(userId);
        fallbackUser.setFirstName("Unknown");
        fallbackUser.setLastName("User");
        fallbackUser.setEmail("unavailable@service.down");
        
        return fallbackUser;
    }
}
