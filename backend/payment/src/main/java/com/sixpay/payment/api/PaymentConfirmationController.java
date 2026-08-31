package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.request.VerifyPaymentConfirmationRequest;
import com.sixpay.payment.api.response.PaymentConfirmationResponse;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.application.port.output.idempotency.PaymentConfirmationIdempotencyPort;
import com.sixpay.payment.application.port.input.CreatePaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ReadPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.ResendPaymentConfirmationUseCase;
import com.sixpay.payment.application.port.input.VerifyPaymentConfirmationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@ConditionalOnBean({
        PaymentConfirmationGateway.class,
        PaymentConfirmationIdempotencyPort.class
})
@RequestMapping("/v1/payments/{paymentReference}/confirmation-challenge")
@Tag(
        name = "Payment Confirmation",
        description = "TRESOR PAY customer confirmation operations"
)
@SecurityRequirement(name = "mutualTLS")
@SecurityRequirement(name = "oauth2")
public class PaymentConfirmationController {

    private final CreatePaymentConfirmationUseCase createUseCase;
    private final ReadPaymentConfirmationUseCase readUseCase;
    private final VerifyPaymentConfirmationUseCase verifyUseCase;
    private final ResendPaymentConfirmationUseCase resendUseCase;
    private final PaymentConfirmationApiMapper mapper;
    private final CorrelationIdResolver correlationIdResolver;

    public PaymentConfirmationController(
            CreatePaymentConfirmationUseCase createUseCase,
            ReadPaymentConfirmationUseCase readUseCase,
            VerifyPaymentConfirmationUseCase verifyUseCase,
            ResendPaymentConfirmationUseCase resendUseCase,
            PaymentConfirmationApiMapper mapper,
            CorrelationIdResolver correlationIdResolver
    ) {
        this.createUseCase = createUseCase;
        this.readUseCase = readUseCase;
        this.verifyUseCase = verifyUseCase;
        this.resendUseCase = resendUseCase;
        this.mapper = mapper;
        this.correlationIdResolver = correlationIdResolver;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_payment.confirmation.create')")
    @Operation(
            operationId = "createPaymentConfirmationChallenge",
            summary = "Create and send a confirmation challenge"
    )
    public ResponseEntity<PaymentConfirmationResponse> create(
            @PathVariable String paymentReference,
            @RequestHeader(name = IntegrationHttpHeaders.IDEMPOTENCY_KEY)
            @NotBlank @Size(max = 128) String idempotencyKey,
            @RequestHeader(
                    name = IntegrationHttpHeaders.CORRELATION_ID,
                    required = false
            )
            @Size(max = 64) String correlationHeader
    ) {
        CorrelationId correlationId =
                correlationIdResolver.resolve(correlationHeader);
        var view = createUseCase.create(
                mapper.toCreateCommand(
                        paymentReference,
                        correlationId,
                        idempotencyKey
                )
        );
        return ok(correlationId, mapper.toResponse(view));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_payment.confirmation.read')")
    @Operation(
            operationId = "getPaymentConfirmationChallenge",
            summary = "Read the current confirmation challenge"
    )
    public ResponseEntity<PaymentConfirmationResponse> read(
            @PathVariable String paymentReference,
            @RequestHeader(
                    name = IntegrationHttpHeaders.CORRELATION_ID,
                    required = false
            )
            @Size(max = 64) String correlationHeader
    ) {
        CorrelationId correlationId =
                correlationIdResolver.resolve(correlationHeader);
        var view = readUseCase.read(
                mapper.toReadQuery(paymentReference, correlationId)
        );
        return ok(correlationId, mapper.toResponse(view));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('SCOPE_payment.confirmation.verify')")
    @Operation(
            operationId = "verifyPaymentConfirmationChallenge",
            summary = "Verify the customer OTP"
    )
    public ResponseEntity<PaymentConfirmationResponse> verify(
            @PathVariable String paymentReference,
            @Valid @RequestBody VerifyPaymentConfirmationRequest request,
            @RequestHeader(name = IntegrationHttpHeaders.IDEMPOTENCY_KEY)
            @NotBlank @Size(max = 128) String idempotencyKey,
            @RequestHeader(
                    name = IntegrationHttpHeaders.CORRELATION_ID,
                    required = false
            )
            @Size(max = 64) String correlationHeader
    ) {
        CorrelationId correlationId =
                correlationIdResolver.resolve(correlationHeader);
        var view = verifyUseCase.verify(
                mapper.toVerifyCommand(
                        paymentReference,
                        correlationId,
                        idempotencyKey,
                        request
                )
        );
        return ok(correlationId, mapper.toResponse(view));
    }

    @PostMapping("/resend")
    @PreAuthorize("hasAuthority('SCOPE_payment.confirmation.resend')")
    @Operation(
            operationId = "resendPaymentConfirmationChallenge",
            summary = "Replace and resend the confirmation challenge"
    )
    public ResponseEntity<PaymentConfirmationResponse> resend(
            @PathVariable String paymentReference,
            @RequestHeader(name = IntegrationHttpHeaders.IDEMPOTENCY_KEY)
            @NotBlank @Size(max = 128) String idempotencyKey,
            @RequestHeader(
                    name = IntegrationHttpHeaders.CORRELATION_ID,
                    required = false
            )
            @Size(max = 64) String correlationHeader
    ) {
        CorrelationId correlationId =
                correlationIdResolver.resolve(correlationHeader);
        var view = resendUseCase.resend(
                mapper.toResendCommand(
                        paymentReference,
                        correlationId,
                        idempotencyKey
                )
        );
        return ok(correlationId, mapper.toResponse(view));
    }

    private static ResponseEntity<PaymentConfirmationResponse> ok(
            CorrelationId correlationId,
            PaymentConfirmationResponse response
    ) {
        return ResponseEntity.ok()
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.value()
                )
                .body(response);
    }
}
