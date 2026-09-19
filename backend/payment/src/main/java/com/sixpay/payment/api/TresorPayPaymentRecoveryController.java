package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.response.TresorPayPaymentRecoveryResponse;
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
 * External TRESOR PAY recovery endpoint.
 */
@RestController
@RequestMapping("/api/v1/integrations/tresorpay/payments")
@Tag(
        name = "TRESOR PAY payments",
        description = "TRESOR PAY Payment recovery API"
)
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "subscriptionKey")
public class TresorPayPaymentRecoveryController {

    private final PaymentRecoveryUseCase recoveryUseCase;
    private final CorrelationIdResolver correlationIdResolver;

    public TresorPayPaymentRecoveryController(
            PaymentRecoveryUseCase recoveryUseCase,
            CorrelationIdResolver correlationIdResolver
    ) {
        this.recoveryUseCase = recoveryUseCase;
        this.correlationIdResolver = correlationIdResolver;
    }

    @GetMapping("/{paymentReference}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            operationId = "getTresorPayPayment",
            summary = "Recover the current SIXPAY Payment state"
    )
    public ResponseEntity<TresorPayPaymentRecoveryResponse> getPayment(
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

        var response = new TresorPayPaymentRecoveryResponse(
                view.paymentId(),
                view.paymentReference(),
                view.externalPaymentReference(),
                view.status(),
                new TresorPayPaymentRecoveryResponse.Money(
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
