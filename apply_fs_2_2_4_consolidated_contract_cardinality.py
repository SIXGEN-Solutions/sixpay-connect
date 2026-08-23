from pathlib import Path
import sys

ROOT = Path.cwd()

ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
GATE = ROOT / "frontend/scripts/verify-contract-consolidation.mjs"
README = ROOT / "documentation/contracts/README.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

CARDINALITY_MODEL = '''  physicalContractCardinality:
    ONE_CAPABILITY_TO_ONE_PHYSICAL_CONTRACT:
      description: >-
        Default mapping. One registry capability references one physical
        contract and the physical contract identifies that capability.
    MANY_CAPABILITIES_TO_ONE_PHYSICAL_CONTRACT:
      description: >-
        Explicit consolidation mapping. Multiple registry capabilities may
        reference the same physical contract when they share a coherent API
        ownership boundary. Registry IDs and capability IDs remain unique.
      requirements:
        - "Registry contract ids remain globally unique"
        - "Registry capability values remain globally unique"
        - "Shared physical path is allowed"
        - "All registry entries sharing a path use the same domain"
        - "All registry entries sharing a path use the same businessOwner"
        - "The physical contract declares info.x-sixpay-contract.registryIds"
        - "The physical contract declares info.x-sixpay-contract.capabilities"
        - "Physical metadata exactly matches registry entries sharing the path"
'''

GATE_INSERTION_POINT = '''    const admin = registry.contracts.find(
      (contract) => contract.id === 'administration-query-api-v1',
    );
'''

GATE_BLOCK = r'''
    /*
     * FS-2.2.4 — Consolidated-contract cardinality
     *
     * Registry ids and capabilities identify logical capabilities and are
     * globally unique. A physical path is NOT a logical identifier and may be
     * shared by multiple capabilities when the consolidation is intentional.
     */
    const capabilityOwners = new Map();
    const contractsByPhysicalPath = new Map();

    for (const contract of registry.contracts) {
      if (typeof contract.capability === 'string') {
        const previous = capabilityOwners.get(contract.capability);

        if (previous) {
          fail(
            `${registryRelative}: duplicate capability `
              + `${contract.capability} used by ${previous} and ${contract.id}`,
          );
        } else {
          capabilityOwners.set(contract.capability, contract.id);
        }
      }

      if (typeof contract.path === 'string') {
        const group = contractsByPhysicalPath.get(contract.path) ?? [];
        group.push(contract);
        contractsByPhysicalPath.set(contract.path, group);
      }
    }

    function sameValue(entries, field) {
      const values = new Set(
        entries.map((entry) => entry?.[field] ?? null),
      );
      return values.size === 1;
    }

    function sameStringSet(left, right) {
      if (!Array.isArray(left) || !Array.isArray(right)) {
        return false;
      }

      const leftSet = new Set(left);
      const rightSet = new Set(right);

      if (
        leftSet.size !== left.length
        || rightSet.size !== right.length
        || leftSet.size !== rightSet.size
      ) {
        return false;
      }

      for (const value of leftSet) {
        if (!rightSet.has(value)) {
          return false;
        }
      }

      return true;
    }

    for (const [physicalPath, entries] of contractsByPhysicalPath) {
      if (entries.length <= 1) {
        continue;
      }

      if (!sameValue(entries, 'domain')) {
        fail(
          `${registryRelative}: consolidated physical contract `
            + `${physicalPath} mixes domains`,
        );
      }

      if (!sameValue(entries, 'businessOwner')) {
        fail(
          `${registryRelative}: consolidated physical contract `
            + `${physicalPath} mixes businessOwner values`,
        );
      }

      const physical = parseYaml(physicalPath);

      if (!physical) {
        continue;
      }

      if (!physical.openapi) {
        fail(
          `${physicalPath}: shared physical contracts must expose `
            + 'machine-readable OpenAPI metadata',
        );
        continue;
      }

      const metadata = physical.info?.['x-sixpay-contract'];

      if (!metadata || typeof metadata !== 'object') {
        fail(
          `${physicalPath}: consolidated contract requires `
            + 'info.x-sixpay-contract metadata',
        );
        continue;
      }

      const expectedRegistryIds = entries.map((entry) => entry.id);
      const expectedCapabilities = entries.map(
        (entry) => entry.capability,
      );

      if (
        !sameStringSet(
          metadata.registryIds,
          expectedRegistryIds,
        )
      ) {
        fail(
          `${physicalPath}: x-sixpay-contract.registryIds must exactly `
            + 'match registry entries sharing this path',
        );
      }

      if (
        !sameStringSet(
          metadata.capabilities,
          expectedCapabilities,
        )
      ) {
        fail(
          `${physicalPath}: x-sixpay-contract.capabilities must exactly `
            + 'match registry capabilities sharing this path',
        );
      }
    }
'''

README_SECTION = '''## Consolidated-contract cardinality

Registry identity and physical-file identity are intentionally different
concepts.

The default model is:

```text
1 registry capability
        ↓
1 physical contract
```

A bounded API ownership may intentionally consolidate several logical
capabilities into one physical contract:

```text
N registry capabilities
        ↓
1 physical contract
```

For such a consolidation:

- every registry `id` remains globally unique;
- every registry `capability` remains globally unique;
- `path` is allowed to be non-unique;
- entries sharing a path must remain within one coherent `domain`;
- entries sharing a path must have the same `businessOwner`;
- the physical OpenAPI contract must declare
  `info.x-sixpay-contract.registryIds`;
- it must also declare `info.x-sixpay-contract.capabilities`;
- those two arrays must exactly match the registry entries sharing the path.

The current canonical example is:

```text
administration-query-api-v1
  capability: ADMINISTRATION_OPERATIONAL_QUERY ─┐
                                                │
                                                ├─>
                                                │  administration-operational-api-v1.yaml
                                                │
incident-query-api-v1                           │
  capability: OPERATIONAL_INCIDENT_QUERY ───────┘
```

The two capabilities remain independently classifiable in the registry while
sharing one coherent Administration-owned OpenAPI boundary.

Physical consolidation must never be used to hide ownership differences or to
collapse unrelated capabilities merely to reduce file count.
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

    merged_path = (
        "documentation/contracts/internal/"
        "administration-operational-api-v1.yaml"
    )

    if registry.count(f'path: "{merged_path}"') != 2:
        fail(
            "Expected exactly two registry entries pointing to "
            f"{merged_path}."
        )

    for capability in [
        'capability: "ADMINISTRATION_OPERATIONAL_QUERY"',
        'capability: "OPERATIONAL_INCIDENT_QUERY"',
    ]:
        if capability not in registry:
            fail(f"Missing expected consolidated capability: {capability}")

    if "\n  physicalContractCardinality:\n" not in registry:
        marker = "\ncontracts:\n"
        if marker not in registry:
            fail("Could not find registry contracts marker.")

        header, tail = registry.split(marker, 1)

        registry = (
            header.rstrip()
            + "\n\n"
            + CARDINALITY_MODEL.rstrip()
            + marker
            + tail
        )

    if "FS-2.2.4 — Consolidated-contract cardinality" not in gate:
        if GATE_INSERTION_POINT not in gate:
            fail(
                "Could not find cardinality validation insertion point in "
                "verify-contract-consolidation.mjs."
            )

        gate = gate.replace(
            GATE_INSERTION_POINT,
            GATE_BLOCK + "\n" + GATE_INSERTION_POINT,
            1,
        )

    if "## Consolidated-contract cardinality" not in readme:
        insertion_point = "## Registry ↔ filesystem integrity"

        if insertion_point not in readme:
            fail("Could not find README insertion point.")

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

    final_gate = GATE.read_text(encoding="utf-8")
    final_registry = REGISTRY.read_text(encoding="utf-8")

    for token in [
        "physicalContractCardinality:",
        "MANY_CAPABILITIES_TO_ONE_PHYSICAL_CONTRACT:",
    ]:
        if token not in final_registry:
            fail(f"Registry cardinality model incomplete: {token}")

    for token in [
        "contractsByPhysicalPath",
        "duplicate capability",
        "x-sixpay-contract.registryIds must exactly",
        "x-sixpay-contract.capabilities must exactly",
    ]:
        if token not in final_gate:
            fail(f"Cardinality gate incomplete: {token}")

    print("FS-2.2.4 consolidated-contract cardinality applied.")
    print()
    print("Rules now enforced:")
    print(" - registry ids are globally unique")
    print(" - capabilities are globally unique")
    print(" - physical paths may be shared")
    print(" - shared-path entries must share domain and businessOwner")
    print(" - shared OpenAPI metadata must exactly mirror ids/capabilities")
    print()
    print("Current legitimate mapping:")
    print(" - ADMINISTRATION_OPERATIONAL_QUERY")
    print(" - OPERATIONAL_INCIDENT_QUERY")
    print("   -> administration-operational-api-v1.yaml")
    print()
    print("Validate:")
    print("  cd frontend")
    print("  npm run verify:contract-consolidation")
    print("  npm run build:integration")


if __name__ == "__main__":
    main()
