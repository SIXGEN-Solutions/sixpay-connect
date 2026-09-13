package com.sixpay.accounting.infrastructure.tfj.persistence;

import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.accounting.domain.model.TfjStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountingTfjConfirmationRepositoryAdapter
        implements TfjConfirmationRepository {

    private final AccountingTfjConfirmationSpringDataRepository repository;

    public AccountingTfjConfirmationRepositoryAdapter(
            AccountingTfjConfirmationSpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public Optional<TfjConfirmation> findByConfirmationId(
            UUID confirmationId
    ) {
        return repository.findById(confirmationId)
                .map(
                        AccountingTfjConfirmationJpaEntity::toDomain
                );
    }

    @Override
    public Optional<TfjConfirmation> findByIdempotencyKey(
            String idempotencyKey
    ) {
        return repository.findByIdempotencyKey(
                        idempotencyKey
                )
                .map(
                        AccountingTfjConfirmationJpaEntity::toDomain
                );
    }

    @Override
    public TfjConfirmationRepository.OperationalPage searchOperational(
            java.time.LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            int page,
            int size
    ) {
        var result = repository.searchOperational(
                businessDate,
                paymentReference,
                bankPostingReference,
                PageRequest.of(page, size)
        );

        return new TfjConfirmationRepository.OperationalPage(
                result.getContent().stream()
                        .map(AccountingTfjConfirmationJpaEntity::toDomain)
                        .toList(),
                result.getTotalElements()
        );
    }

    @Override
    public List<TfjConfirmation> searchOperationalAll(
            java.time.LocalDate businessDate,
            String paymentReference,
            String bankPostingReference
    ) {
        return repository.searchOperationalAll(
                        businessDate,
                        paymentReference,
                        bankPostingReference
                )
                .stream()
                .map(AccountingTfjConfirmationJpaEntity::toDomain)
                .toList();
    }

    @Override
    public TfjConfirmation save(
            TfjConfirmation confirmation
    ) {
        var existing = repository.findById(
                confirmation.confirmationId()
        );

        AccountingTfjConfirmationJpaEntity entity;
        if (existing.isPresent()) {
            entity = existing.orElseThrow();
            entity.synchronizeMutableState(confirmation);
        } else {
            entity =
                    AccountingTfjConfirmationJpaEntity
                            .fromDomain(confirmation);
        }

        return repository.save(entity).toDomain();
    }

    public long countPendingFinalityPublication() {
        return repository
                .countByMatchedPaymentIdIsNotNullAndFinalityPublishedAtIsNullAndStatusIn(
                        List.of(TfjStatus.INTEGRATED, TfjStatus.FAILED)
                );
    }

    @Override
    public List<TfjConfirmation>
    findPendingFinalityPublication(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException(
                    "limit must be positive"
            );
        }

        return repository
                .findByMatchedPaymentIdIsNotNullAndFinalityPublishedAtIsNullOrderByReceivedAtAsc(
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(
                        AccountingTfjConfirmationJpaEntity::toDomain
                )
                .filter(TfjConfirmation::terminal)
                .toList();
    }
}
