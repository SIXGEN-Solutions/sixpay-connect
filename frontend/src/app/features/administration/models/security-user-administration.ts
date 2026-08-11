export type SecurityUserStatus = 'ACTIVE' | 'DISABLED';
export type AuthenticationIdentityType = 'LOCAL' | 'OIDC';

export interface SecurityUserSummary {
  readonly id: string;
  readonly username: string;
  readonly email: string | null;
  readonly status: SecurityUserStatus;
  readonly localEnabled: boolean;
  readonly oidcLinked: boolean;
  readonly lastAuthenticationAt: string | null;
}

export interface SecurityIdentityView {
  readonly id: string;
  readonly identityType: AuthenticationIdentityType;
  readonly provider: string;
  readonly providerSubject: string;
  readonly status: 'LINKED';
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface SecurityAuditView {
  readonly eventType: string;
  readonly actorSubject: string | null;
  readonly provider: string | null;
  readonly detail: string | null;
  readonly occurredAt: string;
}

export interface SecurityUserDetail {
  readonly id: string;
  readonly username: string;
  readonly email: string | null;
  readonly status: SecurityUserStatus;
  readonly localEnabled: boolean;
  readonly oidcLinked: boolean;
  readonly roles: readonly string[];
  readonly permissions: readonly string[];
  readonly identities: readonly SecurityIdentityView[];
  readonly recentAuthenticationEvents: readonly SecurityAuditView[];
}
