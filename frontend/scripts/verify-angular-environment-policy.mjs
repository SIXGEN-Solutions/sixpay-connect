import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const envRoot = path.join(root, 'src', 'environments');

const read = (relative) =>
  fs.readFileSync(path.join(root, relative), 'utf8');

const errors = [];

const files = {
  production: 'src/environments/environment.ts',
  development: 'src/environments/environment.development.ts',
  integration: 'src/environments/environment.integration.ts',
  netlify: 'src/environments/environment.netlify.ts',
};

const source = Object.fromEntries(
  Object.entries(files).map(([name, file]) => [name, read(file)]),
);

function requireToken(name, token, message) {
  if (!source[name].includes(token)) {
    errors.push(`${name}: ${message} (missing ${JSON.stringify(token)})`);
  }
}

function forbidToken(name, token, message) {
  if (source[name].includes(token)) {
    errors.push(`${name}: ${message} (found ${JSON.stringify(token)})`);
  }
}

function section(text, startToken, endToken) {
  const start = text.indexOf(startToken);
  if (start < 0) return '';
  const end = endToken ? text.indexOf(endToken, start) : -1;
  return end > start ? text.slice(start, end) : text.slice(start);
}

// Structural contract.
const model = read('src/environments/environment.model.ts');

for (const token of [
  "export type BackendMode = 'mock' | 'api';",
  'readonly production: boolean;',
  'readonly apiBaseUrl: string;',
  'readonly backend: BackendEnvironment;',
  'readonly authentication: AuthenticationEnvironment;',
]) {
  if (!model.includes(token)) {
    errors.push(`environment.model.ts: canonical environment contract changed: ${token}`);
  }
}

// Every environment must be typed against AppEnvironment.
for (const [name, text] of Object.entries(source)) {
  if (!text.includes("satisfies AppEnvironment")) {
    errors.push(`${name}: environment must satisfy AppEnvironment`);
  }
}

// Production policy.
requireToken('production', "production: true", 'production flag must remain true');
requireToken('production', "mode: 'api'", 'production must remain API-backed');
requireToken('production', 'standalone: false', 'production standalone auth must remain disabled');

{
  const local = section(source.production, 'local:', 'oidc:');
  const oidc = section(source.production, 'oidc:', 'standaloneUser:');

  if (!local.includes('enabled: true')) {
    errors.push('production: local authentication must remain enabled');
  }
  if (!oidc.includes('enabled: true')) {
    errors.push('production: OIDC must remain enabled');
  }

  for (const token of ['authority:', 'clientId:', 'scope:']) {
    if (!oidc.includes(token)) {
      errors.push(`production: enabled OIDC must keep ${token}`);
    }
  }
}

forbidToken('production', "mode: 'mock'", 'production must never use mock datasource');

// Integration policy.
requireToken('integration', "production: false", 'integration production flag must remain false');
requireToken('integration', "mode: 'api'", 'integration must remain API-backed');
requireToken('integration', 'standalone: false', 'integration standalone auth must remain disabled');

{
  const local = section(source.integration, 'local:', 'oidc:');
  const oidc = section(source.integration, 'oidc:');

  if (!local.includes('enabled: true')) {
    errors.push('integration: local authentication must remain enabled');
  }
  if (!oidc.includes('enabled: false')) {
    errors.push('integration: OIDC must remain disabled');
  }
}

forbidToken('integration', "mode: 'mock'", 'integration must never use mock datasource');

// Development policy.
requireToken('development', "production: false", 'development production flag must remain false');
requireToken('development', "mode: 'mock'", 'development must remain explicit mock mode');
requireToken('development', 'standalone: true', 'development standalone auth must remain enabled');

{
  const local = section(source.development, 'local:', 'oidc:');
  const oidc = section(source.development, 'oidc:', 'standaloneUser:');

  if (!local.includes('enabled: false')) {
    errors.push('development: local authentication must remain disabled');
  }
  if (!oidc.includes('enabled: false')) {
    errors.push('development: OIDC must remain disabled');
  }
}

// Netlify/demo policy.
requireToken('netlify', "production: false", 'netlify production flag must remain false');
requireToken('netlify', "mode: 'mock'", 'netlify must remain explicit mock mode');
requireToken('netlify', 'standalone: true', 'netlify standalone auth must remain enabled');

{
  const local = section(source.netlify, 'local:', 'oidc:');
  const oidc = section(source.netlify, 'oidc:', 'standaloneUser:');

  if (!local.includes('enabled: false')) {
    errors.push('netlify: local authentication must remain disabled');
  }
  if (!oidc.includes('enabled: false')) {
    errors.push('netlify: OIDC must remain disabled');
  }
}

// Angular CLI environment mapping.
const angular = JSON.parse(read('angular.json'));
const buildConfigurations =
  angular?.projects?.frontend?.architect?.build?.configurations ?? {};

const expectedReplacements = {
  development: 'src/environments/environment.development.ts',
  integration: 'src/environments/environment.integration.ts',
  netlify: 'src/environments/environment.netlify.ts',
};

for (const [configuration, expectedWith] of Object.entries(expectedReplacements)) {
  const replacements = buildConfigurations[configuration]?.fileReplacements ?? [];
  const expected = replacements.find(
    (replacement) =>
      replacement.replace === 'src/environments/environment.ts' &&
      replacement.with === expectedWith,
  );

  if (!expected) {
    errors.push(
      `angular.json: ${configuration} must replace environment.ts with ${expectedWith}`,
    );
  }
}

if (buildConfigurations.production?.fileReplacements?.length) {
  errors.push(
    'angular.json: production must use canonical environment.ts without replacement',
  );
}

if (angular?.projects?.frontend?.architect?.build?.defaultConfiguration !== 'production') {
  errors.push('angular.json: default build configuration must remain production');
}

if (angular?.projects?.frontend?.architect?.serve?.defaultConfiguration !== 'development') {
  errors.push('angular.json: default serve configuration must remain development');
}

// Authentication runtime validator must remain active in source.
const authValidator = read('src/environments/authentication-environment.ts');

for (const token of [
  'Standalone authentication is not allowed in production',
  'At least one production authentication capability must be enabled',
  'Standalone authentication cannot be combined with Local or OIDC authentication',
  'OIDC authority must be configured when OIDC is enabled',
  'OIDC clientId must be configured when OIDC is enabled',
  'OIDC scope must be configured when OIDC is enabled',
]) {
  if (!authValidator.includes(token)) {
    errors.push(
      `authentication-environment.ts: reviewed runtime validation changed: ${token}`,
    );
  }
}

// Explicitly prohibit common fallback patterns in environment definitions.
for (const [name, text] of Object.entries(source)) {
  for (const forbidden of [
    "mode: 'api-or-mock'",
    "mode: 'fallback'",
    'fallbackToMock',
    'useMockOnError',
  ]) {
    if (text.includes(forbidden)) {
      errors.push(`${name}: implicit API-to-mock fallback is forbidden: ${forbidden}`);
    }
  }
}

if (errors.length > 0) {
  console.error('\nFS-2.5.6 Angular environment validation FAILED:\n');
  for (const error of errors) {
    console.error(` - ${error}`);
  }
  process.exit(1);
}

console.log('FS-2.5.6 Angular environment validation PASSED.');
console.log(
  'Production/integration remain API-only; development/netlify remain explicit mock/standalone environments; Angular CLI mappings and auth validation are canonical.',
);
