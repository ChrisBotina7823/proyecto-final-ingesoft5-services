package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.helper.ProductMappingHelper;
import com.selimhorri.app.repository.ProductRepository;
import com.selimhorri.app.service.ProductService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
	
	private final ProductRepository productRepository;
	private final MeterRegistry meterRegistry;
	
	@Override
	public List<ProductDto> findAll() {
		log.info("*** ProductDto List, service; fetch all products *");
		return this.productRepository.findAll()
				.stream()
					.map(ProductMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public ProductDto findById(final Integer productId) {
		log.info("*** ProductDto, service; fetch product by id *");
		
		// Business metric: Track product views
		Counter.builder("product_views_total")
				.description("Total number of product views")
				.tag("application", "product-service")
				.tag("product_id", String.valueOf(productId))
				.register(meterRegistry)
				.increment();
		
		return this.productRepository.findById(productId)
				.map(ProductMappingHelper::map)
				.orElseThrow(() -> new ProductNotFoundException(String.format("Product with id: %d not found", productId)));
	}
	
	@Override
	public ProductDto save(final ProductDto productDto) {
		log.info("*** ProductDto, service; save product *");
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(productDto)));
	}
	
	@Override
	public ProductDto update(final ProductDto productDto) {
		log.info("*** ProductDto, service; update product *");
		
		// Business metric: Track stock levels with gauge (for low stock alerts)
		if (productDto.getQuantity() != null) {
			Gauge.builder("product_stock_gauge", productDto, p -> p.getQuantity() != null ? p.getQuantity() : 0)
					.description("Current stock level for product")
					.tag("application", "product-service")
					.tag("product_id", String.valueOf(productDto.getProductId()))
					.tag("product_title", productDto.getProductTitle() != null ? productDto.getProductTitle() : "unknown")
					.register(meterRegistry);
		}
		
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(productDto)));
	}
	
	@Override
	public ProductDto update(final Integer productId, final ProductDto productDto) {
		log.info("*** ProductDto, service; update product with productId *");
		return ProductMappingHelper.map(this.productRepository
				.save(ProductMappingHelper.map(this.findById(productId))));
	}
	
	@Override
	public void deleteById(final Integer productId) {
		log.info("*** Void, service; delete product by id *");
		this.productRepository.delete(ProductMappingHelper
				.map(this.findById(productId)));
	}
	
	
	
}









