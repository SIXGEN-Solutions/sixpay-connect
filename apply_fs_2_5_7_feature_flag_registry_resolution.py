from pathlib import Path
import re
import sys

ROOT = Path.cwd()

REGISTRY = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FEATURE_FLAG_REGISTRY.yaml"
)

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.7_FEATURE_FLAG_REGISTRY.md"
)

VERIFY = (
    ROOT
    / "scripts/"
    / "verify_feature_flag_registry.py"
)

REMOVE_UNQUALIFIED = {
    "enabled",
    "metrics-enabled",
    "retention-enabled",
}

OWNER_CATEGORY = {
    "spring.flyway.enabled": (
        "BOOTSTRAP_RUNTIME",
        "INFRASTRUCTURE_RUNTIME",
    ),
    "sixpay.integration.kafka.enabled": (
        "INTEGRATION_SHARED",
        "TRANSPORT",
    ),
    "sixpay.e2e.customer.enabled": (
        "TEST_E2E_RUNTIME",
        "TEST_FIXTURE",
    ),
}

REQUIRED_QUALIFIED = {
    "sixpay.notification.operational.operations.metrics-enabled",
    "sixpay.notification.operational.operations.retention-enabled",
}

VERIFY_CONTENT = 'from pathlib import Path\nimport re\nimport sys\n\nROOT = Path.cwd()\n\nREGISTRY = (\n    ROOT\n    / "documentation/architecture/configuration/"\n    / "FEATURE_FLAG_REGISTRY.yaml"\n)\n\nBUSINESS = [\n    "partner",\n    "customer",\n    "payment",\n    "accounting",\n    "reporting",\n    "notification",\n    "security",\n    "administration",\n]\n\nFORBIDDEN_UNQUALIFIED = {\n    "enabled",\n    "metrics-enabled",\n    "retention-enabled",\n}\n\nSPECIAL_EXPECTED_OWNERS = {\n    "spring.flyway.enabled": "BOOTSTRAP_RUNTIME",\n    "sixpay.integration.kafka.enabled": "INTEGRATION_SHARED",\n    "sixpay.e2e.customer.enabled": "TEST_E2E_RUNTIME",\n}\n\nREQUIRED_FLAGS = {\n    "backend.mode",\n    "authentication.standalone",\n    "authentication.local.enabled",\n    "authentication.oidc.enabled",\n    "sixpay.notification.operational.operations.metrics-enabled",\n    "sixpay.notification.operational.operations.retention-enabled",\n}\n\ndef fail(errors):\n    print("\\nFS-2.5.7 feature-flag registry validation FAILED:\\n")\n    for error in errors:\n        print(" -", error)\n    sys.exit(1)\n\ndef registry_entries():\n    text = REGISTRY.read_text(encoding="utf-8")\n    blocks = re.split(r"(?=^  - id: )", text, flags=re.MULTILINE)\n    entries = []\n\n    for block in blocks:\n        if not block.startswith("  - id: "):\n            continue\n\n        def field(name):\n            match = re.search(\n                rf"^    {re.escape(name)}:\\s*(.+)$",\n                block,\n                re.MULTILINE,\n            )\n            return match.group(1).strip().strip(\'"\') if match else None\n\n        id_match = re.search(\n            r"^  - id:\\s*(.+)$",\n            block,\n            re.MULTILINE,\n        )\n\n        entries.append({\n            "id": id_match.group(1).strip(),\n            "key": field("key"),\n            "owner": field("owner"),\n            "category": field("category"),\n        })\n\n    return entries\n\ndef main():\n    errors = []\n\n    if not REGISTRY.is_file():\n        fail(["FEATURE_FLAG_REGISTRY.yaml is missing"])\n\n    entries = registry_entries()\n    keys = [entry["key"] for entry in entries if entry["key"]]\n\n    for key in sorted(set(keys)):\n        if keys.count(key) > 1:\n            errors.append(f"duplicate registry key: {key}")\n\n    for entry in entries:\n        key = entry["key"]\n        owner = entry["owner"]\n\n        if owner == "REVIEW_REQUIRED":\n            errors.append(\n                f"{entry[\'id\']}: owner remains REVIEW_REQUIRED"\n            )\n\n        if key in FORBIDDEN_UNQUALIFIED:\n            errors.append(\n                f"unqualified parser artifact is forbidden: {key}"\n            )\n\n        for module in BUSINESS:\n            prefix = f"sixpay.{module}."\n            if key and key.startswith(prefix):\n                expected = module.upper()\n\n                if key.startswith("sixpay.e2e."):\n                    break\n\n                if owner != expected:\n                    errors.append(\n                        f"{entry[\'id\']}: {key} must be owned by "\n                        f"{expected}, found {owner}"\n                    )\n\n    key_set = set(keys)\n\n    for required in sorted(REQUIRED_FLAGS):\n        if required not in key_set:\n            errors.append(\n                f"required canonical flag is not registered: {required}"\n            )\n\n    for key, expected_owner in SPECIAL_EXPECTED_OWNERS.items():\n        matches = [\n            entry for entry in entries\n            if entry["key"] == key\n        ]\n\n        if not matches:\n            errors.append(\n                f"required special-runtime flag is missing: {key}"\n            )\n        elif matches[0]["owner"] != expected_owner:\n            errors.append(\n                f"{key} must be owned by {expected_owner}, found "\n                f"{matches[0][\'owner\']}"\n            )\n\n    if errors:\n        fail(errors)\n\n    print("FS-2.5.7 feature-flag registry validation PASSED.")\n    print(\n        f"Registered entries: {len(entries)}; ownership is explicit; "\n        "no unqualified feature-flag artifacts remain."\n    )\n\nif __name__ == "__main__":\n    main()\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def split_flag_blocks(text):
    prefix, sep, rest = text.partition("\nflags:\n")
    if not sep:
        fail("FEATURE_FLAG_REGISTRY.yaml has no flags section")

    blocks = re.split(
        r"(?=^  - id: )",
        rest,
        flags=re.MULTILINE,
    )

    return prefix + "\nflags:\n", [
        block for block in blocks if block.strip()
    ]

def block_value(block, field):
    match = re.search(
        rf"^    {re.escape(field)}:\s*(.+)$",
        block,
        re.MULTILINE,
    )
    if not match:
        return None
    return match.group(1).strip().strip('"')

def replace_field(block, field, value):
    pattern = re.compile(
        rf"^(    {re.escape(field)}:)\s*.*$",
        re.MULTILINE,
    )
    if not pattern.search(block):
        fail(
            f"Registry block for {block_value(block, 'key')} "
            f"has no {field}"
        )
    return pattern.sub(
        rf"\1 {value}",
        block,
        count=1,
    )

def update_registry():
    text = require(REGISTRY)
    header, blocks = split_flag_blocks(text)

    kept = []
    removed = []
    resolved = []

    for block in blocks:
        key = block_value(block, "key")

        if key in REMOVE_UNQUALIFIED:
            removed.append(key)
            continue

        if key in OWNER_CATEGORY:
            owner, category = OWNER_CATEGORY[key]
            block = replace_field(block, "owner", owner)
            block = replace_field(block, "category", category)
            resolved.append(key)

        kept.append(block.rstrip() + "\n")

    final = header + "".join(kept)

    for forbidden in REMOVE_UNQUALIFIED:
        if re.search(
            rf'^    key:\s*"{re.escape(forbidden)}"\s*$',
            final,
            re.MULTILINE,
        ):
            fail(f"Unqualified flag still remains: {forbidden}")

    qualified_keys = {
        block_value(block, "key")
        for block in kept
    }

    missing = sorted(REQUIRED_QUALIFIED - qualified_keys)

    if missing:
        fail(
            "Qualified Notification flags are missing from registry:\n - "
            + "\n - ".join(missing)
        )

    unresolved = re.findall(
        r"^    owner:\s*REVIEW_REQUIRED\s*$",
        final,
        re.MULTILINE,
    )

    if unresolved:
        fail(
            "REVIEW_REQUIRED owners still remain after "
            "known FS-2.5.7 resolution."
        )

    REGISTRY.write_text(final, encoding="utf-8")
    return removed, resolved, len(kept)

def update_documentation():
    text = require(DOC)

    replacement = (
        "## Unresolved ownership\n\n"
        "No unresolved feature-flag ownership.\n\n"
        "The former unqualified parser artifacts "
        "`enabled`, `metrics-enabled` and `retention-enabled` "
        "were removed. Their qualified runtime properties remain "
        "the canonical registry entries.\n"
    )

    updated = re.sub(
        r"## Unresolved ownership\n.*?(?=\n## |\Z)",
        replacement,
        text,
        flags=re.DOTALL,
    )

    DOC.write_text(updated, encoding="utf-8")

def main():
    removed, resolved, count = update_registry()
    update_documentation()

    VERIFY.write_text(
        VERIFY_CONTENT,
        encoding="utf-8",
    )

    print("FS-2.5.7 feature-flag registry resolution applied.")
    print()
    print("Removed parser artifacts:")
    for key in removed:
        print(" -", key)

    print()
    print("Resolved special ownership:")
    for key in resolved:
        print(" -", key)

    print()
    print("Qualified Notification flags preserved:")
    for key in sorted(REQUIRED_QUALIFIED):
        print(" -", key)

    print()
    print("Registry entries now:", count)
    print("Unresolved owners: 0")
    print()
    print("No runtime values/defaults/profile behavior were changed.")
    print()
    print("Validate:")
    print("  py scripts/verify_feature_flag_registry.py")

if __name__ == "__main__":
    main()
