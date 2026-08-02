package com.sixpay.payment.infrastructure.web;

import com.sixpay.payment.application.port.in.PaymentProjectionQueryUseCase;
import com.sixpay.payment.application.query.PaymentSearchSort;
import com.sixpay.payment.application.query.SearchPaymentProjectionsQuery;
import com.sixpay.payment.infrastructure.web.dto.PaymentQueryResponses;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/payments")
@Validated
@ConditionalOnBean(PaymentProjectionQueryUseCase.class)
public class PaymentQueryController {

    private final PaymentProjectionQueryUseCase queryUseCase;
    private final PaymentQueryRestMapper mapper;

    public PaymentQueryController(
            PaymentProjectionQueryUseCase queryUseCase,
            PaymentQueryRestMapper mapper
    ) {
        this.queryUseCase = queryUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("@paymentAccessPolicy.canSearch()")
    public ResponseEntity<
            PaymentQueryResponses.PaymentSearchPageResponse
            > searchPayments(
            @RequestHeader("X-Correlation-ID")
            UUID correlationId,
            @RequestParam(required = false)
            @Size(max = 2048)
            String cursor,
            @RequestParam(defaultValue = "50")
            @Min(1)
            @Max(200)
            int size,
            @RequestParam(required = false)
            @Size(max = 64)
            String paymentReference,
            @RequestParam(required = false)
            @Size(max = 100)
            String tresorPayRequestId,
            @RequestParam(required = false)
            UUID observedCustomerId,
            @RequestParam(required = false)
            @Size(max = 32)
            String financialInstitutionCode,
            @RequestParam(required = false)
            String status,
            @RequestParam(required = false)
            @Size(max = 64)
            String reasonCode,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant createdTo,
            @RequestParam(required = false)
            BigDecimal amountMin,
            @RequestParam(required = false)
            BigDecimal amountMax,
            @RequestParam(required = false)
            @Pattern(regexp = "^[A-Z]{3}$")
            String currency,
            @RequestParam(defaultValue = "CREATED_AT_DESC")
            PaymentSearchSort sort
    ) {
        var page = queryUseCase.search(
                new SearchPaymentProjectionsQuery(
                        cursor,
                        size,
                        paymentReference,
                        tresorPayRequestId,
                        observedCustomerId,
                        financialInstitutionCode,
                        status,
                        reasonCode,
                        createdFrom,
                        createdTo,
                        amountMin,
                        amountMax,
                        currency,
                        sort
                )
        );

        return ResponseEntity.ok()
                .header(
                        "X-Correlation-ID",
                        correlationId.toString()
                )
                .body(mapper.toResponse(page));
    }

    @GetMapping("/{paymentId}")
    @PreAuthorize("@paymentAccessPolicy.canRead()")
    public ResponseEntity<
            PaymentQueryResponses.PaymentDetailResponse
            > getPayment(
            @RequestHeader("X-Correlation-ID")
            UUID correlationId,
            @PathVariable
            UUID paymentId
    ) {
        var detail = queryUseCase.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentProjectionNotFoundException(
                                paymentId
                        )
                );

        return ResponseEntity.ok()
                .header(
                        "X-Correlation-ID",
                        correlationId.toString()
                )
                .body(mapper.toResponse(detail));
    }
}
