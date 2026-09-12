package com.sixpay.accounting.application.service;

import com.sixpay.accounting.application.exception.TfjOperationalConfirmationNotFoundException;
import com.sixpay.accounting.application.port.output.TfjConfirmationRepository;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.accounting.domain.model.TfjMatchStatus;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.domain.model.TfjOperationalCategory;
import com.sixpay.accounting.domain.model.TfjRecoveryAction;
import com.sixpay.accounting.domain.model.TfjStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TfjOperationalQueryServiceTest {

    private final TfjConfirmationRepository repository =
            mock(TfjConfirmationRepository.class);

    private final TfjOperationalQueryService service =
            new TfjOperationalQueryService(repository);

    @Test
    void exposesMatchedConfirmation() {
        var confirmation = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.MATCHED,
                null,
                null,
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );

        when(repository.findByConfirmationId(confirmation.confirmationId()))
                .thenReturn(Optional.of(confirmation));

        var snapshot = service.findByConfirmationId(
                confirmation.confirmationId()
        );

        assertThat(snapshot.category())
                .isEqualTo(TfjOperationalCategory.MATCHED);
        assertThat(snapshot.matchStatus())
                .isEqualTo(TfjMatchStatus.MATCHED);
    }

    @Test
    void exposesUnmatchedConfirmationAsQuarantined() {
        var confirmation = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.UNMATCHED,
                null,
                null,
                null
        );

        when(repository.findByConfirmationId(confirmation.confirmationId()))
                .thenReturn(Optional.of(confirmation));

        var snapshot = service.findByConfirmationId(
                confirmation.confirmationId()
        );

        assertThat(snapshot.category())
                .isEqualTo(
                        TfjOperationalCategory.QUARANTINED_UNMATCHED
                );
    }

    @Test
    void exposesAmbiguousConfirmationAsQuarantined() {
        var confirmation = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.AMBIGUOUS,
                null,
                null,
                null
        );

        when(repository.findByConfirmationId(confirmation.confirmationId()))
                .thenReturn(Optional.of(confirmation));

        var snapshot = service.findByConfirmationId(
                confirmation.confirmationId()
        );

        assertThat(snapshot.category())
                .isEqualTo(
                        TfjOperationalCategory.QUARANTINED_AMBIGUOUS
                );
    }

    @Test
    void exposesFailedTfjConfirmation() {
        var confirmation = confirmation(
                TfjStatus.FAILED,
                TfjMatchStatus.MATCHED,
                null,
                TfjRecoveryAction.MANUAL_RECONCILIATION,
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );

        when(repository.findByConfirmationId(confirmation.confirmationId()))
                .thenReturn(Optional.of(confirmation));

        var snapshot = service.findByConfirmationId(
                confirmation.confirmationId()
        );

        assertThat(snapshot.category())
                .isEqualTo(TfjOperationalCategory.FAILED);
        assertThat(snapshot.failureCode()).isEqualTo("TFJ_REJECTED");
        assertThat(snapshot.recoveryAction())
                .isEqualTo(TfjRecoveryAction.MANUAL_RECONCILIATION);
    }

    @Test
    void exposesTerminalConfirmationWithUnpublishedFinality() {
        var confirmation = confirmation(
                TfjStatus.INTEGRATED,
                TfjMatchStatus.MATCHED,
                null,
                null,
                UUID.fromString("22222222-2222-4222-8222-222222222222")
        );

        when(repository.findByConfirmationId(confirmation.confirmationId()))
                .thenReturn(Optional.of(confirmation));

        var snapshot = service.findByConfirmationId(
                confirmation.confirmationId()
        );

        assertThat(snapshot.category())
                .isEqualTo(
                        TfjOperationalCategory.FINALITY_PUBLICATION_PENDING
                );
        assertThat(snapshot.finalityPublishedAt()).isNull();
    }

    @Test
    void categoryFilterKeepsPaginationConsistent() {
        var first = confirmation(
                TfjStatus.PENDING,
                TfjMatchStatus.UNMATCHED,
                null,
                null,
                null
        );
        var second = confirmation(
                UUID.fromString("33333333-3333-4333-8333-333333333333"),
                TfjStatus.PENDING,
                TfjMatchStatus.UNMATCHED,
                null,
                null,
                null
        );
        var matched = confirmation(
                UUID.fromString("44444444-4444-4444-8444-444444444444"),
                TfjStatus.PENDING,
                TfjMatchStatus.MATCHED,
                null,
                null,
                UUID.fromString("55555555-5555-4555-8555-555555555555")
        );

        when(repository.searchOperational(
                LocalDate.of(2026, 9, 11),
                "PAY",
                "BANK",
                1,
                1
        )).thenReturn(
                new TfjConfirmationRepository.OperationalPage(
                        List.of(second),
                        3
                )
        );

        when(repository.searchOperationalAll(
                LocalDate.of(2026, 9, 11),
                "PAY",
                "BANK"
        )).thenReturn(List.of(first, second, matched));

        var page = service.search(
                LocalDate.of(2026, 9, 11),
                TfjOperationalCategory.QUARANTINED_UNMATCHED,
                " PAY ",
                " BANK ",
                1,
                1
        );

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.totalPages()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidPagination() {
        assertThatThrownBy(
                () -> service.search(
                        null,
                        null,
                        null,
                        null,
                        -1,
                        20
                )
        ).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(
                () -> service.search(
                        null,
                        null,
                        null,
                        null,
                        0,
                        201
                )
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void returnsNotFoundForUnknownConfirmation() {
        UUID confirmationId =
                UUID.fromString("99999999-9999-4999-8999-999999999999");

        when(repository.findByConfirmationId(confirmationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.findByConfirmationId(confirmationId)
        ).isInstanceOf(TfjOperationalConfirmationNotFoundException.class);
    }

    private static TfjConfirmation confirmation(
            TfjStatus status,
            TfjMatchStatus matchStatus,
            Instant finalityPublishedAt,
            TfjRecoveryAction recoveryAction,
            UUID matchedPaymentId
    ) {
        return confirmation(
                UUID.fromString("11111111-1111-4111-8111-111111111111"),
                status,
                matchStatus,
                finalityPublishedAt,
                recoveryAction,
                matchedPaymentId
        );
    }

    private static TfjConfirmation confirmation(
            UUID confirmationId,
            TfjStatus status,
            TfjMatchStatus matchStatus,
            Instant finalityPublishedAt,
            TfjRecoveryAction recoveryAction,
            UUID matchedPaymentId
    ) {
        boolean failed = status == TfjStatus.FAILED;

        return new TfjConfirmation(
                confirmationId,
                "idem-" + confirmationId,
                "hash-" + confirmationId,
                "LAREGIONALE",
                LocalDate.of(2026, 9, 11),
                "PAY-001",
                "BANK-POST-001",
                "TFJ-BATCH-001",
                status,
                Instant.parse("2026-09-11T23:00:00Z"),
                failed ? "TFJ_REJECTED" : null,
                failed ? "TFJ rejected the accounting entry" : null,
                failed ? recoveryAction : null,
                Instant.parse("2026-09-11T23:05:00Z"),
                TfjObservationChannel.ASYNC_CALLBACK,
                "corr-001",
                matchStatus,
                matchedPaymentId,
                finalityPublishedAt
        );
    }
}
