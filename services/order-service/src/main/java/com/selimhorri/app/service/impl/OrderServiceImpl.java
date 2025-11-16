package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.selimhorri.app.config.feature.OrderFeatures;
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
					.map(OrderMappingHelper::map)
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public OrderDto findById(final Integer orderId) {
		log.info("*** OrderDto, service; fetch order by id *");
		return this.orderRepository.findById(orderId)
				.map(OrderMappingHelper::map)
				.orElseThrow(() -> new OrderNotFoundException(String
						.format("Order with id: %d not found", orderId)));
	}
	
	@Override
	public OrderDto save(final OrderDto orderDto) {
		log.info("*** OrderDto, service; save order *");
		
		// Feature Toggle: Enable order validation
		if (OrderFeatures.ORDER_VALIDATION_ENABLED.isActive()) {
			log.info("*** Order validation enabled - validating order data *");
			// TODO: Implement order validation logic
			// Validate product availability, prices, user data, etc.
		}
		
		// Feature Toggle: Enable automatic inventory reservation
		if (OrderFeatures.AUTO_INVENTORY_RESERVATION.isActive()) {
			log.info("*** Auto inventory reservation enabled - reserving items *");
			// TODO: Call product service to reserve inventory
		}
		
		// Feature Toggle: Use SAGA orchestrated pattern
		if (OrderFeatures.SAGA_ORCHESTRATED.isActive()) {
			log.info("*** SAGA orchestrated pattern enabled - starting saga *");
			// TODO: Implement SAGA orchestrator
			// 1. Reserve inventory
			// 2. Process payment
			// 3. Arrange shipping
			// With compensation logic if any step fails
		} else {
			log.info("*** Using standard order processing *");
		}
		
		// Feature Toggle: Two-step checkout process
		if (OrderFeatures.TWO_STEP_CHECKOUT.isActive()) {
			log.info("*** Two-step checkout enabled - order will require confirmation *");
			// TODO: Set order status to PENDING_CONFIRMATION
		}
		
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
	
	
	
}










