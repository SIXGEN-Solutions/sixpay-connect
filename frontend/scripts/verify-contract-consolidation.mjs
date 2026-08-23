import fs from 'node:fs';
import path from 'node:path';
import { parse } from 'yaml';

const frontendRoot = process.cwd();
const repoRoot = path.resolve(frontendRoot, '..');
const failures = [];

const oldContractNames = [
  'administration-query-api-v1',
  'incident-query-api-v1',
].map((name) => `${name}.yaml`);

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

/*
 * FS-2.2.1 — Historical artifacts cleanup
 *
 * documentation/contracts describes the current contractual baseline.
 * Change-history artifacts belong to Git history, not to this tree.
 */
function isForbiddenHistoricalContractArtifact(file) {
  const basename = path.basename(file);
  const lower = basename.toLowerCase();

  if (
    lower.endsWith('.patch')
    || lower.endsWith('.diff')
    || lower.endsWith('.rej')
    || lower.endsWith('.orig')
    || lower.endsWith('.bak')
    || lower.endsWith('.tmp')
  ) {
    return true;
  }

  if (
    lower.endsWith('.md')
    && /(^|[_-])patch([_-]|\.md$)/i.test(basename)
  ) {
    return true;
  }

  return false;
}

for (const file of walk(contractsRoot)) {
  if (!isForbiddenHistoricalContractArtifact(file)) {
    continue;
  }

  const relative = path
    .relative(repoRoot, file)
    .replaceAll('\\', '/');

  fail(
    `${relative}: historical/transitional contract artifact is forbidden `
      + 'from the canonical contractual baseline',
  );
}

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


    /*
     * FS-2.2.2 — Registry <-> filesystem integrity
     */
    const registeredPhysicalPaths = new Set(
      registry.contracts
        .map((contract) => contract?.path)
        .filter((contractPath) => typeof contractPath === 'string'),
    );

    const canonicalContractRoots = [
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


    /*
     * FS-2.2.3 - Registry semantic normalization
     */
    const semanticModel = registry.classificationModel ?? {};

    const allowedDomains = new Set(Object.keys(semanticModel.domains ?? {}));
    const allowedOwners = new Set(Object.keys(semanticModel.ownershipValues ?? {}));
    const allowedSystems = new Set(Object.keys(semanticModel.systems ?? {}));
    const allowedDirectionEndpoints = new Set(
      Object.keys(semanticModel.directionEndpoints ?? {}),
    );
    const allowedDataClassifications = new Set(
      Object.keys(semanticModel.dataClassifications ?? {}),
    );
    const allowedPaginationModes = new Set(
      Object.keys(semanticModel.paginationModes ?? {}),
    );
    const allowedLifecycleStatuses = new Set(
      Object.keys(semanticModel.lifecycleStatuses ?? {}),
    );
    const allowedApprovalStatuses = new Set(
      Object.keys(semanticModel.approvalStatuses ?? {}),
    );
    const allowedGenerationPolicies = new Set(
      Object.keys(semanticModel.generationPolicy ?? {}),
    );

    function checkControlledValue(contractId, field, value, allowedValues) {
      if (typeof value !== 'string' || !allowedValues.has(value)) {
        fail(
          `${registryRelative}: ${contractId}.${field} has uncontrolled value `
            + `${JSON.stringify(value)}`,
        );
      }
    }

    function checkOwner(contractId, field, value) {
      if (value === undefined) {
        return;
      }

      const values = Array.isArray(value) ? value : [value];

      if (values.length === 0) {
        fail(`${registryRelative}: ${contractId}.${field} must not be empty`);
        return;
      }

      for (const owner of values) {
        checkControlledValue(contractId, field, owner, allowedOwners);
      }
    }

    function checkDirection(contractId, field, value) {
      if (typeof value !== 'string') {
        fail(`${registryRelative}: ${contractId}.${field} must be a string`);
        return;
      }

      const separator = '_TO_';
      const separatorIndex = value.indexOf(separator);

      if (separatorIndex <= 0) {
        fail(
          `${registryRelative}: ${contractId}.${field} must use `
            + 'SOURCE_TO_TARGET semantics',
        );
        return;
      }

      const source = value.slice(0, separatorIndex);
      const target = value.slice(separatorIndex + separator.length);

      if (
        !allowedDirectionEndpoints.has(source)
        || !allowedDirectionEndpoints.has(target)
      ) {
        fail(
          `${registryRelative}: ${contractId}.${field} uses uncontrolled `
            + `direction endpoint(s): ${value}`,
        );
      }
    }

    for (const contract of registry.contracts) {
      const contractId = contract.id ?? '<missing-id>';

      checkControlledValue(
        contractId,
        'domain',
        contract.domain,
        allowedDomains,
      );

      if (
        typeof contract.capability !== 'string'
        || !/^[A-Z][A-Z0-9_]*$/.test(contract.capability)
      ) {
        fail(
          `${registryRelative}: ${contractId}.capability must use `
            + 'UPPER_SNAKE_CASE',
        );
      }

      for (const field of [
        'businessOwner',
        'deliveryOwner',
        'securityOwner',
        'transportOwner',
        'lifecycleConsumer',
        'evidenceOwners',
        'sourceFactOwners',
        'deliveryBoundary',
      ]) {
        checkOwner(contractId, field, contract[field]);
      }

      const hasDirection = typeof contract.direction === 'string';
      const hasPrimaryDirection =
        typeof contract.primaryDirection === 'string';
      const hasFallbackDirection =
        typeof contract.fallbackDirection === 'string';

      if (hasDirection) {
        checkDirection(contractId, 'direction', contract.direction);

        if (hasPrimaryDirection || hasFallbackDirection) {
          fail(
            `${registryRelative}: ${contractId} must not mix direction `
              + 'with primaryDirection/fallbackDirection',
          );
        }
      } else if (hasPrimaryDirection && hasFallbackDirection) {
        checkDirection(
          contractId,
          'primaryDirection',
          contract.primaryDirection,
        );
        checkDirection(
          contractId,
          'fallbackDirection',
          contract.fallbackDirection,
        );
      } else {
        fail(
          `${registryRelative}: ${contractId} must define either direction `
            + 'or both primaryDirection and fallbackDirection',
        );
      }

      if (contract.sourceSystem !== undefined) {
        checkControlledValue(
          contractId,
          'sourceSystem',
          contract.sourceSystem,
          allowedSystems,
        );
      }

      if (contract.systemOfRecord !== undefined) {
        checkControlledValue(
          contractId,
          'systemOfRecord',
          contract.systemOfRecord,
          allowedSystems,
        );
      } else {
        fail(`${registryRelative}: ${contractId}.systemOfRecord is required`);
      }

      checkControlledValue(
        contractId,
        'lifecycleStatus',
        contract.lifecycleStatus,
        allowedLifecycleStatuses,
      );
      checkControlledValue(
        contractId,
        'approvalStatus',
        contract.approvalStatus,
        allowedApprovalStatuses,
      );
      checkControlledValue(
        contractId,
        'generationPolicy',
        contract.generationPolicy,
        allowedGenerationPolicies,
      );

      if (typeof contract.codeGenerationAllowed !== 'boolean') {
        fail(
          `${registryRelative}: ${contractId}.codeGenerationAllowed must be boolean`,
        );
      }

      if (
        contract.generationPolicy === 'EXCLUDED'
        && contract.codeGenerationAllowed !== false
      ) {
        fail(
          `${registryRelative}: ${contractId} EXCLUDED contracts cannot `
            + 'allow code generation',
        );
      }

      const usage = contract.mvpUsage;

      if (!usage || typeof usage !== 'object') {
        fail(`${registryRelative}: ${contractId}.mvpUsage is required`);
      } else {
        if (typeof usage.included !== 'boolean') {
          fail(
            `${registryRelative}: ${contractId}.mvpUsage.included must be boolean`,
          );
        }

        if (
          contract.lifecycleStatus === 'DEFERRED_FUTURE'
          && usage.included !== false
        ) {
          fail(
            `${registryRelative}: ${contractId} DEFERRED_FUTURE must have `
              + 'mvpUsage.included=false',
          );
        }

        if (
          (
            contract.lifecycleStatus === 'ACTIVE_MVP'
            || contract.lifecycleStatus === 'REFERENCE_MVP'
          )
          && usage.included !== true
        ) {
          fail(
            `${registryRelative}: ${contractId} ${contract.lifecycleStatus} `
              + 'must have mvpUsage.included=true',
          );
        }

        if (usage.pagination !== undefined) {
          checkControlledValue(
            contractId,
            'mvpUsage.pagination',
            usage.pagination,
            allowedPaginationModes,
          );
        }
      }

      if (contract.security !== undefined) {
        if (
          !contract.security
          || typeof contract.security !== 'object'
          || Array.isArray(contract.security)
        ) {
          fail(`${registryRelative}: ${contractId}.security must be an object`);
        } else if (contract.security.dataClassification !== undefined) {
          checkControlledValue(
            contractId,
            'security.dataClassification',
            contract.security.dataClassification,
            allowedDataClassifications,
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
