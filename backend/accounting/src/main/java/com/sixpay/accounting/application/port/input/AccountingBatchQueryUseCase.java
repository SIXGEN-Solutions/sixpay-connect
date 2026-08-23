package com.sixpay.accounting.application.port.input;

import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchStatus;

import java.time.LocalDate;
import java.util.List;

public interface AccountingBatchQueryUseCase {

    AccountingBatchPage search(
            LocalDate businessDate,
            AccountingBatchStatus status,
            int page,
            int size
    );

    AccountingBatch findById(AccountingBatchId batchId);

    record AccountingBatchPage(
            List<AccountingBatch> content,
            int page,
            int size,
            long totalElements
    ) {
        public AccountingBatchPage {
            content = List.copyOf(content);
        }

        public int totalPages() {
            return totalElements == 0L
                    ? 0
                    : (int) Math.ceil((double) totalElements / size);
        }
    }
}
