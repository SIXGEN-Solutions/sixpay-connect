import { SixpayRole } from '../../../core/auth/authentication.model';

export type IdentityStatus = 'ACTIVE' | 'DISABLED' | 'PENDING';

export interface IdentityUser {
  readonly userId: string;
  readonly displayName: string;
  readonly subject: string;
  readonly type: 'USER' | 'SERVICE' | 'PARTNER';
  readonly roles: readonly SixpayRole[];
  readonly status: IdentityStatus;
  readonly lastLoginAt: Date | null;
}

export interface RoleDefinition {
  readonly role: SixpayRole;
  readonly description: string;
  readonly capabilities: readonly string[];
  readonly userCount: number;
}
