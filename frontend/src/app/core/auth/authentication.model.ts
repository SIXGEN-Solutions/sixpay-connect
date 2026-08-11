export const SIXPAY_ROLES = ['ADMIN', 'MANAGER', 'PARTNER', 'AUDITOR'] as const;

export type SixpayRole = (typeof SIXPAY_ROLES)[number];

export interface AuthenticatedIdentity {
  readonly subject: string;
  readonly roles: ReadonlySet<SixpayRole>;
  readonly permissions: ReadonlySet<string>;
}

export interface LocalLoginRequest {
  readonly username: string;
  readonly password: string;
}

/**
 * Mechanism-neutral session returned by SIXPAY.
 *
 * Local and OIDC sessions use exactly this same authorization representation.
 */
export interface AuthenticationSessionResponse {
  readonly subject: string;
  readonly username: string;
  readonly roles: readonly string[];
  readonly permissions: readonly string[];
}

/**
 * Compatibility alias for existing Local client/tests.
 */
export type LocalSessionResponse = AuthenticationSessionResponse;

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
  const supported = new Set<string>(SIXPAY_ROLES);

  return new Set(
    values
      .map((role) => role.replace(/^ROLE_/, '').toUpperCase())
      .filter((role): role is SixpayRole => supported.has(role)),
  );
}

/**
 * @deprecated Production OIDC authorization must come from SIXPAY /auth/me,
 * never from JWT claims. Kept only for standalone/backward test compatibility.
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
