package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.integration.http.CorrelationIdResolver;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import com.sixpay.payment.api.request.InitiateDebitRequest;
import com.sixpay.payment.api.response.InitiateDebitResponse;
import com.sixpay.payment.application.port.in.PaymentInitiationUseCase;
import com.sixpay.security.authentication.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/payments")
@Tag(name = "Payment Commands", description = "TresorPay Payment initiation API")
@SecurityRequirement(name = "mutualTLS")
@SecurityRequirement(name = "oauth2")
public class PaymentCommandController {

    private final PaymentInitiationUseCase initiationUseCase;
    private final PaymentCommandApiMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final CorrelationIdResolver correlationIdResolver;

    public PaymentCommandController(
            PaymentInitiationUseCase initiationUseCase,
            PaymentCommandApiMapper mapper,
            CurrentUserProvider currentUserProvider,
            CorrelationIdResolver correlationIdResolver
    ) {
        this.initiationUseCase = initiationUseCase;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.correlationIdResolver = correlationIdResolver;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('SCOPE_payment.initiate')")
    @Operation(operationId = "initiateDebit", summary = "Initiate a debit order")
    public ResponseEntity<InitiateDebitResponse> initiateDebit(
            @Valid @RequestBody InitiateDebitRequest request,
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

        String authenticatedPartner =
                currentUserProvider.requireCurrentUser().username();

        var result = initiationUseCase.initiateDebit(
                mapper.toCommand(
                        request,
                        authenticatedPartner,
                        idempotencyKey,
                        correlationId
                )
        );

        return ResponseEntity.ok()
                .header(
                        IntegrationHttpHeaders.CORRELATION_ID,
                        correlationId.value()
                )
                .body(mapper.toResponse(result));
    }
}
