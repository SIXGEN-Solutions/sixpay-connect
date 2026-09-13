package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingT1OperationalCandidateNotFoundException;
import com.sixpay.accounting.application.port.input.AccountingT1OperationalQueryUseCase;
import com.sixpay.accounting.application.port.output.AccountingT1OperationalQueryPort;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingSubmissionState;
import com.sixpay.accounting.domain.model.AccountingT1EligibilityReason;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1OperationalSnapshot;
import com.sixpay.accounting.domain.model.AccountingT1TechnicalIssue;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AccountingT1OperationalQueryService
        implements AccountingT1OperationalQueryUseCase {

    private final AccountingT1OperationalQueryPort queryPort;
    private final AccountingCutoffPolicy cutoffPolicy;
    private final AccountingBatchTrackingRepository trackingRepository;

    public AccountingT1OperationalQueryService(
            AccountingT1OperationalQueryPort queryPort,
            AccountingCutoffPolicy cutoffPolicy,
            AccountingBatchTrackingRepository trackingRepository
    ) {
        this.queryPort = Objects.requireNonNull(queryPort);
        this.cutoffPolicy = Objects.requireNonNull(cutoffPolicy);
        this.trackingRepository = Objects.requireNonNull(trackingRepository);
    }

    @Override
    public Page search(
            java.time.LocalDate businessDate,
            AccountingT1OperationalCandidateStatus status,
            String paymentReference,
            int page,
            int size
    ) {
        validatePagination(page, size);
        String normalizedReference = normalize(paymentReference);

        if (status == null) {
            var result = queryPort.search(
                    businessDate,
                    normalizedReference,
                    page,
                    size
            );

            return new Page(
                    result.content().stream()
                            .map(this::toSnapshot)
                            .toList(),
                    page,
                    size,
                    result.totalElements()
            );
        }

        List<AccountingT1OperationalSnapshot> matching = queryPort
                .searchAll(businessDate, normalizedReference)
                .stream()
                .map(this::toSnapshot)
                .filter(snapshot -> snapshot.status() == status)
                .toList();

        long start = (long) page * size;
        List<AccountingT1OperationalSnapshot> content;

        if (start >= matching.size()) {
            content = List.of();
        } else {
            int fromIndex = (int) start;
            int toIndex = Math.min(fromIndex + size, matching.size());
            content = matching.subList(fromIndex, toIndex);
        }

        return new Page(
                content,
                page,
                size,
                matching.size()
        );
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
        AccountingSelectionWindow window = windowFor(candidate);

        AccountingT1OperationalCandidateStatus status;
        AccountingT1EligibilityReason reason;

        if (candidate.batchId() != null) {
            status = AccountingT1OperationalCandidateStatus.ASSIGNED_TO_BATCH;
            reason = AccountingT1EligibilityReason.NONE;
        } else if (candidate.tresorPayStatusEvidence() == null) {
            status = AccountingT1OperationalCandidateStatus.AWAITING_TRESORPAY_VERIFICATION;
            reason = AccountingT1EligibilityReason.TRESORPAY_STATUS_UNAVAILABLE;
        } else if (!window.contains(candidate.paymentOccurredAt())) {
            status = AccountingT1OperationalCandidateStatus.INELIGIBLE_FOR_CURRENT_SELECTION;
            reason = AccountingT1EligibilityReason.OUTSIDE_SELECTION_WINDOW;
        } else if (candidate.tresorPayStatusEvidence()
                .checkedAt()
                .isAfter(window.toExclusive())) {
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
                technicalIssue(candidate)
        );
    }

    private AccountingSelectionWindow windowFor(
            AccountingCandidateProjection candidate
    ) {
        return cutoffPolicy.resolve(
                candidate.paymentOccurredAt(),
                AccountingCutoffMode.MANUAL,
                Optional.of(candidate.accountingBusinessDate())
        );
    }

    private AccountingT1TechnicalIssue technicalIssue(
            AccountingCandidateProjection candidate
    ) {
        if (candidate.batchId() == null) {
            return AccountingT1TechnicalIssue.NONE;
        }

        return trackingRepository
                .findByBatchId(new AccountingBatchId(candidate.batchId()))
                .map(tracking -> mapTechnicalIssue(tracking.submissionState()))
                .orElse(AccountingT1TechnicalIssue.NONE);
    }

    private static AccountingT1TechnicalIssue mapTechnicalIssue(
            AccountingSubmissionState state
    ) {
        return switch (state) {
            case OUTCOME_UNKNOWN ->
                    AccountingT1TechnicalIssue.ACCOUNTING_SUBMISSION_OUTCOME_UNKNOWN;
            case RECONCILIATION_REQUIRED ->
                    AccountingT1TechnicalIssue.ACCOUNTING_RECONCILIATION_PENDING;
            default -> AccountingT1TechnicalIssue.NONE;
        };
    }

    private static void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException("size must be between 1 and 200");
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
