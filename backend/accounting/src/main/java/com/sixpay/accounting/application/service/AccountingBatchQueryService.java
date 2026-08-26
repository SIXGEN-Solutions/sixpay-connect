package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingBatchNotFoundException;
import com.sixpay.accounting.application.port.input.AccountingBatchQueryUseCase;
import com.sixpay.accounting.application.port.output.AccountingBatchQueryPort;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class AccountingBatchQueryService
        implements AccountingBatchQueryUseCase {

    private final AccountingBatchQueryPort queryPort;

    public AccountingBatchQueryService(AccountingBatchQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    @Override
    public AccountingBatchPage search(
            LocalDate businessDate,
            AccountingBatchStatus status,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }

        var result = queryPort.search(businessDate, status, page, size);

        return new AccountingBatchPage(
                result.content(),
                page,
                size,
                result.totalElements()
        );
    }

    @Override
    public AccountingBatch findById(AccountingBatchId batchId) {
        Objects.requireNonNull(batchId, "batchId is required");

        return queryPort.findById(batchId)
                .orElseThrow(() ->
                        new AccountingBatchNotFoundException(batchId)
                );
    }
}
