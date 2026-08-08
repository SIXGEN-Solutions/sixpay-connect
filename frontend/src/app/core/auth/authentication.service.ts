import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';
import { Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Observable, of, ReplaySubject } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuthenticationEnvironment } from '../../../environments/environment.model';
import {
  AuthenticatedIdentity,
  extractSixpayRoles,
  JwtClaims,
  SixpayRole,
} from './authentication.model';

const RETURN_URL_STORAGE_KEY = 'sixpay.authentication.return-url';
const STANDALONE_ROLE_STORAGE_KEY = 'sixpay.authentication.standalone-role';
const FALLBACK_STANDALONE_PARTNER_SUBJECT =
  '11111111-1111-4111-8111-111111111111';

const authenticationEnvironment: AuthenticationEnvironment =
  environment.authentication;

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private readonly document = inject(DOCUMENT);
  private readonly router = inject(Router);
  private readonly oidc = inject(OidcSecurityService, { optional: true });
  private readonly identityState = signal<AuthenticatedIdentity | null>(null);
  private readonly readyState = new ReplaySubject<boolean>(1);

  readonly identity = this.identityState.asReadonly();
  readonly isAuthenticated = computed(() => this.identityState() !== null);
  readonly subject = computed(() => this.identityState()?.subject ?? null);
  readonly roles = computed(
    () => this.identityState()?.roles ?? new Set<SixpayRole>(),
  );
  readonly ready$ = this.readyState.asObservable();
  readonly isStandaloneMode = authenticationEnvironment.mode === 'standalone';

  constructor() {
    if (authenticationEnvironment.mode === 'standalone') {
      const localUser = authenticationEnvironment.standaloneUser;
      const storedRole = this.storage?.getItem(
        STANDALONE_ROLE_STORAGE_KEY,
      ) as SixpayRole | null;

      const roles = storedRole
        ? new Set<SixpayRole>([storedRole])
        : extractSixpayRoles({ roles: localUser?.roles ?? [] });

      const effectiveRole =
        storedRole ?? this.firstConfiguredStandaloneRole(localUser?.roles ?? []);

      this.identityState.set({
        subject: this.standaloneSubjectForRole(effectiveRole),
        roles,
      });

      this.readyState.next(true);
      return;
    }

    if (!this.oidc) {
      this.readyState.next(false);
      return;
    }

    this.oidc.checkAuth().subscribe({
      next: (response) => {
        if (response.isAuthenticated) {
          this.setAuthenticatedSession(
            response.accessToken,
            response.userData as JwtClaims,
          );
        } else {
          this.clearLocalSession();
        }
        this.readyState.next(response.isAuthenticated);
      },
      error: () => {
        this.clearLocalSession();
        this.readyState.next(false);
      },
    });
  }

  hasRole(role: SixpayRole | string): boolean {
    return this.roles().has(
      role.replace(/^ROLE_/, '').toUpperCase() as SixpayRole,
    );
  }

  hasAnyRole(roles: readonly SixpayRole[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  simulateStandaloneRole(role: SixpayRole): void {
    if (!this.isStandaloneMode) {
      return;
    }

    this.storage?.setItem(STANDALONE_ROLE_STORAGE_KEY, role);

    this.identityState.set({
      subject: this.standaloneSubjectForRole(role),
      roles: new Set<SixpayRole>([role]),
    });
  }

  accessTokenForRequest(): Observable<string | null> {
    if (authenticationEnvironment.mode === 'oidc' && this.oidc) {
      return this.oidc.getAccessToken();
    }
    return of(null);
  }

  login(returnUrl = '/'): void {
    const safeReturnUrl = this.safeReturnUrl(returnUrl);
    this.storage?.setItem(RETURN_URL_STORAGE_KEY, safeReturnUrl);
    this.oidc?.authorize();
  }

  completeLoginNavigation(): void {
    if (!this.isAuthenticated()) {
      return;
    }

    const returnUrl = this.safeReturnUrl(
      this.storage?.getItem(RETURN_URL_STORAGE_KEY) ?? '/',
    );
    this.storage?.removeItem(RETURN_URL_STORAGE_KEY);
    void this.router.navigateByUrl(returnUrl);
  }

  logout(): void {
    this.clearLocalSession();
    this.storage?.removeItem(RETURN_URL_STORAGE_KEY);

    if (authenticationEnvironment.mode === 'oidc' && this.oidc) {
      this.oidc
        .logoffAndRevokeTokens()
        .subscribe({ error: () => this.oidc?.logoffLocal() });
      return;
    }

    void this.router.navigate(['/login']);
  }

  expireSession(): void {
    this.clearLocalSession();
    this.oidc?.logoffLocal();
  }

  private standaloneSubjectForRole(role: SixpayRole | null): string {
    if (role === 'PARTNER') {
      return (
        authenticationEnvironment.standalonePartner?.subject ??
        FALLBACK_STANDALONE_PARTNER_SUBJECT
      );
    }

    return authenticationEnvironment.standaloneUser?.subject ?? 'local-user';
  }

  private firstConfiguredStandaloneRole(
    roles: readonly string[],
  ): SixpayRole | null {
    return extractSixpayRoles({ roles }).values().next().value ?? null;
  }

  private setAuthenticatedSession(
    accessToken: string,
    userData: JwtClaims | null,
  ): void {
    const claims = this.decodePayload(accessToken) ?? userData ?? {};
    const subject = claims.sub ?? userData?.sub;

    if (!subject || this.isExpired(claims)) {
      this.clearLocalSession();
      return;
    }

    this.identityState.set({
      subject,
      roles: extractSixpayRoles(claims),
    });
  }

  private clearLocalSession(): void {
    this.identityState.set(null);
  }

  private get storage(): Storage | undefined {
    return this.document.defaultView?.sessionStorage;
  }

  private safeReturnUrl(returnUrl: string): string {
    return returnUrl.startsWith('/') && !returnUrl.startsWith('//')
      ? returnUrl
      : '/';
  }

  private isExpired(claims: JwtClaims): boolean {
    return claims.exp !== undefined && claims.exp * 1000 <= Date.now();
  }

  private decodePayload(token: string): JwtClaims | null {
    try {
      const encodedPayload = token.split('.')[1];
      if (!encodedPayload) {
        return null;
      }

      const normalizedPayload = encodedPayload
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=');

      return JSON.parse(atob(normalizedPayload)) as JwtClaims;
    } catch {
      return null;
    }
  }
}
