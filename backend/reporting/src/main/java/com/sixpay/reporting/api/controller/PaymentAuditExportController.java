package com.sixpay.reporting.api.controller;

import com.sixpay.reporting.api.dto.PaymentAuditExportJobResponse;
import com.sixpay.reporting.api.dto.PaymentAuditExportRequest;
import com.sixpay.reporting.api.mapper.PaymentAuditExportApiMapper;
import com.sixpay.reporting.application.port.input.GetPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.input.RequestPaymentAuditExportUseCase;
import com.sixpay.reporting.application.port.output.PaymentAuditAccessRecorder;
import com.sixpay.reporting.application.query.PaymentAuditExportJobView;
import com.sixpay.reporting.application.query.RequestPaymentAuditExportCommand;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/payment-audit-exports")
public final class PaymentAuditExportController {

    private static final String EXPORT_SCOPE =
            "hasAuthority('SCOPE_payment.audit.read') "
                    + "and hasAuthority('SCOPE_payment.audit.export')";
    private static final String CORRELATION = "X-Correlation-ID";

    private final RequestPaymentAuditExportUseCase requestUseCase;
    private final GetPaymentAuditExportUseCase getUseCase;
    private final PaymentAuditExportApiMapper mapper;
    private final PaymentAuditAccessRecorder accessRecorder;

    public PaymentAuditExportController(
            RequestPaymentAuditExportUseCase requestUseCase,
            GetPaymentAuditExportUseCase getUseCase,
            PaymentAuditExportApiMapper mapper,
            PaymentAuditAccessRecorder accessRecorder
    ) {
        this.requestUseCase = Objects.requireNonNull(requestUseCase);
        this.getUseCase = Objects.requireNonNull(getUseCase);
        this.mapper = Objects.requireNonNull(mapper);
        this.accessRecorder = Objects.requireNonNull(accessRecorder);
    }

    @PostMapping
    @PreAuthorize(EXPORT_SCOPE)
    public ResponseEntity<PaymentAuditExportJobResponse> request(
            @RequestHeader(CORRELATION) UUID correlationId,
            @RequestHeader("Idempotency-Key")
            @NotBlank @Size(max = 150)
            String idempotencyKey,
            @Valid @RequestBody PaymentAuditExportRequest request,
            Authentication authentication
    ) {
        String actor = actor(authentication);

        PaymentAuditExportJobView job =
                requestUseCase.request(
                        new RequestPaymentAuditExportCommand(
                                idempotencyKey,
                                request.occurredFrom(),
                                request.occurredTo(),
                                request.paymentIds(),
                                request.financialInstitutionCodes(),
                                request.actions(),
                                request.results(),
                                request.businessPurpose(),
                                request.format(),
                                actor,
                                correlationId
                        )
                );

        URI location = URI.create(
                "/internal/api/v1/payment-audit-exports/"
                        + job.exportId()
        );

        accessRecorder.recordSuccessfulRead(
                "REQUEST_PAYMENT_AUDIT_EXPORT",
                "AUDIT_EXPORT",
                job.exportId().toString(),
                correlationId,
                actor
        );

        return ResponseEntity.accepted()
                .header(
                        CORRELATION,
                        correlationId.toString()
                )
                .header(
                        HttpHeaders.LOCATION,
                        location.toString()
                )
                .body(mapper.toResponse(job));
    }

    @GetMapping("/{exportId}")
    @PreAuthorize(EXPORT_SCOPE)
    public PaymentAuditExportJobResponse get(
            @RequestHeader(CORRELATION) UUID correlationId,
            @PathVariable UUID exportId,
            Authentication authentication,
            HttpServletResponse response
    ) {
        response.setHeader(
                CORRELATION,
                correlationId.toString()
        );

        PaymentAuditExportJobView job =
                getUseCase.get(exportId);

        accessRecorder.recordSuccessfulRead(
                "GET_PAYMENT_AUDIT_EXPORT",
                "AUDIT_EXPORT",
                exportId.toString(),
                correlationId,
                actor(authentication)
        );

        return mapper.toResponse(job);
    }

    private static String actor(
            Authentication authentication
    ) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "unknown-authenticated-actor";
        }
        return authentication.getName();
    }
}
