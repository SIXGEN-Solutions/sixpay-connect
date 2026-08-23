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
      } else if (
        entry.isFile()
        && (
          entry.name.endsWith('.ts')
          || entry.name.endsWith('.html')
        )
      ) {
        files.push(resolved);
      }
    }
  }

  return files;
}

function relative(file) {
  return path
    .relative(frontendRoot, file)
    .replaceAll('\\', '/');
}

/*
 * FS-1.4.11 runtime datasource policy
 *
 * production  => NEVER mock
 * integration => NEVER mock
 * development => mock allowed
 * demo        => mock allowed
 * tests       => mock allowed
 */

const productionEnvironment = read(
  'src/environments/environment.ts',
);

const integrationEnvironment = read(
  'src/environments/environment.integration.ts',
);

const developmentEnvironment = read(
  'src/environments/environment.development.ts',
);

const demoEnvironment = read(
  'src/environments/environment.netlify.ts',
);

assertContains(
  productionEnvironment,
  'production: true',
  'production environment',
);

assertContains(
  productionEnvironment,
  "mode: 'api'",
  'production environment',
);

assertContains(
  integrationEnvironment,
  "mode: 'api'",
  'integration environment',
);

assertContains(
  developmentEnvironment,
  "mode: 'mock'",
  'development environment',
);

assertContains(
  demoEnvironment,
  "mode: 'mock'",
  'demo environment',
);

/*
 * The shared backend mode abstraction must remain binary and explicit.
 */
const backendModeService = read(
  'src/app/core/backend/backend-mode.service.ts',
);

for (const token of [
  "this.mode === 'api'",
  "this.mode === 'mock'",
]) {
  assertContains(
    backendModeService,
    token,
    'BackendModeService',
  );
}

/*
 * Accounting is the reference policy already adopted:
 * service-level datasource selection is allowed,
 * silent HTTP-to-mock fallback is not.
 *
 * Administration and Incidents must follow the same rule.
 */
const policyServices = [
  {
    name: 'Accounting',
    path:
      'src/app/features/accounting/services/accounting.service.ts',
    apiType: 'AccountingApiClient',
    mockType: 'AccountingMockService',
  },
  {
    name: 'Operational Administration',
    path:
      'src/app/features/administration/services/administration.service.ts',
    apiType: 'AdministrationApiClient',
    mockType: 'AdministrationMockService',
  },
  {
    name: 'Incidents',
    path:
      'src/app/features/incidents/services/incidents.service.ts',
    apiType: 'IncidentsApiClient',
    mockType: 'IncidentsMockService',
  },
];

for (const service of policyServices) {
  const source = read(service.path);

  for (const token of [
    'BackendModeService',
    'backendMode.usesApi',
    service.apiType,
    service.mockType,
  ]) {
    assertContains(
      source,
      token,
      `${service.name} service`,
    );
  }

  const catchErrorBlocks = source.match(
    /catchError\s*\([\s\S]{0,700}?\)/g,
  ) ?? [];

  for (const block of catchErrorBlocks) {
    if (
      block.includes('this.mock.')
      || block.includes('mock.')
      || block.includes(service.mockType)
    ) {
      fail(
        `${service.name}: silent API-to-mock fallback is forbidden`,
      );
    }
  }
}

/*
 * Physical mock datasource files are allowed.
 * UI code must never depend directly on them.
 */
const forbiddenUiMocks = [
  {
    root: 'src/app/features/administration',
    serviceDirectory:
      'src/app/features/administration/services',
    mockType: 'AdministrationMockService',
  },
  {
    root: 'src/app/features/incidents',
    serviceDirectory:
      'src/app/features/incidents/services',
    mockType: 'IncidentsMockService',
  },
];

for (const policy of forbiddenUiMocks) {
  for (const file of walk(policy.root)) {
    const rel = relative(file);

    if (
      rel.startsWith(
        `${policy.serviceDirectory}/`,
      )
    ) {
      continue;
    }

    const source = fs.readFileSync(
      file,
      'utf8',
    );

    const importsDatasourceMock =
      /from\s+['"][^'"]*-mock\.service['"]/.test(
        source,
      );

    if (
      source.includes(policy.mockType)
      || importsDatasourceMock
    ) {
      fail(
        `${rel}: UI must not depend directly on ${policy.mockType}`,
      );
    }
  }
}

/*
 * Production and integration Angular configurations must resolve
 * to their API environments. Development/demo may select mock.
 */
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
  assertContains(
    angularJson,
    token,
    'Angular environment configuration',
  );
}

if (failures.length > 0) {
  console.error(
    '\nFS-1.4.11 runtime datasource policy FAILED:\n',
  );

  for (const failure of failures) {
    console.error(` - ${failure}`);
  }

  console.error('');
  process.exit(1);
}

console.log(
  'FS-1.4.11 runtime datasource policy PASSED.',
);

console.log(
  'production/integration are API-only; development/demo/tests may use mocks.',
);
