package com.sixpay.payment.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.api.request.InitiateDebitRequest;
import com.sixpay.payment.api.response.InitiateDebitResponse;
import com.sixpay.payment.application.port.in.PaymentInitiationUseCase;
import com.sixpay.payment.application.port.out.idempotency
        .PaymentInitiationIdempotencyPort;
import com.sixpay.payment.application.port.out.initiation
        .PaymentInitiationPreparationPort;
import com.sixpay.security.authentication.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnWebApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.UUID;

/**
 * Partner-facing Payment command API.
 */
@RestController
@RequestMapping("/v1/payments")
@Validated
@Tag(
        name = "Payment Commands",
        description = "TresorPay Payment initiation API"
)
@SecurityRequirement(name = "bearerAuth")
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnBean({
        PaymentInitiationIdempotencyPort.class,
        PaymentInitiationPreparationPort.class
})
public class PaymentCommandController {

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";

    private static final String IDEMPOTENCY_HEADER =
            "Idempotency-Key";

    private static final int HEADER_MAX_LENGTH = 150;

    private final PaymentInitiationUseCase initiationUseCase;
    private final PaymentCommandApiMapper mapper;
    private final CurrentUserProvider currentUserProvider;

    public PaymentCommandController(
            PaymentInitiationUseCase initiationUseCase,
            PaymentCommandApiMapper mapper,
            CurrentUserProvider currentUserProvider
    ) {
        this.initiationUseCase = Objects.requireNonNull(
                initiationUseCase,
                "Payment initiation use case"
        );

        this.mapper = Objects.requireNonNull(
                mapper,
                "Payment command API mapper"
        );

        this.currentUserProvider = Objects.requireNonNull(
                currentUserProvider,
                "Current user provider"
        );
    }

    @PostMapping("/initiate")
    @PreAuthorize(
            "hasAuthority('SCOPE_payment.initiate')"
    )
    @Operation(
            operationId = "initiateDebit",
            summary = "Initiate a debit order"
    )
    public ResponseEntity<InitiateDebitResponse>
    initiateDebit(
            @Valid
            @RequestBody
            InitiateDebitRequest request,

            @RequestHeader(
                    name = IDEMPOTENCY_HEADER
            )
            @NotBlank
            @Size(max = HEADER_MAX_LENGTH)
            String idempotencyKey,

            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = HEADER_MAX_LENGTH)
            String correlationHeader
    ) {

        CorrelationId correlationId =
                correlation(correlationHeader);

        var authenticatedUser =
                currentUserProvider.requireCurrentUser();

        var command = mapper.toCommand(
                request,
                authenticatedUser.username(),
                idempotencyKey,
                correlationId
        );

        var result =
                initiationUseCase.initiateDebit(command);

        return ResponseEntity.ok()
                .header(
                        CORRELATION_HEADER,
                        correlationId.value()
                )
                .body(
                        mapper.toResponse(result)
                );
    }

    private static CorrelationId correlation(
            String correlationHeader
    ) {
        if (correlationHeader == null
                || correlationHeader.isBlank()) {

            return CorrelationId.generate();
        }

        UUID value = UUID.fromString(
                correlationHeader.strip()
        );

        return CorrelationId.of(
                value.toString()
        );
    }
}