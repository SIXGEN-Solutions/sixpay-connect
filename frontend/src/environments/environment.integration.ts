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
    standalonePartner: {
      subject: 'f88166d1-39df-4900-bb31-1700d25c3bfa',
    },
  },
} satisfies AppEnvironment;
