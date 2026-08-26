package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentRepositoryAdapterTest {

    private PaymentSpringDataRepository springDataRepository;
    private PaymentPersistenceMapper mapper;
    private PaymentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        springDataRepository = Mockito.mock(
                PaymentSpringDataRepository.class
        );
        mapper = Mockito.mock(PaymentPersistenceMapper.class);
        adapter = new PaymentRepositoryAdapter(
                springDataRepository,
                mapper
        );
    }

    @Test
    void readsAndMapsOnePaymentById() {
        PaymentId paymentId = new PaymentId(UUID.randomUUID());
        PaymentJpaEntity entity = Mockito.mock(
                PaymentJpaEntity.class
        );
        Payment payment = Mockito.mock(Payment.class);

        when(springDataRepository.findById(paymentId.value()))
                .thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(payment);

        Optional<Payment> result = adapter.findById(paymentId);

        assertSame(payment, result.orElseThrow());
        verify(springDataRepository).findById(paymentId.value());
        verify(mapper).toDomain(entity);
    }
}
