#!/usr/bin/env python3
from pathlib import Path
import sys
import yaml

ROOT = Path(__file__).resolve().parents[1]
CONTRACT_DIR = ROOT / "documentation/contracts/internal"
EXPECTED = [
    "payment-query-api-v1.yaml",
    "observed-customer-query-api-v1.yaml",
    "payment-audit-query-api-v1.yaml",
]

errors = []
operation_ids = set()

for name in EXPECTED:
    path = CONTRACT_DIR / name
    if not path.is_file():
        errors.append(f"missing: {path}")
        continue

    try:
        doc = yaml.safe_load(path.read_text(encoding="utf-8"))
    except Exception as exc:
        errors.append(f"{name}: invalid YAML: {exc}")
        continue

    if doc.get("openapi") != "3.1.0":
        errors.append(f"{name}: openapi must be 3.1.0")

    info = doc.get("info") or {}
    if info.get("version") != "1.0.0":
        errors.append(f"{name}: info.version must be 1.0.0")

    meta = info.get("x-sixpay-contract") or {}
    required_meta = [
        "registryId", "gate", "phaseStep", "lifecycleStatus",
        "approvalStatus", "generationPolicy", "codeGenerationAllowed",
        "capability", "direction", "readOnly",
    ]
    for key in required_meta:
        if key not in meta:
            errors.append(f"{name}: missing info.x-sixpay-contract.{key}")

    if meta.get("phaseStep") != "1.7":
        errors.append(f"{name}: phaseStep must be 1.7")
    if meta.get("readOnly") is not True:
        errors.append(f"{name}: contract must be read-only")
    if meta.get("codeGenerationAllowed") is not False:
        errors.append(f"{name}: codeGenerationAllowed must be false before approval")

    paths = doc.get("paths") or {}
    if not paths:
        errors.append(f"{name}: no paths declared")

    for route, item in paths.items():
        for method, operation in item.items():
            if method.lower() not in {"get", "post", "put", "patch", "delete", "options", "head", "trace"}:
                continue
            # The only POST accepted in this pack is an asynchronous audit export request.
            if method.lower() != "get":
                allowed = (
                    name == "payment-audit-query-api-v1.yaml"
                    and route == "/internal/api/v1/payment-audit-exports"
                    and method.lower() == "post"
                )
                if not allowed:
                    errors.append(f"{name}: forbidden mutation-like operation {method.upper()} {route}")
            op_id = operation.get("operationId")
            if not op_id:
                errors.append(f"{name}: missing operationId for {method.upper()} {route}")
            elif op_id in operation_ids:
                errors.append(f"{name}: duplicate operationId {op_id}")
            else:
                operation_ids.add(op_id)

            parameters = operation.get("parameters") or []
            if not any(
                isinstance(p, dict) and p.get("$ref") == "#/components/parameters/CorrelationId"
                for p in parameters
            ):
                errors.append(f"{name}: {op_id or route} must require X-Correlation-ID")

            responses = operation.get("responses") or {}
            if "200" not in responses and "202" not in responses:
                errors.append(f"{name}: {op_id or route} must define 200 or 202")
            for status in ("400", "401", "403"):
                if status not in responses:
                    errors.append(f"{name}: {op_id or route} missing {status} response")

    schemas = ((doc.get("components") or {}).get("schemas") or {})
    problem = schemas.get("ProblemDetail") or {}
    required_problem = set(problem.get("required") or [])
    for key in ("type", "title", "status", "code", "correlationId"):
        if key not in required_problem:
            errors.append(f"{name}: ProblemDetail must require {key}")

    text = path.read_text(encoding="utf-8").lower()
    forbidden_markers = [
        "password:",
        "clientsecret:",
        "subscriptionkey:",
        "authorization: bearer ",
        "iban: cm",
        "accountnumber:",
    ]
    for marker in forbidden_markers:
        if marker in text:
            errors.append(f"{name}: potential sensitive example detected: {marker}")

if errors:
    print("Payment internal contract pack validation FAILED")
    for error in errors:
        print(f" - {error}")
    sys.exit(1)

print("Payment internal contract pack validation PASSED")
print(f" - YAML files: {len(EXPECTED)}")
print(f" - Unique operations: {len(operation_ids)}")
print(" - OpenAPI version: 3.1.0")
print(" - Contract version: 1.0.0")
print(" - Required metadata: present")
print(" - Read-only boundary: enforced")
print(" - Correlation header: required")
print(" - RFC 7807 fields: present")
