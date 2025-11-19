package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.helper.PaymentMappingHelper;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.PaymentService;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
	
	private final PaymentRepository paymentRepository;
	private final RestTemplate restTemplate;
	private final MeterRegistry meterRegistry;
	
	@Override
	public List<PaymentDto> findAll() {
		log.info("*** PaymentDto List, service; fetch all payments *");
		return this.paymentRepository.findAll()
				.stream()
					.map(PaymentMappingHelper::map)
					.map(p -> {
						p.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
								.ORDER_SERVICE_API_URL + "/" + p.getOrderDto().getOrderId(), OrderDto.class));
						return p;
					})
					.distinct()
					.collect(Collectors.toUnmodifiableList());
	}
	
	@Override
	public PaymentDto findById(final Integer paymentId) {
		log.info("*** PaymentDto, service; fetch payment by id *");
		return this.paymentRepository.findById(paymentId)
				.map(PaymentMappingHelper::map)
				.map(p -> {
					p.setOrderDto(this.restTemplate.getForObject(AppConstant.DiscoveredDomainsApi
							.ORDER_SERVICE_API_URL + "/" + p.getOrderDto().getOrderId(), OrderDto.class));
					return p;
				})
				.orElseThrow(() -> new PaymentNotFoundException(String.format("Payment with id: %d not found", paymentId)));
	}
	
	@Override
	public PaymentDto save(final PaymentDto paymentDto) {
		log.info("*** PaymentDto, service; save payment *");
		
		// Business metric: Track payments by status
		if (paymentDto.getPaymentStatus() != null) {
			Counter.builder("payments_processed_total")
					.description("Total number of payments processed")
					.tag("application", "payment-service")
					.tag("status", paymentDto.getPaymentStatus().name())
					.register(meterRegistry)
					.increment();
		}
		
		// Business metric: Track payment volume
		if (paymentDto.getOrderDto() != null && paymentDto.getOrderDto().getOrderFee() != null) {
			DistributionSummary.builder("payment_volume_dollars")
					.description("Distribution of payment volumes in dollars")
					.tag("application", "payment-service")
					.tag("status", paymentDto.getPaymentStatus() != null ? paymentDto.getPaymentStatus().name() : "UNKNOWN")
					.register(meterRegistry)
					.record(paymentDto.getOrderDto().getOrderFee());
		}
		
		return PaymentMappingHelper.map(this.paymentRepository
				.save(PaymentMappingHelper.map(paymentDto)));
	}
	
	@Override
	public PaymentDto update(final PaymentDto paymentDto) {
		log.info("*** PaymentDto, service; update payment *");
		return PaymentMappingHelper.map(this.paymentRepository
				.save(PaymentMappingHelper.map(paymentDto)));
	}
	
	@Override
	public void deleteById(final Integer paymentId) {
		log.info("*** Void, service; delete payment by id *");
		this.paymentRepository.deleteById(paymentId);
	}
	
	
	
}









