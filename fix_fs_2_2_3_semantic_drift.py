from pathlib import Path
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
GATE = ROOT / "frontend/scripts/verify-contract-consolidation.mjs"
README = ROOT / "documentation/contracts/README.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def replace_once(source: str, old: str, new: str, label: str) -> str:
    if old not in source:
        if new in source:
            return source
        fail(f"Could not find expected block for {label}")
    return source.replace(old, new, 1)


def main() -> None:
    engineering = require(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail(f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}.")

    registry = require(REGISTRY)
    gate = require(GATE)
    readme = require(README)

    registry = replace_once(
        registry,
        '''    generationPolicy: "REFERENCE_ONLY"
    reviewedForGate: "IA-0.5P_PAYMENT_CONTRACT_PACK"''',
        '''    generationPolicy: "REFERENCE_ONLY"
    codeGenerationAllowed: false
    reviewedForGate: "IA-0.5P_PAYMENT_CONTRACT_PACK"''',
        "amplitude-customer-verification-api-v1 codeGenerationAllowed",
    )

    webhook_old = '''    direction: "SIXPAY_TO_TRESOR_PAY"
    sourceSystem: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"'''

    webhook_new = '''    direction: "SIXPAY_TO_TRESOR_PAY"
    sourceSystem: "SIXPAY"
    systemOfRecord: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"'''

    registry = replace_once(
        registry,
        webhook_old,
        webhook_new,
        "tresorpay-payment-status-webhook-v1 systemOfRecord",
    )

    registry = replace_once(
        registry,
        webhook_old,
        webhook_new,
        "tresorpay-treasury-integration-webhook-v1 systemOfRecord",
    )

    deferred_ids = [
        "tresorpay-authorization-request-api-v1",
        "tresorpay-authorization-decision-webhook-v1",
    ]

    for contract_id in deferred_ids:
        marker = f'  - id: "{contract_id}"'
        start = registry.find(marker)
        if start < 0:
            fail(f"Missing registry entry: {contract_id}")

        next_start = registry.find("\n  - id: ", start + len(marker))
        end = len(registry) if next_start < 0 else next_start
        block = registry[start:end]

        if 'codeGenerationAllowed:' not in block:
            needle = '    generationPolicy: "EXCLUDED"\n'
            if needle not in block:
                fail(
                    f"{contract_id}: expected generationPolicy EXCLUDED "
                    "before adding codeGenerationAllowed=false"
                )

            block = block.replace(
                needle,
                needle + "    codeGenerationAllowed: false\n",
                1,
            )
            registry = registry[:start] + block + registry[end:]

    old_function = r'''    function isCanonicalPhysicalContract(file) {
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
'''

    new_function = r'''    const canonicalContractRoots = [
      'documentation/contracts/amplitude/',
      'documentation/contracts/tresorpay/',
      'documentation/contracts/internal/',
    ];

    function isCanonicalPhysicalContract(file) {
      const relative = path
        .relative(repoRoot, file)
        .replaceAll('\\', '/');

      if (relative === registryRelative) {
        return false;
      }

      if (
        !canonicalContractRoots.some(
          (root) => relative.startsWith(root),
        )
      ) {
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
'''

    gate = replace_once(
        gate,
        old_function,
        new_function,
        "canonical physical contract roots",
    )

    old_readme = '''The integrity gate treats the following files as canonical contracts in the
current repository layout:

- YAML/YML/JSON specifications below `documentation/contracts/`, excluding the
  registry itself;
- Markdown contracts directly below `documentation/contracts/internal/`.

Governance documents such as this `README.md` are not physical contracts and
must not be registered as capabilities.
'''

    new_readme = '''The integrity gate treats the following roots as the current canonical
physical-contract baseline:

- `documentation/contracts/amplitude/`;
- `documentation/contracts/tresorpay/`;
- `documentation/contracts/internal/`.

Within those roots, YAML/YML/JSON specifications are physical contracts.
Markdown contracts are canonical only when directly under
`documentation/contracts/internal/`.

Other trees such as `external/`, `integration/` and `events/` remain repository
inventory until an explicit consolidation decision promotes, supersedes or
removes them. Their physical presence alone does not make them canonical.

Governance documents such as this `README.md` are not physical contracts and
must not be registered as capabilities.
'''

    readme = replace_once(
        readme,
        old_readme,
        new_readme,
        "README canonical roots",
    )

    REGISTRY.write_text(registry, encoding="utf-8")
    GATE.write_text(gate, encoding="utf-8")
    README.write_text(readme, encoding="utf-8")

    print("FS-2.2.2/FS-2.2.3 corrective normalization applied.")
    print()
    print("Registry fixes:")
    print(" - amplitude-customer-verification-api-v1: codeGenerationAllowed=false")
    print(" - tresorpay-payment-status-webhook-v1: systemOfRecord=SIXPAY")
    print(" - tresorpay-treasury-integration-webhook-v1: systemOfRecord=SIXPAY")
    print(" - deferred authorization request: codeGenerationAllowed=false")
    print(" - deferred authorization decision: codeGenerationAllowed=false")
    print()
    print("Canonical roots:")
    print(" - documentation/contracts/amplitude/")
    print(" - documentation/contracts/tresorpay/")
    print(" - documentation/contracts/internal/")
    print()
    print("Run:")
    print("  cd frontend")
    print("  npm run verify:contract-consolidation")


if __name__ == "__main__":
    main()
