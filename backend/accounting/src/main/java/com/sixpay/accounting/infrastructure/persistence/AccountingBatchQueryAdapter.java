package com.sixpay.accounting.infrastructure.persistence;

import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchIdempotencyKey;
import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class AccountingBatchQueryAdapter
        implements AccountingBatchQueryPort {

    private final AccountingBatchSpringDataRepository repository;

    public AccountingBatchQueryAdapter(
            AccountingBatchSpringDataRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountingBatchPage search(
            LocalDate businessDate,
            AccountingBatchStatus status,
            int page,
            int size
    ) {
        var result = repository.search(
                businessDate,
                status,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        return new AccountingBatchPage(
                result.getContent().stream()
                        .map(this::toDomain)
                        .toList(),
                result.getTotalElements()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AccountingBatch> findById(AccountingBatchId batchId) {
        return repository.findAggregateById(batchId.value())
                .map(this::toDomain);
    }

    private AccountingBatch toDomain(AccountingBatchJpaEntity entity) {
        return new AccountingBatch(
                new AccountingBatchId(entity.id()),
                new AccountingBatchIdempotencyKey(entity.idempotencyKey()),
                entity.businessDate(),
                entity.financialInstitutionCode(),
                entity.createdAt(),
                entity.status(),
                entity.items().stream()
                        .map(item -> new AccountingBatchItem(
                                item.paymentId(),
                                item.publicPaymentReference(),
                                item.partnerId(),
                                item.amount(),
                                item.currency(),
                                item.paymentOccurredAt(),
                                item.paymentBusinessDate(),
                                item.bankPostingReference(),
                                item.tresorPayStatus(),
                                item.tresorPayStatusCheckedAt(),
                                item.status()
                        ))
                        .toList()
        );
    }
}
