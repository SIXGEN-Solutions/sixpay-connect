import { AppEnvironment } from './environment.model';

export const environment = {
  production: false,
  apiBaseUrl: '',
  backend: {
    mode: 'api',
  },
  authentication: {
    mode: 'standalone',
    authority: '',
    clientId: '',
    scope: 'openid profile email roles',
    standaloneUser: {
      subject: 'local-integration-user',
      roles: ['ADMIN'],
    },
  },
} satisfies AppEnvironment;
