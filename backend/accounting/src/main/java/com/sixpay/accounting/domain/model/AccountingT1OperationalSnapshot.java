package com.sixpay.accounting.domain.model;

import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Accounting-owned normalized operational view of an existing T1 candidate.
 *
 * <p>This is a domain model only. It is not an HTTP response contract.</p>
 */
public record AccountingT1OperationalSnapshot(
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
    public AccountingT1OperationalSnapshot {
        candidateId = Objects.requireNonNull(candidateId, "candidateId");
        paymentId = Objects.requireNonNull(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        accountingBusinessDate = Objects.requireNonNull(accountingBusinessDate, "accountingBusinessDate");
        status = Objects.requireNonNull(status, "status");
        tresorPayProviderStatus = optional(tresorPayProviderStatus);
        eligibilityReason = Objects.requireNonNull(eligibilityReason, "eligibilityReason");
        selectionBusinessDate = Objects.requireNonNull(selectionBusinessDate, "selectionBusinessDate");
        selectionFromInclusive = Objects.requireNonNull(selectionFromInclusive, "selectionFromInclusive");
        selectionToExclusive = Objects.requireNonNull(selectionToExclusive, "selectionToExclusive");
        technicalIssue = Objects.requireNonNull(technicalIssue, "technicalIssue");

        if (!selectionFromInclusive.isBefore(selectionToExclusive)) {
            throw new IllegalArgumentException("Selection window must be non-empty");
        }
        if (status == AccountingT1OperationalCandidateStatus.ASSIGNED_TO_BATCH && batchId == null) {
            throw new IllegalArgumentException("batchId is required when candidate is assigned to a batch");
        }
        if (status != AccountingT1OperationalCandidateStatus.ASSIGNED_TO_BATCH && batchId != null) {
            throw new IllegalArgumentException("batchId is only allowed when candidate is assigned to a batch");
        }
    }

    public static AccountingT1OperationalSnapshot from(
            AccountingCandidateProjection candidate,
            AccountingSelectionWindow window,
            AccountingT1OperationalCandidateStatus status,
            AccountingT1EligibilityReason eligibilityReason,
            AccountingT1TechnicalIssue technicalIssue
    ) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(window, "window");

        TresorPayPaymentStatusEvidence evidence = candidate.tresorPayStatusEvidence();

        return new AccountingT1OperationalSnapshot(
                candidate.id(),
                candidate.paymentId(),
                candidate.publicPaymentReference(),
                candidate.financialInstitutionCode(),
                candidate.accountingBusinessDate(),
                Objects.requireNonNull(status, "status"),
                evidence == null ? null : evidence.providerStatus(),
                evidence == null ? null : evidence.checkedAt(),
                Objects.requireNonNull(eligibilityReason, "eligibilityReason"),
                window.businessDate(),
                window.fromInclusive(),
                window.toExclusive(),
                Objects.requireNonNull(technicalIssue, "technicalIssue"),
                candidate.batchId()
        );
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
