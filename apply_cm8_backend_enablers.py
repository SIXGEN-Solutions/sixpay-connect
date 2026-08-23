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
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {branch}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        if p.read_text(encoding="utf-8") == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} already exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

def overwrite(rel, text):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[write] {rel}")

def replace_once(rel, old, new, label):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"[skip] {label}")
        return
    if old not in text:
        raise SystemExit(f"[stop] pattern not found for {label} in {rel}")
    p.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
    print(f"[update] {label}")

guard()

B = "backend/customer/src/main/java/com/sixpay/customer/management"
DOMAIN_REPO = B + "/domain/repository/CustomerRepository.java"
SPRING_REPO = B + "/infrastructure/persistence/CustomerSpringDataRepository.java"
REPO_ADAPTER = B + "/infrastructure/persistence/CustomerRepositoryAdapter.java"
QUERY_UC = B + "/application/port/input/CustomerQueryUseCase.java"
MGMT_SERVICE = B + "/application/service/CustomerManagementService.java"
API = B + "/api"
REQ = API + "/request"
RESP = API + "/response"
PORT_IN = B + "/application/port/input"
SERVICE = B + "/application/service"

replace_once(
    DOMAIN_REPO,
    "import java.util.Optional;\n",
    "import java.util.List;\nimport java.util.Optional;\n",
    "CustomerRepository List import"
)
replace_once(
    DOMAIN_REPO,
    "    Optional<Customer> findById(CustomerId customerId);\n\n",
    "    Optional<Customer> findById(CustomerId customerId);\n\n"
    "    List<Customer> findAll();\n\n",
    "CustomerRepository findAll"
)

replace_once(
    SPRING_REPO,
    "import java.util.Optional;\n",
    "import java.util.List;\nimport java.util.Optional;\n",
    "Spring repository List import"
)
replace_once(
    SPRING_REPO,
    "    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(\n",
    '''    @EntityGraph(attributePaths = "bankAccounts")
    List<CustomerJpaEntity> findAllByOrderByCreatedAtDesc();

    boolean existsByFinancialInstitutionCodeAndBankingCustomerReference(
''',
    "Spring repository list query"
)

replace_once(
    REPO_ADAPTER,
    "import java.util.Optional;\n",
    "import java.util.List;\nimport java.util.Optional;\n",
    "repository adapter List import"
)
replace_once(
    REPO_ADAPTER,
    "    @Override\n    public boolean existsById(CustomerId customerId) {\n",
    '''    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public boolean existsById(CustomerId customerId) {
''',
    "repository adapter findAll"
)

overwrite(
    QUERY_UC,
    '''package com.sixpay.customer.management.application.port.input;

import com.sixpay.customer.management.domain.model.Customer;
import com.sixpay.customer.management.domain.model.CustomerId;

import java.util.List;

public interface CustomerQueryUseCase {

    Customer findById(CustomerId customerId);

    List<Customer> findAll();
}
'''
)

replace_once(
    MGMT_SERVICE,
    "import java.time.Instant;\nimport java.util.Objects;\n",
    "import java.time.Instant;\nimport java.util.List;\nimport java.util.Objects;\n",
    "CustomerManagementService List import"
)
replace_once(
    MGMT_SERVICE,
    "    @Override\n    public Customer updateProfile(\n",
    '''    @Override
    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return repository.findAll();
    }

    @Override
    public Customer updateProfile(
''',
    "CustomerManagementService findAll"
)

create(
    PORT_IN + "/BankingCustomerPreviewUseCase.java",
    '''package com.sixpay.customer.management.application.port.input;

import java.time.Instant;

public interface BankingCustomerPreviewUseCase {

    BankingCustomerPreview preview(BankingCustomerPreviewQuery query);

    record BankingCustomerPreviewQuery(
            String financialInstitutionCode,
            String niu,
            String customerNumber,
            String accountReference,
            String correlationId
    ) {
    }

    record BankingCustomerPreview(
            String financialInstitutionCode,
            String bankingCustomerReference,
            String customerNumber,
            String niu,
            String legalName,
            String email,
            String phoneNumber,
            String accountReference,
            String maskedAccountIdentifier,
            String currency,
            String accountType,
            Instant retrievedAt
    ) {
    }
}
'''
)

create(
    SERVICE + "/BankingCustomerPreviewService.java",
    '''package com.sixpay.customer.management.application.service;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;
import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Transactional(readOnly = true)
public final class BankingCustomerPreviewService
        implements BankingCustomerPreviewUseCase {

    private final BankingCustomerLookupPort lookupPort;

    public BankingCustomerPreviewService(
            BankingCustomerLookupPort lookupPort
    ) {
        this.lookupPort = Objects.requireNonNull(lookupPort);
    }

    @Override
    public BankingCustomerPreview preview(
            BankingCustomerPreviewQuery query
    ) {
        Objects.requireNonNull(query, "query is required");

        var profile = lookupPort.lookup(
                new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                        query.financialInstitutionCode(),
                        query.niu(),
                        query.customerNumber(),
                        query.accountReference(),
                        query.correlationId()
                )
        );

        return new BankingCustomerPreview(
                profile.financialInstitutionCode(),
                profile.customerReference(),
                profile.customerNumber(),
                profile.niu(),
                profile.legalName(),
                profile.email(),
                profile.phoneNumber(),
                profile.account().accountReference(),
                profile.account().maskedAccountIdentifier(),
                profile.account().currency(),
                profile.account().accountType(),
                profile.account().retrievedAt()
        );
    }
}
'''
)

create(
    REQ + "/BankingCustomerPreviewRequest.java",
    '''package com.sixpay.customer.management.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BankingCustomerPreviewRequest(
        @NotBlank @Size(max = 50) String financialInstitutionCode,
        @Size(max = 100) String niu,
        @Size(max = 100) String customerNumber,
        @NotBlank @Size(max = 100) String accountReference
) {
}
'''
)

create(
    RESP + "/BankingCustomerPreviewResponse.java",
    '''package com.sixpay.customer.management.api.response;

import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;

import java.time.Instant;

public record BankingCustomerPreviewResponse(
        String financialInstitutionCode,
        String bankingCustomerReference,
        String customerNumber,
        String niu,
        String legalName,
        String email,
        String phoneNumber,
        String accountReference,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        Instant retrievedAt
) {
    public static BankingCustomerPreviewResponse from(
            BankingCustomerPreviewUseCase.BankingCustomerPreview preview
    ) {
        return new BankingCustomerPreviewResponse(
                preview.financialInstitutionCode(),
                preview.bankingCustomerReference(),
                preview.customerNumber(),
                preview.niu(),
                preview.legalName(),
                preview.email(),
                preview.phoneNumber(),
                preview.accountReference(),
                preview.maskedAccountIdentifier(),
                preview.currency(),
                preview.accountType(),
                preview.retrievedAt()
        );
    }
}
'''
)

controller = ROOT / (API + "/CustomerController.java")
text = controller.read_text(encoding="utf-8")

if "BankingCustomerPreviewRequest" not in text:
    text = text.replace(
        "import com.sixpay.customer.management.api.request.AddCustomerBankAccountRequest;\n",
        "import com.sixpay.customer.management.api.request.AddCustomerBankAccountRequest;\n"
        "import com.sixpay.customer.management.api.request.BankingCustomerPreviewRequest;\n"
    )
if "BankingCustomerPreviewResponse" not in text:
    text = text.replace(
        "import com.sixpay.customer.management.api.response.CustomerResponse;\n",
        "import com.sixpay.customer.management.api.response.CustomerResponse;\n"
        "import com.sixpay.customer.management.api.response.BankingCustomerPreviewResponse;\n"
    )
if "BankingCustomerPreviewUseCase" not in text:
    text = text.replace(
        "import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;\n",
        "import com.sixpay.customer.management.application.port.input.AddBankAccountCommand;\n"
        "import com.sixpay.customer.management.application.port.input.BankingCustomerPreviewUseCase;\n"
    )

if "private final BankingCustomerPreviewUseCase bankingPreview;" not in text:
    text = text.replace(
        "    private final CustomerQueryUseCase query;\n",
        "    private final CustomerQueryUseCase query;\n"
        "    private final BankingCustomerPreviewUseCase bankingPreview;\n"
    )
    text = text.replace(
        "            CustomerManagementUseCase management,\n"
        "            CustomerQueryUseCase query",
        "            CustomerManagementUseCase management,\n"
        "            CustomerQueryUseCase query,\n"
        "            BankingCustomerPreviewUseCase bankingPreview"
    )
    text = text.replace(
        "        this.query = query;\n",
        "        this.query = query;\n"
        "        this.bankingPreview = bankingPreview;\n"
    )

marker = '''    @GetMapping("/{customerId}")
'''
if 'summary = "List enrolled SIXPAY customers"' not in text:
    addition = '''    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_customer.read')")
    @Operation(summary = "List enrolled SIXPAY customers")
    public java.util.List<CustomerResponse> list() {
        return query.findAll()
                .stream()
                .map(CustomerResponse::from)
                .toList();
    }

    @PostMapping("/banking-preview")
    @PreAuthorize("hasAuthority('SCOPE_customer.create')")
    @Operation(
            summary = "Preview a banking customer before enrollment",
            description = "Lookup only. Does not create Customer master data and is not verification evidence."
    )
    public BankingCustomerPreviewResponse bankingPreview(
            @Valid @RequestBody BankingCustomerPreviewRequest request,
            @RequestHeader(name = CORRELATION_HEADER, required = false)
            @Size(max = HEADER_MAX_LENGTH) String correlationId
    ) {
        String effectiveCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? UUID.randomUUID().toString()
                        : correlationId.strip();

        return BankingCustomerPreviewResponse.from(
                bankingPreview.preview(
                        new BankingCustomerPreviewUseCase.BankingCustomerPreviewQuery(
                                request.financialInstitutionCode(),
                                request.niu(),
                                request.customerNumber(),
                                request.accountReference(),
                                effectiveCorrelationId
                        )
                )
        );
    }

'''
    if marker not in text:
        raise SystemExit("[stop] CustomerController insertion point not found")
    text = text.replace(marker, addition + marker, 1)

controller.write_text(text, encoding="utf-8", newline="\n")
print("[update] CustomerController CM-8 backend enablers")

print("CM-8 backend enablers applied.")
