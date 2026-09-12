import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(here, '..');
const repositoryRoot = resolve(frontendDir, '..');
const backendDir = join(repositoryRoot, 'backend');
const bootstrapTarget = join(backendDir, 'bootstrap', 'target');

const docker = process.platform === 'win32' ? 'docker.exe' : 'docker';
const maven = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
const java = process.platform === 'win32' ? 'java.exe' : 'java';
const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';

const postgresContainer = `sixpay-fullstack-postgres-${process.pid}`;
const amplitudeStubPort = 18081;
let backendProcess;

const backendStartupTimeoutMs = Number.parseInt(
  process.env.SIXPAY_E2E_BACKEND_STARTUP_TIMEOUT_MS ?? '240000',
  10,
);

let amplitudeStubProcess;

function run(command, args, options = {}) {
  const isWindowsCmd = process.platform === 'win32' && command.toLowerCase().endsWith('.cmd');

  const executable = isWindowsCmd ? process.env.ComSpec || 'cmd.exe' : command;

  const executableArgs = isWindowsCmd ? ['/d', '/s', '/c', command, ...args] : args;

  const result = spawnSync(executable, executableArgs, {
    stdio: 'inherit',
    ...options,
  });

  if (result.error) throw result.error;

  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with exit code ${result.status}`);
  }
}

function output(command, args, options = {}) {
  return execFileSync(command, args, { encoding: 'utf8', ...options }).trim();
}

function sleep(ms) {
  return new Promise((resolvePromise) => setTimeout(resolvePromise, ms));
}

async function waitForPostgres() {
  const deadline = Date.now() + 60_000;
  while (Date.now() < deadline) {
    const result = spawnSync(
      docker,
      ['exec', postgresContainer, 'pg_isready', '-U', 'sixpay', '-d', 'sixpay'],
      { stdio: 'ignore' },
    );
    if (result.status === 0) return;
    await sleep(500);
  }
  throw new Error('PostgreSQL did not become ready within 60 seconds.');
}

async function waitForAmplitudeStub() {
  const deadline = Date.now() + 30_000;
  while (Date.now() < deadline) {
    if (amplitudeStubProcess && amplitudeStubProcess.exitCode !== null) {
      throw new Error(
        `CM-9 Amplitude stub exited before becoming healthy (exit code ${amplitudeStubProcess.exitCode}).`,
      );
    }

    try {
      const response = await fetch(`http://127.0.0.1:${amplitudeStubPort}/__health`);
      if (response.ok) {
        const payload = await response.json();
        if (payload.status === 'UP') return;
      }
    } catch {
      // Still starting.
    }

    await sleep(250);
  }

  throw new Error('CM-9 Amplitude stub did not become healthy within 30 seconds.');
}

async function waitForBackend() {
  const deadline = Date.now() + backendStartupTimeoutMs;
  while (Date.now() < deadline) {
    if (backendProcess && backendProcess.exitCode !== null) {
      throw new Error(
        `SIXPAY backend exited before becoming healthy (exit code ${backendProcess.exitCode}).`,
      );
    }

    try {
      const response = await fetch('http://127.0.0.1:8080/actuator/health');
      if (response.ok) {
        const payload = await response.json();
        if (payload.status === 'UP') return;
      }
    } catch {
      // Still starting.
    }

    await sleep(750);
  }

  throw new Error(
    `SIXPAY backend did not become healthy within ${Math.round(backendStartupTimeoutMs / 1000)} seconds.`,
  );
}

function findBootstrapJar() {
  if (!existsSync(bootstrapTarget)) {
    throw new Error(`Bootstrap target directory does not exist: ${bootstrapTarget}`);
  }

  const candidates = readdirSync(bootstrapTarget).filter(
    (name) =>
      name.endsWith('.jar') && !name.endsWith('.jar.original') && !name.startsWith('original-'),
  );

  if (candidates.length !== 1) {
    throw new Error(
      `Expected exactly one executable bootstrap jar in ${bootstrapTarget}, found: ${candidates.join(', ')}`,
    );
  }

  return join(bootstrapTarget, candidates[0]);
}

function mappedPostgresPort() {
  const mapping = output(docker, ['port', postgresContainer, '5432/tcp']);
  const firstLine = mapping.split(/\r?\n/).find(Boolean);
  const match = firstLine?.match(/:(\d+)$/);

  if (!match) {
    throw new Error(`Unable to determine PostgreSQL mapped port from: ${mapping}`);
  }

  return match[1];
}

function seedPaymentVerticalFixture() {
  const sql = `
INSERT INTO payments (
    payment_id,
    public_payment_reference,
    payment_source,
    external_payment_reference,
    external_subscription_reference,
    financial_institution_code,
    requested_amount,
    requested_currency,
    status,
    business_version,
    received_at,
    updated_at,
    finalized_at,
    state_payload,
    persistence_version
) VALUES (
    '59040000-0000-0000-0000-000000000001',
    'PAY-0123456789ABCDEFGHJKMNPQRS',
    'TRESOR_PAY',
    'L594-E2E-REQUEST-001',
    'partner:L594',
    'SIXPAY',
    12500.00,
    'XAF',
    'RECEIVED',
    1,
    '2026-09-12T12:00:00Z',
    '2026-09-12T12:00:00Z',
    NULL,
    '{
      "schemaVersion": 1,
      "requestIdentity": {
        "correlationId": {
          "value": "59040000-0000-0000-0000-000000000099"
        }
      },
      "debtorAccountReference": {
        "bindingFingerprint": "acct:v1:l594",
        "maskedDisplay": "RIB-****-5940"
      }
    }'::jsonb,
    0
)
ON CONFLICT (payment_id) DO NOTHING;

INSERT INTO reporting_payment_audit_evidence (
    evidence_id,
    timeline_visible,
    audit_visible,
    payment_id,
    payment_reference,
    observed_customer_id,
    category,
    event_type,
    timeline_result,
    actor_type,
    actor_id,
    actor_roles,
    action,
    target_type,
    target_id,
    audit_result,
    reason_code,
    correlation_id,
    trace_id,
    source_system,
    external_reference,
    before_state,
    after_state,
    aggregate_version,
    integrity_scheme,
    integrity_value,
    occurred_at
) VALUES (
    '59040000-0000-0000-0000-000000000002',
    TRUE,
    TRUE,
    '59040000-0000-0000-0000-000000000001',
    'PAY-0123456789ABCDEFGHJKMNPQRS',
    NULL,
    'DOMAIN',
    'PAYMENT_RECEIVED',
    'SUCCESS',
    'EXTERNAL_SYSTEM',
    'TRESOR_PAY',
    NULL,
    'PAYMENT_RECEIVED',
    'PAYMENT',
    '59040000-0000-0000-0000-000000000001',
    'SUCCESS',
    'PAYMENT_RECEIVED',
    '59040000-0000-0000-0000-000000000099',
    NULL,
    'TRESOR_PAY',
    'L594-E2E-REQUEST-001',
    NULL,
    'RECEIVED',
    1,
    'WORM_REFERENCE',
    'lot-5.9.4-e2e-proof',
    '2026-09-12T12:00:00Z'
)
ON CONFLICT (evidence_id) DO NOTHING;
`;

  run(
    docker,
    [
      'exec',
      postgresContainer,
      'psql',
      '-v',
      'ON_ERROR_STOP=1',
      '-U',
      'sixpay',
      '-d',
      'sixpay',
      '-c',
      sql,
    ],
    { cwd: repositoryRoot },
  );
}

function seedAccountingVerticalFixture() {
  const sql = `
INSERT INTO accounting_batches (
    id,
    idempotency_key,
    business_date,
    financial_institution_code,
    created_at,
    status,
    version
) VALUES (
    '59050000-0000-0000-0000-000000000001',
    '9aa0a2f094d472d7aa4973054b98c5f00f96e50547f0df9ffda0504d8fc6001d',
    '2026-09-12',
    'SIXPAY',
    '2026-09-12T15:00:00Z',
    'COMPLETED',
    0
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO accounting_batch_items (
    id,
    batch_id,
    payment_id,
    public_payment_reference,
    partner_id,
    amount,
    currency,
    payment_occurred_at,
    payment_business_date,
    bank_posting_reference,
    tresorpay_status,
    tresorpay_status_checked_at,
    status
) VALUES (
    '59050000-0000-0000-0000-000000000002',
    '59050000-0000-0000-0000-000000000001',
    '59040000-0000-0000-0000-000000000001',
    'PAY-0123456789ABCDEFGHJKMNPQRS',
    'L595-PARTNER',
    12500.00,
    'XAF',
    '2026-09-12T12:00:00Z',
    '2026-09-12',
    'AMP-L595-POSTING-001',
    'COMPLETED',
    '2026-09-12T14:45:00Z',
    'COMPLETED'
)
ON CONFLICT (id) DO NOTHING;
`;

  run(
    docker,
    [
      'exec',
      postgresContainer,
      'psql',
      '-v',
      'ON_ERROR_STOP=1',
      '-U',
      'sixpay',
      '-d',
      'sixpay',
      '-c',
      sql,
    ],
    { cwd: repositoryRoot },
  );
}

function seedAdministrationIdentityIncidentsFixture() {
  const sql = `
INSERT INTO operational_incident (
    incident_id,
    severity,
    component,
    summary,
    status,
    description,
    impact,
    accounting_batch_id,
    payment_id,
    payment_reference,
    correlation_id,
    opened_at,
    updated_at
) VALUES (
    'INC-L596-E2E-001',
    'HIGH',
    'PAYMENT',
    'Dégradation contrôlée LOT 5.9.6',
    'INVESTIGATING',
    'Incident de preuve full-stack Administration / Identity / Incidents.',
    'Validation E2E uniquement dans la base PostgreSQL jetable.',
    '59050000-0000-0000-0000-000000000001',
    '59040000-0000-0000-0000-000000000001',
    'PAY-0123456789ABCDEFGHJKMNPQRS',
    '59060000-0000-0000-0000-000000000099',
    '2026-09-12T16:00:00Z',
    '2026-09-12T16:05:00Z'
)
ON CONFLICT (incident_id) DO NOTHING;

INSERT INTO operational_incident_timeline (
    event_id,
    incident_id,
    occurred_at,
    message,
    actor,
    sequence_no
) VALUES
(
    'EVT-L596-E2E-001',
    'INC-L596-E2E-001',
    '2026-09-12T16:00:00Z',
    'Incident détecté par la supervision SIXPAY.',
    'SYSTEM',
    0
),
(
    'EVT-L596-E2E-002',
    'INC-L596-E2E-001',
    '2026-09-12T16:05:00Z',
    'Investigation opérationnelle démarrée.',
    'admin',
    1
)
ON CONFLICT (event_id) DO NOTHING;
`;

  run(
    docker,
    [
      'exec',
      postgresContainer,
      'psql',
      '-v',
      'ON_ERROR_STOP=1',
      '-U',
      'sixpay',
      '-d',
      'sixpay',
      '-c',
      sql,
    ],
    { cwd: repositoryRoot },
  );
}

function terminateProcessTree(child) {
  if (!child || child.exitCode !== null) return;

  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' });
  } else {
    child.kill('SIGTERM');
  }
}

function removePostgresContainer() {
  spawnSync(docker, ['rm', '-f', postgresContainer], { stdio: 'ignore' });
}

async function main() {
  run(docker, [
    'run',
    '--detach',
    '--rm',
    '--name',
    postgresContainer,
    '--publish',
    '127.0.0.1::5432',
    '--env',
    'POSTGRES_DB=sixpay',
    '--env',
    'POSTGRES_USER=sixpay',
    '--env',
    'POSTGRES_PASSWORD=sixpay-test',
    'postgres:15-alpine',
  ]);

  await waitForPostgres();
  const postgresPort = mappedPostgresPort();

  run(
    maven,
    ['-f', join(backendDir, 'pom.xml'), '-pl', 'bootstrap', '-am', '-DskipTests', 'package'],
    { cwd: repositoryRoot },
  );

  const bootstrapJar = findBootstrapJar();

  amplitudeStubProcess = spawn(
    process.execPath,
    [join(frontendDir, 'scripts', 'cm9-amplitude-stub.mjs')],
    {
      cwd: frontendDir,
      env: {
        ...process.env,
        AMPLITUDE_STUB_PORT: String(amplitudeStubPort),
      },
      stdio: 'inherit',
    },
  );

  await waitForAmplitudeStub();

  backendProcess = spawn(java, ['-jar', bootstrapJar], {
    cwd: backendDir,
    env: {
      ...process.env,
      SPRING_PROFILES_ACTIVE: 'integration',
      SPRING_DATASOURCE_URL: `jdbc:postgresql://127.0.0.1:${postgresPort}/sixpay`,
      SPRING_DATASOURCE_USERNAME: 'sixpay',
      SPRING_DATASOURCE_PASSWORD: 'sixpay-test',
      SIXPAY_LOCAL_ADMIN_PASSWORD: 'admin-dev-2026',
      SIXPAY_MESSAGING_OUTBOX_ENABLED: 'false',
      SIXPAY_E2E_CUSTOMER_ENABLED: 'true',
      SIXPAY_E2E_CUSTOMER_AMPLITUDE_BASE_URL: `http://127.0.0.1:${amplitudeStubPort}`,
    },
    stdio: 'inherit',
  });

  await waitForBackend();

  seedPaymentVerticalFixture();
  seedAccountingVerticalFixture();
  seedAdministrationIdentityIncidentsFixture();

  run(npx, ['playwright', 'test', '--config', 'playwright.fullstack.config.ts'], {
    cwd: frontendDir,
    env: process.env,
  });
}

try {
  await main();
} finally {
  terminateProcessTree(backendProcess);
  terminateProcessTree(amplitudeStubProcess);
  removePostgresContainer();
}
