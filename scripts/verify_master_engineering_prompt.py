#!/usr/bin/env python3
from pathlib import Path
import json
import re
import subprocess
import sys


ROOT = Path(__file__).resolve().parents[1]
PROMPT = ROOT / "MASTER_ENGINEERING_PROMPT.md"
MANIFEST = ROOT / "MASTER_PROMPT_INPUT_MANIFEST.yaml"
CLASSIFICATION = ROOT / "documentation/DOCUMENTATION_CLASSIFICATION.yaml"
PACKAGE_JSON = ROOT / "frontend/package.json"


def fail(message):
    print("ACTIVE MASTER ENGINEERING PROMPT GATE FAILED")
    print(" - " + message)
    sys.exit(1)


def read(path):
    if not path.is_file():
        fail(f"required file is missing: {path.relative_to(ROOT)}")
    return path.read_text(encoding="utf-8")


def verify_source_commit(prompt):
    match = re.search(r"\| Commit source \| `([0-9a-f]{40})` \|", prompt)
    if match is None:
        fail("prompt source commit is missing or is not a full Git SHA")
    completed = subprocess.run(
        ["git", "merge-base", "--is-ancestor", match.group(1), "HEAD"],
        cwd=ROOT,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        check=False,
    )
    if completed.returncode != 0:
        fail("prompt source commit is not an ancestor of HEAD")


def main():
    prompt = read(PROMPT)
    manifest = read(MANIFEST)
    classification = read(CLASSIFICATION)
    package = json.loads(read(PACKAGE_JSON))

    versioned_prompt_duplicates = sorted(ROOT.glob("MASTER_ENGINEERING_PROMPT_V*.md"))
    if versioned_prompt_duplicates:
        fail(
            "versioned prompt duplicate is present: "
            + versioned_prompt_duplicates[0].name
        )

    metadata = [
        "ACTIVE — Prompt canonique d’ingénierie assistée par IA",
        "Version | **2.0.0**",
        "`MASTER_PROMPT_INPUT_MANIFEST.yaml` version `1.0`",
        "`backend/partner`",
    ]
    for value in metadata:
        if value not in prompt:
            fail(f"missing prompt metadata: {value}")

    required_headings = [
        "## 1. Rôle et mission",
        "## 2. Contrat d’invocation",
        "## 3. Activation du contexte",
        "## 4. Ordre d’autorité",
        "## 5. Préflight obligatoire",
        "## 6. Gouvernance des décisions et approbations",
        "## 7. Architecture du repository",
        "## 8. Golden module et ownership Subscription",
        "## 9. Contract-first et traçabilité",
        "## 10. Séquence d’implémentation",
        "## 11. Règles backend",
        "## 12. Persistance et transactions",
        "## 13. Intégrations, événements et fiabilité financière",
        "## 14. Sécurité et identité",
        "## 15. Configuration et observabilité",
        "## 16. Règles frontend",
        "## 17. Tests et validations",
        "## 18. Contrôle du changement",
        "## 19. Gestion des blocages",
        "## 20. Protocole de collaboration et de sortie",
        "## 21. Definition of Ready",
        "## 22. Definition of Done",
        "## 23. Directive finale",
    ]
    prompt_headings = re.findall(r"(?m)^## .+$", prompt)
    if prompt_headings != required_headings:
        fail("prompt section set or order differs from the canonical structure")

    required_rules = [
        "La cohérence avec les sources d’autorité prévaut sur la créativité.",
        "ne charge jamais les 38 documents",
        "ACTIVE_MVP` ou `REFERENCE_MVP`",
        "`approvalStatus`, `generationPolicy` et `codeGenerationAllowed`",
        "SIXPAY CONNECT est un monolithe modulaire.",
        "`backend/partner` est la référence structurelle",
        "ne crée jamais `backend/subscription`",
        "`payment` ne possède ni ne gère `CustomerSubscription`",
        "l’IdP prouve l’identité ; SIXPAY possède les rôles et permissions métier",
        "la session applicative backend unifiée",
        "aucun retry aveugle d’une commande financière",
        "npm run verify:sixpay",
        "mvn -Pfull-tests clean verify",
        "py scripts/verify_baseline.py",
        "git diff --check",
        "Une demande d’analyse n’autorise pas une modification.",
    ]
    for rule in required_rules:
        if rule not in prompt:
            fail(f"missing canonical prompt rule: {rule}")

    expected_precedence = [
        "branche d’implémentation autoritative",
        "`documentation/architecture/`",
        "`documentation/requirements/`",
        "`documentation/contracts/`",
        "`documentation/ai/`",
        "engineering assets",
        "`ENGINEERING_CONTEXT.md`",
    ]
    authority = prompt.split("## 4. Ordre d’autorité", 1)[1].split(
        "## 5. Préflight obligatoire", 1
    )[0]
    numbered = re.findall(r"(?m)^\d+\. (.+?)(?: ;|\.)$", authority)
    if numbered != expected_precedence:
        fail("prompt source-of-truth precedence is incomplete or reordered")

    historical_block = classification.split(
        "excludedHistoricalDocuments:", 1
    )[1].split("precedence:", 1)[0]
    historical_paths = re.findall(
        r"^\s+- (documentation/ai/[^\n]+)$",
        historical_block,
        re.MULTILINE,
    )
    if len(historical_paths) != 38:
        fail("documentation classification no longer exposes 38 exclusions")
    leaked = [path for path in historical_paths if path in prompt]
    if leaked:
        fail(f"historical AI path leaked into active prompt: {leaked[0]}")

    forbidden_content = [
        "sessionStorage",
        "extractSixpayRoles",
        "npm run gate:",
        "TODO",
        "FIXME",
        "TBD",
    ]
    for value in forbidden_content:
        if value in prompt:
            fail(f"obsolete or placeholder content found in prompt: {value}")
    if re.search(r"\b(?:FS|DA|IA)-\d|LOT_\d", prompt):
        fail("phase/lot implementation identifier found in active prompt")

    scripts = package.get("scripts", {})
    for script in ("verify:sixpay", "verify:ci"):
        if script not in scripts:
            fail(f"prompt references missing frontend script: {script}")

    manifest_rules = [
        'path: "MASTER_ENGINEERING_PROMPT.md"',
        'version: "2.0.0"',
        'status: "ACTIVE"',
        'validationCommand: "py scripts/verify_master_engineering_prompt.py"',
        'retentionPolicy: "GIT_HISTORY_ONLY"',
        'activeInstructionAllowed: false',
        'repositoryFilesRequired: false',
    ]
    for rule in manifest_rules:
        if rule not in manifest:
            fail(f"active manifest is not synchronized with prompt: {rule}")
    if '    - "MASTER_ENGINEERING_PROMPT.md"' not in manifest:
        fail("active prompt is not part of contextSelection.alwaysLoad")

    classification_rules = [
        "activePrompt: MASTER_ENGINEERING_PROMPT.md",
        "supersededPromptRetention: GIT_HISTORY_ONLY",
    ]
    for rule in classification_rules:
        if rule not in classification:
            fail(f"documentation classification is not synchronized: {rule}")

    line_count = len(prompt.splitlines())
    if line_count < 450 or line_count > 900:
        fail(f"unexpected prompt size: {line_count} lines")

    verify_source_commit(prompt)

    print("Active Master Engineering Prompt gate PASSED.")
    print(" - version: 2.0.0")
    print(" - versioned prompt duplicates in repository root: none")
    print(f" - canonical sections: {len(required_headings)}")
    print(f" - historical AI paths excluded: {len(historical_paths)}")
    print(" - backend, frontend and full-stack rules: present")
    print(" - Partner golden-module rule: present")
    print(" - CustomerSubscription / TRESOR PAY boundary: present")
    print(" - unified Local/OIDC backend-session model: present")
    print(" - canonical validation commands: present")


if __name__ == "__main__":
    main()
