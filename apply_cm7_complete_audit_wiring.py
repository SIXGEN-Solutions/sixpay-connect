#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def guard():
    r = run("git", "branch", "--show-current")
    if r.stdout.strip() != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {r.stdout.strip()}")

def replace_once(rel, old, new, label):
    p = ROOT / rel
    if not p.exists():
        print(f"[warn] missing {rel}: {label}")
        return
    text = p.read_text(encoding="utf-8")
    if new in text:
        print(f"[skip] {label}")
        return
    if old not in text:
        print(f"[warn] pattern not found: {label}")
        return
    p.write_text(text.replace(old, new, 1), encoding="utf-8", newline="\n")
    print(f"[update] {label}")

guard()
API = "backend/customer/src/main/java/com/sixpay/customer/management/api"

replace_once(API + "/CustomerController.java", '''        return CustomerResponse.from(
                management.updateProfile(
                        new CustomerId(customerId),
                        request.legalName(),
                        request.email(),
                        request.phoneNumber(),
                        Instant.now()
                )
        );
''', '''        CustomerResponse response = CustomerResponse.from(
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
''', "audit customer update")

replace_once(API + "/CustomerController.java", '''        return CustomerResponse.from(
                management.suspend(
                        new CustomerId(customerId),
                        request.reason(),
                        Instant.now()
                )
        );
''', '''        CustomerResponse response = CustomerResponse.from(
                management.suspend(
                        new CustomerId(customerId),
                        request.reason(),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_SUSPENDED", null,
                "Customer suspended; reason=" + request.reason());
        return response;
''', "audit customer suspend")

replace_once(API + "/CustomerController.java", '''        return CustomerResponse.from(
                management.reactivate(
                        new CustomerId(customerId),
                        Instant.now()
                )
        );
''', '''        CustomerResponse response = CustomerResponse.from(
                management.reactivate(
                        new CustomerId(customerId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_REACTIVATED", null,
                "Customer reactivated");
        return response;
''', "audit customer reactivate")

replace_once(API + "/CustomerController.java", '''        management.close(
                new CustomerId(customerId),
                request.reason(),
                Instant.now()
        );
        return ResponseEntity.noContent().build();
''', '''        management.close(
                new CustomerId(customerId),
                request.reason(),
                Instant.now()
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_CLOSED", null,
                "Customer logically closed; reason=" + request.reason());
        return ResponseEntity.noContent().build();
''', "audit customer close")

replace_once(API + "/CustomerController.java", '''        return CustomerResponse.from(
                management.makeDefaultBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
''', '''        CustomerResponse response = CustomerResponse.from(
                management.makeDefaultBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_DEFAULT_ACCOUNT_CHANGED", null,
                "Default bank account changed to " + accountId);
        return response;
''', "audit default account")

replace_once(API + "/CustomerController.java", '''        return CustomerResponse.from(
                management.removeBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
''', '''        CustomerResponse response = CustomerResponse.from(
                management.removeBankAccount(
                        new CustomerId(customerId),
                        new CustomerBankAccountId(accountId),
                        Instant.now()
                )
        );
        audit.success("CUSTOMER", customerId, "CUSTOMER_BANK_ACCOUNT_UNLINKED", null,
                "Bank account unlinked: " + accountId);
        return response;
''', "audit account unlink")

replace_once(API + "/CustomerSubscriptionController.java", '''        return CustomerSubscriptionResponse.from(
                subscriptions.activate(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        Instant.now()
                )
        );
''', '''        CustomerSubscriptionResponse response = CustomerSubscriptionResponse.from(
                subscriptions.activate(
                        new CustomerSubscriptionId(subscriptionId),
                        Instant.now()
                )
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_ACTIVATED", null,
                "Subscription activated or reactivated");
        return response;
''', "audit subscription activate")

replace_once(API + "/CustomerSubscriptionController.java", '''        return CustomerSubscriptionResponse.from(
                subscriptions.suspend(
                        new CustomerSubscriptionId(
                                subscriptionId
                        ),
                        request.reason(),
                        Instant.now()
                )
        );
''', '''        CustomerSubscriptionResponse response = CustomerSubscriptionResponse.from(
                subscriptions.suspend(
                        new CustomerSubscriptionId(subscriptionId),
                        request.reason(),
                        Instant.now()
                )
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_SUSPENDED", null,
                "Subscription suspended; reason=" + request.reason());
        return response;
''', "audit subscription suspend")

replace_once(API + "/CustomerSubscriptionController.java", '''        subscriptions.close(
                new CustomerSubscriptionId(
                        subscriptionId
                ),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
''', '''        subscriptions.close(
                new CustomerSubscriptionId(subscriptionId),
                request.reason(),
                Instant.now()
        );
        audit.success("SUBSCRIPTION", subscriptionId, "SUBSCRIPTION_CLOSED", null,
                "Subscription closed; reason=" + request.reason());
        return ResponseEntity.noContent().build();
''', "audit subscription close")

p = ROOT / (API + "/ObservedCustomerLinkController.java")
if p.exists():
    text = p.read_text(encoding="utf-8")
    if "CustomerAuditRecorder" not in text:
        text = text.replace(
            "import com.sixpay.customer.management.api.response.ObservedCustomerLinkResponse;\n",
            "import com.sixpay.customer.management.api.response.ObservedCustomerLinkResponse;\nimport com.sixpay.customer.management.application.audit.CustomerAuditRecorder;\n"
        )
        text = text.replace(
            "    private final CurrentUserProvider currentUserProvider;\n",
            "    private final CurrentUserProvider currentUserProvider;\n    private final CustomerAuditRecorder audit;\n"
        )
        text = text.replace(
            '''    public ObservedCustomerLinkController(
            ObservedCustomerLinkUseCase links,
            CurrentUserProvider currentUserProvider
    ) {
        this.links = links;
        this.currentUserProvider = currentUserProvider;
    }
''',
            '''    public ObservedCustomerLinkController(
            ObservedCustomerLinkUseCase links,
            CurrentUserProvider currentUserProvider,
            CustomerAuditRecorder audit
    ) {
        this.links = links;
        this.currentUserProvider = currentUserProvider;
        this.audit = audit;
    }
'''
        )
    old = '''        return ObservedCustomerLinkResponse.from(
                links.link(
                        observedCustomerId,
                        new CustomerId(request.customerId()),
                        actor(),
                        correlation(correlationId),
                        request.reason(),
                        Instant.now()
                )
        );
'''
    new = '''        String effectiveCorrelationId = correlation(correlationId);
        ObservedCustomerLinkResponse response = ObservedCustomerLinkResponse.from(
                links.link(
                        observedCustomerId,
                        new CustomerId(request.customerId()),
                        actor(),
                        effectiveCorrelationId,
                        request.reason(),
                        Instant.now()
                )
        );
        audit.success("OBSERVED_CUSTOMER_LINK", observedCustomerId,
                "OBSERVED_CUSTOMER_LINKED", effectiveCorrelationId,
                "ObservedCustomer linked to Customer " + request.customerId());
        return response;
'''
    if old in text:
        text = text.replace(old, new, 1)
    old = '''        links.unlink(
                observedCustomerId,
                actor(),
                correlation(correlationId),
                request.reason(),
                Instant.now()
        );

        return ResponseEntity.noContent().build();
'''
    new = '''        String effectiveCorrelationId = correlation(correlationId);
        links.unlink(
                observedCustomerId,
                actor(),
                effectiveCorrelationId,
                request.reason(),
                Instant.now()
        );
        audit.success("OBSERVED_CUSTOMER_LINK", observedCustomerId,
                "OBSERVED_CUSTOMER_UNLINKED", effectiveCorrelationId,
                "ObservedCustomer link removed; reason=" + request.reason());
        return ResponseEntity.noContent().build();
'''
    if old in text:
        text = text.replace(old, new, 1)
    p.write_text(text, encoding="utf-8", newline="\n")
    print("[update] observed customer link audit wiring")

print("CM-7 complete successful-mutation audit wiring applied.")
