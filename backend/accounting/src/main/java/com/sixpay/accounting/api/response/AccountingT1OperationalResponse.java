package com.sixpay.accounting.api.response;

import com.sixpay.accounting.domain.model.AccountingT1EligibilityReason;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1OperationalSnapshot;
import com.sixpay.accounting.domain.model.AccountingT1TechnicalIssue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AccountingT1OperationalResponse(
        UUID candidateId,
        UUID paymentId,
        String publicPaymentReference,
        String financialInstitutionCode,
        LocalDate accountingBusinessDate,
        AccountingT1OperationalCandidateStatus status,
        String tresorPayProviderStatus,
        Instant tresorPayCheckedAt,
        AccountingT1EligibilityReason eligibilityReason,
        LocalDate selectionBusinessDate,
        Instant selectionFromInclusive,
        Instant selectionToExclusive,
        AccountingT1TechnicalIssue technicalIssue,
        UUID batchId
) {
    public static AccountingT1OperationalResponse from(
            AccountingT1OperationalSnapshot snapshot
    ) {
        return new AccountingT1OperationalResponse(
                snapshot.candidateId(),
                snapshot.paymentId(),
                snapshot.publicPaymentReference(),
                snapshot.financialInstitutionCode(),
                snapshot.accountingBusinessDate(),
                snapshot.status(),
                snapshot.tresorPayProviderStatus(),
                snapshot.tresorPayCheckedAt(),
                snapshot.eligibilityReason(),
                snapshot.selectionBusinessDate(),
                snapshot.selectionFromInclusive(),
                snapshot.selectionToExclusive(),
                snapshot.technicalIssue(),
                snapshot.batchId()
        );
    }
}
