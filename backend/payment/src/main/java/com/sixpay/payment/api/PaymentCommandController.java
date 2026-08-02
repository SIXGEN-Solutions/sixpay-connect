package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/payments")
@Tag(
        name = "Payment Commands",
        description = "TresorPay Payment initiation API"
)
@SecurityRequirement(name = "bearerAuth")
public class PaymentCommandController {

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";
    private static final String IDEMPOTENCY_HEADER =
            "Idempotency-Key";

    private final PaymentInitiationUseCase initiationUseCase;
    private final PaymentCommandApiMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    public PaymentCommandController(
            PaymentInitiationUseCase initiationUseCase,
            PaymentCommandApiMapper mapper,
            CurrentUserProvider currentUserProvider
    ) {
        this.initiationUseCase = initiationUseCase;
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/initiate")
    @PreAuthorize(
            "hasAuthority('SCOPE_payment.initiate')"
    )
    @Operation(
            operationId = "initiateDebit",
            summary = "Initiate a debit order"
    )
    public ResponseEntity<InitiateDebitResponse> initiateDebit(
            @Valid @RequestBody InitiateDebitRequest request,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = 128)
            String idempotencyKey,
            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = 150)
            String correlationHeader
    ) {
        CorrelationId correlationId =
                correlation(correlationHeader);

        String authenticatedPartnerLoginName =
                currentUserProvider
                        .requireCurrentUser()
                        .username();

        var result = initiationUseCase.initiateDebit(
                mapper.toCommand(
                        request,
                        authenticatedPartnerLoginName,
                        idempotencyKey,
                        correlationId
                )
        );

        return ResponseEntity.ok()
                .header(
                        CORRELATION_HEADER,
                        correlationId.value()
                )
                .body(mapper.toResponse(result));
    }

    private static CorrelationId correlation(
            String correlationHeader
    ) {
        return correlationHeader == null
                || correlationHeader.isBlank()
                ? CorrelationId.generate()
                : CorrelationId.of(
                        correlationHeader.strip()
                );
    }
}
