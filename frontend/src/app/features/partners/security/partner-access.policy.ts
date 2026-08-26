import { inject, Injectable } from '@angular/core';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SixpayRole } from '../../../core/auth/authentication.model';
import { PartnerStatus } from '../models/partners.response';

export type PartnerLifecycleAction = 'approve' | 'reject' | 'suspend' | 'reactivate';

const INTERNAL_ROLES: readonly SixpayRole[] = ['ADMIN', 'MANAGER', 'AUDITOR'];

@Injectable({ providedIn: 'root' })
export class PartnerAccessPolicy {
  private readonly authentication = inject(AuthenticationService);

  canAccessInternalWorkspace(): boolean {
    return this.authentication.hasAnyRole(INTERNAL_ROLES);
  }

  canReadOwnStatus(): boolean {
    return this.authentication.hasRole('PARTNER');
  }

  canCreate(): boolean {
    return this.authentication.hasRole('ADMIN');
  }

  canReadPartner(): boolean {
    return this.authentication.hasAnyRole(INTERNAL_ROLES);
  }

  canConfigureThreshold(): boolean {
    return this.authentication.hasRole('ADMIN');
  }

  canReadAudit(): boolean {
    return this.authentication.hasRole('AUDITOR');
  }

  canPerformLifecycleAction(action: PartnerLifecycleAction, status: PartnerStatus): boolean {
    if (action === 'approve' || action === 'reject') {
      return status === 'PENDING_VALIDATION' && this.authentication.hasRole('MANAGER');
    }
    if (action === 'suspend') {
      return status === 'ACTIVE' && this.authentication.hasRole('ADMIN');
    }
    return status === 'SUSPENDED' && this.authentication.hasRole('ADMIN');
  }

  canManageLifecycle(status: PartnerStatus): boolean {
    return (['approve', 'reject', 'suspend', 'reactivate'] as const).some((action) =>
      this.canPerformLifecycleAction(action, status),
    );
  }
}
