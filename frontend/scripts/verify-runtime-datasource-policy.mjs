import fs from 'node:fs';
import path from 'node:path';

const frontendRoot = process.cwd();
const failures = [];

function fail(message) {
  failures.push(message);
}

function read(relativePath) {
  const absolute = path.join(frontendRoot, relativePath);
  if (!fs.existsSync(absolute)) {
    fail(`Missing required file: ${relativePath}`);
    return '';
  }
  return fs.readFileSync(absolute, 'utf8');
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

function walk(root) {
  const absoluteRoot = path.join(frontendRoot, root);
  if (!fs.existsSync(absoluteRoot)) {
    return [];
  }

  const files = [];
  const stack = [absoluteRoot];

  while (stack.length > 0) {
    const current = stack.pop();

    for (const entry of fs.readdirSync(current, {
      withFileTypes: true,
    })) {
      const resolved = path.join(current, entry.name);

      if (entry.isDirectory()) {
        stack.push(resolved);
      } else if (entry.isFile() && (entry.name.endsWith('.ts') || entry.name.endsWith('.html'))) {
        files.push(resolved);
      }
    }
  }

  return files;
}

function relative(file) {
  return path.relative(frontendRoot, file).replaceAll('\\', '/');
}

/*
 * FS-1.5 — Remove mock dependency from integration/prod path
 *
 * development/mock => mocks allowed
 * demo             => mocks allowed
 * tests            => mocks allowed
 * integration      => API only
 * production       => API only
 *
 * API failure -> silent mock fallback is forbidden.
 */

const productionEnvironment = read('src/environments/environment.ts');
const integrationEnvironment = read('src/environments/environment.integration.ts');
const developmentEnvironment = read('src/environments/environment.development.ts');
const demoEnvironment = read('src/environments/environment.netlify.ts');

assertContains(productionEnvironment, 'production: true', 'production environment');
assertContains(productionEnvironment, "mode: 'api'", 'production environment');
assertNotContains(productionEnvironment, "mode: 'mock'", 'production environment');

assertContains(integrationEnvironment, "mode: 'api'", 'integration environment');
assertNotContains(integrationEnvironment, "mode: 'mock'", 'integration environment');

assertContains(developmentEnvironment, "mode: 'mock'", 'development environment');
assertContains(demoEnvironment, "mode: 'mock'", 'demo environment');

const backendModeService = read('src/app/core/backend/backend-mode.service.ts');

for (const token of ["this.mode === 'api'", "this.mode === 'mock'"]) {
  assertContains(backendModeService, token, 'BackendModeService');
}

const switchedDomains = [
  {
    name: 'Partner',
    service: 'src/app/features/partners/services/partners.service.ts',
    apiType: 'PartnerApiClient',
    mockType: 'PartnersMockService',
  },
  {
    name: 'Payment',
    service: 'src/app/features/payments/services/payments.service.ts',
    apiType: 'PaymentsApiClient',
    mockType: 'PaymentsMockService',
  },
  {
    name: 'Reporting',
    service: 'src/app/features/reporting/services/reporting.service.ts',
    apiType: 'ReportingApiClient',
    mockType: 'ReportingMockService',
  },
  {
    name: 'Observed Customer',
    service: 'src/app/features/customers/services/customers.service.ts',
    apiType: 'CustomersApiClient',
    mockType: 'CustomersMockService',
  },
  {
    name: 'Accounting',
    service: 'src/app/features/accounting/services/accounting.service.ts',
    apiType: 'AccountingApiClient',
    mockType: 'AccountingMockService',
  },
  {
    name: 'Operational Administration',
    service: 'src/app/features/administration/services/administration.service.ts',
    apiType: 'AdministrationApiClient',
    mockType: 'AdministrationMockService',
  },
  {
    name: 'Incidents',
    service: 'src/app/features/incidents/services/incidents.service.ts',
    apiType: 'IncidentsApiClient',
    mockType: 'IncidentsMockService',
  },
];

for (const domain of switchedDomains) {
  const source = read(domain.service);

  for (const token of ['BackendModeService', domain.apiType, domain.mockType]) {
    assertContains(source, token, `${domain.name} service`);
  }

  if (!source.includes('backendMode.usesApi') && !source.includes('backendMode.usesMock')) {
    fail(`${domain.name}: explicit API/mock runtime boundary is missing`);
  }

  const catchErrorBlocks = source.match(/catchError\s*\([\s\S]{0,1000}?\)\s*[,)]/g) ?? [];

  for (const block of catchErrorBlocks) {
    if (block.includes('this.mock') || block.includes(domain.mockType)) {
      fail(`${domain.name}: silent API-to-mock fallback is forbidden`);
    }
  }
}

const apiOnlyDomains = [
  {
    name: 'Customer Management',
    service: 'src/app/features/customers/services/customer-management.service.ts',
    apiType: 'CustomerManagementApiClient',
  },
  {
    name: 'Security User Administration',
    service: 'src/app/features/administration/services/security-user-administration.service.ts',
    apiType: 'HttpClient',
  },
];

for (const domain of apiOnlyDomains) {
  const source = read(domain.service);

  assertContains(source, domain.apiType, `${domain.name} service`);

  for (const forbidden of ['MockService', 'backendMode.usesMock', 'backendMode.usesApi']) {
    assertNotContains(source, forbidden, `${domain.name} service`);
  }
}

const expectedDomains = [
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

const declaredDomains = new Set([
  ...switchedDomains.map((domain) => domain.name),
  ...apiOnlyDomains.map((domain) => domain.name),
]);

for (const domain of expectedDomains) {
  if (!declaredDomains.has(domain)) {
    fail(`FS-1.5 required domain is missing from runtime datasource policy: ${domain}`);
  }
}

const uiPolicies = [
  ['src/app/features/partners', 'src/app/features/partners/services'],
  ['src/app/features/payments', 'src/app/features/payments/services'],
  ['src/app/features/reporting', 'src/app/features/reporting/services'],
  ['src/app/features/customers', 'src/app/features/customers/services'],
  ['src/app/features/accounting', 'src/app/features/accounting/services'],
  ['src/app/features/administration', 'src/app/features/administration/services'],
  ['src/app/features/incidents', 'src/app/features/incidents/services'],
];

for (const [root, serviceDirectory] of uiPolicies) {
  for (const file of walk(root)) {
    const rel = relative(file);

    if (rel.startsWith(`${serviceDirectory}/`)) {
      continue;
    }

    const source = fs.readFileSync(file, 'utf8');

    const importsMockDatasource = /from\s+['"][^'"]*-mock\.service['"]/.test(source);

    const referencesMockService = /\b[A-Za-z0-9]+MockService\b/.test(source);

    if (importsMockDatasource || referencesMockService) {
      fail(`${rel}: application UI must not depend directly on a mock datasource service`);
    }
  }
}

const angularJson = read('angular.json');

for (const token of [
  '"production"',
  '"integration"',
  '"development"',
  '"netlify"',
  '"src/environments/environment.integration.ts"',
  '"src/environments/environment.development.ts"',
  '"src/environments/environment.netlify.ts"',
]) {
  assertContains(angularJson, token, 'Angular environment configuration');
}

if (failures.length > 0) {
  console.error('\nFS-1.5 runtime datasource policy FAILED:\n');

  for (const failure of failures) {
    console.error(` - ${failure}`);
  }

  console.error('');
  process.exit(1);
}

console.log('FS-1.5 runtime datasource policy PASSED.');
console.log(
  'Production and integration are API-only. ' +
    'Development/demo/tests may use mocks. ' +
    'Silent API-to-mock fallback is forbidden for all contract-backed domains.',
);
