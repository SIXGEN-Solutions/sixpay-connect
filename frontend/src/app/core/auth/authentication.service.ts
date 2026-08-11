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
  ActiveAuthenticationMethod,
  AuthenticatedIdentity,
  AuthenticationSessionResponse,
  LocalLoginRequest,
  normalizeSixpayRoles,
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
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);

  private readonly oidc = inject(
    OidcSecurityService,
    { optional: true },
  );

  private readonly localClient =
    inject(LocalAuthenticationClient);

  private readonly errorService =
    inject(ErrorService);

  private readonly identityState =
    signal<AuthenticatedIdentity | null>(null);

  private readonly usernameState =
    signal<string | null>(null);

  private readonly activeAuthenticationMethodState =
    signal<ActiveAuthenticationMethod>(null);

  private readonly readyState =
    new ReplaySubject<boolean>(1);

  readonly identity =
    this.identityState.asReadonly();

  readonly username =
    this.usernameState.asReadonly();

  readonly activeAuthenticationMethod =
    this.activeAuthenticationMethodState.asReadonly();

  readonly isAuthenticated = computed(
    () => this.identityState() !== null,
  );

  readonly subject = computed(
    () => this.identityState()?.subject ?? null,
  );

  readonly roles = computed(
    () =>
      this.identityState()?.roles ??
      new Set<SixpayRole>(),
  );

  readonly permissions = computed(
    () =>
      this.identityState()?.permissions ??
      new Set<string>(),
  );

  readonly ready$ =
    this.readyState.asObservable();

  readonly isStandaloneMode =
    authenticationEnvironment.standalone;

  readonly localEnabled =
    authenticationEnvironment.local.enabled;

  readonly oidcEnabled =
    authenticationEnvironment.oidc.enabled;

  /**
   * Transitional aliases kept to avoid breaking callers outside DA-7.
   * New UI/service code must use localEnabled / oidcEnabled.
   */
  readonly isLocalEnabled = this.localEnabled;
  readonly isOidcEnabled = this.oidcEnabled;
  readonly isLocalMode = this.localEnabled;
  readonly isOidcMode = this.oidcEnabled;

  constructor() {
    if (this.isStandaloneMode) {
      this.initializeStandaloneIdentity();
      return;
    }

    this.initializeAvailableSession();
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

  hasPermission(
    permission: string,
  ): boolean {
    return this.permissions().has(permission);
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
        this.standaloneSubjectForRole(role),
      roles: new Set<SixpayRole>([role]),
      permissions: new Set<string>(),
    });
  }

  loginLocal(
    request: LocalLoginRequest,
    returnUrl = '/',
  ): Observable<void> {
    if (!this.localEnabled) {
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
          this.activeAuthenticationMethodState.set('local');
          this.setCanonicalSession(session);
        }),
        tap(() =>
          this.completeLoginNavigation(),
        ),
        map(() => undefined),
      );
  }

  loginOidc(
    returnUrl = '/',
  ): void {
    if (!this.oidcEnabled) {
      return;
    }

    this.storage?.setItem(
      RETURN_URL_STORAGE_KEY,
      this.safeReturnUrl(returnUrl),
    );

    this.oidc?.authorize();
  }

  /**
   * Compatibility alias for existing callers.
   */
  login(
    returnUrl = '/',
  ): void {
    this.loginOidc(returnUrl);
  }

  completeLoginNavigation(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const returnUrl = this.safeReturnUrl(
      this.storage?.getItem(
        RETURN_URL_STORAGE_KEY,
      ) ?? '/',
    );

    this.storage?.removeItem(
      RETURN_URL_STORAGE_KEY,
    );

    void this.router.navigateByUrl(returnUrl);
  }

  logout(): void {
    this.storage?.removeItem(
      RETURN_URL_STORAGE_KEY,
    );

    switch (this.activeAuthenticationMethodState()) {
      case 'oidc':
        this.logoutOidc();
        return;

      case 'local':
        this.logoutLocal();
        return;

      default:
        this.errorService.clear();
        this.clearSession();
        void this.router.navigate(['/login']);
    }
  }

  expireSession(): void {
    const activeMethod =
      this.activeAuthenticationMethodState();

    this.errorService.clear();
    this.clearSession();

    if (activeMethod === 'oidc') {
      this.oidc?.logoffLocal();
    }
  }

  accessTokenForRequest():
    Observable<string | null> {
    if (
      this.activeAuthenticationMethodState() === 'oidc' &&
      this.oidc
    ) {
      return this.oidc.getAccessToken();
    }

    return of(null);
  }

  private initializeAvailableSession(): void {
    if (this.oidcEnabled) {
      this.initializeOidcSession();
      return;
    }

    if (this.localEnabled) {
      this.restoreCanonicalSession('local');
      return;
    }

    this.readyState.next(true);
  }

  private initializeStandaloneIdentity(): void {
    const localUser =
      authenticationEnvironment.standaloneUser;

    const storedRole =
      this.storage?.getItem(
        STANDALONE_ROLE_STORAGE_KEY,
      ) as SixpayRole | null;

    const roles = storedRole
      ? new Set<SixpayRole>([storedRole])
      : normalizeSixpayRoles(
          localUser?.roles ?? [],
        );

    const effectiveRole =
      storedRole ??
      roles.values().next().value ??
      null;

    this.identityState.set({
      subject:
        this.standaloneSubjectForRole(
          effectiveRole,
        ),
      roles,
      permissions: new Set<string>(),
    });

    this.usernameState.set(null);
    this.activeAuthenticationMethodState.set(null);
    this.readyState.next(true);
  }

  private initializeOidcSession(): void {
    if (!this.oidc) {
      this.fallbackToLocalOrReady();
      return;
    }

    this.oidc
      .checkAuth()
      .subscribe({
        next: (response) => {
          if (response.isAuthenticated) {
            this.activeAuthenticationMethodState.set('oidc');
            this.restoreCanonicalSession('oidc');
            return;
          }

          this.fallbackToLocalOrReady();
        },
        error: () => {
          this.fallbackToLocalOrReady();
        },
      });
  }

  private fallbackToLocalOrReady(): void {
    this.clearSession();

    if (this.localEnabled) {
      this.restoreCanonicalSession('local');
      return;
    }

    this.readyState.next(true);
  }

  private restoreCanonicalSession(
    method: Exclude<ActiveAuthenticationMethod, null>,
  ): void {
    /*
     * Set the candidate method before /me so the request interceptor knows
     * whether it should attach an OIDC bearer token.
     */
    this.activeAuthenticationMethodState.set(method);

    this.localClient
      .currentUser()
      .subscribe({
        next: (session) => {
          this.errorService.clear();
          this.setCanonicalSession(session);
          this.readyState.next(true);
        },
        error: () => {
          this.clearSession();
          this.readyState.next(true);
        },
      });
  }

  private setCanonicalSession(
    session: AuthenticationSessionResponse,
  ): void {
    this.identityState.set({
      subject: session.subject,
      roles: normalizeSixpayRoles(
        session.roles,
      ),
      permissions: new Set(
        session.permissions,
      ),
    });

    this.usernameState.set(
      session.username,
    );
  }

  private logoutLocal(): void {
    this.localClient
      .logout()
      .pipe(
        catchError(() => of(undefined)),
        finalize(() => {
          this.errorService.clear();
          this.clearSession();
          void this.router.navigate(['/login']);
        }),
      )
      .subscribe();
  }

  private logoutOidc(): void {
    this.errorService.clear();
    this.clearSession();

    if (!this.oidc) {
      void this.router.navigate(['/login']);
      return;
    }

    this.oidc
      .logoffAndRevokeTokens()
      .subscribe({
        error: () =>
          this.oidc?.logoffLocal(),
      });
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

  private clearSession(): void {
    this.identityState.set(null);
    this.usernameState.set(null);
    this.activeAuthenticationMethodState.set(null);
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
}
