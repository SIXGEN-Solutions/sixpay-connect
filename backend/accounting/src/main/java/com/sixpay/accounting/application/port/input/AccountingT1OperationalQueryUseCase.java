package com.sixpay.accounting.application.port.input;

import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1OperationalSnapshot;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AccountingT1OperationalQueryUseCase {

    Page search(
            LocalDate businessDate,
            AccountingT1OperationalCandidateStatus status,
            String paymentReference,
            int page,
            int size
    );

    AccountingT1OperationalSnapshot findByCandidateId(UUID candidateId);

    record Page(
            List<AccountingT1OperationalSnapshot> content,
            int page,
            int size,
            long totalElements
    ) {
        public int totalPages() {
            return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        }
    }
}
