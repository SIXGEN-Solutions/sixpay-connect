package com.sixpay.customer.management.api;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.api.request.LinkObservedCustomerRequest;
import com.sixpay.customer.management.api.request.UnlinkObservedCustomerRequest;
import com.sixpay.customer.management.api.response.ObservedCustomerLinkResponse;
import com.sixpay.customer.management.application.port.input.ObservedCustomerLinkUseCase;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.security.authentication.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/observed-customers")
@Tag(
        name = "Observed Customer Linking",
        description = "Explicit optional correlation between observed projections and Customer master data"
)
@SecurityRequirement(name = "bearerAuth")
public class ObservedCustomerLinkController {

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";
    private static final int HEADER_MAX_LENGTH = 150;

    private final ObservedCustomerLinkUseCase links;
    private final CurrentUserProvider currentUserProvider;

    public ObservedCustomerLinkController(
            ObservedCustomerLinkUseCase links,
            CurrentUserProvider currentUserProvider
    ) {
        this.links = links;
        this.currentUserProvider = currentUserProvider;
    }

    @PutMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Explicitly link an ObservedCustomer to an enrolled Customer",
            description = "Does not create or update Customer master data"
    )
    public ObservedCustomerLinkResponse link(
            @PathVariable UUID observedCustomerId,
            @Valid @RequestBody LinkObservedCustomerRequest request,
            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = HEADER_MAX_LENGTH)
            String correlationId
    ) {
        return ObservedCustomerLinkResponse.from(
                links.link(
                        observedCustomerId,
                        new CustomerId(request.customerId()),
                        actor(),
                        correlation(correlationId),
                        request.reason(),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove the active Customer correlation",
            description = "The ObservedCustomer projection remains unchanged"
    )
    public ResponseEntity<Void> unlink(
            @PathVariable UUID observedCustomerId,
            @Valid @RequestBody UnlinkObservedCustomerRequest request,
            @RequestHeader(
                    name = CORRELATION_HEADER,
                    required = false
            )
            @Size(max = HEADER_MAX_LENGTH)
            String correlationId
    ) {
        links.unlink(
                observedCustomerId,
                actor(),
                correlation(correlationId),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{observedCustomerId}/customer-link")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
    )
    @Operation(summary = "Get the active Customer correlation")
    public ResponseEntity<ObservedCustomerLinkResponse> find(
            @PathVariable UUID observedCustomerId
    ) {
        return links.findLinked(observedCustomerId)
                .map(ObservedCustomerLinkResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(
                        () -> ResponseEntity.notFound().build()
                );
    }

    @GetMapping("/customer-links")
    @PreAuthorize(
            "hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')"
    )
    @Operation(
            summary = "List observed projections explicitly linked to one Customer"
    )
    public List<ObservedCustomerLinkResponse> findByCustomer(
            @RequestParam UUID customerId
    ) {
        return links.findByCustomerId(
                        new CustomerId(customerId)
                )
                .stream()
                .map(ObservedCustomerLinkResponse::from)
                .toList();
    }

    private String actor() {
        return currentUserProvider
                .requireCurrentUser()
                .subject();
    }

    private static String correlation(
            String correlationId
    ) {
        return (
                correlationId == null
                        || correlationId.isBlank()
        )
                ? CorrelationId.generate().value()
                : CorrelationId.of(
                        correlationId.strip()
                ).value();
    }
}
