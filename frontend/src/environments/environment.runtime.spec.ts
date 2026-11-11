import { validateRuntimeEnvironment } from './environment.runtime';

const valid = {
  production: true,
  apiBaseUrl: '',
  backend: { mode: 'api' },
  authentication: {
    standalone: false,
    local: { enabled: false },
    oidc: { enabled: false },
    ldap: { enabled: true },
  },
};

describe('runtime configuration', () => {
  it('accepts an API-only LDAP configuration', () => {
    expect(validateRuntimeEnvironment(valid).authentication.ldap.enabled).toBe(true);
  });
  it('rejects missing configuration keys', () => {
    expect(() => validateRuntimeEnvironment({ ...valid, authentication: {} })).toThrow();
  });
  it('rejects mock mode in production', () => {
    expect(() => validateRuntimeEnvironment({ ...valid, backend: { mode: 'mock' } })).toThrow();
  });
  it('rejects missing authentication capabilities in production', () => {
    expect(() => validateRuntimeEnvironment({ ...valid, authentication: {
      standalone: false, local: { enabled: false }, oidc: { enabled: false }, ldap: { enabled: false },
    } })).toThrow();
  });
  it('rejects insecure API origins', () => {
    expect(() => validateRuntimeEnvironment({ ...valid, apiBaseUrl: 'http://example.invalid' })).toThrow();
  });
});
