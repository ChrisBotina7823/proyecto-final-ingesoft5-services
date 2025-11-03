package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;

public interface OrderMappingHelper {
	
	public static OrderDto map(final Order order) {
		return OrderDto.builder()
				.orderId(order.getOrderId())
				.orderDate(order.getOrderDate())
				.orderDesc(order.getOrderDesc())
				.orderFee(order.getOrderFee())
				.cartDto(
						CartDto.builder()
							.cartId(order.getCart().getCartId())
							.build())
				.build();
	}
	
	public static Order map(final OrderDto orderDto) {
		Order.OrderBuilder orderBuilder = Order.builder()
				.orderDate(orderDto.getOrderDate())
				.orderDesc(orderDto.getOrderDesc())
				.orderFee(orderDto.getOrderFee());
		
		// Solo establecer orderId si NO es null (para updates)
		if (orderDto.getOrderId() != null) {
			orderBuilder.orderId(orderDto.getOrderId());
		}
		
		// Crear Cart builder
		Cart.CartBuilder cartBuilder = Cart.builder();
		if (orderDto.getCartDto() != null && orderDto.getCartDto().getCartId() != null) {
			cartBuilder.cartId(orderDto.getCartDto().getCartId());
		}
		
		orderBuilder.cart(cartBuilder.build());
		
		return orderBuilder.build();
	}
	
	
	
}










