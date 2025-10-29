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
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.dto.OrderDto;

/**
 * Integration tests for PaymentResource
 * Tests payment processing endpoints
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})
class PaymentResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
        private RestTemplate restTemplate;

    private PaymentDto testPaymentDto;
    private OrderDto testOrderDto;

    @BeforeEach
    void setUp() {
        testOrderDto = OrderDto.builder()
                .orderId(100)
                .orderDesc("Test order")
                .orderFee(299.99)
                .build();

        testPaymentDto = PaymentDto.builder()
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .orderDto(testOrderDto)
                .build();
    }

    @Test
    void testGetAllPayments_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/payments")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray());
    }

    @Test
    void testCreatePayment_ShouldReturnCreatedPayment() throws Exception {
        String paymentJson = objectMapper.writeValueAsString(testPaymentDto);

        mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));
    }

    @Test
    void testGetPaymentById_ShouldReturnPayment_WhenExists() throws Exception {
        // Create payment
        String paymentJson = objectMapper.writeValueAsString(testPaymentDto);
        String response = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andReturn().getResponse().getContentAsString();

        PaymentDto created = objectMapper.readValue(response, PaymentDto.class);

        // Get by ID
        mockMvc.perform(get("/api/payments/" + created.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(created.getPaymentId()));
    }

    @Test
    void testUpdatePayment_ShouldProcessPayment() throws Exception {
        // Create payment
        String paymentJson = objectMapper.writeValueAsString(testPaymentDto);
        String response = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andReturn().getResponse().getContentAsString();

        PaymentDto created = objectMapper.readValue(response, PaymentDto.class);

        // Process payment
        created.setIsPayed(true);
        created.setPaymentStatus(PaymentStatus.COMPLETED);
        String updatedJson = objectMapper.writeValueAsString(created);

        mockMvc.perform(put("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    void testDeletePayment_ShouldReturnTrue() throws Exception {
        // Create payment
        String paymentJson = objectMapper.writeValueAsString(testPaymentDto);
        String response = mockMvc.perform(post("/api/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(paymentJson))
                .andReturn().getResponse().getContentAsString();

        PaymentDto created = objectMapper.readValue(response, PaymentDto.class);

        // Delete
        mockMvc.perform(delete("/api/payments/" + created.getPaymentId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
