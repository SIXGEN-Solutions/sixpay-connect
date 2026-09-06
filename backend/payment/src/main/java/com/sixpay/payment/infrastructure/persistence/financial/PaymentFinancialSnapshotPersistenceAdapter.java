package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Repository
@Transactional(readOnly = true)
public class PaymentFinancialSnapshotPersistenceAdapter
        implements PaymentFinancialSnapshotRepository {

    private final
    PaymentFinancialEventSnapshotSpringDataRepository
            springDataRepository;
    private final
    PaymentFinancialSnapshotPersistenceMapper mapper;

    public PaymentFinancialSnapshotPersistenceAdapter(
            PaymentFinancialEventSnapshotSpringDataRepository
                    springDataRepository,
            PaymentFinancialSnapshotPersistenceMapper mapper
    ) {
        this.springDataRepository = Objects.requireNonNull(
                springDataRepository,
                "Financial snapshot Spring Data repository"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "Financial snapshot persistence mapper"
        );
    }

    @Override
    @Transactional
    public PaymentFinancialEventSnapshot save(
            PaymentFinancialEventSnapshot snapshot
    ) {
        Objects.requireNonNull(
                snapshot,
                "Financial snapshot"
        );

        if (snapshot.status()
                != FinancialSnapshotStatus.FINALIZED) {
            throw new PaymentPersistenceException(
                    "Only FINALIZED financial snapshots "
                            + "may be persisted"
            );
        }

        Optional<PaymentFinancialEventSnapshotJpaEntity>
                existingEntity =
                springDataRepository.findByPaymentId(
                        snapshot.paymentId().value()
                );

        if (existingEntity.isPresent()) {
            PaymentFinancialEventSnapshot existing =
                    mapper.toDomain(
                            existingEntity.orElseThrow()
                    );

            if (!existing.sameSnapshot(snapshot)) {
                throw new PaymentPersistenceException(
                        "A different financial snapshot "
                                + "already exists for Payment "
                                + snapshot.paymentId()
                );
            }
            return existing;
        }

        try {
            return mapper.toDomain(
                    springDataRepository.saveAndFlush(
                            mapper.toNewEntity(snapshot)
                    )
            );
        } catch (DataIntegrityViolationException exception) {
            throw new PaymentPersistenceException(
                    "Financial snapshot constraint "
                            + "violation for Payment "
                            + snapshot.paymentId(),
                    exception
            );
        }
    }

    @Override
    public Optional<PaymentFinancialEventSnapshot>
    findByPaymentId(PaymentId paymentId) {
        Objects.requireNonNull(
                paymentId,
                "Payment ID"
        );

        return springDataRepository
                .findByPaymentId(paymentId.value())
                .map(mapper::toDomain);
    }
}
