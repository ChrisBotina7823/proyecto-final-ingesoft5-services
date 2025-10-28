package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.OrderItem;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.impl.OrderItemServiceImpl;

/**
 * Unit tests for OrderItemService (Shipping Service)
 * Tests order item management with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class OrderItemServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderItemServiceImpl orderItemService;

    private OrderItem testOrderItem;
    private OrderItemDto testOrderItemDto;
    private OrderItemId testOrderItemId;

    @BeforeEach
    void setUp() {
        testOrderItemId = new OrderItemId(1, 100);

        testOrderItem = OrderItem.builder()
                .productId(1)
                .orderId(100)
                .orderedQuantity(5)
                .build();

        testOrderItemDto = OrderItemDto.builder()
                .productId(1)
                .orderId(100)
                .orderedQuantity(5)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllOrderItems() {
        // Given
        List<OrderItem> orderItems = Arrays.asList(testOrderItem);
        when(orderItemRepository.findAll()).thenReturn(orderItems);

        // When
        List<OrderItemDto> result = orderItemService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProductId());
        assertEquals(5, result.get(0).getOrderedQuantity());
        verify(orderItemRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ShouldReturnOrderItem_WhenExists() {
        // Given
        when(orderItemRepository.findById(testOrderItemId)).thenReturn(Optional.of(testOrderItem));

        // When
        OrderItemDto result = orderItemService.findById(testOrderItemId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals(100, result.getOrderId());
        assertEquals(5, result.getOrderedQuantity());
        verify(orderItemRepository, times(1)).findById(testOrderItemId);
    }

    @Test
    void testSave_ShouldCreateNewOrderItem() {
        // Given
        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // When
        OrderItemDto result = orderItemService.save(testOrderItemDto);

        // Then
        assertNotNull(result);
        assertEquals(5, result.getOrderedQuantity());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void testUpdate_ShouldUpdateOrderItemQuantity() {
        // Given
        OrderItemDto updatedDto = OrderItemDto.builder()
                .productId(1)
                .orderId(100)
                .orderedQuantity(10)
                .build();

        when(orderItemRepository.save(any(OrderItem.class))).thenReturn(testOrderItem);

        // When
        OrderItemDto result = orderItemService.update(updatedDto);

        // Then
        assertNotNull(result);
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));
    }

    @Test
    void testDeleteById_ShouldDeleteOrderItem() {
        // Given
        doNothing().when(orderItemRepository).deleteById(testOrderItemId);

        // When
        orderItemService.deleteById(testOrderItemId);

        // Then
        verify(orderItemRepository, times(1)).deleteById(testOrderItemId);
    }
}
