#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def guard():
    r = run("git", "rev-parse", "--show-toplevel")
    if r.returncode != 0:
        raise SystemExit("Run inside sixpay-connect.")
    if Path(r.stdout.strip()).resolve() != ROOT.resolve():
        raise SystemExit("Run from repository root.")
    b = run("git", "branch", "--show-current").stdout.strip()
    if b != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {b}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        existing = p.read_text(encoding="utf-8")
        if existing == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} already exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

def replace_once(rel, old, new, label):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"[skip] {label}: already applied")
        return
    if text.count(old) != 1:
        raise SystemExit(f"[stop] {label}: expected one match, found {text.count(old)}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
    print(f"[update] {label}")

guard()

B = "backend/customer/src/main/java/com/sixpay/customer/management"
API = B + "/api"
REQ = API + "/request"
RESP = API + "/response"
IN = B + "/application/port/input"
SVC = B + "/application/service"
TEST = "backend/customer/src/test/java/com/sixpay/customer/management"

# ------------------------------------------------------------------
# Domain update capability
# ------------------------------------------------------------------
customer_rel = B + "/domain/model/Customer.java"
replace_once(
    customer_rel,
    "    private final String legalName;\n"
    "    private final String email;\n"
    "    private final String phoneNumber;\n",
    "    private String legalName;\n"
    "    private String email;\n"
    "    private String phoneNumber;\n",
    "make customer editable profile fields"
)

replace_once(
    customer_rel,
    """    public void suspend(String reason, Instant now) {
""",
    """    public void updateProfile(
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    ) {
        requireMutable("update customer profile");
        requireTime(now);

        this.legalName = requireText(
                legalName,
                "legalName",
                MAX_LEGAL_NAME_LENGTH
        );
        this.email = normalizeEmail(email);
        this.phoneNumber = optionalText(
                phoneNumber,
                "phoneNumber",
                MAX_PHONE_LENGTH
        );
        this.updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
""",
    "add customer profile update"
)

# ------------------------------------------------------------------
# Application API
# ------------------------------------------------------------------
create(IN + "/CustomerManagementUseCase.java", """package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.time.Instant;

public interface CustomerManagementUseCase {

    Customer updateProfile(
            CustomerId customerId,
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    );

    Customer suspend(
            CustomerId customerId,
            String reason,
            Instant now
    );

    Customer reactivate(
            CustomerId customerId,
            Instant now
    );

    Customer close(
            CustomerId customerId,
            String reason,
            Instant now
    );

    Customer addBankAccount(
            CustomerId customerId,
            AddBankAccountCommand command,
            Instant now
    );

    Customer makeDefaultBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    );

    Customer removeBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    );
}
""")

create(IN + "/CustomerQueryUseCase.java", """package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

public interface CustomerQueryUseCase {
    Customer findById(CustomerId customerId);
}
""")

create(IN + "/AddBankAccountCommand.java", """package com.sixpay.customer.management.application.port.input;

import java.time.Instant;

public record AddBankAccountCommand(
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        Instant verifiedAt
) {
}
""")

create(SVC + "/CustomerManagementService.java", """package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.input.CustomerManagementUseCase;
import com.sixpay.customer.management.application.port.input.CustomerQueryUseCase;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
@Transactional
public final class CustomerManagementService
        implements CustomerManagementUseCase, CustomerQueryUseCase {

    private final CustomerRepository repository;
    private final CustomerEnrollmentIdGenerator idGenerator;

    public CustomerManagementService(
            CustomerRepository repository,
            CustomerEnrollmentIdGenerator idGenerator
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.idGenerator = Objects.requireNonNull(idGenerator);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer findById(CustomerId customerId) {
        return repository.findById(customerId)
                .orElseThrow(() -> new CustomerDomainException(
                        "customer not found: " + customerId
                ));
    }

    @Override
    public Customer updateProfile(
            CustomerId customerId,
            String legalName,
            String email,
            String phoneNumber,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.updateProfile(legalName, email, phoneNumber, now);
        return repository.save(customer);
    }

    @Override
    public Customer suspend(
            CustomerId customerId,
            String reason,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.suspend(reason, now);
        return repository.save(customer);
    }

    @Override
    public Customer reactivate(
            CustomerId customerId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.reactivate(now);
        return repository.save(customer);
    }

    @Override
    public Customer close(
            CustomerId customerId,
            String reason,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.close(reason, now);
        return repository.save(customer);
    }

    @Override
    public Customer addBankAccount(
            CustomerId customerId,
            AddBankAccountCommand command,
            Instant now
    ) {
        Customer customer = findById(customerId);

        customer.addBankAccount(
                CustomerBankAccount.create(
                        new CustomerBankAccountId(
                                idGenerator.nextId()
                        ),
                        customerId,
                        command.bankingAccountReference(),
                        command.accountBindingFingerprint(),
                        command.maskedAccountIdentifier(),
                        command.currency(),
                        command.accountType(),
                        command.verifiedAt()
                ),
                now
        );

        return repository.save(customer);
    }

    @Override
    public Customer makeDefaultBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.makeDefaultBankAccount(accountId, now);
        return repository.save(customer);
    }

    @Override
    public Customer removeBankAccount(
            CustomerId customerId,
            CustomerBankAccountId accountId,
            Instant now
    ) {
        Customer customer = findById(customerId);
        customer.removeBankAccount(accountId, now);
        return repository.save(customer);
    }
}
""")

# ------------------------------------------------------------------
# Requests
# ------------------------------------------------------------------
create(REQ + "/UpdateCustomerRequest.java", """package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCustomerRequest(
        @NotBlank @Size(max = 200) String legalName,
        @Email @Size(max = 254) String email,
        @Size(max = 32) String phoneNumber
) {
}
""")

create(REQ + "/CustomerStatusReasonRequest.java", """package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerStatusReasonRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
""")

create(REQ + "/AddCustomerBankAccountRequest.java", """package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record AddCustomerBankAccountRequest(
        @NotBlank @Size(max = 100) String bankingAccountReference,
        @NotBlank
        @Pattern(regexp = "^v1:[0-9a-f]{64}$")
        String accountBindingFingerprint,
        @NotBlank @Size(max = 100) String maskedAccountIdentifier,
        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,
        @Size(max = 40) String accountType,
        Instant verifiedAt
) {
}
""")

# ------------------------------------------------------------------
# Responses
# ------------------------------------------------------------------
create(RESP + "/CustomerBankAccountResponse.java", """package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.CustomerBankAccount;

import java.time.Instant;
import java.util.UUID;

public record CustomerBankAccountResponse(
        UUID id,
        String bankingAccountReference,
        String accountBindingFingerprint,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        boolean defaultAccount,
        Instant verifiedAt
) {
    public static CustomerBankAccountResponse from(
            CustomerBankAccount account
    ) {
        return new CustomerBankAccountResponse(
                account.id().value(),
                account.bankingAccountReference(),
                account.accountBindingFingerprint(),
                account.maskedAccountIdentifier(),
                account.currency(),
                account.accountType(),
                account.defaultAccount(),
                account.verifiedAt()
        );
    }
}
""")

create(RESP + "/CustomerResponse.java", """package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.domain.model.Customer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CustomerResponse(
        UUID id,
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String email,
        String phoneNumber,
        String status,
        String statusReason,
        Instant createdAt,
        Instant updatedAt,
        List<CustomerBankAccountResponse> bankAccounts
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.id().value(),
                customer.financialInstitutionCode(),
                customer.bankingCustomerReference(),
                customer.customerNumber().orElse(null),
                customer.niu().orElse(null),
                customer.legalName(),
                customer.email().orElse(null),
                customer.phoneNumber().orElse(null),
                customer.status().name(),
                customer.statusReason().orElse(null),
                customer.createdAt(),
                customer.updatedAt(),
                customer.bankAccounts().stream()
                        .map(CustomerBankAccountResponse::from)
                        .toList()
        );
    }
}
""")

# ------------------------------------------------------------------
# Controller
# ------------------------------------------------------------------
create(API + "/CustomerController.java", """package com.sixpay.customer.management.api;

import com.sixpay.customer.management.api.request.AddCustomerBankAccountRequest;
import com.sixpay.customer.management.api.request.CustomerStatusReasonRequest;
import com.sixpay.customer.management.api.request.UpdateCustomerRequest;
import com.sixpay.customer.management.api.response.CustomerResponse;
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

    public CustomerController(
            EnrollCustomerUseCase enrollment,
            CustomerManagementUseCase management,
            CustomerQueryUseCase query
    ) {
        this.enrollment = enrollment;
        this.management = management;
        this.query = query;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update editable SIXPAY customer profile fields")
    public CustomerResponse update(
            @PathVariable UUID customerId,
            @Valid @RequestBody UpdateCustomerRequest request
    ) {
        return CustomerResponse.from(
                management.updateProfile(
                        new CustomerId(customerId),
                        request.legalName(),
                        request.email(),
                        request.phoneNumber(),
                        Instant.now()
                )
        );
    }

    @PostMapping("/{customerId}/suspension")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Suspend a SIXPAY customer")
    public CustomerResponse suspend(
            @PathVariable UUID customerId,
            @Valid @RequestBody CustomerStatusReasonRequest request
    ) {
        return CustomerResponse.from(
                management.suspend(
                        new CustomerId(customerId),
                        request.reason(),
                        Instant.now()
                )
        );
    }

    @PostMapping("/{customerId}/reactivation")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reactivate a suspended SIXPAY customer")
    public CustomerResponse reactivate(
            @PathVariable UUID customerId
    ) {
        return CustomerResponse.from(
                management.reactivate(
                        new CustomerId(customerId),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
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
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{customerId}/accounts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'AUDITOR')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Link a verified bank account to a customer")
    public CustomerResponse addAccount(
            @PathVariable UUID customerId,
            @Valid @RequestBody AddCustomerBankAccountRequest request
    ) {
        return CustomerResponse.from(
                management.addBankAccount(
                        new CustomerId(customerId),
                        new AddBankAccountCommand(
                                request.bankingAccountReference(),
                                request.accountBindingFingerprint(),
                                request.maskedAccountIdentifier(),
                                request.currency(),
                                request.accountType(),
                                request.verifiedAt()
                        ),
                        Instant.now()
                )
        );
    }

    @PutMapping("/{customerId}/accounts/{accountId}/default")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Set the default customer bank account")
    public CustomerResponse makeDefaultAccount(
            @PathVariable UUID customerId,
            @PathVariable UUID accountId
    ) {
        return CustomerResponse.from(
                management.makeDefaultBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
    }

    @DeleteMapping("/{customerId}/accounts/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unlink a bank account from a customer")
    public CustomerResponse removeAccount(
            @PathVariable UUID customerId,
            @PathVariable UUID accountId
    ) {
        return CustomerResponse.from(
                management.removeBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
    }
}
""")

# Fix missing response import in generated controller
controller_path = ROOT / (API + "/CustomerController.java")
text = controller_path.read_text(encoding="utf-8")
text = text.replace(
    "import com.sixpay.customer.management.api.response.CustomerResponse;\n",
    "import com.sixpay.customer.management.api.response.CustomerResponse;\n"
    "import com.sixpay.customer.management.api.response.CustomerBankAccountResponse;\n"
)
controller_path.write_text(text, encoding="utf-8", newline="\n")

# ------------------------------------------------------------------
# Exception handler
# ------------------------------------------------------------------
create(API + "/CustomerApiExceptionHandler.java", """package com.sixpay.customer.management.api;

import com.sixpay.customer.management.domain.exception.CustomerDomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(
        assignableTypes = CustomerController.class
)
public class CustomerApiExceptionHandler {

    @ExceptionHandler(CustomerDomainException.class)
    ProblemDetail domain(CustomerDomainException exception) {
        String message = exception.getMessage();

        HttpStatus status =
                message != null
                        && message.startsWith("customer not found")
                        ? HttpStatus.NOT_FOUND
                        : HttpStatus.CONFLICT;

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        status,
                        message == null
                                ? "Customer operation failed"
                                : message
                );
        detail.setTitle("Customer management error");
        return detail;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail badRequest(IllegalArgumentException exception) {
        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        exception.getMessage()
                );
        detail.setTitle("Invalid customer request");
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(
            MethodArgumentNotValidException exception
    ) {
        String detailMessage =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .findFirst()
                        .map(error ->
                                error.getField()
                                        + ": "
                                        + error.getDefaultMessage()
                        )
                        .orElse("Request validation failed");

        ProblemDetail detail =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.BAD_REQUEST,
                        detailMessage
                );
        detail.setTitle("Invalid customer request");
        return detail;
    }
}
""")

# ------------------------------------------------------------------
# Tests
# ------------------------------------------------------------------
create(TEST + "/application/service/CustomerManagementServiceTest.java", """package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerBankAccount;
import com.sixpay.customer.management.domain.model.CustomerBankAccountId;
import com.sixpay.customer.management.domain.model.CustomerId;
import com.sixpay.customer.management.domain.repository.CustomerRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CustomerManagementServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T20:00:00Z");

    @Test
    void updatesProfileAndPersistsAggregate() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();

        when(repository.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerManagementService service =
                new CustomerManagementService(repository, ids);

        Customer updated = service.updateProfile(
                customer.id(),
                "Updated Name",
                "updated@example.com",
                "+237699999999",
                NOW.plusSeconds(1)
        );

        assertThat(updated.legalName())
                .isEqualTo("Updated Name");
        verify(repository).save(customer);
    }

    @Test
    void addsAccountThroughAggregate() {
        CustomerRepository repository = mock(CustomerRepository.class);
        CustomerEnrollmentIdGenerator ids =
                mock(CustomerEnrollmentIdGenerator.class);
        Customer customer = customer();

        when(repository.findById(customer.id()))
                .thenReturn(Optional.of(customer));
        when(ids.nextId()).thenReturn(UUID.randomUUID());
        when(repository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomerManagementService service =
                new CustomerManagementService(repository, ids);

        Customer updated = service.addBankAccount(
                customer.id(),
                new AddBankAccountCommand(
                        "ACC-002",
                        "v1:" + "b".repeat(64),
                        "****0002",
                        "XAF",
                        "SAVINGS",
                        NOW
                ),
                NOW.plusSeconds(1)
        );

        assertThat(updated.bankAccounts()).hasSize(2);
        verify(repository).save(customer);
    }

    private static Customer customer() {
        CustomerId id = new CustomerId(UUID.randomUUID());
        return Customer.create(
                id,
                "SIXPAY_BANK",
                "BANK-CUSTOMER-001",
                "000123",
                "NIU-001",
                "Customer One",
                "customer@example.com",
                "+237600000001",
                CustomerBankAccount.create(
                        new CustomerBankAccountId(UUID.randomUUID()),
                        id,
                        "ACC-001",
                        "v1:" + "a".repeat(64),
                        "****0001",
                        "XAF",
                        "CURRENT",
                        NOW
                ),
                NOW
        );
    }
}
""")

print("\nCM-4 internal Customer API applied.")
print("Run:")
print("  ./mvnw -pl customer -am test")
print("  git diff --check")
print("  git status --short")
