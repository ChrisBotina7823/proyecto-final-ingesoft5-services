package com.selimhorri.app.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.CartDto;

/**
 * Integration tests for OrderResource
 * Tests order management endpoints with database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})
class OrderResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
        private RestTemplate restTemplate;

    private OrderDto testOrderDto;
    private CartDto testCartDto;

    @BeforeEach
    void setUp() {
        testCartDto = CartDto.builder()
                .userId(200)
                .build();

        testOrderDto = OrderDto.builder()
                .orderDate(LocalDateTime.now())
                .orderDesc("Integration test order")
                .orderFee(599.99)
                .cartDto(testCartDto)
                .build();
    }

    @Test
    void testGetAllOrders_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray());
    }

    @Test
    void testCreateOrder_ShouldReturnCreatedOrder() throws Exception {
        String orderJson = objectMapper.writeValueAsString(testOrderDto);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderDesc").value("Integration test order"))
                .andExpect(jsonPath("$.orderFee").value(599.99));
    }

    @Test
    void testGetOrderById_ShouldReturnOrder_WhenExists() throws Exception {
        // Create order
        String orderJson = objectMapper.writeValueAsString(testOrderDto);
        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andReturn().getResponse().getContentAsString();

        OrderDto created = objectMapper.readValue(response, OrderDto.class);

        // Get by ID
        mockMvc.perform(get("/api/orders/" + created.getOrderId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(created.getOrderId()))
                .andExpect(jsonPath("$.orderFee").value(599.99));
    }

    @Test
    void testUpdateOrder_ShouldReturnUpdatedOrder() throws Exception {
        // Create order
        String orderJson = objectMapper.writeValueAsString(testOrderDto);
        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andReturn().getResponse().getContentAsString();

        OrderDto created = objectMapper.readValue(response, OrderDto.class);

        // Update
        created.setOrderDesc("Updated order description");
        created.setOrderFee(799.99);
        String updatedJson = objectMapper.writeValueAsString(created);

        mockMvc.perform(put("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderDesc").value("Updated order description"))
                .andExpect(jsonPath("$.orderFee").value(799.99));
    }

    @Test
    void testDeleteOrder_ShouldReturnTrue() throws Exception {
        // Create order
        String orderJson = objectMapper.writeValueAsString(testOrderDto);
        String response = mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andReturn().getResponse().getContentAsString();

        OrderDto created = objectMapper.readValue(response, OrderDto.class);

        // Delete
        mockMvc.perform(delete("/api/orders/" + created.getOrderId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
