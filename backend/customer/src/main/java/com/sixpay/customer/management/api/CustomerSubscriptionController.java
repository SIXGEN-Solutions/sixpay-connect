package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.request.CreateCustomerSubscriptionRequest;
import com.sixpay.customer.management.api.request.SubscriptionReasonRequest;
import com.sixpay.customer.management.api.response.CustomerSubscriptionResponse;
import com.sixpay.customer.management.application.audit.CustomerAuditRecorder;
import com.sixpay.customer.management.application.port.input.CustomerSubscriptionUseCase;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.model.CustomerSubscriptionId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/subscriptions")
@Tag(
        name = "Customer Subscriptions",
        description = "Internal SIXPAY customer-partner subscription lifecycle"
)
@SecurityRequirement(name = "bearerAuth")
public class CustomerSubscriptionController {

    private final CustomerSubscriptionUseCase subscriptions;
    private final CustomerAuditRecorder audit;

    public CustomerSubscriptionController(
            CustomerSubscriptionUseCase subscriptions,
            CustomerAuditRecorder audit
    ) {
        this.subscriptions = subscriptions;
        this.audit = audit;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_subscription.create')")
    @Operation(summary = "Create a pending customer subscription")
    public ResponseEntity<CustomerSubscriptionResponse> create(
            @Valid @RequestBody CreateCustomerSubscriptionRequest request
    ) {
        CustomerSubscriptionResponse response =
                CustomerSubscriptionResponse.from(
                        subscriptions.create(
                                new CustomerId(
                                        request.customerId()
                                ),
                                request.partnerId(),
                                new CustomerBankAccountId(
                                        request.bankAccountId()
                                ),
                                Instant.now()
                        )
                );

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        audit.success(
                "SUBSCRIPTION",
                response.id(),
                "SUBSCRIPTION_CREATED",
                null,
                "Customer subscription created input pending activation status"
        );

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{subscriptionId}/activation")
    @PreAuthorize("hasAuthority('SCOPE_subscription.update')")
    @Operation(summary = "Activate or reactivate a subscription")
    public CustomerSubscriptionResponse activate(
            @PathVariable UUID subscriptionId
    ) {
        CustomerSubscriptionResponse response = CustomerSubscriptionResponse.from(
                subscriptions.activate(
                        new CustomerSubscriptionId(subscriptionId),
                        Instant.now()
                )
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_ACTIVATED", null,
                "Subscription activated or reactivated");
        return response;
    }

    @PostMapping("/{subscriptionId}/suspension")
    @PreAuthorize("hasAuthority('SCOPE_subscription.suspend')")
    @Operation(summary = "Suspend an active subscription")
    public CustomerSubscriptionResponse suspend(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        CustomerSubscriptionResponse response = CustomerSubscriptionResponse.from(
                subscriptions.suspend(
                        new CustomerSubscriptionId(subscriptionId),
                        request.reason(),
                        Instant.now()
                )
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_SUSPENDED", null,
                "Subscription suspended; reason=" + request.reason());
        return response;
    }

    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasAuthority('SCOPE_subscription.close')")
    @Operation(
            summary = "Close a subscription",
            description = "Logical close; history is retained"
    )
    public ResponseEntity<Void> close(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        subscriptions.close(
                new CustomerSubscriptionId(subscriptionId),
                request.reason(),
                Instant.now()
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_CLOSED", null,
                "Subscription closed; reason=" + request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasAuthority('SCOPE_subscription.read')")
    public CustomerSubscriptionResponse findById(
            @PathVariable UUID subscriptionId
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.findById(
                        new CustomerSubscriptionId(
                                subscriptionId
                        )
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_subscription.read')")
    public List<CustomerSubscriptionResponse> findByCustomer(
            @RequestParam UUID customerId
    ) {
        return subscriptions.findByCustomerId(
                        new CustomerId(customerId)
                )
                .stream()
                .map(CustomerSubscriptionResponse::from)
                .toList();
    }
}
