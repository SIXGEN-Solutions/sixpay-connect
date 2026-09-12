package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.AccountingT1OperationalCandidateNotFoundException;
import com.sixpay.accounting.application.port.output.AccountingT1OperationalQueryPort;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.AccountingT1EligibilityReason;
import com.sixpay.accounting.domain.model.AccountingT1OperationalCandidateStatus;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountingT1OperationalQueryServiceCoverageTest {

    private final AccountingT1OperationalQueryPort port =
            mock(AccountingT1OperationalQueryPort.class);

    private final AccountingT1OperationalQueryService service =
            new AccountingT1OperationalQueryService(port);

    @Test
    void mapsMissingTresorPayEvidenceToAwaitingVerification() {
        var candidate = candidate(null, null);
        when(port.search(null, null, 0, 20))
                .thenReturn(new AccountingT1OperationalQueryPort.Page(
                        List.of(candidate), 1
                ));

        var result = service.search(null, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status())
                .isEqualTo(AccountingT1OperationalCandidateStatus.AWAITING_TRESORPAY_VERIFICATION);
        assertThat(result.content().getFirst().eligibilityReason())
                .isEqualTo(AccountingT1EligibilityReason.TRESORPAY_STATUS_UNAVAILABLE);
    }

    @Test
    void mapsNonCompletedTresorPayEvidenceToIneligible() {
        var candidate = candidate(null, evidence("PENDING"));
        when(port.search(null, null, 0, 20))
                .thenReturn(new AccountingT1OperationalQueryPort.Page(
                        List.of(candidate), 1
                ));

        var result = service.search(null, null, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status())
                .isEqualTo(AccountingT1OperationalCandidateStatus.INELIGIBLE_FOR_CURRENT_SELECTION);
        assertThat(result.content().getFirst().eligibilityReason())
                .isEqualTo(AccountingT1EligibilityReason.TRESORPAY_STATUS_NOT_COMPLETED);
    }

    @Test
    void filtersNormalizedOperationalStatus() {
        var eligible = candidate(null, evidence("COMPLETED"));
        var ineligible = candidate(null, evidence("PENDING"));

        when(port.search(null, null, 0, 20))
                .thenReturn(new AccountingT1OperationalQueryPort.Page(
                        List.of(eligible, ineligible), 2
                ));

        var result = service.search(
                null,
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                null,
                0,
                20
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().status())
                .isEqualTo(AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH);
    }

    @Test
    void normalizesPaymentReferenceBeforeQueryingPort() {
        when(port.search(null, "PAY-001", 0, 20))
                .thenReturn(new AccountingT1OperationalQueryPort.Page(
                        List.of(), 0
                ));

        service.search(null, null, "  PAY-001  ", 0, 20);

        verify(port).search(null, "PAY-001", 0, 20);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(() -> service.search(null, null, null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search(null, null, null, 0, 201))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void raisesNotFoundWhenCandidateDoesNotExist() {
        UUID candidateId =
                UUID.fromString("11111111-1111-4111-8111-111111111111");

        when(port.findByCandidateId(candidateId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCandidateId(candidateId))
                .isInstanceOf(AccountingT1OperationalCandidateNotFoundException.class);
    }

    private static AccountingCandidateProjection candidate(
            UUID batchId,
            TresorPayPaymentStatusEvidence evidence
    ) {
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

    private static TresorPayPaymentStatusEvidence evidence(String providerStatus) {
        return new TresorPayPaymentStatusEvidence(
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
    }
}
