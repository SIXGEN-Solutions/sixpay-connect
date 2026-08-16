export const SIXPAY_ROLES = [
  'ADMIN',
  'MANAGER',
  'PARTNER',
  'AUDITOR',
] as const;

export type SixpayRole =
  (typeof SIXPAY_ROLES)[number];

export type ActiveAuthenticationMethod =
  'local' | 'oidc' | null;

export type BackendAuthenticationMethod =
  'LOCAL' | 'OIDC';

export interface AuthenticatedIdentity {
  readonly subject: string;
  readonly roles: ReadonlySet<SixpayRole>;
  readonly permissions: ReadonlySet<string>;
}

export interface LocalLoginRequest {
  readonly username: string;
  readonly password: string;
}

export interface AuthenticationSessionResponse {
  /**
   * Added by DA-10.5. Optional during frontend/back-end rolling upgrades.
   * Authenticated session endpoints normally return true.
   */
  readonly authenticated?: boolean;

  readonly subject: string;
  readonly username: string;
  readonly roles: readonly string[];
  readonly permissions: readonly string[];
  readonly authenticationMethod:
    BackendAuthenticationMethod;

  /**
   * LOCAL only. OIDC lifecycle remains owned by the IdP.
   */
  readonly passwordChangeRequired?: boolean;
}

export type LocalSessionResponse =
  AuthenticationSessionResponse;

export interface JwtClaims {
  readonly exp?: number;
  readonly sub?: string;
  readonly roles?: readonly string[];
  readonly authorities?: readonly string[];
  readonly realm_access?: {
    readonly roles?: readonly string[];
  };
}

export function normalizeSixpayRoles(
  values: readonly string[],
): ReadonlySet<SixpayRole> {
  const supported =
    new Set<string>(SIXPAY_ROLES);

  return new Set(
    values
      .map((role) =>
        role
          .replace(/^ROLE_/, '')
          .toUpperCase(),
      )
      .filter(
        (role): role is SixpayRole =>
          supported.has(role),
      ),
  );
}

/**
 * @deprecated Production authorization comes from SIXPAY /api/v1/auth/me.
 * Retained only for standalone/backward compatibility.
 */
export function extractSixpayRoles(
  claims: JwtClaims | null,
): ReadonlySet<SixpayRole> {
  const values = [
    ...(claims?.roles ?? []),
    ...(claims?.authorities ?? []),
    ...(claims?.realm_access?.roles ?? []),
  ];

  return normalizeSixpayRoles(values);
}
