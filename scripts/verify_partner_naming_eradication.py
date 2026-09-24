#!/usr/bin/env python3
from pathlib import Path
import subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
stem = "tresor" + "pay"
TOKENS = (stem, "tresor" + "_pay", "tresor" + " pay")

EXCLUDED = {
    "MASTER_PROMPT_INPUT_MANIFEST.yaml",
    "documentation/contracts/CONTRACT_REGISTRY.yaml",
    "documentation/DOCUMENTATION_CLASSIFICATION.yaml",
    "documentation/contracts/" + stem + "/" + stem + "-authorization-request-api-v1.yaml",
    "documentation/contracts/" + stem + "/" + stem + "-authorization-decision-webhook-v1.yaml",
}
TEXT_SUFFIXES = {
    ".md", ".yaml", ".yml", ".json", ".txt", ".csv",
    ".java", ".ts", ".html", ".scss", ".css",
    ".py", ".sh", ".ps1", ".xml", ".properties",
}
p = subprocess.run(["git", "ls-files"], cwd=ROOT, text=True,
                   stdout=subprocess.PIPE, stderr=subprocess.PIPE)
if p.returncode != 0:
    print(p.stderr.strip())
    sys.exit(p.returncode)

errors = []
for rel in filter(None, p.stdout.splitlines()):
    normalized = rel.replace("\\", "/")
    if normalized in EXCLUDED:
        continue
    low_path = normalized.lower()
    if any(token in low_path for token in TOKENS):
        errors.append(f"{normalized} [path]")
        continue
    path = ROOT / normalized
    if not path.is_file() or path.suffix.lower() not in TEXT_SUFFIXES:
        continue
    try:
        text = path.read_text(encoding="utf-8").lower()
    except UnicodeDecodeError:
        continue
    if any(token in text for token in TOKENS):
        errors.append(f"{normalized} [content]")

if errors:
    print("PARTNER NAMING ERADICATION GATE FAILED")
    for item in sorted(set(errors)):
        print(" -", item)
    sys.exit(1)

print("Partner naming eradication PASSED.")
print(" - tracked active/current-state sources are Partner-neutral")
print(" - frontend/mocks/fixtures/tests/scripts are Partner-neutral")
print(" - stale build output and IDE-local files are outside repository-source scope")
print(" - only explicit deferred contract-governance exceptions remain")
