package com.sixpay.reporting.application.service;

import com.sixpay.reporting.application.exception.AuditExportNotFoundException;
import com.sixpay.reporting.application.port.output.AuditExportDispatchPort;
import com.sixpay.reporting.application.port.output.AuditExportJobStore;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentAuditExportServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-07T21:00:00Z");

    @Test
    void newRequestIsAcceptedBeforeDispatch() {
        AuditExportJobStore store =
                mock(AuditExportJobStore.class);
        AtomicReference<UUID> dispatched =
                new AtomicReference<>();

        AuditExportDispatchPort dispatch =
                dispatched::set;

        RequestPaymentAuditExportCommand command =
                command();

        UUID exportId = UUID.randomUUID();
        AuditExportJobDefinition job =
                job(exportId, command);

        when(store.accept(
                eq(command),
                anyString(),
                eq(NOW),
                eq(NOW.plus(Duration.ofHours(1)))
        )).thenReturn(
                new AuditExportAcceptance(job, true)
        );

        PaymentAuditExportService service =
                new PaymentAuditExportService(
                        store,
                        dispatch,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofHours(1)
                );

        PaymentAuditExportJobView result =
                service.request(command);

        assertEquals(exportId, result.exportId());
        assertEquals(exportId, dispatched.get());
    }

    @Test
    void idempotentReplayDoesNotDispatchAgain() {
        AuditExportJobStore store =
                mock(AuditExportJobStore.class);
        AuditExportDispatchPort dispatch =
                mock(AuditExportDispatchPort.class);

        RequestPaymentAuditExportCommand command =
                command();
        AuditExportJobDefinition job =
                job(UUID.randomUUID(), command);

        when(store.accept(
                eq(command),
                anyString(),
                any(),
                any()
        )).thenReturn(
                new AuditExportAcceptance(job, false)
        );

        PaymentAuditExportService service =
                new PaymentAuditExportService(
                        store,
                        dispatch,
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofHours(1)
                );

        service.request(command);

        verifyNoInteractions(dispatch);
    }

    @Test
    void unknownExportIsNotFound() {
        AuditExportJobStore store =
                mock(AuditExportJobStore.class);

        when(store.find(any())).thenReturn(Optional.empty());

        PaymentAuditExportService service =
                new PaymentAuditExportService(
                        store,
                        exportId -> { },
                        Clock.fixed(NOW, ZoneOffset.UTC),
                        Duration.ofHours(1)
                );

        assertThrows(
                AuditExportNotFoundException.class,
                () -> service.get(UUID.randomUUID())
        );
    }

    private static RequestPaymentAuditExportCommand command() {
        return new RequestPaymentAuditExportCommand(
                "audit-export-001",
                NOW.minus(Duration.ofDays(1)),
                NOW.minus(Duration.ofMinutes(1)),
                List.of(),
                List.of("SIXPAY_BANK"),
                List.of("POSTING_CONFIRMED"),
                List.of(AuditResult.SUCCESS),
                "Regulatory evidence review",
                AuditExportFormat.CSV,
                "audit-service",
                UUID.randomUUID()
        );
    }

    private static AuditExportJobDefinition job(
            UUID exportId,
            RequestPaymentAuditExportCommand command
    ) {
        return new AuditExportJobDefinition(
                exportId,
                command.idempotencyKey(),
                "fingerprint",
                AuditExportStatus.ACCEPTED,
                command.occurredFrom(),
                command.occurredTo(),
                command.paymentIds(),
                command.financialInstitutionCodes(),
                command.actions(),
                command.results(),
                command.businessPurpose(),
                command.format(),
                command.requestedBy(),
                command.correlationId(),
                NOW,
                NOW.plus(Duration.ofHours(1)),
                null,
                null,
                null,
                null
        );
    }
}
