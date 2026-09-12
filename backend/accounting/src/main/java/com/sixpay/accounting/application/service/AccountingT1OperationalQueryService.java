package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingT1OperationalCandidateNotFoundException;
import com.sixpay.accounting.application.port.input.AccountingT1OperationalQueryUseCase;
import com.sixpay.accounting.application.port.output.AccountingT1OperationalQueryPort;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingT1EligibilityReason;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1OperationalSnapshot;
import com.sixpay.accounting.domain.model.AccountingT1TechnicalIssue;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountingT1OperationalQueryService
        implements AccountingT1OperationalQueryUseCase {

    private final AccountingT1OperationalQueryPort queryPort;

    public AccountingT1OperationalQueryService(
            AccountingT1OperationalQueryPort queryPort
    ) {
        this.queryPort = Objects.requireNonNull(queryPort);
    }

    @Override
    public Page search(
            LocalDate businessDate,
            AccountingT1OperationalCandidateStatus status,
            String paymentReference,
            int page,
            int size
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }

        var result = queryPort.search(
                businessDate,
                normalize(paymentReference),
                page,
                size
        );

        var content = result.content().stream()
                .map(this::toSnapshot)
                .filter(snapshot -> status == null || snapshot.status() == status)
                .toList();

        return new Page(content, page, size, result.totalElements());
    }

    @Override
    public AccountingT1OperationalSnapshot findByCandidateId(UUID candidateId) {
        Objects.requireNonNull(candidateId, "candidateId is required");

        return queryPort.findByCandidateId(candidateId)
                .map(this::toSnapshot)
                .orElseThrow(() ->
                        new AccountingT1OperationalCandidateNotFoundException(candidateId)
                );
    }

    private AccountingT1OperationalSnapshot toSnapshot(
            AccountingCandidateProjection candidate
    ) {
        AccountingSelectionWindow window = windowFor(candidate.accountingBusinessDate());

        AccountingT1OperationalCandidateStatus status;
        AccountingT1EligibilityReason reason;
        AccountingT1TechnicalIssue issue = AccountingT1TechnicalIssue.NONE;

        if (candidate.batchId() != null) {
            status = AccountingT1OperationalCandidateStatus.ASSIGNED_TO_BATCH;
            reason = AccountingT1EligibilityReason.NONE;
        } else if (candidate.tresorPayStatusEvidence() == null
                || candidate.tresorPayStatusEvidence().providerStatus() == null) {
            status = AccountingT1OperationalCandidateStatus.AWAITING_TRESORPAY_VERIFICATION;
            reason = AccountingT1EligibilityReason.TRESORPAY_STATUS_UNAVAILABLE;
        } else if (candidate.tresorPayStatusEvidence().confirmsPaidPayment()) {
            status = AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH;
            reason = AccountingT1EligibilityReason.NONE;
        } else {
            status = AccountingT1OperationalCandidateStatus.INELIGIBLE_FOR_CURRENT_SELECTION;
            reason = AccountingT1EligibilityReason.TRESORPAY_STATUS_NOT_COMPLETED;
        }

        return AccountingT1OperationalSnapshot.from(
                candidate,
                window,
                status,
                reason,
                issue
        );
    }

    private static AccountingSelectionWindow windowFor(LocalDate businessDate) {
        return new AccountingSelectionWindow(
                businessDate,
                businessDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                businessDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)
        );
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
