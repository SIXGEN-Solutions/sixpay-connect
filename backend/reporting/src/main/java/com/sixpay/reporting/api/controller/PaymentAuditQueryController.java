package com.sixpay.reporting.api.controller;

import com.sixpay.reporting.api.dto.PaymentAuditPageResponse;
import com.sixpay.reporting.api.dto.PaymentAuditRecordResponse;
import com.sixpay.reporting.api.dto.PaymentTimelinePageResponse;
import com.sixpay.reporting.api.mapper.PaymentAuditQueryApiMapper;
import com.sixpay.reporting.application.port.input.GetPaymentAuditRecordUseCase;
import com.sixpay.reporting.application.port.input.GetPaymentTimelineUseCase;
import com.sixpay.reporting.application.port.input.SearchPaymentAuditRecordsUseCase;
import com.sixpay.reporting.application.port.output.PaymentAuditAccessRecorder;
import com.sixpay.reporting.application.query.*;
import com.sixpay.reporting.domain.model.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@RestController
public class PaymentAuditQueryController {

    private static final String READ_SCOPE =
            "hasAuthority('SCOPE_payment.audit.read')";
    private static final String CORRELATION =
            "X-Correlation-ID";

    private final GetPaymentTimelineUseCase timelineUseCase;
    private final SearchPaymentAuditRecordsUseCase searchUseCase;
    private final GetPaymentAuditRecordUseCase getUseCase;
    private final PaymentAuditQueryApiMapper mapper;
    private final PaymentAuditAccessRecorder accessRecorder;
    private final @Qualifier("reportingAuditClock") Clock clock;

    public PaymentAuditQueryController(
            GetPaymentTimelineUseCase timelineUseCase,
            SearchPaymentAuditRecordsUseCase searchUseCase,
            GetPaymentAuditRecordUseCase getUseCase,
            PaymentAuditQueryApiMapper mapper,
            PaymentAuditAccessRecorder accessRecorder,
            @Qualifier("reportingAuditClock") Clock clock
    ) {
        this.timelineUseCase = Objects.requireNonNull(timelineUseCase);
        this.searchUseCase = Objects.requireNonNull(searchUseCase);
        this.getUseCase = Objects.requireNonNull(getUseCase);
        this.mapper = Objects.requireNonNull(mapper);
        this.accessRecorder = Objects.requireNonNull(accessRecorder);
        this.clock = Objects.requireNonNull(clock);
    }

    @GetMapping("/internal/api/v1/payments/{paymentId}/timeline")
    @PreAuthorize(READ_SCOPE)
    public PaymentTimelinePageResponse timeline(
            @RequestHeader(CORRELATION) UUID correlationId,
            @PathVariable UUID paymentId,
            @RequestParam(required = false)
            AuditEvidenceCategory category,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant occurredFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant occurredTo,
            @RequestParam(required = false)
            @Size(min = 1, max = 2048)
            String cursor,
            @RequestParam(required = false)
            @Min(1) @Max(200)
            Integer size,
            Authentication authentication,
            HttpServletResponse response
    ) {
        response.setHeader(CORRELATION, correlationId.toString());

        PaymentTimelinePageResponse result = mapper.toResponse(
                timelineUseCase.getTimeline(
                        new PaymentTimelineQuery(
                                paymentId,
                                category,
                                occurredFrom,
                                occurredTo,
                                cursor(cursor),
                                size(size),
                                snapshot(cursor)
                        )
                )
        );

        accessRecorder.recordSuccessfulRead(
                "GET_PAYMENT_TIMELINE",
                "PAYMENT",
                paymentId.toString(),
                correlationId,
                actor(authentication)
        );

        return result;
    }

    @GetMapping("/internal/api/v1/payment-audit-records")
    @PreAuthorize(READ_SCOPE)
    public PaymentAuditPageResponse search(
            @RequestHeader(CORRELATION) UUID correlationId,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false)
            @Size(min = 1, max = 64)
            String paymentReference,
            @RequestParam(required = false) UUID observedCustomerId,
            @RequestParam(required = false)
            @Size(min = 1, max = 128)
            String actorId,
            @RequestParam(required = false) AuditActorType actorType,
            @RequestParam(required = false)
            @Size(min = 1, max = 100)
            String action,
            @RequestParam(required = false) AuditResult result,
            @RequestParam(required = false)
            @Size(min = 1, max = 64)
            String reasonCode,
            @RequestParam(required = false) UUID correlationIdFilter,
            @RequestParam(required = false) AuditSourceSystem sourceSystem,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant occurredFrom,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant occurredTo,
            @RequestParam(required = false) AuditSort sort,
            @RequestParam(required = false)
            @Size(min = 1, max = 2048)
            String cursor,
            @RequestParam(required = false)
            @Min(1) @Max(200)
            Integer size,
            Authentication authentication,
            HttpServletResponse response
    ) {
        response.setHeader(CORRELATION, correlationId.toString());

        PaymentAuditPageResponse page = mapper.toResponse(
                searchUseCase.search(
                        new PaymentAuditSearchQuery(
                                paymentId,
                                paymentReference,
                                observedCustomerId,
                                actorId,
                                actorType,
                                action,
                                result,
                                reasonCode,
                                correlationIdFilter,
                                sourceSystem,
                                occurredFrom,
                                occurredTo,
                                sort,
                                cursor(cursor),
                                size(size),
                                snapshot(cursor)
                        )
                )
        );

        accessRecorder.recordSuccessfulRead(
                "SEARCH_PAYMENT_AUDIT_RECORDS",
                "AUDIT_QUERY",
                "payment-audit-records",
                correlationId,
                actor(authentication)
        );

        return page;
    }

    @GetMapping("/internal/api/v1/payment-audit-records/{auditId}")
    @PreAuthorize(READ_SCOPE)
    public PaymentAuditRecordResponse get(
            @RequestHeader(CORRELATION) UUID correlationId,
            @PathVariable UUID auditId,
            Authentication authentication,
            HttpServletResponse response
    ) {
        response.setHeader(CORRELATION, correlationId.toString());

        PaymentAuditRecordResponse record = mapper.toResponse(
                getUseCase.get(
                        new GetPaymentAuditRecordQuery(auditId)
                )
        );

        accessRecorder.recordSuccessfulRead(
                "GET_PAYMENT_AUDIT_RECORD",
                "AUDIT_QUERY",
                auditId.toString(),
                correlationId,
                actor(authentication)
        );

        return record;
    }

    private AuditCursor cursor(String value) {
        return value == null ? null : new AuditCursor(value);
    }

    private int size(Integer value) {
        return value == null
                ? PaymentAuditSearchQuery.DEFAULT_SIZE
                : value;
    }

    private Instant snapshot(String cursor) {
        return cursor == null ? clock.instant() : null;
    }

    private static String actor(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "unknown-authenticated-actor";
        }
        return authentication.getName();
    }
}
