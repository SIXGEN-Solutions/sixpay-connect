package com.sixpay.partner.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.partner.api.request.ConfigureValidationThresholdRequest;
import com.sixpay.partner.api.request.CreatePartnerRequest;
import com.sixpay.partner.api.request.PartnerDecisionRequest;
import com.sixpay.partner.api.request.SuspendPartnerRequest;
import com.sixpay.partner.api.response.PartnerResponse;
import com.sixpay.partner.api.response.PartnerAuditPageResponse;
import com.sixpay.partner.api.response.PartnerStatusResponse;
import com.sixpay.partner.application.command.ConfigureValidationThresholdCommand;
import com.sixpay.partner.application.command.CreatePartnerCommand;
import com.sixpay.partner.application.command.DecidePartnerCommand;
import com.sixpay.partner.application.command.ReactivatePartnerCommand;
import com.sixpay.partner.application.command.SuspendPartnerCommand;
import com.sixpay.partner.application.port.in.PartnerManagementUseCase;
import com.sixpay.partner.application.port.in.PartnerQueryUseCase;
import com.sixpay.partner.domain.model.PartnerId;
import com.sixpay.security.authentication.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.validation.annotation.Validated;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/partners")
@Tag(name = "Partners", description = "Partner lifecycle and validation policy")
@SecurityRequirement(name = "bearerAuth")
public class PartnerController {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final int HEADER_MAX_LENGTH = 150;

    private final PartnerManagementUseCase management;
    private final PartnerQueryUseCase query;
    private final CurrentUserProvider currentUserProvider;

    public PartnerController(
            PartnerManagementUseCase management,
            PartnerQueryUseCase query,
            CurrentUserProvider currentUserProvider
    ) {
        this.management = management;
        this.query = query;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a partner in pending validation status")
    public ResponseEntity<PartnerResponse> create(
            @Valid @RequestBody CreatePartnerRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = HEADER_MAX_LENGTH) String idempotencyKey
    ) {
        var partner = PartnerResponse.from(management.create(new CreatePartnerCommand(
                request.legalName(),
                request.technicalContactName(),
                request.technicalContactEmail(),
                request.authorizedTransactionTypes(),
                actor(),
                correlation(correlationId),
                idempotencyKey
        )));
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(partner.id())
                .toUri();
        return ResponseEntity.created(location).body(partner);
    }

    @PostMapping("/{partnerId}/validation")
    @PreAuthorize("hasRole('MANAGER')")
    @Operation(summary = "Approve or reject a pending partner")
    public PartnerResponse decide(
            @PathVariable UUID partnerId,
            @Valid @RequestBody PartnerDecisionRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = HEADER_MAX_LENGTH) String idempotencyKey
    ) {
        return PartnerResponse.from(management.decide(new DecidePartnerCommand(
                new PartnerId(partnerId),
                request.decision(),
                request.reason(),
                actor(),
                correlation(correlationId),
                idempotencyKey
        )));
    }

    @PostMapping("/{partnerId}/suspension")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend an active partner")
    public PartnerResponse suspend(
            @PathVariable UUID partnerId,
            @Valid @RequestBody SuspendPartnerRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = HEADER_MAX_LENGTH) String idempotencyKey
    ) {
        return PartnerResponse.from(management.suspend(new SuspendPartnerCommand(
                new PartnerId(partnerId),
                request.reason(),
                actor(),
                correlation(correlationId),
                idempotencyKey
        )));
    }

    @PostMapping("/{partnerId}/reactivation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate a suspended partner")
    public PartnerResponse reactivate(
            @PathVariable UUID partnerId,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = HEADER_MAX_LENGTH) String idempotencyKey
    ) {
        return PartnerResponse.from(management.reactivate(new ReactivatePartnerCommand(
                new PartnerId(partnerId),
                actor(),
                correlation(correlationId),
                idempotencyKey
        )));
    }

    @PutMapping("/{partnerId}/validation-thresholds/{transactionType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Configure a validation threshold for a transaction type")
    public PartnerResponse configureValidationThreshold(
            @PathVariable UUID partnerId,
            @PathVariable String transactionType,
            @Valid @RequestBody ConfigureValidationThresholdRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId,
            @RequestHeader(name = IDEMPOTENCY_HEADER)
            @NotBlank @Size(max = HEADER_MAX_LENGTH) String idempotencyKey
    ) {
        return PartnerResponse.from(management.configureValidationThreshold(
                new ConfigureValidationThresholdCommand(
                        new PartnerId(partnerId),
                        transactionType,
                        request.currency(),
                        request.amount(),
                        request.validationLevels(),
                        actor(),
                        correlation(correlationId),
                        idempotencyKey
                )
        ));
    }

    @GetMapping("/{partnerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
    @Operation(summary = "Get the complete partner record")
    public PartnerResponse findById(@PathVariable UUID partnerId) {
        return PartnerResponse.from(query.findById(new PartnerId(partnerId)));
    }

    @GetMapping("/{partnerId}/status")
    @PreAuthorize("@partnerAccessPolicy.canRead(#partnerId)")
    @Operation(summary = "Get current partner connection eligibility status")
    public PartnerStatusResponse status(@PathVariable UUID partnerId) {
        return PartnerStatusResponse.from(query.findById(new PartnerId(partnerId)));
    }

    @GetMapping("/{partnerId}/audit")
    @PreAuthorize("hasRole('AUDITOR')")
    @Operation(summary = "Query the immutable audit trail over a period")
    public PartnerAuditPageResponse audit(
            @PathVariable UUID partnerId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        return PartnerAuditPageResponse.from(
                query.findAuditTrail(new PartnerId(partnerId), from, to, page, size)
        );
    }

    private String actor() {
        return currentUserProvider.requireCurrentUser().subject();
    }

    private static CorrelationId correlation(String correlationId) {
        return correlationId == null || correlationId.isBlank()
                ? CorrelationId.generate()
                : CorrelationId.of(correlationId.strip());
    }
}