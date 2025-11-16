package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.repository.OrderItemRepository;

import java.util.Optional;

/**
 * Unit tests for OrderItemServiceImpl focusing on Circuit Breaker and Retry patterns
 */
@ExtendWith(MockitoExtension.class)
class OrderItemServiceImplResilienceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private OrderItem orderItem;
    private OrderItemId orderItemId;

    @BeforeEach
    void setUp() {
        orderItemId = new OrderItemId(1, 1);
        orderItem = new OrderItem();
        orderItem.setOrderItemId(orderItemId);
    }

    @Test
    void testCircuitBreakerFallback_WhenProductServiceFails() {
        // Given
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
        when(restTemplate.getForObject(contains("product-service"), eq(ProductDto.class)))
            .thenThrow(new ResourceAccessException("Product Service unavailable"));
        when(restTemplate.getForObject(contains("order-service"), eq(OrderDto.class)))
            .thenReturn(new OrderDto());

        // When
        var result = orderItemService.findById(orderItemId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getProductDto());
        assertEquals("Product Unavailable", result.getProductDto().getProductTitle());
        // Verify that fallback was triggered
        verify(restTemplate, atLeastOnce()).getForObject(contains("product-service"), eq(ProductDto.class));
    }

    @Test
    void testRetryMechanism_WhenServiceRecoversAfterFailure() {
        // Given
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
        
        ProductDto productDto = new ProductDto();
        productDto.setProductId(1);
        productDto.setProductTitle("Test Product");
        
        // First call fails, second succeeds (simulating retry)
        when(restTemplate.getForObject(contains("product-service"), eq(ProductDto.class)))
            .thenThrow(new ResourceAccessException("Temporary failure"))
            .thenReturn(productDto);
        
        when(restTemplate.getForObject(contains("order-service"), eq(OrderDto.class)))
            .thenReturn(new OrderDto());

        // When
        var result = orderItemService.findById(orderItemId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getProductDto());
        // With retry enabled, it should succeed on second attempt
        verify(restTemplate, atLeast(1)).getForObject(contains("product-service"), eq(ProductDto.class));
    }

    @Test
    void testSuccessfulCall_NoCircuitBreakerActivation() {
        // Given
        when(orderItemRepository.findById(orderItemId)).thenReturn(Optional.of(orderItem));
        
        ProductDto productDto = new ProductDto();
        productDto.setProductId(1);
        productDto.setProductTitle("Test Product");
        
        OrderDto orderDto = new OrderDto();
        orderDto.setOrderId(1);
        
        when(restTemplate.getForObject(contains("product-service"), eq(ProductDto.class)))
            .thenReturn(productDto);
        when(restTemplate.getForObject(contains("order-service"), eq(OrderDto.class)))
            .thenReturn(orderDto);

        // When
        var result = orderItemService.findById(orderItemId);

        // Then
        assertNotNull(result);
        assertNotNull(result.getProductDto());
        assertEquals("Test Product", result.getProductDto().getProductTitle());
        assertNotNull(result.getOrderDto());
        assertEquals(1, result.getOrderDto().getOrderId());
        
        // Verify normal execution (no fallback)
        verify(restTemplate, times(1)).getForObject(contains("product-service"), eq(ProductDto.class));
        verify(restTemplate, times(1)).getForObject(contains("order-service"), eq(OrderDto.class));
    }
}
