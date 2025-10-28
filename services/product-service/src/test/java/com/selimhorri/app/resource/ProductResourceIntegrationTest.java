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
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;

/**
 * Integration tests for ProductResource
 * Tests product endpoints with real database interactions
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})
class ProductResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

        @MockBean
        private RestTemplate restTemplate;

    private ProductDto testProductDto;
    private CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        testCategoryDto = CategoryDto.builder()
                .categoryTitle("Test Category")
                .build();

        testProductDto = ProductDto.builder()
                .productTitle("Test Product")
                .sku("TEST-001")
                .priceUnit(99.99)
                .quantity(50)
                .categoryDto(testCategoryDto)
                .build();
    }

    @Test
    void testGetAllProducts_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/products")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection").isArray());
    }

    @Test
    void testCreateProduct_ShouldReturnCreatedProduct() throws Exception {
        String productJson = objectMapper.writeValueAsString(testProductDto);

        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle").value("Test Product"))
                .andExpect(jsonPath("$.sku").value("TEST-001"))
                .andExpect(jsonPath("$.priceUnit").value(99.99));
    }

    @Test
    void testGetProductById_ShouldReturnProduct_WhenExists() throws Exception {
        // Create product first
        String productJson = objectMapper.writeValueAsString(testProductDto);
        String response = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andReturn().getResponse().getContentAsString();

        ProductDto created = objectMapper.readValue(response, ProductDto.class);

        // Get by ID
        mockMvc.perform(get("/api/products/" + created.getProductId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(created.getProductId()))
                .andExpect(jsonPath("$.sku").value("TEST-001"));
    }

    @Test
    void testUpdateProduct_ShouldReturnUpdatedProduct() throws Exception {
        // Create product
        String productJson = objectMapper.writeValueAsString(testProductDto);
        String response = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andReturn().getResponse().getContentAsString();

        ProductDto created = objectMapper.readValue(response, ProductDto.class);

        // Update
        created.setProductTitle("Updated Product");
        created.setPriceUnit(149.99);
        String updatedJson = objectMapper.writeValueAsString(created);

        mockMvc.perform(put("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updatedJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productTitle").value("Updated Product"))
                .andExpect(jsonPath("$.priceUnit").value(149.99));
    }

    @Test
    void testDeleteProduct_ShouldReturnTrue() throws Exception {
        // Create product
        String productJson = objectMapper.writeValueAsString(testProductDto);
        String response = mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(productJson))
                .andReturn().getResponse().getContentAsString();

        ProductDto created = objectMapper.readValue(response, ProductDto.class);

        // Delete
        mockMvc.perform(delete("/api/products/" + created.getProductId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
