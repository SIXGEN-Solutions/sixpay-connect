import { validateAuthenticationEnvironment } from './authentication-environment';
import type { AppEnvironment } from './environment.model';

let activeEnvironment: Readonly<AppEnvironment> | undefined;

function object(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error('Invalid SIXPAY runtime configuration object');
  }
  return value as Record<string, unknown>;
}

function booleanField(value: unknown, name: string): boolean {
  if (typeof value !== 'boolean') throw new Error(`Invalid runtime configuration: ${name}`);
  return value;
}

function stringField(value: unknown, name: string): string {
  if (typeof value !== 'string') throw new Error(`Invalid runtime configuration: ${name}`);
  return value;
}

/** Parse and validate public, non-secret configuration; never fall back to mocks. */
export function validateRuntimeEnvironment(raw: unknown): AppEnvironment {
  const source = object(raw);
  const backend = object(source['backend']);
  const authentication = object(source['authentication']);
  const local = object(authentication['local']);
  const oidc = object(authentication['oidc']);
  const ldap = object(authentication['ldap']);
  const production = booleanField(source['production'], 'production');
  const apiBaseUrl = stringField(source['apiBaseUrl'], 'apiBaseUrl');
  const mode = backend['mode'];
  if (mode !== 'api' && mode !== 'mock') {
    throw new Error('Invalid runtime configuration: backend.mode');
  }
  if (apiBaseUrl && !/^https:\/\/[^\s]+$/.test(apiBaseUrl) && !/^\/(?!\/)[^\s]*$/.test(apiBaseUrl)) {
    throw new Error('Invalid runtime configuration: apiBaseUrl');
  }

  const authenticationConfig: AppEnvironment['authentication'] = {
    standalone: booleanField(authentication['standalone'], 'authentication.standalone'),
    local: { enabled: booleanField(local['enabled'], 'authentication.local.enabled') },
    oidc: {
      enabled: booleanField(oidc['enabled'], 'authentication.oidc.enabled'),
      ...(oidc['authority'] === undefined ? {} : { authority: stringField(oidc['authority'], 'authentication.oidc.authority') }),
      ...(oidc['clientId'] === undefined ? {} : { clientId: stringField(oidc['clientId'], 'authentication.oidc.clientId') }),
      ...(oidc['scope'] === undefined ? {} : { scope: stringField(oidc['scope'], 'authentication.oidc.scope') }),
    },
    ldap: { enabled: booleanField(ldap['enabled'], 'authentication.ldap.enabled') },
    ...(authentication['standaloneUser'] === undefined ? {} : {
      standaloneUser: (() => {
        const user = object(authentication['standaloneUser']);
        if (!Array.isArray(user['roles']) || !user['roles'].every((role: unknown) => typeof role === 'string')) {
          throw new Error('Invalid runtime configuration: standaloneUser.roles');
        }
        return { subject: stringField(user['subject'], 'standaloneUser.subject'), roles: user['roles'] as string[] };
      })(),
    }),
    ...(authentication['standalonePartner'] === undefined ? {} : {
      standalonePartner: { subject: stringField(object(authentication['standalonePartner'])['subject'], 'standalonePartner.subject') },
    }),
  };
  const result: AppEnvironment = {
    production,
    apiBaseUrl,
    backend: { mode },
    authentication: authenticationConfig,
  };
  validateAuthenticationEnvironment(result);
  if (production && mode !== 'api') throw new Error('Production must use API-backed data');
  return result;
}

export async function loadRuntimeEnvironment(): Promise<void> {
  if (activeEnvironment) return;
  const response = await fetch('/runtime-config.json', {
    cache: 'no-store',
    credentials: 'same-origin',
    redirect: 'error',
    headers: { Accept: 'application/json' },
  });
  if (!response.ok || !response.headers.get('content-type')?.includes('application/json')) {
    throw new Error('SIXPAY runtime configuration unavailable or invalid content type');
  }
  const raw: unknown = await response.json();
  activeEnvironment = Object.freeze(validateRuntimeEnvironment(raw));
}

/** App code retains the established environment interface, resolved only after bootstrap preflight. */
export const environment: AppEnvironment = new Proxy({} as AppEnvironment, {
  get(_target, property: string | symbol) {
    if (!activeEnvironment) throw new Error('SIXPAY runtime configuration not initialized');
    return Reflect.get(activeEnvironment, property);
  },
});
