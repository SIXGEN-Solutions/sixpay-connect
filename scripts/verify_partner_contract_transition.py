#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
MANIFEST = ROOT / "MASTER_PROMPT_INPUT_MANIFEST.yaml"

PARTNER_IDS = {
    "partner-payment-request-api-v1",
    "partner-payment-confirmation-api-v1",
    "partner-payment-status-query-api-v1",
    "partner-payment-callback-webhook-v1",
}
SUPERSEDED = {
    "tresorpay-payment-request-api-v1": "partner-payment-request-api-v1",
    "tresorpay-payment-confirmation-api-v1": "partner-payment-confirmation-api-v1",
    "tresorpay-payment-status-query-api-v1": "partner-payment-status-query-api-v1",
}
DEFERRED = {
    "tresorpay-authorization-request-api-v1",
    "tresorpay-authorization-decision-webhook-v1",
}

def fail(errors):
    print("PARTNER CONTRACT TRANSITION GATE FAILED")
    for error in errors:
        print(" -", error)
    sys.exit(1)

def entries(text):
    if "\ncontracts:\n" not in text:
        fail(["contract registry has no contracts section"])
    body = text.split("\ncontracts:\n", 1)[1]
    raw = re.split(r'(?m)^  - id: "([^"]+)"\s*$', body)[1:]
    out = {}
    for i in range(0, len(raw), 2):
        out[raw[i]] = raw[i + 1]
    return out

registry = REGISTRY.read_text(encoding="utf-8")
manifest = MANIFEST.read_text(encoding="utf-8")
parsed = entries(registry)
errors = []

for cid in PARTNER_IDS:
    body = parsed.get(cid)
    if body is None:
        errors.append(f"missing Partner contract registry entry: {cid}")
        continue
    for token in [
        'lifecycleStatus: "ACTIVE_MVP"',
        'approvalStatus: "APPROVED"',
        'generationPolicy: "ACTIVE"',
        'codeGenerationAllowed: true',
    ]:
        if token not in body:
            errors.append(f"{cid}: missing {token}")

for old, replacement in SUPERSEDED.items():
    body = parsed.get(old)
    if body is None:
        errors.append(f"missing superseded contract registry entry: {old}")
        continue
    for token in [
        f'replacementContractId: "{replacement}"',
        'lifecycleStatus: "SUPERSEDED"',
        'generationPolicy: "REFERENCE_ONLY"',
        'codeGenerationAllowed: false',
        'included: false',
    ]:
        if token not in body:
            errors.append(f"{old}: missing {token}")

for cid in DEFERRED:
    body = parsed.get(cid)
    if body is None:
        errors.append(f"missing deferred contract registry entry: {cid}")
        continue
    for token in [
        'lifecycleStatus: "DEFERRED_FUTURE"',
        'generationPolicy: "EXCLUDED"',
        'codeGenerationAllowed: false',
        'included: false',
    ]:
        if token not in body:
            errors.append(f"{cid}: missing {token}")

# PA-7 closes only the migrated Payment/Accounting contract transition.
# CustomerSubscription and other non-Payment capabilities are outside this transition.
for cid in PARTNER_IDS:
    body = parsed.get(cid)
    if body is None:
        continue
    for marker in [
        'sourceSystem: "TRESOR_PAY"',
        'systemOfRecord: "TRESOR_PAY"',
        'direction: "TRESOR_PAY_TO_',
        '_TO_TRESOR_PAY"',
    ]:
        if marker in body:
            errors.append(f"{cid}: migrated Partner contract still depends on TRESOR_PAY via {marker}")

active_section = manifest.split("activeContractCapabilities:", 1)[1].split("excludedContracts:", 1)[0]
for cid in PARTNER_IDS:
    if f'- id: "{cid}"' not in active_section:
        errors.append(f"active manifest missing Partner contract: {cid}")
for cid in SUPERSEDED:
    if f'- id: "{cid}"' in active_section:
        errors.append(f"superseded contract appears in active manifest: {cid}")

# Physical Partner contracts must exist and identify their registered Partner capability.
# CONTRACT_REGISTRY.yaml is authoritative for lifecycle, approval and generation
# classification. PA-7 does not rewrite physical OpenAPI generation metadata.
for cid in PARTNER_IDS:
    body = parsed.get(cid)
    if body is None:
        continue
    path_match = re.search(r'(?m)^    path: "([^"]+)"', body)
    if not path_match:
        errors.append(f"{cid}: registry path missing")
        continue
    relative_path = path_match.group(1)
    path = ROOT / relative_path
    if not path.is_file():
        errors.append(f"Partner physical contract missing: {relative_path}")
        continue
    physical = path.read_text(encoding="utf-8")
    if "x-sixpay-contract:" not in physical:
        errors.append(f"{relative_path}: x-sixpay-contract metadata missing")
    if cid != "partner-payment-callback-webhook-v1":
        patterns = [f'registryId: "{cid}"', f"registryId: {cid}"]
        if not any(token in physical for token in patterns):
            errors.append(f"{relative_path}: physical metadata does not identify registry contract {cid}")

if errors:
    fail(sorted(set(errors)))

print("Partner contract final transition PASSED.")
print(" - Partner Payment contracts are ACTIVE_MVP / APPROVED / ACTIVE")
print(" - superseded TresorPay Payment contracts are REFERENCE_ONLY")
print(" - deferred TresorPay subscription contracts remain excluded")
print(" - no ACTIVE_MVP/REFERENCE_MVP registry entry depends on TRESOR_PAY")
