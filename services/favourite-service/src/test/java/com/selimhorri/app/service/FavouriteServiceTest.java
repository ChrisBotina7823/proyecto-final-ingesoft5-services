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
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.helper.FavouriteMappingHelper;
import com.selimhorri.app.repository.FavouriteRepository;
import com.selimhorri.app.service.impl.FavouriteServiceImpl;

/**
 * Unit tests for FavouriteService
 * Tests favourite (wishlist) management with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class FavouriteServiceTest {

    @Mock
    private FavouriteRepository favouriteRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private FavouriteServiceImpl favouriteService;

    private Favourite testFavourite;
    private FavouriteDto testFavouriteDto;
    private FavouriteId testFavouriteId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        testFavouriteId = new FavouriteId(1, 100, now);

        testFavourite = Favourite.builder()
                .userId(1)
                .productId(100)
                .likeDate(now)
                .build();

        testFavouriteDto = FavouriteDto.builder()
                .userId(1)
                .productId(100)
                .likeDate(now)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllFavourites() {
        // Given
        List<Favourite> favourites = Arrays.asList(testFavourite);
        when(favouriteRepository.findAll()).thenReturn(favourites);

        // When
        List<FavouriteDto> result = favouriteService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getUserId());
        assertEquals(100, result.get(0).getProductId());
        verify(favouriteRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ShouldReturnFavourite_WhenExists() {
        // Given
        when(favouriteRepository.findById(testFavouriteId)).thenReturn(Optional.of(testFavourite));

        // When
        FavouriteDto result = favouriteService.findById(testFavouriteId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(100, result.getProductId());
        assertNotNull(result.getLikeDate());
        verify(favouriteRepository, times(1)).findById(testFavouriteId);
    }

    @Test
    void testSave_ShouldAddToFavourites() {
        // Given
        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);

        // When
        FavouriteDto result = favouriteService.save(testFavouriteDto);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getUserId());
        assertEquals(100, result.getProductId());
        verify(favouriteRepository, times(1)).save(any(Favourite.class));
    }

    @Test
    void testUpdate_ShouldUpdateFavourite() {
        // Given
        FavouriteDto updatedDto = FavouriteDto.builder()
                .userId(1)
                .productId(100)
                .likeDate(LocalDateTime.now().plusDays(1))
                .build();

        when(favouriteRepository.save(any(Favourite.class))).thenReturn(testFavourite);

        // When
        FavouriteDto result = favouriteService.update(updatedDto);

        // Then
        assertNotNull(result);
        assertNotNull(result.getLikeDate());
        verify(favouriteRepository, times(1)).save(any(Favourite.class));
    }

    @Test
    void testDeleteById_ShouldRemoveFromFavourites() {
        // Given
        doNothing().when(favouriteRepository).deleteById(testFavouriteId);

        // When
        favouriteService.deleteById(testFavouriteId);

        // Then
        verify(favouriteRepository, times(1)).deleteById(testFavouriteId);
    }
}
