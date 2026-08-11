import {
  AppEnvironment,
  AuthenticationEnvironment,
} from './environment.model';

export function validateAuthenticationEnvironment(
  environment: AppEnvironment,
): void {
  const authentication = environment.authentication;

  if (environment.production && authentication.standalone) {
    throw new Error(
      'Standalone authentication is not allowed in production',
    );
  }

  if (
    environment.production &&
    !authentication.local.enabled &&
    !authentication.oidc.enabled
  ) {
    throw new Error(
      'At least one production authentication capability must be enabled',
    );
  }

  if (
    authentication.standalone &&
    (authentication.local.enabled || authentication.oidc.enabled)
  ) {
    throw new Error(
      'Standalone authentication cannot be combined with Local or OIDC authentication',
    );
  }

  if (authentication.oidc.enabled) {
    requireNonBlank(
      authentication.oidc.authority,
      'OIDC authority must be configured when OIDC is enabled',
    );
    requireNonBlank(
      authentication.oidc.clientId,
      'OIDC clientId must be configured when OIDC is enabled',
    );
    requireNonBlank(
      authentication.oidc.scope,
      'OIDC scope must be configured when OIDC is enabled',
    );
  }
}

export function isLocalAuthenticationEnabled(
  authentication: AuthenticationEnvironment,
): boolean {
  return authentication.local.enabled;
}

export function isOidcAuthenticationEnabled(
  authentication: AuthenticationEnvironment,
): boolean {
  return authentication.oidc.enabled;
}

function requireNonBlank(
  value: string | undefined,
  message: string,
): void {
  if (!value || value.trim().length === 0) {
    throw new Error(message);
  }
}
