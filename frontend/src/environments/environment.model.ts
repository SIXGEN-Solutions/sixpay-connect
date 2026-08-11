export type BackendMode = 'mock' | 'api';

export interface AuthenticationEnvironment {
  readonly standalone: boolean;
  readonly local: {
    readonly enabled: boolean;
  };
  readonly oidc: {
    readonly enabled: boolean;
    readonly authority?: string;
    readonly clientId?: string;
    readonly scope?: string;
  };
  readonly standaloneUser?: {
    readonly subject: string;
    readonly roles: readonly string[];
  };
  readonly standalonePartner?: {
    readonly subject: string;
  };
}

export interface BackendEnvironment {
  readonly mode: BackendMode;
}

export interface AppEnvironment {
  readonly production: boolean;
  readonly apiBaseUrl: string;
  readonly backend: BackendEnvironment;
  readonly authentication: AuthenticationEnvironment;
}
