from pathlib import Path
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
GATE = ROOT / "frontend/scripts/verify-contract-consolidation.mjs"
README = ROOT / "documentation/contracts/README.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

TRIGGER_PATH = (
    "documentation/contracts/internal/"
    "notification-operational-trigger-v1.md"
)
EMAIL_PATH = (
    "documentation/contracts/internal/"
    "notification-operational-email-v1.md"
)

TRIGGER_ENTRY = '''
  - id: "notification-operational-trigger-v1"
    path: "documentation/contracts/internal/notification-operational-trigger-v1.md"
    domain: "notification"
    businessOwner: "notification"
    sourceFactOwners:
      - "payment"
      - "accounting"
    capability: "OPERATIONAL_NOTIFICATION_TRIGGER"
    direction: "INTERNAL_DOMAIN_TO_NOTIFICATION"
    sourceSystem: "SIXPAY"
    systemOfRecord: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "PENDING_APPROVAL"
    generationPolicy: "REFERENCE_ONLY"
    codeGenerationAllowed: false
    gate: "FS-2_REPOSITORY_BASELINE_CONSOLIDATION"
    phaseStep: "FS-2.2.2"
    security:
      authentication: "IN_PROCESS_TRUST_BOUNDARY"
      dataClassification: "INTERNAL"
    mvpUsage:
      included: true
      readOnly: true
      transport: "IN_PROCESS_MODULAR_MONOLITH"
      purpose:
        - "Consume the Payment posted semantic fact"
        - "Consume the Accounting batch completed semantic fact"
        - "Drive Notification routing and template selection"
      constraints:
        - "The contract does not mandate Kafka"
        - "Payment remains owner of payment.posted.v1"
        - "Accounting remains owner of accounting.batch.completed.v1"
        - "Notification owns trigger-consumption semantics"
        - "Raw account, NIU, full Customer identity and credentials are excluded"
'''

EMAIL_ENTRY = '''
  - id: "notification-operational-email-v1"
    path: "documentation/contracts/internal/notification-operational-email-v1.md"
    domain: "notification"
    businessOwner: "notification"
    capability: "OPERATIONAL_EMAIL_DISPATCH"
    direction: "NOTIFICATION_TO_EMAIL_PROVIDER"
    sourceSystem: "SIXPAY"
    systemOfRecord: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "PENDING_APPROVAL"
    generationPolicy: "REFERENCE_ONLY"
    codeGenerationAllowed: false
    gate: "FS-2_REPOSITORY_BASELINE_CONSOLIDATION"
    phaseStep: "FS-2.2.2"
    security:
      authentication: "DEPLOYMENT_SECRET_BACKED_PROVIDER_AUTHENTICATION"
      dataClassification: "INTERNAL"
    mvpUsage:
      included: true
      readOnly: false
      providerMode: "SMTP"
      purpose:
        - "Dispatch an EMAIL NotificationIntent through the SMTP adapter"
        - "Return the normalized NotificationDispatchResult"
        - "Classify retryable and permanent delivery errors"
      constraints:
        - "ACCEPTED does not claim final mailbox delivery"
        - "SMTP credentials come from deployment configuration or secret store"
        - "Provider error messages are not part of the contract and are not persisted"
'''

GATE_INSERTION_POINT = '''    const admin = registry.contracts.find(
      (contract) => contract.id === 'administration-query-api-v1',
    );
'''

GATE_BLOCK = r'''
    /*
     * FS-2.2.2 — Registry <-> filesystem integrity
     */
    const registeredPhysicalPaths = new Set(
      registry.contracts
        .map((contract) => contract?.path)
        .filter((contractPath) => typeof contractPath === 'string'),
    );

    function isCanonicalPhysicalContract(file) {
      const relative = path
        .relative(repoRoot, file)
        .replaceAll('\\', '/');

      if (relative === registryRelative) {
        return false;
      }

      const extension = path.extname(file).toLowerCase();

      if (
        extension === '.yaml'
        || extension === '.yml'
        || extension === '.json'
      ) {
        return true;
      }

      if (
        extension === '.md'
        && path.dirname(relative)
          === 'documentation/contracts/internal'
      ) {
        return true;
      }

      return false;
    }

    for (const contract of registry.contracts) {
      if (!contract?.path) {
        continue;
      }

      const absolute = path.join(repoRoot, contract.path);

      if (!fs.existsSync(absolute)) {
        fail(
          `${registryRelative}: ${contract.id} points to missing file `
            + `${contract.path}`,
        );
        continue;
      }

      if (isForbiddenHistoricalContractArtifact(absolute)) {
        fail(
          `${registryRelative}: ${contract.id} points to forbidden `
            + `historical artifact ${contract.path}`,
        );
      }
    }

    for (const file of walk(contractsRoot)) {
      if (!isCanonicalPhysicalContract(file)) {
        continue;
      }

      if (isForbiddenHistoricalContractArtifact(file)) {
        continue;
      }

      const relative = path
        .relative(repoRoot, file)
        .replaceAll('\\', '/');

      if (!registeredPhysicalPaths.has(relative)) {
        fail(
          `${relative}: canonical physical contract is not registered in `
            + 'CONTRACT_REGISTRY.yaml',
        );
      }
    }
'''

README_MARKER = "## Registry ↔ filesystem integrity"

README_SECTION = '''## Registry ↔ filesystem integrity

The canonical registry and the physical contract tree must remain consistent in
both directions.

The following invariants are mandatory:

1. every `contracts[*].path` declared by `CONTRACT_REGISTRY.yaml` must resolve
   to an existing file;
2. a registry path must never reference a historical/transitional artifact;
3. every canonical physical contract must be referenced by at least one
   registry entry;
4. multiple capabilities may reference the same physical contract when the
   consolidation is intentional and ownership remains explicit.

The Administration Operational contract is the current canonical example of
rule 4:

```text
ADMINISTRATION_OPERATIONAL_QUERY ─┐
                                  ├─> administration-operational-api-v1.yaml
OPERATIONAL_INCIDENT_QUERY ───────┘
```

The integrity gate treats the following files as canonical contracts in the
current repository layout:

- YAML/YML/JSON specifications below `documentation/contracts/`, excluding the
  registry itself;
- Markdown contracts directly below `documentation/contracts/internal/`.

Governance documents such as this `README.md` are not physical contracts and
must not be registered as capabilities.
'''


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

    registry = require(REGISTRY)
    gate = require(GATE)
    readme = require(README)

    for relative in [TRIGGER_PATH, EMAIL_PATH]:
        if not (ROOT / relative).is_file():
            fail(f"Canonical Notification contract is missing: {relative}")

    entries_to_add = []

    if 'id: "notification-operational-trigger-v1"' not in registry:
        entries_to_add.append(TRIGGER_ENTRY.strip("\n"))

    if 'id: "notification-operational-email-v1"' not in registry:
        entries_to_add.append(EMAIL_ENTRY.strip("\n"))

    if entries_to_add:
        marker = "\nmissingMvpContracts:\n"

        if marker not in registry:
            fail("Could not find missingMvpContracts insertion marker.")

        registry = registry.replace(
            marker,
            "\n"
            + "\n\n".join(entries_to_add)
            + marker,
            1,
        )

    if "FS-2.2.2 — Registry <-> filesystem integrity" not in gate:
        if GATE_INSERTION_POINT not in gate:
            fail(
                "Could not find registry integrity insertion point in "
                "verify-contract-consolidation.mjs."
            )

        gate = gate.replace(
            GATE_INSERTION_POINT,
            GATE_BLOCK + "\n" + GATE_INSERTION_POINT,
            1,
        )

    if README_MARKER not in readme:
        insertion_point = "## Historical artifact policy"

        if insertion_point not in readme:
            fail(
                "Could not find README insertion point for FS-2.2.2."
            )

        readme = readme.replace(
            insertion_point,
            README_SECTION.rstrip()
            + "\n\n"
            + insertion_point,
            1,
        )

    REGISTRY.write_text(registry, encoding="utf-8")
    GATE.write_text(gate, encoding="utf-8")
    README.write_text(readme, encoding="utf-8")

    final_registry = REGISTRY.read_text(encoding="utf-8")
    final_gate = GATE.read_text(encoding="utf-8")

    for token in [
        'id: "notification-operational-trigger-v1"',
        f'path: "{TRIGGER_PATH}"',
        'id: "notification-operational-email-v1"',
        f'path: "{EMAIL_PATH}"',
    ]:
        if token not in final_registry:
            fail(f"Registry update incomplete: missing {token}")

    for token in [
        "registeredPhysicalPaths",
        "isCanonicalPhysicalContract",
        "canonical physical contract is not registered",
        "points to forbidden",
    ]:
        if token not in final_gate:
            fail(f"Gate update incomplete: missing token {token}")

    print("FS-2.2.2 Registry <-> filesystem integrity applied.")
    print()
    print("Registry normalization:")
    print(" - registered notification-operational-trigger-v1.md")
    print(" - registered notification-operational-email-v1.md")
    print()
    print("Gate invariants:")
    print(" - every registry path exists")
    print(" - no registry path targets a historical artifact")
    print(" - every canonical physical contract is registered")
    print(" - intentional many-capabilities -> one-file mapping remains allowed")
    print()
    print("Updated:")
    print(" - documentation/contracts/CONTRACT_REGISTRY.yaml")
    print(" - frontend/scripts/verify-contract-consolidation.mjs")
    print(" - documentation/contracts/README.md")
    print()
    print("Validate:")
    print("  cd frontend")
    print("  npm run verify:contract-consolidation")
    print("  npm run build:integration")


if __name__ == "__main__":
    main()
