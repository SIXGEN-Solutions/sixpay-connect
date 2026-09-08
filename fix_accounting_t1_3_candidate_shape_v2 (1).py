from pathlib import Path
import subprocess

BASELINE = "b11a82657be7c9ff9ece16358b53e5f23c7c9a4d"

def ensure(cond, msg):
    if not cond:
        raise RuntimeError(msg)

def run(*args):
    return subprocess.check_output(args, text=True).strip()

root = Path.cwd()
ensure((root / ".git").exists(), "Run this script from repository root.")

head = run("git", "rev-parse", "HEAD")
ensure(
    subprocess.run(["git", "merge-base", "--is-ancestor", BASELINE, head]).returncode == 0,
    f"Current HEAD {head} does not descend from baseline {BASELINE}"
)

candidate_path = root / "backend/accounting/src/main/java/com/sixpay/accounting/domain/model/AccountingPaymentCandidate.java"
adapter_path = root / "backend/accounting/src/main/java/com/sixpay/accounting/infrastructure/persistence/AccountingCandidateProjectionRepositoryAdapter.java"

ensure(candidate_path.exists(), f"Missing file: {candidate_path}")
ensure(adapter_path.exists(), f"Missing file: {adapter_path}")

candidate = '''package com.sixpay.accounting.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record AccountingPaymentCandidate(
        UUID paymentId,
        String publicPaymentReference,
        String partnerId,
        String financialInstitutionCode,
        UUID financialSnapshotId,
        String financialSnapshotVersion,
        Instant financialSnapshotFinalizedAt,
        String debtorAccountReference,
        String creditorAccountReference,
        BigDecimal amount,
        Currency currency,
        Instant paymentOccurredAt,
        LocalDate paymentBusinessDate,
        String bankPostingReference,
        TresorPayPaymentStatusEvidence tresorPayStatusEvidence,
        List<FrozenEntry> entries
) {
    public AccountingPaymentCandidate {
        paymentId = nonNil(paymentId, "paymentId");
        publicPaymentReference = required(publicPaymentReference, "publicPaymentReference");
        partnerId = required(partnerId, "partnerId");
        financialInstitutionCode = required(financialInstitutionCode, "financialInstitutionCode");
        financialSnapshotId = nonNil(financialSnapshotId, "financialSnapshotId");
        financialSnapshotVersion = required(financialSnapshotVersion, "financialSnapshotVersion");
        financialSnapshotFinalizedAt = Objects.requireNonNull(
                financialSnapshotFinalizedAt,
                "financialSnapshotFinalizedAt"
        );
        debtorAccountReference = required(debtorAccountReference, "debtorAccountReference");
        creditorAccountReference = required(creditorAccountReference, "creditorAccountReference");
        amount = Objects.requireNonNull(amount, "amount");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
        currency = Objects.requireNonNull(currency, "currency");
        paymentOccurredAt = Objects.requireNonNull(paymentOccurredAt, "paymentOccurredAt");
        paymentBusinessDate = Objects.requireNonNull(paymentBusinessDate, "paymentBusinessDate");
        bankPostingReference = required(bankPostingReference, "bankPostingReference");
        tresorPayStatusEvidence = Objects.requireNonNull(
                tresorPayStatusEvidence,
                "tresorPayStatusEvidence"
        );
        entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("frozen financial entries must not be empty");
        }
    }

    public record FrozenEntry(
            UUID entrySnapshotId,
            int sequence,
            String direction,
            String accountReference,
            BigDecimal amount,
            Currency currency,
            Instant createdAt
    ) {
        public FrozenEntry {
            entrySnapshotId = nonNil(entrySnapshotId, "entrySnapshotId");
            if (sequence <= 0) {
                throw new IllegalArgumentException("entry sequence must be positive");
            }
            direction = required(direction, "direction");
            if (!"DEBIT".equals(direction) && !"CREDIT".equals(direction)) {
                throw new IllegalArgumentException("direction must be DEBIT or CREDIT");
            }
            accountReference = required(accountReference, "accountReference");
            amount = Objects.requireNonNull(amount, "amount");
            if (amount.signum() <= 0) {
                throw new IllegalArgumentException("entry amount must be positive");
            }
            currency = Objects.requireNonNull(currency, "currency");
            createdAt = Objects.requireNonNull(createdAt, "createdAt");
        }
    }

    private static UUID nonNil(UUID value, String name) {
        value = Objects.requireNonNull(value, name);
        if (value.equals(new UUID(0L, 0L))) {
            throw new IllegalArgumentException(name + " must not be nil");
        }
        return value;
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
'''

current = candidate_path.read_text(encoding="utf-8")
ensure(
    "public record AccountingPaymentCandidate(" in current
    and "TresorPayPaymentStatusEvidence" in current,
    "AccountingPaymentCandidate has an unexpected semantic shape; refusing to overwrite."
)

if current != candidate:
    candidate_path.write_text(candidate, encoding="utf-8")
    print("Updated AccountingPaymentCandidate canonical T1.3 shape.")
else:
    print("AccountingPaymentCandidate already has canonical T1.3 shape.")

text = adapter_path.read_text(encoding="utf-8")
needle = "new AccountingPaymentCandidate("
start = text.find(needle)
ensure(start >= 0, "AccountingPaymentCandidate construction not found in projection adapter.")

open_paren = start + len("new AccountingPaymentCandidate")
ensure(text[open_paren] == "(", "Unexpected AccountingPaymentCandidate constructor syntax.")

depth = 0
end = None
for i in range(open_paren, len(text)):
    ch = text[i]
    if ch == "(":
        depth += 1
    elif ch == ")":
        depth -= 1
        if depth == 0:
            end = i + 1
            break

ensure(end is not None, "Could not parse AccountingPaymentCandidate constructor in projection adapter.")

replacement = '''new AccountingPaymentCandidate(
                p.paymentId(),
                p.publicPaymentReference(),
                p.partnerId(),
                p.financialInstitutionCode(),
                p.financialSnapshotId(),
                p.financialSnapshotVersion(),
                p.financialSnapshotFinalizedAt(),
                p.debtorAccountReference(),
                p.creditorAccountReference(),
                p.amount(),
                p.currency(),
                p.paymentOccurredAt(),
                p.accountingBusinessDate(),
                p.bankReference(),
                p.tresorPayStatusEvidence(),
                p.entries().stream()
                        .map(entry -> new AccountingPaymentCandidate.FrozenEntry(
                                entry.entrySnapshotId(),
                                entry.sequence(),
                                entry.direction(),
                                entry.accountReference(),
                                entry.amount(),
                                entry.currency(),
                                entry.createdAt()
                        ))
                        .toList()
        )'''

old_constructor = text[start:end]
if old_constructor != replacement:
    adapter_path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
    print("Updated AccountingCandidateProjectionRepositoryAdapter mapping.")
else:
    print("Projection adapter mapping already matches T1.3.")

candidate_text = candidate_path.read_text(encoding="utf-8")
adapter_text = adapter_path.read_text(encoding="utf-8")

for token in [
    "UUID financialSnapshotId",
    "String financialSnapshotVersion",
    "Instant financialSnapshotFinalizedAt",
    "String debtorAccountReference",
    "String creditorAccountReference",
    "List<FrozenEntry> entries",
]:
    ensure(token in candidate_text, f"Missing expected candidate token: {token}")

for token in [
    "p.financialSnapshotId()",
    "p.financialSnapshotVersion()",
    "p.financialSnapshotFinalizedAt()",
    "p.debtorAccountReference()",
    "p.creditorAccountReference()",
    "AccountingPaymentCandidate.FrozenEntry",
]:
    ensure(token in adapter_text, f"Missing expected adapter token: {token}")

print("T1.3 candidate-shape corrective patch v2 applied successfully.")
print("Baseline ancestor:", BASELINE)
print("Current HEAD:", head)
print("No commit, push, PR or deployment performed.")
