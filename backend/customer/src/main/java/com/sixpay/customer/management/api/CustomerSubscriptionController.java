package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.request.CreateCustomerSubscriptionRequest;
import com.sixpay.customer.management.api.request.SubscriptionReasonRequest;
import com.sixpay.customer.management.api.response.CustomerSubscriptionResponse;
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

    public CustomerSubscriptionController(
            CustomerSubscriptionUseCase subscriptions
    ) {
        this.subscriptions = subscriptions;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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

        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/{subscriptionId}/activation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or reactivate a subscription")
    public CustomerSubscriptionResponse activate(
            @PathVariable UUID subscriptionId
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.activate(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        Instant.now()
                )
        );
    }

    @PostMapping("/{subscriptionId}/suspension")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend an active subscription")
    public CustomerSubscriptionResponse suspend(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        return CustomerSubscriptionResponse.from(
                subscriptions.suspend(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        request.reason(),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{subscriptionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Close a subscription",
            description = "Logical close; history is retained"
    )
    public ResponseEntity<Void> close(
            @PathVariable UUID subscriptionId,
            @Valid @RequestBody SubscriptionReasonRequest request
    ) {
        subscriptions.close(
                new CustomerSubscriptionId(
                        subscriptionId
                ),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{subscriptionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
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
