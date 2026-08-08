import { AppEnvironment } from './environment.model';

export const environment = {
  production: true,
  apiBaseUrl: '',
  backend: {
    mode: 'api',
  },
  authentication: {
    mode: 'oidc',
    authority: 'https://identity.sixpay.example',
    clientId: 'sixpay-connect-frontend',
    scope: 'openid profile email roles offline_access',
  },
} satisfies AppEnvironment;
