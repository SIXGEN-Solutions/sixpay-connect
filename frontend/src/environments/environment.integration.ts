import { AppEnvironment } from './environment.model';

export const environment = {
  production: false,
  apiBaseUrl: '',
  backend: {
    mode: 'api',
  },
  authentication: {
    mode: 'local',
    authority: '',
    clientId: '',
    scope: '',
  },
} satisfies AppEnvironment;
