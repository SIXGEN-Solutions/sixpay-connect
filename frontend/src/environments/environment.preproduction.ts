import { AppEnvironment } from './environment.model';
export const environment = {
  production: true, apiBaseUrl: '', backend: { mode: 'api' },
  authentication: { standalone: false, local: { enabled: false }, oidc: { enabled: false }, ldap: { enabled: true } },
} satisfies AppEnvironment;
