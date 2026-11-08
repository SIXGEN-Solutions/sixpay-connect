import { AppEnvironment } from './environment.model';

export const environment = {
  production: true,
  apiBaseUrl: '',
  backend: { mode: 'api' },
  authentication: {
    standalone: false,
    local: { enabled: false },
    oidc: { 
      enabled: false,
      authority: 'https://identity.sixpay.example',
      clientId: 'sixpay-connect-frontend',
      scope: 'openid profile email roles offline_access'
    },
    ldap: { enabled: true },
  },
} satisfies AppEnvironment;
