package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingT1OperationalQueryPort;
import com.sixpay.accounting.domain.model.AccountingBatchId;
import com.sixpay.accounting.domain.model.AccountingBatchTracking;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingSubmissionState;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.AccountingT1TechnicalIssue;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.DailyAccountingCutoffPolicy;
import com.sixpay.accounting.domain.repository.AccountingBatchTrackingRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountingT1OperationalQueryClosureTest {

    private final AccountingT1OperationalQueryPort port =
            mock(AccountingT1OperationalQueryPort.class);
    private final AccountingBatchTrackingRepository trackingRepository =
            mock(AccountingBatchTrackingRepository.class);

    private final AccountingT1OperationalQueryService service =
            new AccountingT1OperationalQueryService(
                    port,
                    new DailyAccountingCutoffPolicy(
                            ZoneId.of("Africa/Douala"),
                            LocalTime.of(23, 0)
                    ),
                    trackingRepository
            );

    @Test
    void exposesActualConfiguredT1SelectionWindow() {
        var candidate = candidate(null, "COMPLETED");

        when(port.findByCandidateId(candidate.id()))
                .thenReturn(Optional.of(candidate));

        var snapshot = service.findByCandidateId(candidate.id());

        assertThat(snapshot.selectionBusinessDate())
                .isEqualTo(LocalDate.of(2026, 9, 11));
        assertThat(snapshot.selectionFromInclusive())
                .isEqualTo(Instant.parse("2026-09-10T22:00:00Z"));
        assertThat(snapshot.selectionToExclusive())
                .isEqualTo(Instant.parse("2026-09-11T22:00:00Z"));
    }

    @Test
    void statusFilterKeepsPaginationTotalsConsistent() {
        var eligible = candidate(null, "COMPLETED");
        var ineligible = candidate(null, "PENDING");

        when(port.searchAll(null, null))
                .thenReturn(List.of(eligible, ineligible));

        var result = service.search(
                null,
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                null,
                0,
                1
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void exposesDurableSubmissionOutcomeUnknown() {
        UUID batchId =
                UUID.fromString("33333333-3333-4333-8333-333333333333");
        var candidate = candidate(batchId, "COMPLETED");
        var tracking = AccountingBatchTracking
                .ready(new AccountingBatchId(batchId))
                .outcomeUnknown(
                        Instant.parse("2026-09-11T12:00:00Z"),
                        "TRANSPORT_UNAVAILABLE"
                );

        when(port.findByCandidateId(candidate.id()))
                .thenReturn(Optional.of(candidate));
        when(trackingRepository.findByBatchId(new AccountingBatchId(batchId)))
                .thenReturn(Optional.of(tracking));

        var snapshot = service.findByCandidateId(candidate.id());

        assertThat(snapshot.technicalIssue())
                .isEqualTo(
                        AccountingT1TechnicalIssue
                                .ACCOUNTING_SUBMISSION_OUTCOME_UNKNOWN
                );
    }

    @Test
    void exposesDurableReconciliationPending() {
        UUID batchId =
                UUID.fromString("33333333-3333-4333-8333-333333333333");
        var candidate = candidate(batchId, "COMPLETED");
        var tracking = AccountingBatchTracking
                .ready(new AccountingBatchId(batchId))
                .submissionResolved(
                        AccountingSubmissionState.RECONCILIATION_REQUIRED,
                        "CB-BATCH-001",
                        Instant.parse("2026-09-11T12:00:00Z"),
                        List.of()
                );

        when(port.findByCandidateId(candidate.id()))
                .thenReturn(Optional.of(candidate));
        when(trackingRepository.findByBatchId(new AccountingBatchId(batchId)))
                .thenReturn(Optional.of(tracking));

        var snapshot = service.findByCandidateId(candidate.id());

        assertThat(snapshot.technicalIssue())
                .isEqualTo(
                        AccountingT1TechnicalIssue
                                .ACCOUNTING_RECONCILIATION_PENDING
                );
    }

    private static AccountingCandidateProjection candidate(
            UUID batchId,
            String providerStatus
    ) {
        var evidence = new TresorPayPaymentStatusEvidence(
                "TP-REF",
                "TX-001",
                providerStatus,
                "ACCOUNT",
                "OP-001",
                true,
                true,
                Instant.parse("2026-09-11T10:00:00Z"),
                null,
                Instant.parse("2026-09-11T10:01:00Z"),
                "TP-REF",
                "CORR-001"
        );

        return new AccountingCandidateProjection(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                UUID.fromString("22222222-2222-4222-8222-222222222222"),
                UUID.fromString("44444444-4444-4444-8444-444444444444"),
                "PAY-001",
                "PARTNER-001",
                "LAREGIONALE",
                "BANK-001",
                Instant.parse("2026-09-11T09:00:00Z"),
                LocalDate.of(2026, 9, 11),
                UUID.fromString("55555555-5555-4555-8555-555555555555"),
                "v1",
                Instant.parse("2026-09-11T08:59:00Z"),
                "DEBTOR-001",
                "CREDITOR-001",
                new BigDecimal("1000.00"),
                Currency.getInstance("XAF"),
                Instant.parse("2026-09-11T09:00:00Z"),
                Instant.parse("2026-09-11T09:01:00Z"),
                batchId,
                evidence,
                List.of()
        );
    }
}
