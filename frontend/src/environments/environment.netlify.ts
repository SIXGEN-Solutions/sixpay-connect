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
      subject: 'sixpay-netlify-demo',
      roles: ['ADMIN'],
    },
  },
} satisfies AppEnvironment;
