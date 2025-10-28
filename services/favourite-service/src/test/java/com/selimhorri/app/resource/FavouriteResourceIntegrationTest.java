package com.selimhorri.app.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import io.github.resilience4j.circuitbreaker.autoconfigure.CircuitBreakersHealthIndicatorAutoConfiguration;

/**
 * Integration tests for FavouriteResource endpoints
 * Uses real database (H2) to test complete request/response cycle
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@EnableAutoConfiguration(exclude = {CircuitBreakersHealthIndicatorAutoConfiguration.class})
class FavouriteResourceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void testGetAllFavourites_ShouldReturnOk() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testCreateFavourite_ShouldAddToWishlist() throws Exception {
        // Given
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(5)
                .productId(50)
                .likeDate(LocalDateTime.now())
                .build();

        // When & Then
        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.productId").value(50))
                .andExpect(jsonPath("$.likeDate").exists());
    }

    @Test
    void testGetFavouriteById_ShouldReturnFavourite_WhenExists() throws Exception {
        // Given - create a favourite first with a fixed timestamp
        LocalDateTime fixedLikeDate = LocalDateTime.of(2024, 10, 28, 12, 0, 0, 0);
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(6)
                .productId(60)
                .likeDate(fixedLikeDate)
                .build();

        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)));

        // Mock RestTemplate responses for user and product services
        UserDto mockUser = new UserDto();
        mockUser.setUserId(6);
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        
        ProductDto mockProduct = new ProductDto();
        mockProduct.setProductId(60);
        mockProduct.setProductTitle("Test Product");
        
        when(restTemplate.getForObject(contains("user-service"), eq(UserDto.class)))
                .thenReturn(mockUser);
        when(restTemplate.getForObject(contains("product-service"), eq(ProductDto.class)))
                .thenReturn(mockProduct);

        // When & Then - use all 3 parts of composite key with correct format: dd-MM-yyyy__HH:mm:ss:SSSSSS
        String formattedDate = "28-10-2024__12:00:00:000000";
        mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}", 6, 60, formattedDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(6))
                .andExpect(jsonPath("$.productId").value(60))
                .andExpect(jsonPath("$.likeDate").exists());
    }

    @Test
    void testUpdateFavourite_ShouldUpdateLikeDate() throws Exception {
        // Given - create a favourite
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(7)
                .productId(70)
                .likeDate(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)));

        // Update the likeDate
        FavouriteDto updatedFavourite = FavouriteDto.builder()
                .userId(7)
                .productId(70)
                .likeDate(LocalDateTime.now().plusDays(1))
                .build();

        // When & Then
        mockMvc.perform(put("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedFavourite)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.productId").value(70));
    }

    @Test
    void testDeleteFavourite_ShouldRemoveFromWishlist() throws Exception {
        // Given - create a favourite with fixed timestamp
        LocalDateTime fixedLikeDate = LocalDateTime.of(2024, 10, 28, 13, 0, 0, 0);
        FavouriteDto newFavourite = FavouriteDto.builder()
                .userId(8)
                .productId(80)
                .likeDate(fixedLikeDate)
                .build();

        mockMvc.perform(post("/api/favourites")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newFavourite)));

        // When & Then - use all 3 parts of composite key with correct format: dd-MM-yyyy__HH:mm:ss:SSSSSS
        String formattedDate = "28-10-2024__13:00:00:000000";
        mockMvc.perform(delete("/api/favourites/{userId}/{productId}/{likeDate}", 8, 80, formattedDate)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
