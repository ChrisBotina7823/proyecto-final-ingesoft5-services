package com.selimhorri.app.helper;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;

public interface PaymentMappingHelper {
	
	public static PaymentDto map(final Payment payment) {
		return PaymentDto.builder()
				.paymentId(payment.getPaymentId())
				.isPayed(payment.getIsPayed())
				.paymentStatus(payment.getPaymentStatus())
				.orderDto(
						OrderDto.builder()
							.orderId(payment.getOrderId())
							.build())
				.build();
	}
	
	public static Payment map(final PaymentDto paymentDto) {
		Payment.PaymentBuilder paymentBuilder = Payment.builder()
				.orderId(paymentDto.getOrderDto().getOrderId())
				.isPayed(paymentDto.getIsPayed())
				.paymentStatus(paymentDto.getPaymentStatus());
		
		// Solo establecer paymentId si NO es null (para updates)
		if (paymentDto.getPaymentId() != null) {
			paymentBuilder.paymentId(paymentDto.getPaymentId());
		}
		
		return paymentBuilder.build();
	}
	
	
	
}










