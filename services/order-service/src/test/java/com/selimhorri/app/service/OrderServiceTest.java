package com.selimhorri.app.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.impl.OrderServiceImpl;

/**
 * Unit tests for OrderService
 * Tests order management with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order testOrder;
    private OrderDto testOrderDto;
    private Cart testCart;
    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        testCart = Cart.builder()
                .cartId(1)
                .userId(100)
                .build();

        testCartDto = CartDto.builder()
                .cartId(1)
                .userId(100)
                .build();

        testOrder = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(299.99)
                .cart(testCart)
                .build();

        testOrderDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Test order")
                .orderFee(299.99)
                .cartDto(testCartDto)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllOrders() {
        // Given
        List<Order> orders = Arrays.asList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // When
        List<OrderDto> result = orderService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test order", result.get(0).getOrderDesc());
        assertEquals(299.99, result.get(0).getOrderFee());
        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ShouldReturnOrder_WhenExists() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));

        // When
        OrderDto result = orderService.findById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getOrderId());
        assertEquals(299.99, result.getOrderFee());
        verify(orderRepository, times(1)).findById(1);
    }

    @Test
    void testSave_ShouldCreateNewOrder() {
        // Given
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.save(testOrderDto);

        // Then
        assertNotNull(result);
        assertEquals("Test order", result.getOrderDesc());
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testUpdate_ShouldUpdateExistingOrder() {
        // Given
        OrderDto updatedDto = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now())
                .orderDesc("Updated order")
                .orderFee(399.99)
                .cartDto(testCartDto)
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // When
        OrderDto result = orderService.update(updatedDto);

        // Then
        assertNotNull(result);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void testDeleteById_ShouldDeleteOrder() {
        // Given
        when(orderRepository.findById(1)).thenReturn(Optional.of(testOrder));
        doNothing().when(orderRepository).delete(any(Order.class));

        // When
        orderService.deleteById(1);

        // Then
        verify(orderRepository, times(1)).findById(1);
        verify(orderRepository, times(1)).delete(any(Order.class));
    }
}
