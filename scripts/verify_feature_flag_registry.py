from pathlib import Path
import re
import sys

ROOT = Path.cwd()

REGISTRY = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FEATURE_FLAG_REGISTRY.yaml"
)

BUSINESS = [
    "partner",
    "customer",
    "payment",
    "accounting",
    "reporting",
    "notification",
    "security",
    "administration",
]

FORBIDDEN_UNQUALIFIED = {
    "enabled",
    "metrics-enabled",
    "retention-enabled",
}

SPECIAL_EXPECTED_OWNERS = {
    "spring.flyway.enabled": "BOOTSTRAP_RUNTIME",
    "sixpay.integration.kafka.enabled": "INTEGRATION_SHARED",
    "sixpay.e2e.customer.enabled": "TEST_E2E_RUNTIME",
}

REQUIRED_FLAGS = {
    "backend.mode",
    "authentication.standalone",
    "authentication.local.enabled",
    "authentication.oidc.enabled",
    "sixpay.notification.operational.operations.metrics-enabled",
    "sixpay.notification.operational.operations.retention-enabled",
}

def fail(errors):
    print("\nFS-2.5.7 feature-flag registry validation FAILED:\n")
    for error in errors:
        print(" -", error)
    sys.exit(1)

def registry_entries():
    text = REGISTRY.read_text(encoding="utf-8")
    blocks = re.split(r"(?=^  - id: )", text, flags=re.MULTILINE)
    entries = []

    for block in blocks:
        if not block.startswith("  - id: "):
            continue

        def field(name):
            match = re.search(
                rf"^    {re.escape(name)}:\s*(.+)$",
                block,
                re.MULTILINE,
            )
            return match.group(1).strip().strip('"') if match else None

        id_match = re.search(
            r"^  - id:\s*(.+)$",
            block,
            re.MULTILINE,
        )

        entries.append({
            "id": id_match.group(1).strip(),
            "key": field("key"),
            "owner": field("owner"),
            "category": field("category"),
        })

    return entries

def main():
    errors = []

    if not REGISTRY.is_file():
        fail(["FEATURE_FLAG_REGISTRY.yaml is missing"])

    entries = registry_entries()
    keys = [entry["key"] for entry in entries if entry["key"]]

    for key in sorted(set(keys)):
        if keys.count(key) > 1:
            errors.append(f"duplicate registry key: {key}")

    for entry in entries:
        key = entry["key"]
        owner = entry["owner"]

        if owner == "REVIEW_REQUIRED":
            errors.append(
                f"{entry['id']}: owner remains REVIEW_REQUIRED"
            )

        if key in FORBIDDEN_UNQUALIFIED:
            errors.append(
                f"unqualified parser artifact is forbidden: {key}"
            )

        for module in BUSINESS:
            prefix = f"sixpay.{module}."
            if key and key.startswith(prefix):
                expected = module.upper()

                if key.startswith("sixpay.e2e."):
                    break

                if owner != expected:
                    errors.append(
                        f"{entry['id']}: {key} must be owned by "
                        f"{expected}, found {owner}"
                    )

    key_set = set(keys)

    for required in sorted(REQUIRED_FLAGS):
        if required not in key_set:
            errors.append(
                f"required canonical flag is not registered: {required}"
            )

    for key, expected_owner in SPECIAL_EXPECTED_OWNERS.items():
        matches = [
            entry for entry in entries
            if entry["key"] == key
        ]

        if not matches:
            errors.append(
                f"required special-runtime flag is missing: {key}"
            )
        elif matches[0]["owner"] != expected_owner:
            errors.append(
                f"{key} must be owned by {expected_owner}, found "
                f"{matches[0]['owner']}"
            )

    if errors:
        fail(errors)

    print("FS-2.5.7 feature-flag registry validation PASSED.")
    print(
        f"Registered entries: {len(entries)}; ownership is explicit; "
        "no unqualified feature-flag artifacts remain."
    )

if __name__ == "__main__":
    main()
