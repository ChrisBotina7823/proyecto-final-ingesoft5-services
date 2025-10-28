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

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.helper.PaymentMappingHelper;
import com.selimhorri.app.repository.PaymentRepository;
import com.selimhorri.app.service.impl.PaymentServiceImpl;

/**
 * Unit tests for PaymentService
 * Tests payment processing with mocked dependencies
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment testPayment;
    private PaymentDto testPaymentDto;

    @BeforeEach
    void setUp() {
        testPayment = Payment.builder()
                .paymentId(1)
                .orderId(100)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        testPaymentDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();
    }

    @Test
    void testFindAll_ShouldReturnAllPayments() {
        // Given
        List<Payment> payments = Arrays.asList(testPayment);
        when(paymentRepository.findAll()).thenReturn(payments);

        // When
        List<PaymentDto> result = paymentService.findAll();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(PaymentStatus.NOT_STARTED, result.get(0).getPaymentStatus());
        assertFalse(result.get(0).getIsPayed());
        verify(paymentRepository, times(1)).findAll();
    }

    @Test
    void testFindById_ShouldReturnPayment_WhenExists() {
        // Given
        when(paymentRepository.findById(1)).thenReturn(Optional.of(testPayment));

        // When
        PaymentDto result = paymentService.findById(1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getPaymentId());
        verify(paymentRepository, times(1)).findById(1);
    }

    @Test
    void testSave_ShouldCreateNewPayment() {
        // Given
        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        PaymentDto result = paymentService.save(testPaymentDto);

        // Then
        assertNotNull(result);
        assertEquals(PaymentStatus.NOT_STARTED, result.getPaymentStatus());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testUpdate_ShouldUpdatePaymentStatus() {
        // Given
        PaymentDto updatedDto = PaymentDto.builder()
                .paymentId(1)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        when(paymentRepository.save(any(Payment.class))).thenReturn(testPayment);

        // When
        PaymentDto result = paymentService.update(updatedDto);

        // Then
        assertNotNull(result);
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    void testDeleteById_ShouldDeletePayment() {
        // Given
        doNothing().when(paymentRepository).deleteById(1);

        // When
        paymentService.deleteById(1);

        // Then
        verify(paymentRepository, times(1)).deleteById(1);
    }
}
