import fs from 'node:fs';

const docker = fs.readFileSync('Dockerfile', 'utf8');
const nginx = fs.readFileSync('nginx.conf', 'utf8');
const main = fs.readFileSync('src/main.ts', 'utf8');
const angular = JSON.parse(fs.readFileSync('angular.json', 'utf8'));
const prod = angular.projects.frontend.architect.build.configurations.production;
const replacements = prod.fileReplacements ?? [];
const runtimeReplacement = replacements.some((r) =>
  r.replace === 'src/environments/environment.ts' &&
  r.with === 'src/environments/environment.runtime.ts');

const failures = [];
if (!runtimeReplacement) failures.push('Default optimized build must use environment.runtime.ts');
if (/BUILD_CONFIGURATION|--configuration\s+/.test(docker)) failures.push('Dockerfile contains build profile coupling');
if (!main.includes('loadRuntimeEnvironment()')) failures.push('Runtime bootstrap preflight missing');
if (!nginx.includes('location = /runtime-config.json')) failures.push('Runtime JSON endpoint missing');
if (!nginx.includes('location ^~ /actuator/')) failures.push('Actuator block missing');
if (/location\s+\/actuator\/\s*\{/.test(nginx)) failures.push('Actuator public proxy found');
if (fs.existsSync('public/runtime-config.json')) failures.push('Runtime deployment config must not be embedded in image');
if (failures.length) {
  failures.forEach((x) => console.error(`ERROR: ${x}`));
  process.exit(1);
}
console.log('Frontend immutable-runtime-image policy PASSED');
