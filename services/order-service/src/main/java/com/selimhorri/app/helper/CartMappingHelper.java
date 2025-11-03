package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;

public interface CartMappingHelper {
	
	public static CartDto map(final Cart cart) {
		return CartDto.builder()
				.cartId(cart.getCartId())
				.userId(cart.getUserId())
				.userDto(
						UserDto.builder()
							.userId(cart.getUserId())
							.build())
				.build();
	}
	
	public static Cart map(final CartDto cartDto) {
		Cart.CartBuilder cartBuilder = Cart.builder()
				.userId(cartDto.getUserId());
		
		// Solo establecer cartId si NO es null (para updates)
		if (cartDto.getCartId() != null) {
			cartBuilder.cartId(cartDto.getCartId());
		}
		
		return cartBuilder.build();
	}
	
	
	
}










