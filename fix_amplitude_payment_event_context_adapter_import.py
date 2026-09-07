#!/usr/bin/env python3
from pathlib import Path
import subprocess

BASE_SHA = "04506dc2afd4dceff25063100c2581ebab4bdde5"
ROOT = Path.cwd()
TARGET = ROOT / "backend/payment/src/main/java/com/sixpay/payment/infrastructure/banking/amplitude/posting/configuration/AmplitudePostingConfiguration.java"

def require(cond, msg):
    if not cond:
        raise RuntimeError(msg)

def git(*args):
    return subprocess.run(
        ["git", *args],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )

require((ROOT / ".git").exists(), "Run from repository root")
head = git("rev-parse", "HEAD").stdout.strip()
require(
    git("merge-base", "--is-ancestor", BASE_SHA, "HEAD").returncode == 0,
    f"Local HEAD {head} is not based on {BASE_SHA}",
)
require(TARGET.exists(), f"Missing file: {TARGET}")

s = TARGET.read_text(encoding="utf-8")

adapter_fqcn = (
    "import com.sixpay.payment.infrastructure.banking.amplitude.posting."
    "AmplitudePaymentEventContextAdapter;\n"
)

if adapter_fqcn in s:
    print("Import already present; nothing to do.")
else:
    anchor = (
        "import com.sixpay.payment.infrastructure.banking.amplitude.posting."
        "AmplitudePaymentEventAdapter;\n"
    )
    require(anchor in s, "Expected import anchor not found")
    s = s.replace(anchor, anchor + adapter_fqcn, 1)
    TARGET.write_text(s, encoding="utf-8", newline="\n")
    print("Added missing AmplitudePaymentEventContextAdapter import.")

print("Base SHA:", BASE_SHA)
print("Observed local HEAD:", head)
print("No commit or push performed.")
