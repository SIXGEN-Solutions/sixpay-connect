import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from './authentication.service';

describe('AuthenticationService roles', () => {
  let authentication: AuthenticationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    authentication = TestBed.inject(AuthenticationService);
    authentication.clearSession();
  });

  it('normalizes direct and Spring Security roles', () => {
    authentication.setAccessToken(
      token({ sub: 'admin@sixpay', roles: ['ADMIN'], authorities: ['ROLE_AUDITOR'] }),
    );

    expect(authentication.subject()).toBe('admin@sixpay');
    expect(authentication.hasRole('ADMIN')).toBe(true);
    expect(authentication.hasRole('AUDITOR')).toBe(true);
  });

  it('reads roles from the standard realm access claim', () => {
    authentication.setAccessToken(token({ realm_access: { roles: ['auditor'] } }));

    expect(authentication.hasRole('AUDITOR')).toBe(true);
    expect(authentication.hasRole('ADMIN')).toBe(false);
  });

  function token(payload: object): string {
    const encoded = btoa(JSON.stringify(payload))
      .replaceAll('+', '-')
      .replaceAll('/', '_')
      .replaceAll('=', '');
    return `header.${encoded}.signature`;
  }
});
