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
let backendProcess;

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

async function waitForBackend() {
  const deadline = Date.now() + 120_000;
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

  throw new Error('SIXPAY backend did not become healthy within 120 seconds.');
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
    },
    stdio: 'inherit',
  });

  await waitForBackend();

  run(npx, ['playwright', 'test', '--config', 'playwright.fullstack.config.ts'], {
    cwd: frontendDir,
    env: process.env,
  });
}

try {
  await main();
} finally {
  terminateProcessTree(backendProcess);
  removePostgresContainer();
}
