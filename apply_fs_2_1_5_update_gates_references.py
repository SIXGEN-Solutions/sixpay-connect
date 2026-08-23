from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

OLD_NAMES = [
    "administration-operational-api-v1.yaml",
    "administration-operational-api-v1.yaml",
]
NEW_NAME = "administration-operational-api-v1.yaml"

VALIDATOR = ROOT / "frontend/scripts/verify-contract-consolidation.mjs"
PACKAGE = ROOT / "frontend/package.json"

SKIP_DIRS = {
    ".git", ".idea", ".vscode", "node_modules",
    "target", "dist", "coverage", ".angular"
}

TEXT_SUFFIXES = {
    ".java", ".kt", ".xml", ".yaml", ".yml", ".json", ".md",
    ".mjs", ".js", ".ts", ".html", ".css", ".scss",
    ".properties", ".txt", ".sh", ".ps1", ".py"
}

VALIDATOR_SOURCE = r"""import fs from 'node:fs';
import path from 'node:path';
import { parse } from 'yaml';

const frontendRoot = process.cwd();
const repoRoot = path.resolve(frontendRoot, '..');
const failures = [];

const oldContractNames = [
  'administration-operational-api-v1.yaml',
  'administration-operational-api-v1.yaml',
];

const mergedContractRelative =
  'documentation/contracts/internal/administration-operational-api-v1.yaml';

const registryRelative =
  'documentation/contracts/CONTRACT_REGISTRY.yaml';

const expectedMergedEndpoints = [
  '/internal/api/v1/administration/overview',
  '/internal/api/v1/administration/settings',
  '/internal/api/v1/administration/integrations',
  '/internal/api/v1/incidents',
  '/internal/api/v1/incidents/{incidentId}',
];

function fail(message) {
  failures.push(message);
}

function read(relativePath) {
  const absolute = path.join(repoRoot, relativePath);

  if (!fs.existsSync(absolute)) {
    fail(`Missing required file: ${relativePath}`);
    return '';
  }

  return fs.readFileSync(absolute, 'utf8');
}

function parseYaml(relativePath) {
  const source = read(relativePath);

  if (!source) {
    return null;
  }

  try {
    return parse(source);
  } catch (error) {
    fail(`${relativePath}: invalid YAML: ${error.message}`);
    return null;
  }
}

function walk(root) {
  const files = [];
  const stack = [root];

  const skippedDirectories = new Set([
    '.git',
    '.idea',
    '.vscode',
    'node_modules',
    'target',
    'dist',
    'coverage',
    '.angular',
  ]);

  while (stack.length > 0) {
    const current = stack.pop();

    for (const entry of fs.readdirSync(current, {
      withFileTypes: true,
    })) {
      if (entry.isDirectory() && skippedDirectories.has(entry.name)) {
        continue;
      }

      const resolved = path.join(current, entry.name);

      if (entry.isDirectory()) {
        stack.push(resolved);
      } else if (entry.isFile()) {
        files.push(resolved);
      }
    }
  }

  return files;
}

const contractsRoot = path.join(
  repoRoot,
  'documentation',
  'contracts',
);

for (const file of walk(contractsRoot)) {
  if (!file.endsWith('.yaml') && !file.endsWith('.yml')) {
    continue;
  }

  const relative = path
    .relative(repoRoot, file)
    .replaceAll('\\', '/');

  const document = parseYaml(relative);

  if (!document) {
    continue;
  }

  if (document.openapi !== undefined) {
    if (
      typeof document.openapi !== 'string'
      || !document.openapi.startsWith('3.')
    ) {
      fail(`${relative}: unsupported OpenAPI version ${document.openapi}`);
    }

    if (!document.info || typeof document.info !== 'object') {
      fail(`${relative}: missing OpenAPI info`);
    }

    if (!document.paths || typeof document.paths !== 'object') {
      fail(`${relative}: missing OpenAPI paths`);
    }
  }
}

const registry = parseYaml(registryRelative);

if (registry) {
  if (registry.schemaVersion !== '1.0') {
    fail(`${registryRelative}: expected schemaVersion "1.0"`);
  }

  if (
    registry.registry?.branch
    !== 'feat/repository-baseline-consolidation'
  ) {
    fail(`${registryRelative}: authoritative branch metadata is stale`);
  }

  if (!Array.isArray(registry.contracts)) {
    fail(`${registryRelative}: contracts must be an array`);
  } else {
    const ids = new Set();

    for (const contract of registry.contracts) {
      if (!contract?.id) {
        fail(`${registryRelative}: contract entry without id`);
        continue;
      }

      if (ids.has(contract.id)) {
        fail(`${registryRelative}: duplicate contract id ${contract.id}`);
      }

      ids.add(contract.id);

      if (contract.path) {
        const absolute = path.join(repoRoot, contract.path);

        if (!fs.existsSync(absolute)) {
          fail(
            `${registryRelative}: ${contract.id} points to missing file `
              + `${contract.path}`,
          );
        }
      }
    }

    const admin = registry.contracts.find(
      (contract) => contract.id === 'administration-query-api-v1',
    );

    const incident = registry.contracts.find(
      (contract) => contract.id === 'incident-query-api-v1',
    );

    if (!admin) {
      fail('Registry capability administration-query-api-v1 is missing');
    }

    if (!incident) {
      fail('Registry capability incident-query-api-v1 is missing');
    }

    if (
      admin?.path !== mergedContractRelative
      || incident?.path !== mergedContractRelative
    ) {
      fail(
        'Administration and Incident registry capabilities must point '
          + `to ${mergedContractRelative}`,
      );
    }

    if (
      admin?.capability !== 'ADMINISTRATION_OPERATIONAL_QUERY'
      || incident?.capability !== 'OPERATIONAL_INCIDENT_QUERY'
    ) {
      fail('Administration/Incident capability identities were altered');
    }
  }
}

const merged = parseYaml(mergedContractRelative);

if (merged) {
  const actualPaths = Object.keys(merged.paths ?? {});

  for (const endpoint of expectedMergedEndpoints) {
    if (!actualPaths.includes(endpoint)) {
      fail(`${mergedContractRelative}: missing endpoint ${endpoint}`);
    }
  }

  if (actualPaths.length !== 5) {
    fail(
      `${mergedContractRelative}: expected exactly 5 paths, `
        + `found ${actualPaths.length}`,
    );
  }

  const metadata = merged.info?.['x-sixpay-contract'];
  const registryIds = metadata?.registryIds ?? [];
  const capabilities = metadata?.capabilities ?? [];

  for (const id of [
    'administration-query-api-v1',
    'incident-query-api-v1',
  ]) {
    if (!registryIds.includes(id)) {
      fail(`${mergedContractRelative}: missing registry id ${id}`);
    }
  }

  for (const capability of [
    'ADMINISTRATION_OPERATIONAL_QUERY',
    'OPERATIONAL_INCIDENT_QUERY',
  ]) {
    if (!capabilities.includes(capability)) {
      fail(`${mergedContractRelative}: missing capability ${capability}`);
    }
  }
}

const textualExtensions = new Set([
  '.java', '.kt', '.xml', '.yaml', '.yml', '.json', '.md',
  '.mjs', '.js', '.ts', '.html', '.css', '.scss',
  '.properties', '.txt', '.sh', '.ps1', '.py',
]);

for (const file of walk(repoRoot)) {
  if (!textualExtensions.has(path.extname(file))) {
    continue;
  }

  const source = fs.readFileSync(file, 'utf8');

  for (const oldName of oldContractNames) {
    if (source.includes(oldName)) {
      const relative = path
        .relative(repoRoot, file)
        .replaceAll('\\', '/');

      fail(`${relative}: stale reference to removed contract ${oldName}`);
    }
  }
}

if (failures.length > 0) {
  console.error('\nFS-2.1 contract consolidation validation FAILED:\n');

  for (const failure of failures) {
    console.error(` - ${failure}`);
  }

  console.error('');
  process.exit(1);
}

console.log('FS-2.1 contract consolidation validation PASSED.');
console.log(
  'OpenAPI YAML is parseable; registry references are valid; '
    + 'Administration/Incident preserve 2 capabilities in 1 physical '
    + 'contract with exactly 5 endpoints; no stale contract filenames remain.',
);
"""


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def iter_text_files():
    for path in ROOT.rglob("*"):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        if path.suffix.lower() in TEXT_SUFFIXES:
            yield path


def main() -> None:
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(f"ENGINEERING_CONTEXT.md does not declare {EXPECTED_BRANCH}.")

    merged = (
        ROOT
        / "documentation/contracts/internal"
        / NEW_NAME
    )

    if not merged.is_file():
        fail(f"Merged contract is missing: {merged}")

    changed = []

    for path in iter_text_files():
        source = path.read_text(encoding="utf-8")
        updated = source

        for old_name in OLD_NAMES:
            updated = updated.replace(old_name, NEW_NAME)

        if updated != source:
            path.write_text(updated, encoding="utf-8")
            changed.append(path)

    stale = []

    for path in iter_text_files():
        source = path.read_text(encoding="utf-8")

        for old_name in OLD_NAMES:
            if old_name in source:
                stale.append(
                    f"{path.relative_to(ROOT)} -> {old_name}"
                )

    if stale:
        fail("Stale references remain:\n  " + "\n  ".join(stale))

    VALIDATOR.write_text(VALIDATOR_SOURCE, encoding="utf-8")

    package_source = require(PACKAGE)

    script_token = (
        '    "verify:runtime-datasource-policy": '
        '"node scripts/verify-runtime-datasource-policy.mjs"'
    )

    new_script_token = (
        '    "verify:contract-consolidation": '
        '"node scripts/verify-contract-consolidation.mjs",\n'
        + script_token
    )

    if '"verify:contract-consolidation"' not in package_source:
        if script_token not in package_source:
            fail("Could not insert verify:contract-consolidation.")
        package_source = package_source.replace(
            script_token,
            new_script_token,
            1,
        )

    old_build = (
        '"build:integration": '
        '"npm run verify:runtime-datasource-policy '
        '&& npm run verify:full-stack-conformance '
        '&& npm run verify:integration-contract-backed '
        '&& ng build --configuration integration"'
    )

    new_build = (
        '"build:integration": '
        '"npm run verify:contract-consolidation '
        '&& npm run verify:runtime-datasource-policy '
        '&& npm run verify:full-stack-conformance '
        '&& npm run verify:integration-contract-backed '
        '&& ng build --configuration integration"'
    )

    if old_build in package_source:
        package_source = package_source.replace(
            old_build,
            new_build,
            1,
        )
    elif new_build not in package_source:
        fail("Could not wire verify:contract-consolidation into build:integration.")

    PACKAGE.write_text(package_source, encoding="utf-8")

    print("FS-2.1.5 references and gates updated.")
    print()

    if changed:
        print("Stale contract references replaced in:")
        for path in changed:
            print(" - " + str(path.relative_to(ROOT)).replace("\\", "/"))
    else:
        print("No stale contract references required replacement.")

    print()
    print("Created:")
    print(" - frontend/scripts/verify-contract-consolidation.mjs")
    print()
    print("Updated:")
    print(" - frontend/package.json")
    print(" - every source file still referencing old contract filenames")
    print()
    print("Validate:")
    print("  cd frontend")
    print("  npm run verify:contract-consolidation")
    print("  npm run verify:full-stack-conformance")
    print("  npm run verify:integration-contract-backed")
    print("  npm run build:all")
    print()
    print("  cd ../backend")
    print(
        "  mvn -pl administration -am "
        "-Dtest=AdministrationQueryApiContractTest,IncidentQueryApiContractTest "
        "-Dsurefire.failIfNoSpecifiedTests=false test"
    )
    print("  mvn verify")


if __name__ == "__main__":
    main()
