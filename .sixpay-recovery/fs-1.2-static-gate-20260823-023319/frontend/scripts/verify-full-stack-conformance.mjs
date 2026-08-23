import fs from 'node:fs';
import path from 'node:path';

const repoRoot = path.resolve(process.cwd(), '..');
const frontendRoot = process.cwd();
const failures = [];

function fail(message) { failures.push(message); }
function readAbsolute(file) {
  if (!fs.existsSync(file)) {
    fail(`Missing required file: ${path.relative(repoRoot, file)}`);
    return '';
  }
  return fs.readFileSync(file, 'utf8');
}
function walk(root, predicate = () => true) {
  if (!fs.existsSync(root)) return [];
  const result = [];
  const queue = [root];
  while (queue.length > 0) {
    const current = queue.pop();
    for (const entry of fs.readdirSync(current, { withFileTypes: true })) {
      const resolved = path.join(current, entry.name);
      if (entry.isDirectory()) queue.push(resolved);
      else if (entry.isFile() && predicate(resolved)) result.push(resolved);
    }
  }
  return result;
}

const backendJavaFiles = walk(
  path.join(repoRoot, 'backend'),
  (file) => file.endsWith('.java'),
);
const contractFiles = walk(
  path.join(repoRoot, 'documentation', 'contracts'),
  (file) => file.endsWith('.yaml') || file.endsWith('.yml') || file.endsWith('.json'),
);
function backendContains(token) {
  return backendJavaFiles.some((file) => fs.readFileSync(file, 'utf8').includes(token));
}
function contractContains(token) {
  return contractFiles.some((file) => fs.readFileSync(file, 'utf8').includes(token));
}

const capabilities = [
  {
    name: 'Partner',
    frontend: 'src/app/features/partners/api/partners-api.client.ts',
    basePaths: ['/api/v1/partners'],
    responseModels: 'src/app/features/partners/models/partners.response.ts',
    backendHints: ['PartnerController', 'PartnerRepository'],
  },
  {
    name: 'Payment',
    frontend: 'src/app/features/payments/api/payments-api.client.ts',
    basePaths: ['/internal/api/v1/payments'],
    responseModels: 'src/app/features/payments/models/payments.response.ts',
    backendHints: ['PaymentController', 'PaymentRepository'],
  },
  {
    name: 'Reporting',
    frontend: 'src/app/features/reporting/api/reporting-api.client.ts',
    basePaths: [
      '/internal/api/v1/payment-audit-records',
      '/internal/api/v1/payment-audit-exports',
      '/internal/api/v1/payments/',
    ],
    responseModels: 'src/app/features/reporting/models/reporting.response.ts',
    backendHints: ['PaymentAudit', 'Reporting'],
  },
  {
    name: 'Customer Management',
    frontend: 'src/app/features/customers/api/customer-management-api.client.ts',
    basePaths: ['/internal/api/v1/customers', '/internal/api/v1/subscriptions'],
    responseModels: 'src/app/features/customers/models/customer-management.response.ts',
    backendHints: ['CustomerController', 'CustomerRepository'],
  },
  {
    name: 'Observed Customer',
    frontend: 'src/app/features/customers/api/customers-api.client.ts',
    basePaths: ['/internal/api/v1/observed-customers'],
    responseModels: 'src/app/features/customers/models/customers.response.ts',
    backendHints: ['ObservedCustomer'],
  },
  {
    name: 'Security User Administration',
    frontend: 'src/app/features/administration/services/security-user-administration.service.ts',
    basePaths: ['/internal/api/v1/administration/users'],
    responseModels: 'src/app/features/administration/models/security-user-administration.ts',
    backendHints: ['SecurityUser', 'Administration'],
  },
];

for (const capability of capabilities) {
  const source = readAbsolute(path.join(frontendRoot, capability.frontend));
  const modelFile = path.join(frontendRoot, capability.responseModels);
  const modelSource = readAbsolute(modelFile);

  if (!source.includes('HttpClient')) {
    fail(`${capability.name}: Angular API boundary does not use HttpClient`);
  }

  for (const basePath of capability.basePaths) {
    if (!source.includes(basePath)) {
      fail(`${capability.name}: frontend client no longer declares ${basePath}`);
    }
    const stablePrefix = basePath.endsWith('/') ? basePath.slice(0, -1) : basePath;
    if (!backendContains(stablePrefix)) {
      fail(`${capability.name}: no Spring source contains endpoint prefix ${stablePrefix}`);
    }
    const resourceSegment = stablePrefix.split('/').filter(Boolean).at(-1);
    if (!contractContains(stablePrefix) && resourceSegment && !contractContains(resourceSegment)) {
      fail(`${capability.name}: endpoint ${stablePrefix} is not traceable in documentation/contracts`);
    }
  }

  for (const hint of capability.backendHints) {
    if (!backendContains(hint)) {
      fail(`${capability.name}: expected backend ownership hint "${hint}" not found`);
    }
  }

  const genericResponses = [...source.matchAll(/(?:get|post|put|delete)<([^>]+)>/g)]
    .map((match) => match[1].trim())
    .filter((type) => !type.startsWith('readonly ') && type !== 'void' && !type.includes('[]'));

  for (const responseType of genericResponses) {
    const simpleType = responseType.replace(/^Observable</, '').replace(/>$/, '').trim();
    if (/^[A-Z][A-Za-z0-9_]+$/.test(simpleType) && !modelSource.includes(simpleType)) {
      fail(`${capability.name}: Angular response DTO ${simpleType} is not declared in ${capability.responseModels}`);
    }
  }

  if (/catchError\s*\([\s\S]{0,500}?(?:MockService|this\.mock|mock\.)/.test(source)) {
    fail(`${capability.name}: API boundary contains a silent mock fallback`);
  }
}

const packageJson = JSON.parse(readAbsolute(path.join(frontendRoot, 'package.json')));
if (!packageJson.scripts?.['verify:integration-contract-backed']) {
  fail('FS-1.1 integration contract-backed gate is not registered');
}

if (failures.length > 0) {
  console.error('\nFS-1.2 full-stack static conformance gate FAILED:\n');
  for (const failure of failures) console.error(` - ${failure}`);
  console.error('');
  process.exit(1);
}

console.log('FS-1.2 full-stack static conformance gate PASSED.');
console.log('Angular clients, contract traces and Spring ownership are aligned for all FS-1.1 READY capabilities.');
