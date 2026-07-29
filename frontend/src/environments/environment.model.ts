export type AuthenticationMode = 'oidc' | 'standalone';

export interface AuthenticationEnvironment {
  readonly mode: AuthenticationMode;
  readonly authority: string;
  readonly clientId: string;
  readonly scope: string;
  readonly standaloneUser?: {
    readonly subject: string;
    readonly roles: readonly string[];
  };
}

export interface AppEnvironment {
  readonly production: boolean;
  readonly apiBaseUrl: string;
  readonly authentication: AuthenticationEnvironment;
}
