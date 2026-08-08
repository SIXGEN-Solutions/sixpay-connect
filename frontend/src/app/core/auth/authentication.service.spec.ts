import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { extractSixpayRoles } from './authentication.model';
import { AuthenticationService } from './authentication.service';

describe('AuthenticationService', () => {
  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('initializes the explicitly configured standalone identity', () => {
    const authentication = TestBed.inject(AuthenticationService);

    expect(authentication.isAuthenticated()).toBe(true);
    expect(authentication.subject()).toBe('local-security-user');
    expect(
      authentication.hasAnyRole(['ADMIN', 'MANAGER', 'AUDITOR']),
    ).toBe(true);
    expect(authentication.hasRole('PARTNER')).toBe(false);
  });

  it('uses the configured standalone Partner identity when PARTNER is simulated', () => {
    const authentication = TestBed.inject(AuthenticationService);

    authentication.simulateStandaloneRole('PARTNER');

    expect(authentication.hasRole('PARTNER')).toBe(true);
    expect(authentication.subject()).toBe(
      '11111111-1111-4111-8111-111111111111',
    );
  });

  it('switches back to the configured standalone user for internal roles', () => {
    const authentication = TestBed.inject(AuthenticationService);

    authentication.simulateStandaloneRole('PARTNER');
    authentication.simulateStandaloneRole('MANAGER');

    expect(authentication.hasRole('MANAGER')).toBe(true);
    expect(authentication.subject()).toBe('local-security-user');
  });

  it('normalizes supported roles without retaining unrelated authorities', () => {
    const roles = extractSixpayRoles({
      roles: ['admin'],
      authorities: ['ROLE_AUDITOR', 'SCOPE_partner.read'],
      realm_access: { roles: ['manager'] },
    });

    expect([...roles]).toEqual(['ADMIN', 'AUDITOR', 'MANAGER']);
  });
});
