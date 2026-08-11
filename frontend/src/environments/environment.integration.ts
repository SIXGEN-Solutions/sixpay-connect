import { AppEnvironment } from './environment.model';

export const environment = {
  production: false,
  apiBaseUrl: '',
  backend: {
    mode: 'api',
  },
  authentication: {
    standalone: false,
    local: {
      enabled: true,
    },
    oidc: {
      enabled: false,
    },
  },
} satisfies AppEnvironment;
