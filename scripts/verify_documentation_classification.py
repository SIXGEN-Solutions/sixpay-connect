#!/usr/bin/env python3
from pathlib import Path
import re
import sys

ROOT = Path.cwd()
INDEX = ROOT / "documentation/DOCUMENTATION_CLASSIFICATION.yaml"
text = INDEX.read_text(encoding="utf-8") if INDEX.is_file() else ""

if not text:
    print("DOCUMENTATION CLASSIFICATION GATE FAILED")
    print(" - classification index is missing")
    sys.exit(1)

for category in ("CANONICAL", "REFERENCE_SOURCE", "HISTORICAL", "TEMPLATE"):
    if category + ":" not in text:
        print("DOCUMENTATION CLASSIFICATION GATE FAILED")
        print(" - missing category: " + category)
        sys.exit(1)

historical_block = text.split("  HISTORICAL:", 1)[1].split("  TEMPLATE:", 1)[0]
entries = re.findall(r"^\s+- (documentation/ai/[^\n]+)$", historical_block, re.MULTILINE)
excluded_block = text.split("excludedHistoricalDocuments:", 1)[1].split("precedence:", 1)[0]
excluded = re.findall(r"^\s+- (documentation/ai/[^\n]+)$", excluded_block, re.MULTILINE)

if len(entries) != 38 or len(set(entries)) != 38:
    print("DOCUMENTATION CLASSIFICATION GATE FAILED")
    print(" - expected 38 unique historical AI documents")
    sys.exit(1)
if set(entries) != set(excluded):
    print("DOCUMENTATION CLASSIFICATION GATE FAILED")
    print(" - active Master Prompt exclusions do not match HISTORICAL")
    sys.exit(1)
for relative in entries:
    if not (ROOT / relative).is_file():
        print("DOCUMENTATION CLASSIFICATION GATE FAILED")
        print(" - missing historical document: " + relative)
        sys.exit(1)

print("Documentation classification gate PASSED.")
print(" - categories: CANONICAL, REFERENCE_SOURCE, HISTORICAL, TEMPLATE")
print(" - historical AI documents retained and excluded from active context: 38")
