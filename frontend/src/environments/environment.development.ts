import { AppEnvironment } from './environment.model';

export const environment = {
  production: false,
  apiBaseUrl: '',
  authentication: {
    mode: 'standalone',
    authority: '',
    clientId: '',
    scope: 'openid profile email roles',
    standaloneUser: {
      subject: 'local-security-user',
      roles: ['ADMIN'],
    },
  },
} satisfies AppEnvironment;
