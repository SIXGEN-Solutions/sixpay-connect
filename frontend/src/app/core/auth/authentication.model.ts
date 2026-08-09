export const SIXPAY_ROLES = ['ADMIN', 'MANAGER', 'PARTNER', 'AUDITOR'] as const;

export type SixpayRole = (typeof SIXPAY_ROLES)[number];

export interface AuthenticatedIdentity {
  readonly subject: string;
  readonly roles: ReadonlySet<SixpayRole>;
}

export interface LocalLoginRequest {
  readonly username: string;
  readonly password: string;
}

export interface LocalSessionResponse {
  readonly subject: string;
  readonly username: string;
  readonly roles: readonly string[];
}

export interface JwtClaims {
  readonly exp?: number;
  readonly sub?: string;
  readonly roles?: readonly string[];
  readonly authorities?: readonly string[];
  readonly realm_access?: {
    readonly roles?: readonly string[];
  };
}

export function extractSixpayRoles(claims: JwtClaims | null): ReadonlySet<SixpayRole> {
  const values = [
    ...(claims?.roles ?? []),
    ...(claims?.authorities ?? []),
    ...(claims?.realm_access?.roles ?? []),
  ];
  const supported = new Set<string>(SIXPAY_ROLES);

  return new Set(
    values
      .map((role) => role.replace(/^ROLE_/, '').toUpperCase())
      .filter((role): role is SixpayRole => supported.has(role)),
  );
}
