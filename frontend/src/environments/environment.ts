import { AppEnvironment } from './environment.model';

export const environment = {
  production: true,
  apiBaseUrl: '',
  branding: {
    switcherEnabled: false,
  },
  backend: {
    mode: 'api',
  },
  authentication: {
    standalone: false,
    local: {
      enabled: true,
    },
    oidc: {
      enabled: true,
      authority: 'https://identity.sixpay.example',
      clientId: 'sixpay-connect-frontend',
      scope: 'openid profile email roles offline_access',
    },
  },
} satisfies AppEnvironment;
