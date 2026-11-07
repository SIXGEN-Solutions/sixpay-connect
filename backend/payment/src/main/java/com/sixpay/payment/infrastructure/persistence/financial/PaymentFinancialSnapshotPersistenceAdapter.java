package com.sixpay.payment.infrastructure.persistence.financial;

import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.financial.FinancialSnapshotStatus;
import com.sixpay.payment.domain.model.financial.PaymentFinancialEventSnapshot;
import com.sixpay.payment.domain.repository.PaymentFinancialSnapshotRepository;
import com.sixpay.payment.infrastructure.persistence.PaymentPersistenceException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final PaymentFinancialEntrySnapshotSpringDataRepository entryRepository;

    public PaymentFinancialSnapshotPersistenceAdapter(
            PaymentFinancialEventSnapshotSpringDataRepository
                    springDataRepository,
            PaymentFinancialSnapshotPersistenceMapper mapper
    ) {
        this(springDataRepository, mapper, null);
    }

    @Autowired
    public PaymentFinancialSnapshotPersistenceAdapter(
            PaymentFinancialEventSnapshotSpringDataRepository
                    springDataRepository,
            PaymentFinancialSnapshotPersistenceMapper mapper,
            PaymentFinancialEntrySnapshotSpringDataRepository entryRepository
    ) {
        this.springDataRepository = Objects.requireNonNull(
                springDataRepository,
                "Financial snapshot Spring Data repository"
        );
        this.mapper = Objects.requireNonNull(
                mapper,
                "Financial snapshot persistence mapper"
        );
        this.entryRepository = entryRepository;
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
    @Transactional
    public void enrichCompletedBankingOutcome(
            PaymentId paymentId,
            String principalBankReference,
            LocalDate accountingDate,
            Instant observedAt
    ) {
        Objects.requireNonNull(paymentId, "Payment ID");
        Objects.requireNonNull(entryRepository,
                "Financial entry repository for bank outcome enrichment");
        Objects.requireNonNull(accountingDate, "Core Banking dco");
        Objects.requireNonNull(observedAt, "Original bank outcome observation");

        PaymentFinancialEventSnapshot snapshot = findByPaymentId(paymentId)
                .orElseThrow(() -> new PaymentPersistenceException(
                        "Missing T0 financial snapshot for completed bank outcome"));
        if (snapshot.status() != FinancialSnapshotStatus.FINALIZED
                || snapshot.entries().size() != 2) {
            throw new PaymentPersistenceException(
                    "Completed bank outcome requires finalized T0 event and two entries");
        }
        var existing = snapshot.bankingDetails();
        if (existing == null || existing.operationCode() == null) {
            throw new PaymentPersistenceException(
                    "T0 banking operation code was not frozen");
        }
        String bankEventNumber = eventNumber(
                principalBankReference, existing.operationCode());
        LocalDate operationDate = observedAt.atZone(ZoneOffset.UTC).toLocalDate();

        int parentRows = springDataRepository.enrichCompletedBankingOutcome(
                snapshot.snapshotId(), bankEventNumber, accountingDate, operationDate);
        if (parentRows != 1) {
            throw new PaymentPersistenceException(
                    "Conflicting or missing bank outcome for T0 financial event");
        }
        int entryRows = entryRepository.enrichAccountingDate(
                snapshot.snapshotId(), accountingDate);
        if (entryRows != 2) {
            throw new PaymentPersistenceException(
                    "Conflicting or incomplete bank accounting dates in T0 entries");
        }
    }

    /** Parses operationCode/eventNumber; preserves the bank's zero-padded event number. */
    private static String eventNumber(String bankReference, String operationCode) {
        if (bankReference == null) {
            throw new PaymentPersistenceException("Missing completed bank reference");
        }
        int slash = bankReference.indexOf('/');
        if (slash <= 0 || slash != bankReference.lastIndexOf('/')
                || slash == bankReference.length() - 1
                || !bankReference.substring(0, slash).equals(operationCode)) {
            throw new PaymentPersistenceException(
                    "Bank reference does not match frozen operation code");
        }
        String number = bankReference.substring(slash + 1);
        if (number.length() > 32 || !number.matches("[0-9]+")) {
            throw new PaymentPersistenceException("Invalid bank event number");
        }
        return number;
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
