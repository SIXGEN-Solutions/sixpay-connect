import { execFileSync, spawn, spawnSync } from 'node:child_process';
import { existsSync, readdirSync } from 'node:fs';
import { createServer } from 'node:net';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const frontendDir = resolve(here, '..');
const repositoryRoot = resolve(frontendDir, '..');
const backendDir = join(repositoryRoot, 'backend');
const bootstrapTarget = join(backendDir, 'bootstrap', 'target');
const stubScript = join(here, 'cm9-amplitude-stub.mjs');

const docker = process.platform === 'win32' ? 'docker.exe' : 'docker';
const maven = process.platform === 'win32' ? 'mvn.cmd' : 'mvn';
const java = process.platform === 'win32' ? 'java.exe' : 'java';
const npx = process.platform === 'win32' ? 'npx.cmd' : 'npx';

const postgresContainer = `sixpay-cm9-postgres-${process.pid}`;
let backendProcess;
let stubProcess;

function run(command, args, options = {}) {
  const result = spawnSync(command, args, { stdio: 'inherit', ...options });
  if (result.error) throw result.error;
  if (result.status !== 0) {
    throw new Error(`${command} ${args.join(' ')} failed with ${result.status}`);
  }
}

function output(command, args, options = {}) {
  return execFileSync(command, args, { encoding: 'utf8', ...options }).trim();
}

function sleep(ms) {
  return new Promise((resolvePromise) => setTimeout(resolvePromise, ms));
}

function freePort() {
  return new Promise((resolvePort, reject) => {
    const server = createServer();
    server.on('error', reject);
    server.listen(0, '127.0.0.1', () => {
      const address = server.address();
      if (!address || typeof address === 'string') {
        server.close();
        reject(new Error('Unable to allocate port'));
        return;
      }
      const port = address.port;
      server.close(() => resolvePort(port));
    });
  });
}

async function waitForUrl(url, timeoutMs, label) {
  const deadline = Date.now() + timeoutMs;
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url);
      if (response.ok) return;
    } catch {}
    await sleep(500);
  }
  throw new Error(`${label} did not become ready`);
}

async function waitForPostgres() {
  const deadline = Date.now() + 60000;
  while (Date.now() < deadline) {
    const result = spawnSync(
      docker,
      ['exec', postgresContainer, 'pg_isready', '-U', 'sixpay', '-d', 'sixpay'],
      { stdio: 'ignore' },
    );
    if (result.status === 0) return;
    await sleep(500);
  }
  throw new Error('PostgreSQL did not become ready');
}

function mappedPostgresPort() {
  const mapping = output(docker, ['port', postgresContainer, '5432/tcp']);
  const firstLine = mapping.split(/\r?\n/).find(Boolean);
  const match = firstLine?.match(/:(\d+)$/);
  if (!match) throw new Error(`Unable to parse PostgreSQL port: ${mapping}`);
  return match[1];
}

function findBootstrapJar() {
  if (!existsSync(bootstrapTarget)) {
    throw new Error(`Missing bootstrap target: ${bootstrapTarget}`);
  }
  const jars = readdirSync(bootstrapTarget).filter(
    (name) =>
      name.endsWith('.jar') && !name.endsWith('.jar.original') && !name.startsWith('original-'),
  );
  if (jars.length !== 1) {
    throw new Error(`Expected one bootstrap jar, found ${jars.join(', ')}`);
  }
  return join(bootstrapTarget, jars[0]);
}

function terminate(child) {
  if (!child || child.exitCode !== null) return;
  if (process.platform === 'win32') {
    spawnSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], {
      stdio: 'ignore',
    });
  } else {
    child.kill('SIGTERM');
  }
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

  const amplitudePort = await freePort();
  const amplitudeBaseUrl = `http://127.0.0.1:${amplitudePort}`;

  stubProcess = spawn(process.execPath, [stubScript], {
    cwd: frontendDir,
    env: {
      ...process.env,
      AMPLITUDE_STUB_PORT: String(amplitudePort),
    },
    stdio: 'inherit',
  });

  await waitForUrl(`${amplitudeBaseUrl}/__health`, 30000, 'Amplitude stub');

  run(
    maven,
    ['-f', join(backendDir, 'pom.xml'), '-pl', 'bootstrap', '-am', '-DskipTests', 'package'],
    { cwd: repositoryRoot },
  );

  backendProcess = spawn(java, ['-jar', findBootstrapJar()], {
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
      SIXPAY_E2E_CUSTOMER_AMPLITUDE_BASE_URL: amplitudeBaseUrl,
    },
    stdio: 'inherit',
  });

  await waitForUrl('http://127.0.0.1:8080/actuator/health', 120000, 'SIXPAY backend');

  run(
    npx,
    [
      'playwright',
      'test',
      'e2e/fullstack-customer-postgresql.spec.ts',
      '--config',
      'playwright.fullstack.config.ts',
    ],
    { cwd: frontendDir, env: process.env },
  );
}

try {
  await main();
} finally {
  terminate(backendProcess);
  terminate(stubProcess);
  spawnSync(docker, ['rm', '-f', postgresContainer], { stdio: 'ignore' });
}
