package com.sixpay.reporting.application.service;

import com.sixpay.reporting.application.exception.AuditExportNotFoundException;
import com.sixpay.reporting.application.exception.AuditExportPolicyException;
import com.sixpay.reporting.application.port.input.GetPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.input.RequestPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.output.AuditExportDispatchPort;
import com.sixpay.reporting.application.port.output.AuditExportJobStore;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.AuditExportStatus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class PaymentAuditExportService
        implements RequestPaymentAuditExportUseCase,
        GetPaymentAuditExportUseCase {

    private final AuditExportJobStore jobStore;
    private final AuditExportDispatchPort dispatchPort;
    private final Clock clock;
    private final Duration retention;

    public PaymentAuditExportService(
            AuditExportJobStore jobStore,
            AuditExportDispatchPort dispatchPort,
            Clock clock,
            Duration retention
    ) {
        this.jobStore = Objects.requireNonNull(jobStore);
        this.dispatchPort = Objects.requireNonNull(dispatchPort);
        this.clock = Objects.requireNonNull(clock);
        this.retention = Objects.requireNonNull(retention);
    }

    @Override
    public PaymentAuditExportJobView request(
            RequestPaymentAuditExportCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        Instant now = clock.instant();
        if (command.occurredTo().isAfter(now)) {
            throw new AuditExportPolicyException(
                    "Audit export period cannot end in the future"
            );
        }

        String fingerprint =
                AuditExportRequestFingerprint.compute(command);

        AuditExportAcceptance acceptance =
                jobStore.accept(
                        command,
                        fingerprint,
                        now,
                        now.plus(retention)
                );

        if (acceptance.newlyCreated()) {
            dispatchPort.dispatch(
                    acceptance.job().exportId()
            );
        }

        return acceptance.job().toView();
    }

    @Override
    public PaymentAuditExportJobView get(UUID exportId) {
        Objects.requireNonNull(exportId, "exportId is required");

        jobStore.expire(clock.instant());

        AuditExportJobDefinition job = jobStore.find(exportId)
                .orElseThrow(() ->
                        new AuditExportNotFoundException(
                                "Payment audit export was not found"
                        )
                );

        return job.toView();
    }
}
