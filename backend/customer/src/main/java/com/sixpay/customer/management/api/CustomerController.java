package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.request.AddCustomerBankAccountRequest;
import com.sixpay.customer.management.api.request.CustomerStatusReasonRequest;
import com.sixpay.customer.management.api.request.UpdateCustomerRequest;
import com.sixpay.customer.management.api.response.CustomerResponse;
import com.sixpay.customer.management.api.response.CustomerBankAccountResponse;
import com.sixpay.customer.management.application.audit.CustomerAuditRecorder;
import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.input.CustomerManagementUseCase;
import com.sixpay.customer.management.application.port.input.CustomerQueryUseCase;
import com.sixpay.customer.management.application.port.input.EnrollCustomerCommand;
import com.sixpay.customer.management.application.port.input.EnrollCustomerUseCase;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/internal/api/v1/customers")
@Validated
@Tag(
        name = "Customer Management",
        description = "Internal SIXPAY customer management API"
)
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {

    private static final String CORRELATION_HEADER = "X-Correlation-ID";
    private static final int HEADER_MAX_LENGTH = 150;

    private final EnrollCustomerUseCase enrollment;
    private final CustomerManagementUseCase management;
    private final CustomerQueryUseCase query;
    private final CustomerAuditRecorder audit;

    public CustomerController(
            EnrollCustomerUseCase enrollment,
            CustomerManagementUseCase management,
            CustomerQueryUseCase query,
            CustomerAuditRecorder audit
    ) {
        this.enrollment = enrollment;
        this.management = management;
        this.query = query;
        this.audit = audit;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.create')")
    @Operation(summary = "Enroll a verified banking customer into SIXPAY")
    public ResponseEntity<CustomerResponse> create(
            @RequestParam String financialInstitutionCode,
            @RequestParam(required = false) String niu,
            @RequestParam(required = false) String customerNumber,
            @RequestParam String accountReference,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId
    ) {
        String effectiveCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : correlationId.strip();

        CustomerResponse response = CustomerResponse.from(
                enrollment.enroll(
                        new EnrollCustomerCommand(
                                financialInstitutionCode,
                                niu,
                                customerNumber,
                                accountReference,
                                effectiveCorrelationId
                        )
                ).customer()
        );

        var location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        audit.success(
                "CUSTOMER",
                response.id(),
                "CUSTOMER_CREATED",
                effectiveCorrelationId,
                "Customer enrolled after fresh banking verification"
        );

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_customer.read')")
    @Operation(summary = "Get a SIXPAY customer")
    public CustomerResponse findById(
            @PathVariable UUID customerId
    ) {
        return CustomerResponse.from(
                query.findById(
                        new CustomerId(customerId)
                )
        );
    }

    @PutMapping("/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(summary = "Update editable SIXPAY customer profile fields")
    public CustomerResponse update(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        CustomerResponse response = CustomerResponse.from(
                management.updateProfile(
                        new CustomerId(customerId),
                        request.legalName(),
                        request.email(),
                        request.phoneNumber(),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_UPDATED", null,
                "Editable Customer profile fields updated");
        return response;
    }

    @PostMapping("/{customerId}/suspension")
    @PreAuthorize("hasAuthority('SCOPE_customer.suspend')")
    @Operation(summary = "Suspend a SIXPAY customer")
    public CustomerResponse suspend(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerStatusReasonRequest request
    ) {
        CustomerResponse response = CustomerResponse.from(
                management.suspend(
                        new CustomerId(customerId),
                        request.reason(),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_SUSPENDED", null,
                "Customer suspended; reason=" + request.reason());
        return response;
    }

    @PostMapping("/{customerId}/reactivation")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(summary = "Reactivate a suspended SIXPAY customer")
    public CustomerResponse reactivate(
            @PathVariable UUID customerId
    ) {
        CustomerResponse response = CustomerResponse.from(
                management.reactivate(
                        new CustomerId(customerId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_REACTIVATED", null,
                "Customer reactivated");
        return response;
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(
            summary = "Close a SIXPAY customer",
            description = "Logical delete only; customer data is not physically deleted"
    )
    public ResponseEntity<Void> close(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerStatusReasonRequest request
    ) {
        management.close(
                new CustomerId(customerId),
                request.reason(),
                Instant.now()
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_CLOSED", null,
                "Customer logically closed; reason=" + request.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{customerId}/accounts")
    @PreAuthorize("hasAuthority('SCOPE_customer.read')")
    @Operation(summary = "List customer bank accounts")
    public java.util.List<CustomerBankAccountResponse> accounts(
            @PathVariable UUID customerId
    ) {
        return query.findById(new CustomerId(customerId))
                .bankAccounts()
                .stream()
                .map(CustomerBankAccountResponse::from)
                .toList();
    }

    @PostMapping("/{customerId}/accounts")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(
            summary = "Lookup, freshly verify and link a bank account to a customer"
    )
    public CustomerResponse addAccount(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCustomerBankAccountRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId
    ) {
        String effectiveCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : correlationId.strip();

        CustomerResponse response = CustomerResponse.from(
                management.addBankAccount(
                        new CustomerId(customerId),
                        new AddBankAccountCommand(
                                request.accountReference(),
                                effectiveCorrelationId
                        ),
                        Instant.now()
                )
        );

        audit.success(
                "CUSTOMER",
                customerId,
                "CUSTOMER_BANK_ACCOUNT_LINKED",
                effectiveCorrelationId,
                "Verified bank account linked to Customer"
        );

        return response;
    }

    @PutMapping("/{customerId}/accounts/{accountId}/default")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(summary = "Set the default customer bank account")
    public CustomerResponse makeDefaultAccount(
            @PathVariable UUID customerId,
            @PathVariable UUID accountId
    ) {
        CustomerResponse response = CustomerResponse.from(
                management.makeDefaultBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_DEFAULT_ACCOUNT_CHANGED", null,
                "Default bank account changed to " + accountId);
        return response;
    }

    @DeleteMapping("/{customerId}/accounts/{accountId}")
    @PreAuthorize("hasAuthority('SCOPE_customer.update')")
    @Operation(summary = "Unlink a bank account from a customer")
    public CustomerResponse removeAccount(
            @PathVariable UUID customerId,
            @PathVariable UUID accountId
    ) {
        CustomerResponse response = CustomerResponse.from(
                management.removeBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_BANK_ACCOUNT_UNLINKED", null,
                "Bank account unlinked: " + accountId);
        return response;
    }
}
