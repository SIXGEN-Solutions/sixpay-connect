from pathlib import Path
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
README = ROOT / "documentation/contracts/README.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

CANONICAL_SECTION = """## Canonical contract index

`CONTRACT_REGISTRY.yaml` is the **canonical contractual table of contents** for
the current SIXPAY CONNECT repository baseline.

It answers the repository-level governance questions:

- which contracts exist;
- which domain and capability they belong to;
- who owns the capability and security boundary;
- the interaction direction;
- source system and system of record;
- lifecycle and approval state;
- generation policy;
- security classification;
- current MVP usage;
- the canonical physical contract path.

The registry is therefore authoritative for **contract classification,
ownership, lifecycle, approval, generation policy and usage metadata**.

Physical contract files remain authoritative for the **interface itself**:

- paths/endpoints;
- operations;
- request and response payloads;
- schemas;
- parameters;
- protocol-level security declarations;
- error responses;
- event/message structure for asynchronous contracts.

The intended relationship is:

```text
CONTRACT_REGISTRY.yaml
        |
        +-- contract identity / capability
        +-- ownership / direction
        +-- lifecycle / approval
        +-- generation policy
        +-- security classification
        +-- MVP usage
        +-- canonical physical path
                    |
                    v
          physical contract file
                    |
                    +-- interface/protocol truth
```

A physical contract must not become a second independent registry.
Conversely, the registry must not duplicate full interface definitions.

When a physical contract repeats governance metadata through
`info.x-sixpay-contract`, that metadata is a contract-local mirror used for
traceability and validation. It must remain consistent with
`CONTRACT_REGISTRY.yaml`; the registry remains the canonical cross-contract
index.

### Source-of-truth rule

For the current repository baseline:

1. `CONTRACT_REGISTRY.yaml` is authoritative for registry-level governance
   metadata.
2. The physical contract referenced by `path` is authoritative for the
   interface/protocol definition.
3. Git history is authoritative for historical changes.
4. Transitional patch artifacts are not valid current-state sources of truth.

This separation allows the future Master AI Context to discover the complete
contract landscape from a single registry without flattening bounded API and
integration contracts into one monolithic specification.
"""


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}."
        )

    registry_before = require(REGISTRY)
    readme_before = require(README)

    marker = "## Canonical contract index"

    if marker in readme_before:
        print("FS-2.2.0 already applied; no change required.")
        return

    intro = """# SIXPAY CONNECT — Registre des contrats

Ce dossier contient les contrats d’intégration et d’API versionnés de
SIXPAY CONNECT.

La présence physique d’un contrat dans le dépôt ne détermine pas à elle seule
son usage courant. La classification normative et l’index cross-domain sont
portés par [`CONTRACT_REGISTRY.yaml`](./CONTRACT_REGISTRY.yaml).
"""

    existing_lines = readme_before.splitlines()
    first_section_index = None

    for i, line in enumerate(existing_lines):
        if line.startswith("## "):
            first_section_index = i
            break

    if first_section_index is None:
        historical_tail = ""
    else:
        historical_tail = "\n".join(
            existing_lines[first_section_index:]
        ).strip()

    updated_parts = [
        intro.rstrip(),
        CANONICAL_SECTION.strip(),
    ]

    if historical_tail:
        updated_parts.append(historical_tail)

    updated = "\n\n".join(updated_parts) + "\n"

    README.write_text(updated, encoding="utf-8")

    registry_after = REGISTRY.read_text(encoding="utf-8")

    if registry_before != registry_after:
        fail(
            "Safety check failed: CONTRACT_REGISTRY.yaml changed during "
            "FS-2.2.0."
        )

    print("FS-2.2.0 registry canonicality applied.")
    print()
    print("Modified only:")
    print(" - documentation/contracts/README.md")
    print()
    print("Canonical rule:")
    print(" - CONTRACT_REGISTRY.yaml = contractual table of contents and governance metadata")
    print(" - physical contracts = interface/protocol truth")
    print(" - Git history = change history")
    print(" - patch artifacts = not current-state sources of truth")
    print()
    print("No registry entry, capability, lifecycle, approval or contract was modified.")
    print()
    print("Review:")
    print(" git diff -- documentation/contracts/README.md")


if __name__ == "__main__":
    main()
