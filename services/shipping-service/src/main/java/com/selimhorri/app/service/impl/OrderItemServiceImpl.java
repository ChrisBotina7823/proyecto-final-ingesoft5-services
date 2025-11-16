package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.OrderItemId;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.OrderItemDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.exception.wrapper.OrderItemNotFoundException;
import com.selimhorri.app.helper.OrderItemMappingHelper;
import com.selimhorri.app.repository.OrderItemRepository;
import com.selimhorri.app.service.OrderItemService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {
	
	private final OrderItemRepository orderItemRepository;
	private final RestTemplate restTemplate;
	
	@Override
	public List<OrderItemDto> findAll() {
		log.info("*** OrderItemDto List, service; fetch all orderItems *");
		return this.orderItemRepository.findAll()
				.stream()
					.map(OrderItemMappingHelper::map)
					.map(o -> {
						o.setProductDto(getProductWithResilience(o.getProductDto().getProductId()));
						o.setOrderDto(getOrderWithResilience(o.getOrderDto().getOrderId()));
						return o;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@CircuitBreaker(name = "productServiceCall", fallbackMethod = "getProductFallback")
	@Retry(name = "productServiceCall")
	private ProductDto getProductWithResilience(Integer productId) {
		log.info("*** Calling Product Service for productId: {} *", productId);
		return this.restTemplate.getForObject(
			AppConstant.DiscoveredDomainsApi.PRODUCT_SERVICE_API_URL + "/" + productId, 
			ProductDto.class
		);
	}
	
	@CircuitBreaker(name = "orderServiceCall", fallbackMethod = "getOrderFallback")
	@Retry(name = "orderServiceCall")
	private OrderDto getOrderWithResilience(Integer orderId) {
		log.info("*** Calling Order Service for orderId: {} *", orderId);
		return this.restTemplate.getForObject(
			AppConstant.DiscoveredDomainsApi.ORDER_SERVICE_API_URL + "/" + orderId, 
			OrderDto.class
		);
	}
	
	private ProductDto getProductFallback(Integer productId, Throwable t) {
		log.error("*** Product Service unavailable for productId: {}. Using fallback. Error: {} *", 
			productId, t.getMessage());
		ProductDto fallback = new ProductDto();
		fallback.setProductId(productId);
		fallback.setProductTitle("Product Unavailable");
		return fallback;
	}
	
	private OrderDto getOrderFallback(Integer orderId, Throwable t) {
		log.error("*** Order Service unavailable for orderId: {}. Using fallback. Error: {} *", 
			orderId, t.getMessage());
		OrderDto fallback = new OrderDto();
		fallback.setOrderId(orderId);
		return fallback;
	}
	
	@Override
	public OrderItemDto findById(final OrderItemId orderItemId) {
		log.info("*** OrderItemDto, service; fetch orderItem by id *");
		return this.orderItemRepository.findById(orderItemId)
				.map(OrderItemMappingHelper::map)
				.map(o -> {
					o.setProductDto(getProductWithResilience(o.getProductDto().getProductId()));
					o.setOrderDto(getOrderWithResilience(o.getOrderDto().getOrderId()));
					return o;
				})
				.orElseThrow(() -> new OrderItemNotFoundException(String.format("OrderItem with id: %s not found", orderItemId)));
	}
	
	@Override
	public OrderItemDto save(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; save orderItem *");
		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
	}
	
	@Override
	public OrderItemDto update(final OrderItemDto orderItemDto) {
		log.info("*** OrderItemDto, service; update orderItem *");
		return OrderItemMappingHelper.map(this.orderItemRepository
				.save(OrderItemMappingHelper.map(orderItemDto)));
	}
	
	@Override
	public void deleteById(final OrderItemId orderItemId) {
		log.info("*** Void, service; delete orderItem by id *");
		this.orderItemRepository.deleteById(orderItemId);
	}
	
	
	
}









