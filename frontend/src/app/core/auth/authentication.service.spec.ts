import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { extractSixpayRoles } from './authentication.model';
import { AuthenticationService } from './authentication.service';

describe('AuthenticationService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([])] });
  });

  it('initializes the explicitly configured standalone identity', () => {
    const authentication = TestBed.inject(AuthenticationService);

    expect(authentication.isAuthenticated()).toBe(true);
    expect(authentication.subject()).toBe('local-security-user');
    expect(authentication.hasAnyRole(['ADMIN', 'MANAGER', 'AUDITOR'])).toBe(true);
    expect(authentication.hasRole('PARTNER')).toBe(false);
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
