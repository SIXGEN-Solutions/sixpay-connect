from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

TARGETS = [
    ROOT / "documentation/contracts/internal/payment-query-api-v1-status-alignment.patch",
    ROOT / "documentation/contracts/CONTRACT_REGISTRY_LOT0_PATCH.md",
]

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

REQUIRED_PAYMENT_STATUSES = [
    "BANKING_VERIFICATION_PENDING",
    "FUNDS_CONTROL_PENDING",
    "TREASURY_ACCOUNT_RESOLUTION_PENDING",
    "APPROVED_FOR_POSTING",
    "POSTING_PENDING",
    "POSTING_OUTCOME_UNKNOWN",
    "DEBIT_CONFIRMED",
    "POSTED_PENDING_TFJ",
    "REVERSAL_OUTCOME_UNKNOWN",
    "TREASURY_INTEGRATED",
]

PAYMENT_CONTRACT = (
    ROOT
    / "documentation/contracts/internal/payment-query-api-v1.yaml"
)

REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
PAYMENT_BASELINE = (
    ROOT
    / "documentation/ai/payment/PAYMENT_IA1_BASELINE.md"
)


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            f"{EXPECTED_BRANCH}."
        )

    payment_contract = require(PAYMENT_CONTRACT)
    registry = require(REGISTRY)
    payment_baseline = require(PAYMENT_BASELINE)

    for status in REQUIRED_PAYMENT_STATUSES:
        if status not in payment_contract:
            fail(
                "Payment status alignment is not fully absorbed: "
                f"{status} is missing from payment-query-api-v1.yaml"
            )

    for contract_id in [
        "payment-query-api-v1",
        "observed-customer-query-api-v1",
        "payment-audit-query-api-v1",
    ]:
        if f'id: "{contract_id}"' not in registry:
            fail(
                "Lot 0 registry normalization is not absorbed: "
                f"{contract_id} is missing from CONTRACT_REGISTRY.yaml"
            )

    for rule_token in [
        "PENDING_APPROVAL",
        "cannot",
        "code generation",
        "PAY-BASE-",
        "PAY-CONTRACT-",
        "PAY-AI-",
        "OPEN-",
    ]:
        if rule_token not in payment_baseline:
            fail(
                "Payment Lot 0 governance is not sufficiently preserved "
                f"in PAYMENT_IA1_BASELINE.md: missing token {rule_token}"
            )

    missing = [
        str(path.relative_to(ROOT))
        for path in TARGETS
        if not path.is_file()
    ]

    if missing:
        print("FS-2.1.4 already partially or fully applied.")
        for item in missing:
            print(f" - already absent: {item}")

    for target in TARGETS:
        if target.is_file():
            target.unlink()
            print(
                "Deleted: "
                + str(target.relative_to(ROOT)).replace("\\", "/")
            )

    for target in TARGETS:
        if target.exists():
            fail(
                "Deletion safety check failed: "
                + str(target.relative_to(ROOT))
            )

    print()
    print("FS-2.1.4 development-history cleanup applied.")
    print("No normative contract or registry capability was changed.")
    print()
    print("Review:")
    print("  git status --short")
    print("  git diff -- documentation/contracts")


if __name__ == "__main__":
    main()
