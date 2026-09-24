package com.sixpay.payment.api.partner.partner;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.PaymentNotFoundException;
import com.sixpay.payment.api.partner.partner.response.PartnerPaymentRecoveryResponse;
import com.sixpay.payment.application.port.input.PaymentRecoveryUseCase;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * External PARTNER recovery endpoint.
 */
@RestController
@RequestMapping("/api/v1/integrations/partner/payments")
@Tag(
        name = "PARTNER payments",
        description = "PARTNER Payment recovery API"
)
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "subscriptionKey")
public class PartnerPaymentRecoveryController {

    private final PaymentRecoveryUseCase recoveryUseCase;
    private final CorrelationIdResolver correlationIdResolver;

    public PartnerPaymentRecoveryController(
            PaymentRecoveryUseCase recoveryUseCase,
            CorrelationIdResolver correlationIdResolver
    ) {
        this.recoveryUseCase = recoveryUseCase;
        this.correlationIdResolver = correlationIdResolver;
    }

    @GetMapping("/{paymentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getPartnerPayment",
            summary = "Recover the current SIXPAY Payment state"
    )
    public ResponseEntity<PartnerPaymentRecoveryResponse> getPayment(
            @PathVariable
            @Pattern(regexp = "^PAY-[0-9A-HJKMNP-TV-Z]{26}$")
            String paymentReference,
            @RequestHeader(
                    name = IntegrationHttpHeaders.CORRELATION_ID
            )
            String correlationHeader
    ) {
        CorrelationId correlationId =
                correlationIdResolver.resolve(correlationHeader);

        var view = recoveryUseCase
                .findByPaymentReference(
                        PublicPaymentReference.of(paymentReference)
                )
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentReference)
                );

        var response = new PartnerPaymentRecoveryResponse(
                view.paymentId(),
                view.paymentReference(),
                view.externalPaymentReference(),
                view.status(),
                new PartnerPaymentRecoveryResponse.Money(
                        view.amount().amount(),
                        view.amount().currency()
                ),
                view.receivedAt(),
                view.updatedAt(),
                view.finalizedAt()
        );

        return ResponseEntity.ok()
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.value()
                )
                .body(response);
    }
}
