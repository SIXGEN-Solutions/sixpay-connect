package com.sixpay.payment.infrastructure.persistence;

import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.payment.domain.repository.PaymentRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PaymentRepositoryAdapter implements PaymentRepository, PaymentLookupPort {

    private final PaymentSpringDataRepository springDataRepository;
    private final PaymentPersistenceMapper mapper;

    public PaymentRepositoryAdapter(
            PaymentSpringDataRepository springDataRepository,
            PaymentPersistenceMapper mapper
    ) {
        this.springDataRepository = Objects.requireNonNull(
                springDataRepository,
                "Payment Spring Data repository"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "Payment persistence mapper"
        );
    }

    @Override
    @Transactional
    public Payment save(Payment payment) {
        Objects.requireNonNull(payment, "Payment");

        try {
            PaymentJpaEntity entity = springDataRepository
                    .findById(payment.id().value())
                    .map(existing -> {
                        mapper.synchronize(existing, payment);
                        return existing;
                    })
                    .orElseGet(() -> mapper.toNewEntity(payment));

            return mapper.toDomain(
                    springDataRepository.saveAndFlush(entity)
            );
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new PaymentPersistenceException(
                    "Concurrent Payment update detected for "
                            + payment.id(),
                    exception
            );
        } catch (DataIntegrityViolationException exception) {
            throw new PaymentPersistenceException(
                    "Payment uniqueness or database constraint violation for "
                            + payment.id(),
                    exception
            );
        }
    }

    @Override
    public Optional<Payment> findById(PaymentId paymentId) {
        Objects.requireNonNull(paymentId, "Payment ID");

        return springDataRepository.findById(paymentId.value())
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment> findByPublicPaymentReference(
            PublicPaymentReference publicPaymentReference
    ) {
        Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );

        return springDataRepository
                .findByPublicPaymentReference(
                        publicPaymentReference.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Payment>
            findBySourceAndExternalPaymentReference(
                    PaymentSource source,
                    ExternalPaymentReference externalPaymentReference
            ) {
        Objects.requireNonNull(source, "Payment source");
        Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );

        return springDataRepository
                .findBySourceAndExternalPaymentReference(
                        source,
                        externalPaymentReference.value()
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySourceAndExternalPaymentReference(
            PaymentSource source,
            ExternalPaymentReference externalPaymentReference
    ) {
        Objects.requireNonNull(source, "Payment source");
        Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );

        return springDataRepository
                .existsBySourceAndExternalPaymentReference(
                        source,
                        externalPaymentReference.value()
                );
    }
}
