#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
EXPECTED_BRANCH = "feat/repository-baseline-consolidation-cleanup"
EXPECTED_HEAD = "727b34baef924942de9079b2cb8747ce34e856a4"

def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()

def require(condition, message):
    if not condition:
        raise RuntimeError(message)

def read(relative):
    path = ROOT / relative
    require(path.is_file(), f"Missing expected file: {relative}")
    return path, path.read_text(encoding="utf-8")

def write_if_changed(path, before, after):
    if before == after:
        print("OK      ", path.relative_to(ROOT))
        return
    path.write_text(after, encoding="utf-8", newline="\n")
    print("UPDATE  ", path.relative_to(ROOT))

require(Path(run("git","rev-parse","--show-toplevel")).resolve() == ROOT.resolve(), "Run from repository root")
require(run("git","branch","--show-current") == EXPECTED_BRANCH, f"Expected branch {EXPECTED_BRANCH}")
require(run("git","rev-parse","HEAD") == EXPECTED_HEAD, "Unexpected HEAD for LOT 1.R2 corrective patch")

# Fix remaining AccountBindingFingerprint test fixture.
rel = "backend/customer/src/test/java/com/sixpay/customer/verification/application/port/input/VerifyCustomerResultBankingContextTest.java"
path, text = read(rel)
old = 'AccountBindingFingerprint.of(\n                        "v1:sha256:" + "a".repeat(64)\n                )'
new = 'AccountBindingFingerprint.of(\n                        "v1:" + "a".repeat(64)\n                )'
if old in text:
    updated = text.replace(old, new, 1)
else:
    require(new in text, "Expected AccountBindingFingerprint fixture not found")
    updated = text
write_if_changed(path, text, updated)

# Harden external response validation so incomplete/unknown check sets are rejected before domain mapping.
rel = "backend/customer/src/main/java/com/sixpay/customer/verification/infrastructure/banking/error/AmplitudeResponseValidator.java"
path, text = read(rel)
updated = text

if "REQUIRED_CHECK_TYPES" not in updated:
    marker = '    private static final Set<String> CHECK_RESULTS =\n            Set.of("PASS", "FAIL", "UNKNOWN");\n'
    require(marker in updated, "CHECK_RESULTS marker not found")
    required_block = marker + '\n    private static final Set<String> REQUIRED_CHECK_TYPES = Set.of(\n            "CUSTOMER_EXISTS",\n            "FINANCIAL_INSTITUTION_MATCHES",\n            "NIU_MATCHES",\n            "IDENTITY_MATCHES",\n            "ACCOUNT_EXISTS",\n            "ACCOUNT_BELONGS_TO_CUSTOMER",\n            "ACCOUNT_IS_ACTIVE",\n            "ACCOUNT_NOT_BLOCKED",\n            "ACCOUNT_NOT_OPPOSED",\n            "REQUIRED_KYC_PRESENT",\n            "REQUIRED_KYC_VERIFIED"\n    );\n'
    updated = updated.replace(marker, required_block, 1)

if "Unsupported Amplitude verification check type" not in updated:
    marker = '            String result = required(check.result(), "check result");\n'
    require(marker in updated, "check result marker not found")
    insertion = '            if (!REQUIRED_CHECK_TYPES.contains(type)) {\n                throw invalid(\n                        "Unsupported Amplitude verification check type"\n                );\n            }\n\n' + marker
    updated = updated.replace(marker, insertion, 1)

if "observedTypes.equals(REQUIRED_CHECK_TYPES)" not in updated:
    marker = '        if ("VERIFIED".equals(outcome)) {\n'
    require(marker in updated, "VERIFIED marker not found")
    insertion = '        if (!observedTypes.equals(REQUIRED_CHECK_TYPES)) {\n            Set<String> missing = new HashSet<>(REQUIRED_CHECK_TYPES);\n            missing.removeAll(observedTypes);\n            throw invalid(\n                    "Amplitude verification checks are incomplete; missing=" + missing\n            );\n        }\n\n' + marker
    updated = updated.replace(marker, insertion, 1)

write_if_changed(path, text, updated)

# Final invariant checks.
_, test_text = read("backend/customer/src/test/java/com/sixpay/customer/verification/application/port/input/VerifyCustomerResultBankingContextTest.java")
require('"v1:sha256:" + "a".repeat(64)' not in test_text, "Bad account-binding fingerprint still present")
require('"v1:" + "a".repeat(64)' in test_text, "Correct account-binding fingerprint missing")
_, validator_text = read("backend/customer/src/main/java/com/sixpay/customer/verification/infrastructure/banking/error/AmplitudeResponseValidator.java")
for token in ["REQUIRED_CHECK_TYPES", "REQUIRED_KYC_VERIFIED", "observedTypes.equals(REQUIRED_CHECK_TYPES)", "Unsupported Amplitude verification check type"]:
    require(token in validator_text, f"Missing validator invariant: {token}")

print()
print("LOT 1.R2 remaining test correction applied.")
print("No Payment lifecycle, contract, persistence, migration, commit or push was changed.")
