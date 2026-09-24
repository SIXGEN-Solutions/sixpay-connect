#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
STEM = "tresor" + "pay"
TOKENS = (STEM, "tresor" + "_pay", "tresor" + " pay", "tresor" + "-pay")

TEXT_SUFFIXES = {
    ".md", ".yaml", ".yml", ".json", ".txt", ".csv", ".java", ".kt", ".kts",
    ".ts", ".tsx", ".js", ".jsx", ".html", ".scss", ".sass", ".css", ".py",
    ".sh", ".ps1", ".bat", ".cmd", ".xml", ".properties", ".sql", ".conf",
    ".ini", ".toml", ".gradle",
}

DEFERRED_IDS = (
    STEM + "-authorization-request-api-v1",
    STEM + "-authorization-decision-webhook-v1",
)
DEFERRED_PATH_EXCEPTIONS = {
    "documentation/contracts/" + STEM + "/" + DEFERRED_IDS[0] + ".yaml",
    "documentation/contracts/" + STEM + "/" + DEFERRED_IDS[1] + ".yaml",
}

ALLOWED_GOVERNANCE_LINES = {
    "MASTER_PROMPT_INPUT_MANIFEST.yaml": {
        "external" + "Tresor" + "PaySubscription:",
        'systemOfRecord: "TRESOR' + '_PAY"',
        '- "' + DEFERRED_IDS[0] + '"',
        '- "' + DEFERRED_IDS[1] + '"',
        'rule: "The external TRESOR' + ' PAY subscription lifecycle is outside the MVP."',
        '- id: "' + DEFERRED_IDS[0] + '"',
        'path: "documentation/contracts/' + STEM + '/' + DEFERRED_IDS[0] + '.yaml"',
        '- id: "' + DEFERRED_IDS[1] + '"',
        'path: "documentation/contracts/' + STEM + '/' + DEFERRED_IDS[1] + '.yaml"',
    },
    "documentation/contracts/CONTRACT_REGISTRY.yaml": {
        'TRESOR' + '_PAY: "TRESOR' + ' PAY."',
        '- id: "' + DEFERRED_IDS[0] + '"',
        'path: "documentation/contracts/' + STEM + '/' + DEFERRED_IDS[0] + '.yaml"',
        'direction: "TRESOR' + '_PAY_TO_SIXPAY"',
        'systemOfRecord: "TRESOR' + '_PAY"',
        'reason: "For the MVP, subscription initiation and validation remain entirely in TRESOR' + ' PAY."',
        'purpose: "Allow a SIXPAY bank-agent interface to retrieve and process a pending TRESOR' + ' PAY subscription."',
        'prerequisite: "A future TRESOR' + ' PAY integration contract and explicit architecture approval."',
        '- id: "' + DEFERRED_IDS[1] + '"',
        'path: "documentation/contracts/' + STEM + '/' + DEFERRED_IDS[1] + '.yaml"',
        'direction: "SIXPAY_TO_TRESOR' + '_PAY"',
        'purpose: "Return a bank-agent decision to TRESOR' + ' PAY when the future SIXPAY-assisted subscription journey is activated."',
        'sourceSystem: "TRESOR' + '_PAY"',
        'subscriptionSystemOfRecord: "TRESOR' + '_PAY"',
        'id: "' + STEM + '-subscription-verification-api-v1"',
        '- "TRESOR' + ' PAY confirms signed-token issuance and claims"',
        '- "TRESOR' + ' PAY confirms approved JWKS publication and rotation"',
        '- "The absent ' + STEM + '-subscription-verification-api-v1 contract is intentional while LOCAL_SIGNED_TOKEN_VALIDATION remains approved for the MVP."',
    },
}

def fail(errors):
    print("PERMANENT PARTNER-NEUTRAL ARCHITECTURE GATE FAILED")
    for item in sorted(set(errors)):
        print(" -", item)
    sys.exit(1)

def tracked_files():
    p = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if p.returncode != 0:
        sys.stderr.write(p.stderr.decode("utf-8", errors="replace"))
        sys.exit(p.returncode)
    return [
        raw.decode("utf-8", errors="surrogateescape").replace("\\", "/")
        for raw in p.stdout.split(b"\0") if raw
    ]

def contains_forbidden(value):
    low = value.lower()
    return any(token in low for token in TOKENS)

def deferred_contract_is_still_excluded(registry, contract_id):
    marker = f'- id: "{contract_id}"'
    start = registry.find(marker)
    if start < 0:
        return False
    next_entry = registry.find("\n  - id:", start + len(marker))
    block = registry[start:] if next_entry < 0 else registry[start:next_entry]
    return (
        'lifecycleStatus: "DEFERRED_FUTURE"' in block
        and 'generationPolicy: "EXCLUDED"' in block
        and "codeGenerationAllowed: false" in block
    )

registry = (ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml").read_text(encoding="utf-8")
for contract_id in DEFERRED_IDS:
    if not deferred_contract_is_still_excluded(registry, contract_id):
        fail([f"{contract_id} [deferred governance exception is no longer valid]"])

errors = []
for rel in tracked_files():
    if contains_forbidden(rel) and rel not in DEFERRED_PATH_EXCEPTIONS:
        errors.append(f"{rel} [path]")

    path = ROOT / rel
    if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
        continue
    if rel in DEFERRED_PATH_EXCEPTIONS:
        continue

    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        continue
    if not contains_forbidden(text):
        continue

    allowed = ALLOWED_GOVERNANCE_LINES.get(rel)
    if allowed is not None:
        for line_no, line in enumerate(text.splitlines(), start=1):
            if contains_forbidden(line) and line.strip() not in allowed:
                errors.append(f"{rel}:{line_no} [content]")
        continue

    errors.append(f"{rel} [content]")

if errors:
    fail(errors)

print("Permanent Partner-neutral architecture gate PASSED.")
print(" - tracked content scanned across backend/contracts/config/scripts/frontend/docs/tests")
print(" - tracked file and directory names scanned")
print(" - active/current-state provider naming forbidden")
print(" - deferred exceptions are exact and lifecycle-guarded")
