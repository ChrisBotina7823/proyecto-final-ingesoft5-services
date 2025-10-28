package com.selimhorri.app.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakersHealthIndicatorAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.OrderItemDto;

/**
 * Integration tests for OrderItemResource (Shipping)
 * Tests order item shipping endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})
class OrderItemResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
        private RestTemplate restTemplate;

    private OrderItemDto testOrderItemDto;

    @BeforeEach
    void setUp() {
        testOrderItemDto = OrderItemDto.builder()
                .productId(10)
                .orderId(200)
                .orderedQuantity(3)
                .build();
    }

    @Test
    void testGetAllOrderItems_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray());
    }

    @Test
    void testCreateOrderItem_ShouldReturnCreatedItem() throws Exception {
        String orderItemJson = objectMapper.writeValueAsString(testOrderItemDto);

        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderItemJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.orderId").value(200))
                .andExpect(jsonPath("$.orderedQuantity").value(3));
    }

    @Test
    void testGetOrderItemById_ShouldReturnItem_WhenExists() throws Exception {
        // Create order item
        String orderItemJson = objectMapper.writeValueAsString(testOrderItemDto);
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderItemJson))
                .andExpect(status().isOk());

        // Get by composite ID
        mockMvc.perform(get("/api/shippings/10/200")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.orderId").value(200))
                .andExpect(jsonPath("$.orderedQuantity").value(3));
    }

    @Test
    void testUpdateOrderItem_ShouldReturnUpdatedItem() throws Exception {
        // Create order item
        String orderItemJson = objectMapper.writeValueAsString(testOrderItemDto);
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderItemJson))
                .andExpect(status().isOk());

        // Update quantity
        testOrderItemDto.setOrderedQuantity(7);
        String updatedJson = objectMapper.writeValueAsString(testOrderItemDto);

        mockMvc.perform(put("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderedQuantity").value(7));
    }

    @Test
    void testDeleteOrderItem_ShouldReturnTrue() throws Exception {
        // Create order item
        String orderItemJson = objectMapper.writeValueAsString(testOrderItemDto);
        mockMvc.perform(post("/api/shippings")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderItemJson))
                .andExpect(status().isOk());

        // Delete
        mockMvc.perform(delete("/api/shippings/10/200")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
