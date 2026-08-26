package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AccountingBatchQueryPort {

    AccountingBatchPage search(
            LocalDate businessDate,
            AccountingBatchStatus status,
            int page,
            int size
    );

    Optional<AccountingBatch> findById(AccountingBatchId batchId);

    record AccountingBatchPage(
            List<AccountingBatch> content,
            long totalElements
    ) {
        public AccountingBatchPage {
            content = List.copyOf(content);
        }
    }
}
