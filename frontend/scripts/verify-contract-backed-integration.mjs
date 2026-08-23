import fs from 'node:fs';
import path from 'node:path';

const frontendRoot = process.cwd();
const failures = [];

function fail(message) {
  failures.push(message);
}

function read(relativePath) {
  const absolutePath = path.join(frontendRoot, relativePath);

  if (!fs.existsSync(absolutePath)) {
    fail(`Missing required file: ${relativePath}`);
    return '';
  }

  return fs.readFileSync(absolutePath, 'utf8');
}

function assertContains(source, token, context) {
  if (!source.includes(token)) {
    fail(`${context}: missing required token "${token}"`);
  }
}

function assertNotContains(source, token, context) {
  if (source.includes(token)) {
    fail(`${context}: forbidden token "${token}"`);
  }
}

function walkFiles(relativeRoot) {
  const absoluteRoot = path.join(frontendRoot, relativeRoot);

  if (!fs.existsSync(absoluteRoot)) {
    return [];
  }

  const result = [];
  const queue = [absoluteRoot];

  while (queue.length > 0) {
    const current = queue.pop();

    for (const entry of fs.readdirSync(current, {
      withFileTypes: true,
    })) {
      const resolved = path.join(current, entry.name);

      if (entry.isDirectory()) {
        queue.push(resolved);
        continue;
      }

      if (
        entry.isFile()
        && (
          entry.name.endsWith('.ts')
          || entry.name.endsWith('.html')
        )
      ) {
        result.push(resolved);
      }
    }
  }

  return result;
}

function relative(file) {
  return path
    .relative(frontendRoot, file)
    .replaceAll('\\', '/');
}

/*
 * 1. Integration configuration must be API-only.
 */
const environmentIntegration = read(
  'src/environments/environment.integration.ts',
);

assertContains(
  environmentIntegration,
  "mode: 'api'",
  'integration environment',
);

const angularJson = read('angular.json');

assertContains(
  angularJson,
  '"integration"',
  'Angular integration configuration',
);

assertContains(
  angularJson,
  '"src/environments/environment.integration.ts"',
  'Angular integration file replacement',
);

/*
 * 2. Contract-backed READY domains.
 *
 * Partner remains the golden frontend datasource-selection pattern.
 * Customer Management and Security User Administration are already
 * API-only and therefore require no mock datasource selector.
 */
const switchedDomains = [
  {
    name: 'Partner',
    service:
      'src/app/features/partners/services/partners.service.ts',
    apiClient:
      'src/app/features/partners/api/partners-api.client.ts',
    apiClientType: 'PartnerApiClient',
    mockType: 'PartnersMockService',
  },
  {
    name: 'Payment',
    service:
      'src/app/features/payments/services/payments.service.ts',
    apiClient:
      'src/app/features/payments/api/payments-api.client.ts',
    apiClientType: 'PaymentsApiClient',
    mockType: 'PaymentsMockService',
  },
  {
    name: 'Reporting',
    service:
      'src/app/features/reporting/services/reporting.service.ts',
    apiClient:
      'src/app/features/reporting/api/reporting-api.client.ts',
    apiClientType: 'ReportingApiClient',
    mockType: 'ReportingMockService',
  },
  {
    name: 'Accounting',
    service:
      'src/app/features/accounting/services/accounting.service.ts',
    apiClient:
      'src/app/features/accounting/api/accounting-api.client.ts',
    apiClientType: 'AccountingApiClient',
    mockType: 'AccountingMockService',
  },
  {
    name: 'Incidents',
    service:
      'src/app/features/incidents/services/incidents.service.ts',
    apiClient:
      'src/app/features/incidents/api/incidents-api.client.ts',
    apiClientType: 'IncidentsApiClient',
    mockType: 'IncidentsMockService',
  },
  {
    name: 'Operational Administration',
    service:
      'src/app/features/administration/services/administration.service.ts',
    apiClient:
      'src/app/features/administration/api/administration-api.client.ts',
    apiClientType: 'AdministrationApiClient',
    mockType: 'AdministrationMockService',
  },
  {
    name: 'Observed Customer',
    service:
      'src/app/features/customers/services/customers.service.ts',
    apiClient:
      'src/app/features/customers/api/customers-api.client.ts',
    apiClientType: 'CustomersApiClient',
    mockType: 'CustomersMockService',
  },
];

for (const domain of switchedDomains) {
  const service = read(domain.service);
  read(domain.apiClient);

  assertContains(
    service,
    'BackendModeService',
    `${domain.name} service`,
  );

  assertContains(
    service,
    'backendMode.usesApi',
    `${domain.name} service`,
  );

  assertContains(
    service,
    domain.apiClientType,
    `${domain.name} service`,
  );

  assertContains(
    service,
    domain.mockType,
    `${domain.name} service`,
  );

  /*
   * A mock may exist for development/demo/tests, but API mode must
   * have a positive API branch. This gate intentionally follows the
   * golden Partner mental model instead of inventing another abstraction.
   */
  if (
    !service.includes('? this.api')
    && !service.includes('if (this.backendMode.usesMock)')
  ) {
    fail(
      `${domain.name} service: no explicit API/mock datasource boundary found`,
    );
  }
}

const apiOnlyDomains = [
  {
    name: 'Customer Management',
    service:
      'src/app/features/customers/services/customer-management.service.ts',
    apiClient:
      'src/app/features/customers/api/customer-management-api.client.ts',
    apiClientType: 'CustomerManagementApiClient',
  },
  {
    name: 'Security User Administration',
    service:
      'src/app/features/administration/services/security-user-administration.service.ts',
    apiClient: null,
    apiClientType: 'HttpClient',
  },
];

for (const domain of apiOnlyDomains) {
  const service = read(domain.service);

  if (domain.apiClient) {
    read(domain.apiClient);
  }

  assertContains(
    service,
    domain.apiClientType,
    `${domain.name} service`,
  );

  assertNotContains(
    service,
    'MockService',
    `${domain.name} service`,
  );

  assertNotContains(
    service,
    'backendMode.usesMock',
    `${domain.name} service`,
  );
}

/*
 * 3. Screens must never depend directly on mock services.
 *
 * Domain services may keep their development mock datasource, but
 * components/routes/resolvers/guards must depend on the application
 * service boundary only.
 */
const uiRoots = [
  'src/app/features/partners',
  'src/app/features/payments',
  'src/app/features/reporting',
  'src/app/features/customers',
  'src/app/features/administration',
  'src/app/features/accounting',
];

const serviceDirectories = new Set([
  'src/app/features/partners/services',
  'src/app/features/payments/services',
  'src/app/features/reporting/services',
  'src/app/features/customers/services',
  'src/app/features/administration/services',
  'src/app/features/accounting/services',
]);

for (const root of uiRoots) {
  for (const file of walkFiles(root)) {
    const rel = relative(file);

    if (
      [...serviceDirectories].some(
        (serviceDirectory) =>
          rel.startsWith(`${serviceDirectory}/`),
      )
    ) {
      continue;
    }

    const source = fs.readFileSync(file, 'utf8');

   /*
    * Shared UI components such as MockStatePanelComponent and
    * MockContentStateComponent are presentation helpers and are allowed.
    *
    * What is forbidden is a direct dependency from the UI layer to a
    * datasource mock service such as:
    *
    *   ../services/payments-mock.service
    *   ../services/partners-mock.service
    */
    const importsMockDatasource =
      /from\s+['"][^'"]*-mock\.service['"]/.test(source);

    const referencesMockService =
      /\b[A-Za-z0-9]+MockService\b/.test(source);

    if (
      importsMockDatasource
      || referencesMockService
    ) {
      fail(
        `${rel}: UI layer must not depend directly on a mock datasource service`,
      );
    }
      }
    }

/*
 * 4. No silent API -> mock fallback in READY service boundaries.
 *
 * Explicit 404 -> null mapping remains allowed. What is forbidden is
 * returning the mock datasource from catchError/error handlers.
 */
const readyServiceFiles = [
  ...switchedDomains.map((domain) => domain.service),
  ...apiOnlyDomains.map((domain) => domain.service),
];

for (const serviceFile of readyServiceFiles) {
  const source = read(serviceFile);

  const catchErrorBlocks = source.match(
    /catchError\s*\([\s\S]{0,500}?\)/g,
  ) ?? [];

  for (const block of catchErrorBlocks) {
    if (
      block.includes('this.mock.')
      || block.includes('mock.')
    ) {
      fail(
        `${serviceFile}: silent API-to-mock fallback is forbidden`,
      );
    }
  }
}

if (failures.length > 0) {
  console.error(
    '\nFS-1.1 contract-backed integration gate FAILED:\n',
  );

  for (const failure of failures) {
    console.error(` - ${failure}`);
  }

  console.error('');
  process.exit(1);
}

console.log(
  'FS-1.1 contract-backed integration gate PASSED.',
);
console.log(
  'Integration mode is API-backed for Partner, Payment, Reporting, '
    + 'Customer Management, Observed Customer, Accounting and Security User Administration.',
);
