import { DOCUMENT } from '@angular/common';
import {
  computed,
  inject,
  Injectable,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import {
  catchError,
  finalize,
  map,
  Observable,
  of,
  ReplaySubject,
  tap,
  throwError,
} from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthenticationEnvironment } from '../../../environments/environment.model';
import { ErrorService } from '../errors/error.service';
import {
  AuthenticatedIdentity,
  extractSixpayRoles,
  JwtClaims,
  LocalLoginRequest,
  LocalSessionResponse,
  SixpayRole,
} from './authentication.model';
import { LocalAuthenticationClient } from './local-authentication.client';

const RETURN_URL_STORAGE_KEY =
  'sixpay.authentication.return-url';

const STANDALONE_ROLE_STORAGE_KEY =
  'sixpay.authentication.standalone-role';

const FALLBACK_STANDALONE_PARTNER_SUBJECT =
  '11111111-1111-4111-8111-111111111111';

const authenticationEnvironment:
  AuthenticationEnvironment =
    environment.authentication;

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private readonly document =
    inject(DOCUMENT);

  private readonly router =
    inject(Router);

  private readonly oidc =
    inject(
      OidcSecurityService,
      { optional: true },
    );

  private readonly localClient =
    inject(LocalAuthenticationClient);

  private readonly errorService =
    inject(ErrorService);

  private readonly identityState =
    signal<AuthenticatedIdentity | null>(
      null,
    );

  private readonly usernameState =
    signal<string | null>(null);

  private readonly readyState =
    new ReplaySubject<boolean>(1);

  readonly identity =
    this.identityState.asReadonly();

  readonly username =
    this.usernameState.asReadonly();

  readonly isAuthenticated =
    computed(
      () =>
        this.identityState() !== null,
    );

  readonly subject = computed(
    () =>
      this.identityState()?.subject ??
      null,
  );

  readonly roles = computed(
    () =>
      this.identityState()?.roles ??
      new Set<SixpayRole>(),
  );

  readonly ready$ =
    this.readyState.asObservable();

  readonly isStandaloneMode =
    authenticationEnvironment.standalone;

  readonly isLocalEnabled =
    authenticationEnvironment.local.enabled;

  readonly isOidcEnabled =
    authenticationEnvironment.oidc.enabled;

  /**
   * Compatibility aliases retained until DA-7 updates the login presentation.
   * They now represent enabled capabilities rather than mutually exclusive
   * authentication modes.
   */
  readonly isLocalMode =
    this.isLocalEnabled;

  readonly isOidcMode =
    this.isOidcEnabled;

  constructor() {
    if (this.isStandaloneMode) {
      this.initializeStandaloneIdentity();
      return;
    }

    /*
     * DA-2 changes configuration semantics only.
     *
     * In hybrid mode OIDC remains the bootstrap strategy so the existing
     * production OIDC behavior is preserved. DA-8 will replace this temporary
     * bootstrap precedence with normalized Local/OIDC session discovery.
     */
    if (this.isOidcEnabled) {
      this.initializeOidcSession();
      return;
    }

    if (this.isLocalEnabled) {
      this.restoreLocalSession();
      return;
    }

    this.readyState.next(true);
  }

  hasRole(
    role: SixpayRole | string,
  ): boolean {
    return this.roles().has(
      role
        .replace(/^ROLE_/, '')
        .toUpperCase() as SixpayRole,
    );
  }

  hasAnyRole(
    roles: readonly SixpayRole[],
  ): boolean {
    return roles.some((role) =>
      this.hasRole(role),
    );
  }

  simulateStandaloneRole(
    role: SixpayRole,
  ): void {
    if (!this.isStandaloneMode) {
      return;
    }

    this.storage?.setItem(
      STANDALONE_ROLE_STORAGE_KEY,
      role,
    );

    this.identityState.set({
      subject:
        this.standaloneSubjectForRole(
          role,
        ),
      roles: new Set<SixpayRole>([
        role,
      ]),
    });
  }

  loginLocal(
    request: LocalLoginRequest,
    returnUrl = '/',
  ): Observable<void> {
    if (!this.isLocalEnabled) {
      return throwError(
        () =>
          new Error(
            'Local authentication is not enabled',
          ),
      );
    }

    this.storage?.setItem(
      RETURN_URL_STORAGE_KEY,
      this.safeReturnUrl(returnUrl),
    );

    return this.localClient
      .login(request)
      .pipe(
        tap((session) => {
          this.errorService.clear();
          this.setLocalSession(
            session,
          );
        }),
        tap(() =>
          this.completeLoginNavigation(),
        ),
        map(() => undefined),
      );
  }

  login(
    returnUrl = '/',
  ): void {
    if (!this.isOidcEnabled) {
      return;
    }

    this.storage?.setItem(
      RETURN_URL_STORAGE_KEY,
      this.safeReturnUrl(returnUrl),
    );

    this.oidc?.authorize();
  }

  completeLoginNavigation(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const returnUrl =
      this.safeReturnUrl(
        this.storage?.getItem(
          RETURN_URL_STORAGE_KEY,
        ) ?? '/',
      );

    this.storage?.removeItem(
      RETURN_URL_STORAGE_KEY,
    );

    void this.router.navigateByUrl(
      returnUrl,
    );
  }

  logout(): void {
    this.storage?.removeItem(
      RETURN_URL_STORAGE_KEY,
    );

    /*
     * DA-8 will make logout depend on the active normalized session instead of
     * configuration precedence. Until then, preserve the previous OIDC-first
     * production behavior when both capabilities are enabled.
     */
    if (
      this.isOidcEnabled &&
      this.oidc
    ) {
      this.errorService.clear();
      this.clearLocalSession();

      this.oidc
        .logoffAndRevokeTokens()
        .subscribe({
          error: () =>
            this.oidc?.logoffLocal(),
        });

      return;
    }

    if (this.isLocalEnabled) {
      this.localClient
        .logout()
        .pipe(
          catchError(() =>
            of(undefined),
          ),
          finalize(() => {
            this.errorService.clear();
            this.clearLocalSession();

            void this.router.navigate([
              '/login',
            ]);
          }),
        )
        .subscribe();

      return;
    }

    this.errorService.clear();
    this.clearLocalSession();

    void this.router.navigate([
      '/login',
    ]);
  }

  expireSession(): void {
    this.errorService.clear();
    this.clearLocalSession();

    if (this.isOidcEnabled) {
      this.oidc?.logoffLocal();
    }
  }

  accessTokenForRequest():
    Observable<string | null> {
    if (
      this.isOidcEnabled &&
      this.oidc
    ) {
      return this.oidc.getAccessToken();
    }

    return of(null);
  }

  private initializeStandaloneIdentity():
    void {
    const localUser =
      authenticationEnvironment
        .standaloneUser;

    const storedRole =
      this.storage?.getItem(
        STANDALONE_ROLE_STORAGE_KEY,
      ) as SixpayRole | null;

    const roles = storedRole
      ? new Set<SixpayRole>([
          storedRole,
        ])
      : extractSixpayRoles({
          roles:
            localUser?.roles ?? [],
        });

    const effectiveRole =
      storedRole ??
      this.firstConfiguredStandaloneRole(
        localUser?.roles ?? [],
      );

    this.identityState.set({
      subject:
        this.standaloneSubjectForRole(
          effectiveRole,
        ),
      roles,
    });

    this.usernameState.set(null);

    this.readyState.next(true);
  }

  private restoreLocalSession(): void {
    this.localClient
      .currentUser()
      .subscribe({
        next: (session) => {
          this.errorService.clear();

          this.setLocalSession(
            session,
          );

          this.readyState.next(
            true,
          );
        },
        error: () => {
          this.clearLocalSession();

          this.readyState.next(
            true,
          );
        },
      });
  }

  private initializeOidcSession():
    void {
    if (!this.oidc) {
      this.readyState.next(true);
      return;
    }

    this.oidc
      .checkAuth()
      .subscribe({
        next: (response) => {
          if (
            response.isAuthenticated
          ) {
            this.errorService.clear();

            this.setAuthenticatedSession(
              response.accessToken,
              response.userData as JwtClaims,
            );
          } else {
            this.clearLocalSession();
          }

          this.readyState.next(
            true,
          );
        },
        error: () => {
          this.clearLocalSession();

          this.readyState.next(
            true,
          );
        },
      });
  }

  private setLocalSession(
    session: LocalSessionResponse,
  ): void {
    this.identityState.set({
      subject: session.subject,
      roles: extractSixpayRoles({
        roles: session.roles,
      }),
    });

    this.usernameState.set(
      session.username,
    );
  }

  private standaloneSubjectForRole(
    role: SixpayRole | null,
  ): string {
    if (role === 'PARTNER') {
      return (
        authenticationEnvironment
          .standalonePartner
          ?.subject ??
        FALLBACK_STANDALONE_PARTNER_SUBJECT
      );
    }

    return (
      authenticationEnvironment
        .standaloneUser
        ?.subject ??
      'local-user'
    );
  }

  private firstConfiguredStandaloneRole(
    roles: readonly string[],
  ): SixpayRole | null {
    return (
      extractSixpayRoles({
        roles,
      })
        .values()
        .next()
        .value ?? null
    );
  }

  private setAuthenticatedSession(
    accessToken: string,
    userData: JwtClaims | null,
  ): void {
    const claims =
      this.decodePayload(
        accessToken,
      ) ??
      userData ??
      {};

    const subject =
      claims.sub ??
      userData?.sub;

    if (
      !subject ||
      this.isExpired(claims)
    ) {
      this.clearLocalSession();
      return;
    }

    this.identityState.set({
      subject,
      roles:
        extractSixpayRoles(
          claims,
        ),
    });

    this.usernameState.set(null);
  }

  private clearLocalSession(): void {
    this.identityState.set(null);
    this.usernameState.set(null);
  }

  private get storage():
    Storage | undefined {
    return this.document
      .defaultView
      ?.sessionStorage;
  }

  private safeReturnUrl(
    returnUrl: string,
  ): string {
    return (
      returnUrl.startsWith('/') &&
      !returnUrl.startsWith('//')
    )
      ? returnUrl
      : '/';
  }

  private isExpired(
    claims: JwtClaims,
  ): boolean {
    return (
      claims.exp !== undefined &&
      claims.exp * 1000 <=
        Date.now()
    );
  }

  private decodePayload(
    token: string,
  ): JwtClaims | null {
    try {
      const encodedPayload =
        token.split('.')[1];

      if (!encodedPayload) {
        return null;
      }

      const normalizedPayload =
        encodedPayload
          .replace(/-/g, '+')
          .replace(/_/g, '/')
          .padEnd(
            Math.ceil(
              encodedPayload.length /
                4,
            ) * 4,
            '=',
          );

      return JSON.parse(
        atob(normalizedPayload),
      ) as JwtClaims;
    } catch {
      return null;
    }
  }
}
