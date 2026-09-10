package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.port.output.AccountingCandidateProjectionRepository;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TresorPayPaymentStatusGateway;
import com.sixpay.accounting.domain.model.AccountingBatch;
import com.sixpay.accounting.domain.model.AccountingCandidateProjection;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import com.sixpay.accounting.domain.policy.AccountingCutoffMode;
import com.sixpay.accounting.domain.policy.AccountingCutoffPolicy;
import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import com.sixpay.common.context.CorrelationId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountingT1OrchestrationServiceTest {

    @Test
    void verifiesTresorPayBeforeConstitutingBatch() {
        Instant runAt = Instant.parse("2026-09-09T12:00:00Z");
        AccountingSelectionWindow window =
                new AccountingSelectionWindow(
                        LocalDate.of(2026, 9, 9),
                        Instant.parse("2026-09-09T00:00:00Z"),
                        Instant.parse("2026-09-10T00:00:00Z")
                );

        AccountingCutoffPolicy cutoffPolicy = mock(AccountingCutoffPolicy.class);
        AccountingCandidateProjectionRepository repository =
                mock(AccountingCandidateProjectionRepository.class);
        TresorPayPaymentStatusGateway gateway =
                mock(TresorPayPaymentStatusGateway.class);
        AccountingBatchConstitutionService constitutionService =
                mock(AccountingBatchConstitutionService.class);

        UUID paymentId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");
        AccountingCandidateProjection candidate =
                mock(AccountingCandidateProjection.class);

        when(candidate.paymentId()).thenReturn(paymentId);
        when(candidate.publicPaymentReference())
                .thenReturn("REF-DGI-2026-0042");

        when(cutoffPolicy.resolve(
                runAt,
                AccountingCutoffMode.AUTO,
                Optional.empty()
        )).thenReturn(window);

        when(repository.findUnbatchedForVerification(window))
                .thenReturn(List.of(candidate));

        TresorPayPaymentStatusEvidence evidence =
                mock(TresorPayPaymentStatusEvidence.class);

        when(gateway.findByPaymentReference(
                eq("REF-DGI-2026-0042"),
                any(AccountingIntegrationContext.class)
        )).thenReturn(evidence);

        AccountingBatch batch = mock(AccountingBatch.class);

        when(constitutionService.constitute(
                runAt,
                AccountingCutoffMode.AUTO,
                Optional.empty(),
                "LAREGIONALE"
        )).thenReturn(batch);

        AccountingT1OrchestrationService service =
                new AccountingT1OrchestrationService(
                        cutoffPolicy,
                        repository,
                        gateway,
                        constitutionService
                );

        AccountingBatch result =
                service.execute(
                        runAt,
                        AccountingCutoffMode.AUTO,
                        Optional.empty(),
                        "LAREGIONALE",
                        new CorrelationId(
                                "11111111-2222-3333-4444-555555555555"
                        )
                );

        assertSame(batch, result);

        verify(gateway).findByPaymentReference(
                eq("REF-DGI-2026-0042"),
                any(AccountingIntegrationContext.class)
        );
        verify(repository).recordTresorPayEvidence(
                paymentId,
                evidence
        );
        verify(constitutionService).constitute(
                runAt,
                AccountingCutoffMode.AUTO,
                Optional.empty(),
                "LAREGIONALE"
        );
    }
}
