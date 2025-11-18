package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.config.OrderFeatureConfig;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
	
	private final OrderRepository orderRepository;
	
	@Override
	public List<OrderDto> findAll() {
		log.info("*** OrderDto List, service; fetch all orders *");
		return this.orderRepository.findAll()
				.stream()
					.map(order -> {
						OrderDto dto = OrderMappingHelper.map(order);
						applyFeatureToggles(dto);
						return dto;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderDto findById(final Integer orderId) {
		log.info("*** OrderDto, service; fetch order by id *");
		OrderDto dto = this.orderRepository.findById(orderId)
				.map(OrderMappingHelper::map)
				.orElseThrow(() -> new OrderNotFoundException(String
						.format("Order with id: %d not found", orderId)));
		applyFeatureToggles(dto);
		return dto;
	}
	
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("*** OrderDto, service; save order *");
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
	}
	
	@Override
	public OrderDto update(final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order *");
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(orderDto)));
	}
	
	@Override
	public OrderDto update(final Integer orderId, final OrderDto orderDto) {
		log.info("*** OrderDto, service; update order with orderId *");
		return OrderMappingHelper.map(this.orderRepository
				.save(OrderMappingHelper.map(this.findById(orderId))));
	}
	
	@Override
	public void deleteById(final Integer orderId) {
		log.info("*** Void, service; delete order by id *");
		this.orderRepository.delete(OrderMappingHelper.map(this.findById(orderId)));
	}
	
	/**
	 * Apply feature toggles to OrderDto using Togglz
	 * Feature: PRIORITY_FIELD - Adds priority field to orders
	 */
	private void applyFeatureToggles(OrderDto orderDto) {
		if (OrderFeatureConfig.PRIORITY_FIELD.isActive()) {
			// Calculate priority based on order fee
			String priority = calculatePriority(orderDto.getOrderFee());
			orderDto.setPriority(priority);
			log.debug("Togglz Feature: Priority field enabled - Order {} has priority: {}", 
					orderDto.getOrderId(), priority);
		} else {
			log.debug("Togglz Feature: Priority field disabled");
		}
	}
	
	private String calculatePriority(Double orderFee) {
		if (orderFee == null) return "STANDARD";
		if (orderFee > 1000) return "HIGH";
		if (orderFee > 500) return "MEDIUM";
		return "STANDARD";
	}
	
}










