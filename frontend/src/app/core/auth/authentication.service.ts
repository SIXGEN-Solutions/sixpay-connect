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
  switchMap,
  take,
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
  LocalPasswordChangeRequest,
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

  private readonly authenticationClient =
    inject(LocalAuthenticationClient);

  private readonly errorService =
    inject(ErrorService);

  private readonly identityState =
    signal<AuthenticatedIdentity | null>(null);

  private readonly usernameState =
    signal<string | null>(null);

  private readonly activeAuthenticationMethodState =
    signal<ActiveAuthenticationMethod>(null);

  private readonly passwordChangeRequiredState =
    signal(false);

  private readonly readyState =
    new ReplaySubject<boolean>(1);

  readonly identity = this.identityState.asReadonly();
  readonly username = this.usernameState.asReadonly();
  readonly activeAuthenticationMethod =
    this.activeAuthenticationMethodState.asReadonly();
  readonly passwordChangeRequired =
    this.passwordChangeRequiredState.asReadonly();

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

  readonly ready$ = this.readyState.asObservable();

  readonly isStandaloneMode =
    authenticationEnvironment.standalone;

  readonly localEnabled =
    authenticationEnvironment.local.enabled;

  readonly oidcEnabled =
    authenticationEnvironment.oidc.enabled;

  readonly isLocalEnabled = this.localEnabled;
  readonly isOidcEnabled = this.oidcEnabled;
  readonly isLocalMode = this.localEnabled;
  readonly isOidcMode = this.oidcEnabled;

  constructor() {
    if (this.isStandaloneMode) {
      this.initializeStandaloneIdentity();
      return;
    }

    this.initializeAuthentication();
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
    return roles.some((role) => this.hasRole(role));
  }

  hasPermission(permission: string): boolean {
    return this.permissions().has(permission);
  }

  simulateStandaloneRole(role: SixpayRole): void {
    if (!this.isStandaloneMode) {
      return;
    }

    this.storage?.setItem(
      STANDALONE_ROLE_STORAGE_KEY,
      role,
    );

    this.identityState.set({
      subject: this.standaloneSubjectForRole(role),
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
        () => new Error('Local authentication is not enabled'),
      );
    }

    this.storage?.setItem(
      RETURN_URL_STORAGE_KEY,
      this.safeReturnUrl(returnUrl),
    );

    return this.authenticationClient
      .login(request)
      .pipe(
        tap((session) => {
          this.errorService.clear();
          this.setCanonicalSession(session);
        }),
        tap(() => this.completeLoginNavigation()),
        map(() => undefined),
      );
  }

  /**
   * DA-10.6 user-owned LOCAL password change.
   *
   * The backend promotes the restricted session after the change. SIXPAY then
   * reloads /auth/me so the frontend state remains backend-authoritative before
   * leaving the mandatory password-change route.
   */
  changeLocalPassword(
    request: LocalPasswordChangeRequest,
  ): Observable<void> {
    if (
      this.activeAuthenticationMethodState() !== 'local'
    ) {
      return throwError(
        () =>
          new Error(
            'LOCAL authentication is required to change a LOCAL password',
          ),
      );
    }

    return this.authenticationClient
      .changePassword(request)
      .pipe(
        switchMap(() =>
          this.authenticationClient.currentUser(),
        ),
        tap((session) => {
          this.errorService.clear();
          this.setCanonicalSession(session);
        }),
        tap(() => this.completePasswordChangeNavigation()),
        map(() => undefined),
      );
  }

  loginOidc(returnUrl = '/'): void {
    if (!this.oidcEnabled) {
      return;
    }

    this.storage?.setItem(
      RETURN_URL_STORAGE_KEY,
      this.safeReturnUrl(returnUrl),
    );

    this.oidc?.authorize();
  }

  login(returnUrl = '/'): void {
    this.loginOidc(returnUrl);
  }

  completeLoginNavigation(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    /*
     * Do not consume the requested business return URL yet. The user must
     * complete the LOCAL lifecycle first, then DA-10.6 returns there.
     */
    if (
      this.activeAuthenticationMethodState() === 'local' &&
      this.passwordChangeRequiredState()
    ) {
      void this.router.navigate(['/change-password']);
      return;
    }

    this.navigateToStoredReturnUrl();
  }

  /**
   * DA-8 always terminates the backend SIXPAY session first. If that backend
   * session originated from OIDC, the IdP/browser session is then revoked.
   */
  logout(): void {
    this.storage?.removeItem(RETURN_URL_STORAGE_KEY);

    const authenticationMethod =
      this.activeAuthenticationMethodState();

    if (!this.isAuthenticated()) {
      this.finishFrontendLogout(authenticationMethod);
      return;
    }

    this.authenticationClient
      .logout()
      .pipe(
        catchError(() => of(undefined)),
        finalize(() =>
          this.finishFrontendLogout(authenticationMethod),
        ),
      )
      .subscribe();
  }

  expireSession(): void {
    const authenticationMethod =
      this.activeAuthenticationMethodState();

    this.errorService.clear();
    this.clearSession();

    if (authenticationMethod === 'oidc') {
      this.oidc?.logoffLocal();
    }
  }

  /**
   * Bootstrap priority:
   * 1. existing unified SIXPAY backend session;
   * 2. existing/callback OIDC session, exchanged once for a backend session;
   * 3. anonymous.
   */
  private initializeAuthentication(): void {
    if (!this.localEnabled && !this.oidcEnabled) {
      this.readyState.next(true);
      return;
    }

    this.tryExistingBackendSession();
  }

  private tryExistingBackendSession(): void {
    this.clearSession();

    this.authenticationClient
      .currentUser()
      .subscribe({
        next: (session) => {
          this.errorService.clear();
          this.setCanonicalSession(session);
          this.readyState.next(true);
        },
        error: () => {
          this.tryExistingOidcSession();
        },
      });
  }

  private tryExistingOidcSession(): void {
    if (!this.oidcEnabled || !this.oidc) {
      this.resolveAnonymousState();
      return;
    }

    this.oidc
      .checkAuth()
      .pipe(take(1))
      .subscribe({
        next: (response) => {
          if (!response.isAuthenticated) {
            this.resolveAnonymousState();
            return;
          }

          this.exchangeOidcForBackendSession();
        },
        error: () => {
          this.resolveAnonymousState();
        },
      });
  }

  private exchangeOidcForBackendSession(): void {
    if (!this.oidc) {
      this.resolveAnonymousState();
      return;
    }

    this.oidc
      .getAccessToken()
      .pipe(
        take(1),
        switchMap((accessToken) => {
          if (!accessToken) {
            return throwError(
              () => new Error('OIDC access token is unavailable'),
            );
          }

          return this.authenticationClient
            .establishOidcSession(accessToken);
        }),
      )
      .subscribe({
        next: (session) => {
          this.errorService.clear();
          this.setCanonicalSession(session);
          this.readyState.next(true);
        },
        error: () => {
          this.oidc?.logoffLocal();
          this.resolveAnonymousState();
        },
      });
  }

  private resolveAnonymousState(): void {
    this.clearSession();
    this.readyState.next(true);
  }

  private setCanonicalSession(
    session: AuthenticationSessionResponse,
  ): void {
    this.identityState.set({
      subject: session.subject,
      roles: normalizeSixpayRoles(session.roles),
      permissions: new Set(session.permissions),
    });

    this.usernameState.set(session.username);

    const authenticationMethod =
      session.authenticationMethod.toLowerCase() as Exclude<
        ActiveAuthenticationMethod,
        null
      >;

    this.activeAuthenticationMethodState.set(
      authenticationMethod,
    );

    this.passwordChangeRequiredState.set(
      authenticationMethod === 'local' &&
        (session.passwordChangeRequired ?? false),
    );
  }

  private completePasswordChangeNavigation(): void {
    if (this.passwordChangeRequiredState()) {
      return;
    }

    this.navigateToStoredReturnUrl();
  }

  private navigateToStoredReturnUrl(): void {
    const returnUrl = this.safeReturnUrl(
      this.storage?.getItem(RETURN_URL_STORAGE_KEY) ?? '/',
    );

    this.storage?.removeItem(RETURN_URL_STORAGE_KEY);
    void this.router.navigateByUrl(returnUrl);
  }

  private finishFrontendLogout(
    authenticationMethod: ActiveAuthenticationMethod,
  ): void {
    this.errorService.clear();
    this.clearSession();

    if (authenticationMethod === 'oidc' && this.oidc) {
      this.oidc
        .logoffAndRevokeTokens()
        .subscribe({
          error: () => this.oidc?.logoffLocal(),
        });
      return;
    }

    void this.router.navigate(['/login']);
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
      : normalizeSixpayRoles(localUser?.roles ?? []);

    const effectiveRole =
      storedRole ?? roles.values().next().value ?? null;

    this.identityState.set({
      subject: this.standaloneSubjectForRole(effectiveRole),
      roles,
      permissions: new Set<string>(),
    });

    this.usernameState.set(null);
    this.activeAuthenticationMethodState.set(null);
    this.passwordChangeRequiredState.set(false);
    this.readyState.next(true);
  }

  private standaloneSubjectForRole(
    role: SixpayRole | null,
  ): string {
    if (role === 'PARTNER') {
      return (
        authenticationEnvironment.standalonePartner?.subject ??
        FALLBACK_STANDALONE_PARTNER_SUBJECT
      );
    }

    return (
      authenticationEnvironment.standaloneUser?.subject ??
      'local-user'
    );
  }

  private clearSession(): void {
    this.identityState.set(null);
    this.usernameState.set(null);
    this.activeAuthenticationMethodState.set(null);
    this.passwordChangeRequiredState.set(false);
  }

  private get storage(): Storage | undefined {
    return this.document.defaultView?.sessionStorage;
  }

  private safeReturnUrl(returnUrl: string): string {
    return (
      returnUrl.startsWith('/') &&
      !returnUrl.startsWith('//')
    )
      ? returnUrl
      : '/';
  }
}
