import { AppEnvironment } from './environment.model';

export const environment = {
  production: false,
  apiBaseUrl: '',
  backend: {
    mode: 'mock',
  },
  authentication: {
    mode: 'standalone',
    authority: '',
    clientId: '',
    scope: 'openid profile email roles',
    standaloneUser: {
      subject: 'local-security-user',
      roles: ['ADMIN'],
    },
    standalonePartner: {
      subject: '11111111-1111-4111-8111-111111111111',
    },
  },
} satisfies AppEnvironment;
