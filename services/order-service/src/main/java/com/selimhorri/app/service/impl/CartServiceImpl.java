package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.helper.CartMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.service.CartService;

import com.selimhorri.app.client.UserServiceClient;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
	
	private final CartRepository cartRepository;
	private final RestTemplate restTemplate;
	private final UserServiceClient userServiceClient;
	private final MeterRegistry meterRegistry;
	
	@Override
	public List<CartDto> findAll() {
		log.info("*** CartDto List, service; fetch all carts *");
		return this.cartRepository.findAll()
				.stream()
					.map(CartMappingHelper::map)
					.map(c -> {
						c.setUserDto(this.userServiceClient.getUserById(c.getUserDto().getUserId()));
						return c;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public CartDto findById(final Integer cartId) {
		log.info("*** CartDto, service; fetch cart by id *");
		return this.cartRepository.findById(cartId)
				.map(CartMappingHelper::map)
				.map(c -> {
		    c.setUserDto(this.userServiceClient.getUserById(c.getUserDto().getUserId()));
					return c;
				})
				.orElseThrow(() -> new CartNotFoundException(String
						.format("Cart with id: %d not found", cartId)));
	}
	
	@Override
	public CartDto save(final CartDto cartDto) {
		log.info("*** CartDto, service; save cart *");
		return CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(cartDto)));
	}
	
	@Override
	public CartDto update(final CartDto cartDto) {
		log.info("*** CartDto, service; update cart *");
		return CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(cartDto)));
	}
	
	@Override
	public CartDto update(final Integer cartId, final CartDto cartDto) {
		log.info("*** CartDto, service; update cart with cartId *");
		return CartMappingHelper.map(this.cartRepository
				.save(CartMappingHelper.map(this.findById(cartId))));
	}
	
	@Override
	public void deleteById(final Integer cartId) {
		log.info("*** Void, service; delete cart by id *");
		
		// Business metric: Track cart deletions (potential abandonment indicator)
		Counter.builder("carts_deleted_total")
				.description("Total number of carts deleted (potential abandonment)")
				.tag("application", "order-service")
				.register(meterRegistry)
				.increment();
		
		this.cartRepository.deleteById(cartId);
	}
	
	
	
}










