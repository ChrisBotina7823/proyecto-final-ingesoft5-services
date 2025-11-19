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

import com.selimhorri.app.domain.Product;
import com.selimhorri.app.domain.Category;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.helper.ProductMappingHelper;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.repository.CategoryRepository;
import com.selimhorri.app.service.impl.ProductServiceImpl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Unit tests for ProductService
 * Tests product CRUD operations with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product testProduct;
    private ProductDto testProductDto;
    private Category testCategory;
    private CategoryDto testCategoryDto;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        testCategoryDto = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Electronics")
                .build();

        testProduct = Product.builder()
                .productId(1)
                .productTitle("Laptop")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(10)
                .category(testCategory)
                .build();

        testProductDto = ProductDto.builder()
                .productId(1)
                .productTitle("Laptop")
                .sku("LAP-001")
                .priceUnit(999.99)
                .quantity(10)
                .categoryDto(testCategoryDto)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllProducts() {
        // Given
        List<Product> products = Arrays.asList(testProduct);
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<ProductDto> result = productService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Laptop", result.get(0).getProductTitle());
        verify(productRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ShouldReturnProduct_WhenExists() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));

        // When
        ProductDto result = productService.findById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getProductId());
        assertEquals("LAP-001", result.getSku());
        assertEquals(999.99, result.getPriceUnit());
        verify(productRepository, times(1)).findById(1);
    }

    @Test
    void testSave_ShouldCreateNewProduct() {
        // Given
        Counter mockCounter = mock(Counter.class);
        when(meterRegistry.counter(any())).thenReturn(mockCounter);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDto result = productService.save(testProductDto);

        // Then
        assertNotNull(result);
        assertEquals("Laptop", result.getProductTitle());
        assertEquals(10, result.getQuantity());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testUpdate_ShouldUpdateExistingProduct() {
        // Given
        ProductDto updatedDto = ProductDto.builder()
                .productId(1)
                .productTitle("Gaming Laptop")
                .sku("LAP-001")
                .priceUnit(1299.99)
                .quantity(5)
                .categoryDto(testCategoryDto)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductDto result = productService.update(updatedDto);

        // Then
        assertNotNull(result);
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void testDeleteById_ShouldDeleteProduct() {
        // Given
        when(productRepository.findById(1)).thenReturn(Optional.of(testProduct));
        doNothing().when(productRepository).delete(any(Product.class));

        // When
        productService.deleteById(1);

        // Then
        verify(productRepository, times(1)).findById(1);
        verify(productRepository, times(1)).delete(any(Product.class));
    }
}
