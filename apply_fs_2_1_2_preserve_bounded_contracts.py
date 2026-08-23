from pathlib import Path
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
README = ROOT / "documentation/contracts/README.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

KEEP_FILES = [
    "documentation/contracts/internal/payment-query-api-v1.yaml",
    "documentation/contracts/internal/payment-audit-query-api-v1.yaml",
    "documentation/contracts/internal/observed-customer-query-api-v1.yaml",
    "documentation/contracts/internal/accounting-query-api-v1.yaml",
    "documentation/contracts/internal/notification-operational-trigger-v1.md",
    "documentation/contracts/internal/notification-operational-email-v1.md",
]

MARKER = "## FS-2.1 — Repository baseline consolidation decisions"

SECTION = r'''## FS-2.1 — Repository baseline consolidation decisions

During `FS-2.1 — Contract consolidation`, the following internal contracts are
explicitly classified as **KEEP**.

`KEEP` is a repository-consolidation decision. It does not replace the
normative lifecycle, approval, generation, security or ownership metadata in
`CONTRACT_REGISTRY.yaml`.

| Physical contract | Decision | Preserved boundary |
| --- | --- | --- |
| `internal/payment-query-api-v1.yaml` | `KEEP` | Operational masked Payment query capability |
| `internal/payment-audit-query-api-v1.yaml` | `KEEP` | Privileged immutable Payment audit, timeline and controlled export boundary |
| `internal/observed-customer-query-api-v1.yaml` | `KEEP` | Customer-owned non-authoritative ObservedCustomer query projection |
| `internal/accounting-query-api-v1.yaml` | `KEEP` | Accounting batch query capability |
| `internal/notification-operational-trigger-v1.md` | `KEEP` | Inbound semantic trigger contract consumed by Notification |
| `internal/notification-operational-email-v1.md` | `KEEP` | Outbound operational email dispatch/provider boundary |

### Preservation rationale

These contracts must remain physically independent because they represent
different ownership, security, data-classification, direction or operational
semantics.

In particular:

- Payment Query and Payment Audit remain separate. Audit has stronger
  confidentiality, traceability and export semantics and must not be folded
  into the ordinary Payment query API.
- ObservedCustomer remains separate from authoritative Customer Management.
  It is a non-authoritative projection created from observed Payment facts.
- Accounting Batch Query remains an Accounting-owned bounded query capability.
- Notification Trigger and Notification Email remain separate because the
  trigger contract describes semantic facts entering Notification, while the
  email contract describes the provider-facing dispatch boundary leaving
  Notification.

No endpoint, schema, capability, authorization rule or registry identity is
changed by this preservation decision.
'''


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def main() -> None:
    if not ENGINEERING.is_file():
        fail("Run from the sixpay-connect repository root.")

    engineering = ENGINEERING.read_text(encoding="utf-8")
    if EXPECTED_BRANCH not in engineering:
        fail(
            f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}."
        )

    for relative in KEEP_FILES:
        path = ROOT / relative
        if not path.is_file():
            fail(f"KEEP contract is missing: {relative}")

    if not README.is_file():
        fail(f"Missing contracts README: {README}")

    original = README.read_text(encoding="utf-8")

    if MARKER in original:
        print("FS-2.1.2 already applied; no change required.")
        return

    backup = README.with_suffix(README.suffix + ".fs-2.1.2.bak")
    if not backup.exists():
        backup.write_text(original, encoding="utf-8")

    updated = original.rstrip() + "\n\n" + SECTION.strip() + "\n"
    README.write_text(updated, encoding="utf-8")

    for relative in KEEP_FILES:
        if not (ROOT / relative).is_file():
            fail(f"Safety check failed after update: {relative}")

    print("FS-2.1.2 bounded-contract preservation applied.")
    print()
    print("Explicit KEEP decisions:")
    for relative in KEEP_FILES:
        print(f" - {relative}")
    print()
    print("Modified only:")
    print(" - documentation/contracts/README.md")
    print()
    print("No contract payload, registry capability or endpoint was changed.")
    print()
    print("Review:")
    print(" git diff -- documentation/contracts/README.md")


if __name__ == "__main__":
    main()
