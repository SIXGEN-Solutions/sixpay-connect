package com.sixpay.accounting.application.port.output;

import com.sixpay.accounting.domain.model.AccountingCandidateProjection;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountingT1OperationalQueryPort {

    Page search(
            LocalDate businessDate,
            String paymentReference,
            int page,
            int size
    );

    Optional<AccountingCandidateProjection> findByCandidateId(UUID candidateId);

    record Page(
            List<AccountingCandidateProjection> content,
            long totalElements
    ) {}
}
