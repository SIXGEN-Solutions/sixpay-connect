import fs from 'node:fs';
import path from 'node:path';

const repoRoot = path.resolve(process.cwd(), '..');
const frontendRoot = process.cwd();

const failures = [];

function fail(message) {
  failures.push(message);
}

function read(file) {
  if (!fs.existsSync(file)) {
    fail(`Missing required file: ${path.relative(repoRoot, file)}`);
    return '';
  }
  return fs.readFileSync(file, 'utf8');
}

function walk(root, predicate = () => true) {
  if (!fs.existsSync(root)) {
    return [];
  }

  const files = [];
  const stack = [root];

  while (stack.length > 0) {
    const current = stack.pop();

    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const resolved = path.join(current, entry.name);

      if (entry.isDirectory()) {
        stack.push(resolved);
      } else if (entry.isFile() && predicate(resolved)) {
        files.push(resolved);
      }
    }
  }

  return files;
}

const backendJavaFiles = walk(path.join(repoRoot, 'backend'), (file) => file.endsWith('.java'));

function backendContains(token) {
  return backendJavaFiles.some((file) => fs.readFileSync(file, 'utf8').includes(token));
}

function assertPublishedContract(capability, contractPath, endpointToken) {
  const absolute = path.join(repoRoot, contractPath);
  const source = read(absolute);

  if (!source) {
    return;
  }

  if (!source.includes(endpointToken)) {
    fail(`${capability}: published contract ${contractPath} does not contain ${endpointToken}`);
  }
}

/*
 * FS-1.2 source-of-truth policy:
 *
 * - We require a published OpenAPI contract only when the repository
 *   actually declares one for that capability.
 * - Partner still has no published internal contract in this registry and
 *   therefore remains contract:null in this static gate.
 * - Customer Management and Security User Administration now have normalized
 *   contracts derived from their existing implemented boundaries.
 * - Every published internal contract declared here is checked against its
 *   exact physical file.
 */
const capabilities = [
  {
    name: 'Partner',
    frontend: 'src/app/features/partners/api/partners-api.client.ts',
    endpointTokens: ['/api/v1/partners'],
    backendOwnership: ['PartnerController'],
    contract: null,
  },
  {
    name: 'Payment',
    frontend: 'src/app/features/payments/api/payments-api.client.ts',
    endpointTokens: ['/internal/api/v1/payments'],
    backendOwnership: ['PaymentQueryController'],
    contract: {
      path: 'documentation/contracts/internal/payment-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/payments',
    },
  },
  {
    name: 'Reporting',
    frontend: 'src/app/features/reporting/api/reporting-api.client.ts',
    endpointTokens: [
      '/internal/api/v1/payment-audit-records',
      '/internal/api/v1/payment-audit-exports',
      '/internal/api/v1/payments/',
    ],
    backendOwnership: ['PaymentAudit'],
    contract: {
      path: 'documentation/contracts/internal/payment-audit-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/payment-audit-records',
    },
  },
  {
    name: 'Customer Management',
    frontend: 'src/app/features/customers/api/customer-management-api.client.ts',
    endpointTokens: ['/internal/api/v1/customers'],
    backendOwnership: ['CustomerController'],
    contract: {
      path: 'documentation/contracts/internal/customer-management-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/customers',
    },
  },
  {
    name: 'Observed Customer',
    frontend: 'src/app/features/customers/api/customers-api.client.ts',
    endpointTokens: ['/internal/api/v1/observed-customers'],
    backendOwnership: ['ObservedCustomer'],
    contract: {
      path: 'documentation/contracts/internal/observed-customer-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/observed-customers',
    },
  },
  {
    name: 'Accounting',
    frontend: 'src/app/features/accounting/api/accounting-api.client.ts',
    endpointTokens: ['/internal/api/v1/accounting-batches'],
    backendOwnership: ['AccountingBatchQueryController', 'AccountingBatchQueryUseCase'],
    contract: {
      path: 'documentation/contracts/internal/accounting-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/accounting-batches',
    },
  },
  {
    name: 'Operational Administration',
    frontend: 'src/app/features/administration/api/administration-api.client.ts',

    endpointTokens: ['/internal/api/v1/administration'],

    frontendTokens: ['/internal/api/v1/administration', '/overview', '/settings', '/integrations'],

    backendOwnership: [
      'AdministrationQueryController',
      'AdministrationQueryUseCase',
      'AdministrationQueryService',
    ],

    contract: {
      path: 'documentation/contracts/internal/administration-operational-api-v1.yaml',
      endpointToken: '/internal/api/v1/administration/overview',
    },
  },
  {
    name: 'Incidents',
    frontend: 'src/app/features/incidents/api/incidents-api.client.ts',
    endpointTokens: ['/internal/api/v1/incidents'],
    backendOwnership: [
      'IncidentQueryController',
      'IncidentQueryUseCase',
      'IncidentQueryService',
      'OperationalIncidentRepositoryAdapter',
      'OperationalIncidentSpringDataRepository',
    ],
    contract: {
      path: 'documentation/contracts/internal/administration-operational-api-v1.yaml',
      endpointToken: '/internal/api/v1/incidents',
    },
  },
  {
    name: 'Security User Administration',
    frontend: 'src/app/features/administration/services/security-user-administration.service.ts',
    endpointTokens: ['/internal/api/v1/administration/users'],
    backendOwnership: ['SecurityUserAdministration'],
    contract: {
      path: 'documentation/contracts/internal/security-user-administration-api-v1.yaml',
      endpointToken: '/internal/api/v1/administration/users',
    },
  },
];

const expectedFullStackCapabilities = [
  'Partner',
  'Payment',
  'Reporting',
  'Customer Management',
  'Observed Customer',
  'Security User Administration',
  'Accounting',
  'Operational Administration',
  'Incidents',
];

const declaredFullStackCapabilities = new Set(capabilities.map((capability) => capability.name));

for (const expectedCapability of expectedFullStackCapabilities) {
  if (!declaredFullStackCapabilities.has(expectedCapability)) {
    fail(`FS-1.2 required full-stack capability is missing: ${expectedCapability}`);
  }
}

for (const capability of capabilities) {
  const frontendFile = path.join(frontendRoot, capability.frontend);
  const source = read(frontendFile);

  for (const frontendToken of capability.frontendTokens ?? []) {
    if (!source.includes(frontendToken)) {
      fail(
        `${capability.name}: Angular client ` +
          `does not declare required token ` +
          `${frontendToken}`,
      );
    }
  }

  if (!source.includes('HttpClient')) {
    fail(`${capability.name}: Angular API boundary does not use HttpClient`);
  }

  for (const endpoint of capability.endpointTokens) {
    if (!source.includes(endpoint)) {
      fail(`${capability.name}: Angular client no longer declares endpoint ${endpoint}`);
    }

    const backendToken = endpoint.endsWith('/') ? endpoint.slice(0, -1) : endpoint;

    if (!backendContains(backendToken)) {
      fail(`${capability.name}: no Spring source contains endpoint ${backendToken}`);
    }
  }

  for (const ownershipToken of capability.backendOwnership) {
    if (!backendContains(ownershipToken)) {
      fail(`${capability.name}: expected backend ownership token "${ownershipToken}" not found`);
    }
  }

  if (capability.contract) {
    assertPublishedContract(
      capability.name,
      capability.contract.path,
      capability.contract.endpointToken,
    );
  }

  /*
   * No silent HTTP -> mock fallback.
   * FS-1.1 remains the broader UI-level mock dependency gate.
   */
  if (/catchError\s*\([\s\S]{0,500}?(?:MockService|this\.mock|mock\.)/.test(source)) {
    fail(`${capability.name}: API boundary contains a silent mock fallback`);
  }
}

/*
 * FS-1.4 direct mock regression checks.
 *
 * Mock datasource services may remain under /services for dev/demo/tests,
 * but Angular components, routes, guards and resolvers must never inject
 * AdministrationMockService or IncidentsMockService directly.
 */
for (const check of [
  {
    root: 'src/app/features/administration',
    serviceDirectory: 'src/app/features/administration/services',
    forbiddenType: 'AdministrationMockService',
  },
  {
    root: 'src/app/features/incidents',
    serviceDirectory: 'src/app/features/incidents/services',
    forbiddenType: 'IncidentsMockService',
  },
]) {
  const absoluteRoot = path.join(frontendRoot, check.root);

  for (const file of walk(
    absoluteRoot,
    (candidate) => candidate.endsWith('.ts') || candidate.endsWith('.html'),
  )) {
    const relativeFile = path.relative(frontendRoot, file).replaceAll('\\', '/');

    if (relativeFile.startsWith(`${check.serviceDirectory}/`)) {
      continue;
    }

    const uiSource = fs.readFileSync(file, 'utf8');

    if (
      uiSource.includes(check.forbiddenType) ||
      new RegExp(`from\\s+['"][^'"]*-mock\\.service['"]`).test(uiSource)
    ) {
      fail(`${relativeFile}: integration UI must not depend directly on ${check.forbiddenType}`);
    }
  }
}

const packageJsonPath = path.join(frontendRoot, 'package.json');
const packageJson = JSON.parse(read(packageJsonPath));

if (!packageJson.scripts?.['verify:integration-contract-backed']) {
  fail('FS-1.1 integration contract-backed gate is not registered');
}

if (failures.length > 0) {
  console.error('\nFS-1.2 full-stack static conformance gate FAILED:\n');

  for (const failure of failures) {
    console.error(` - ${failure}`);
  }

  console.error('');
  process.exit(1);
}

console.log('FS-1.2 full-stack static conformance gate PASSED.');
console.log(
  'Angular API paths are backed by Spring ownership; published contracts are checked only where the repository declares them.',
);
