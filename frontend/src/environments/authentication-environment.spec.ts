import {
  AppEnvironment,
  AuthenticationEnvironment,
} from './environment.model';
import {
  validateAuthenticationEnvironment,
} from './authentication-environment';

describe('validateAuthenticationEnvironment', () => {
  it('accepts Local-only production authentication', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: false,
          local: { enabled: true },
          oidc: { enabled: false },
        }),
      ),
    ).not.toThrow();
  });

  it('accepts OIDC-only production authentication', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: false,
          local: { enabled: false },
          oidc: configuredOidc(true),
        }),
      ),
    ).not.toThrow();
  });

  it('accepts simultaneous Local and OIDC production authentication', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: false,
          local: { enabled: true },
          oidc: configuredOidc(true),
        }),
      ),
    ).not.toThrow();
  });

  it('rejects production when neither Local nor OIDC is enabled', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: false,
          local: { enabled: false },
          oidc: { enabled: false },
        }),
      ),
    ).toThrowError(
      'At least one production authentication capability must be enabled',
    );
  });

  it('rejects standalone authentication in production', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: true,
          local: { enabled: false },
          oidc: { enabled: false },
        }),
      ),
    ).toThrowError(
      'Standalone authentication is not allowed in production',
    );
  });

  it('rejects OIDC without its public client configuration', () => {
    expect(() =>
      validateAuthenticationEnvironment(
        productionEnvironment({
          standalone: false,
          local: { enabled: false },
          oidc: { enabled: true },
        }),
      ),
    ).toThrowError(
      'OIDC authority must be configured when OIDC is enabled',
    );
  });

  it('accepts standalone for non-production demo environments', () => {
    const environment: AppEnvironment = {
      production: false,
      apiBaseUrl: '',
      backend: { mode: 'mock' },
      authentication: {
        standalone: true,
        local: { enabled: false },
        oidc: { enabled: false },
      },
    };

    expect(() =>
      validateAuthenticationEnvironment(environment),
    ).not.toThrow();
  });
});

function productionEnvironment(
  authentication: AuthenticationEnvironment,
): AppEnvironment {
  return {
    production: true,
    apiBaseUrl: '',
    backend: { mode: 'api' },
    authentication,
  };
}

function configuredOidc(
  enabled: boolean,
): AuthenticationEnvironment['oidc'] {
  return {
    enabled,
    authority: 'https://identity.sixpay.example',
    clientId: 'sixpay-connect-frontend',
    scope: 'openid profile email roles',
  };
}
