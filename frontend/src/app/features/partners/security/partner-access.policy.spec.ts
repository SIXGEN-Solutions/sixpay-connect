import { TestBed } from '@angular/core/testing';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SixpayRole } from '../../../core/auth/authentication.model';
import { PartnerAccessPolicy } from './partner-access.policy';

describe('PartnerAccessPolicy', () => {
  let currentRoles: readonly SixpayRole[];
  let policy: PartnerAccessPolicy;

  beforeEach(() => {
    currentRoles = [];
    TestBed.configureTestingModule({
      providers: [
        PartnerAccessPolicy,
        {
          provide: AuthenticationService,
          useValue: {
            hasRole: (role: SixpayRole) => currentRoles.includes(role),
            hasAnyRole: (roles: readonly SixpayRole[]) =>
              roles.some((role) => currentRoles.includes(role)),
          },
        },
      ],
    });
    policy = TestBed.inject(PartnerAccessPolicy);
  });

  it('reserves administrative mutations for ADMIN', () => {
    currentRoles = ['ADMIN'];
    expect(policy.canCreate()).toBe(true);
    expect(policy.canConfigureThreshold()).toBe(true);
    expect(policy.canPerformLifecycleAction('suspend', 'ACTIVE')).toBe(true);
    expect(policy.canPerformLifecycleAction('reactivate', 'SUSPENDED')).toBe(true);
    expect(policy.canPerformLifecycleAction('approve', 'PENDING_VALIDATION')).toBe(false);
  });

  it('reserves validation decisions for MANAGER', () => {
    currentRoles = ['MANAGER'];
    expect(policy.canPerformLifecycleAction('approve', 'PENDING_VALIDATION')).toBe(true);
    expect(policy.canPerformLifecycleAction('reject', 'PENDING_VALIDATION')).toBe(true);
    expect(policy.canCreate()).toBe(false);
  });

  it('reserves audit access for AUDITOR', () => {
    currentRoles = ['AUDITOR'];
    expect(policy.canReadPartner()).toBe(true);
    expect(policy.canReadAudit()).toBe(true);
    expect(policy.canConfigureThreshold()).toBe(false);
  });

  it('limits PARTNER to its own status journey', () => {
    currentRoles = ['PARTNER'];
    expect(policy.canReadOwnStatus()).toBe(true);
    expect(policy.canReadPartner()).toBe(false);
    expect(policy.canAccessInternalWorkspace()).toBe(false);
  });

  it('combines role and aggregate status for lifecycle actions', () => {
    currentRoles = ['MANAGER'];
    expect(policy.canPerformLifecycleAction('approve', 'ACTIVE')).toBe(false);
    expect(policy.canPerformLifecycleAction('reject', 'REJECTED')).toBe(false);
  });
});
