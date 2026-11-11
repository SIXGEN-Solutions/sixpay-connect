package com.sixpay.payment.api;

import com.sixpay.payment.infrastructure.recovery.PaymentT0ManualReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Internal operator-only T0 reconciliation; never invokes financial posting. */
@RestController
@RequestMapping("/internal/api/v1/payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentT0ManualReconciliationController {
    public record Request(@NotEmpty @Size(max = 50) List<@NotNull UUID> paymentIds) { }
    public record Response(List<PaymentT0ManualReconciliationService.Result> results) { }

    private final PaymentT0ManualReconciliationService service;

    public PaymentT0ManualReconciliationController(PaymentT0ManualReconciliationService service) {
        this.service = service;
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("@paymentAccessPolicy.canReconcile()")
    @Operation(operationId = "reconcilePaymentsT0Manually",
            summary = "Trigger lookup-only T0 recovery for selected Payments")
    public ResponseEntity<Response> reconcile(
            @RequestHeader("X-Correlation-ID") UUID correlationId,
            @Valid @RequestBody Request request) {
        return ResponseEntity.ok().header("X-Correlation-ID", correlationId.toString())
                .body(new Response(service.reconcile(request.paymentIds(), correlationId)));
    }
}
