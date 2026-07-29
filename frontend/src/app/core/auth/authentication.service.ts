import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';

const ACCESS_TOKEN_STORAGE_KEY = 'sixpay.access-token';

interface JwtPayload {
  readonly exp?: number;
  readonly sub?: string;
  readonly roles?: readonly string[];
  readonly authorities?: readonly string[];
  readonly realm_access?: {
    readonly roles?: readonly string[];
  };
}

@Injectable({ providedIn: 'root' })
export class AuthenticationService {
  private readonly document = inject(DOCUMENT);
  private readonly accessTokenState = signal<string | null>(
    this.storage?.getItem(ACCESS_TOKEN_STORAGE_KEY) ?? null,
  );

  readonly accessToken = this.accessTokenState.asReadonly();
  readonly isAuthenticated = computed(() => {
    const token = this.accessTokenState();
    if (!token) {
      return false;
    }

    const payload = this.decodePayload(token);
    return payload?.exp === undefined || payload.exp * 1000 > Date.now();
  });

  readonly subject = computed(() => this.decodePayload(this.accessTokenState())?.sub ?? null);
  readonly roles = computed(() => {
    const payload = this.decodePayload(this.accessTokenState());
    const roles = [
      ...(payload?.roles ?? []),
      ...(payload?.authorities ?? []),
      ...(payload?.realm_access?.roles ?? []),
    ];

    return new Set(roles.map((role) => role.replace(/^ROLE_/, '').toUpperCase()));
  });

  setAccessToken(accessToken: string): void {
    this.storage?.setItem(ACCESS_TOKEN_STORAGE_KEY, accessToken);
    this.accessTokenState.set(accessToken);
  }

  clearSession(): void {
    this.storage?.removeItem(ACCESS_TOKEN_STORAGE_KEY);
    this.accessTokenState.set(null);
  }

  hasRole(role: string): boolean {
    return this.roles().has(role.replace(/^ROLE_/, '').toUpperCase());
  }

  private get storage(): Storage | undefined {
    return this.document.defaultView?.sessionStorage;
  }

  private decodePayload(token: string | null): JwtPayload | null {
    if (!token) {
      return null;
    }

    try {
      const encodedPayload = token.split('.')[1];
      if (!encodedPayload) {
        return null;
      }

      const normalizedPayload = encodedPayload
        .replace(/-/g, '+')
        .replace(/_/g, '/')
        .padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=');

      return JSON.parse(atob(normalizedPayload)) as JwtPayload;
    } catch {
      return null;
    }
  }
}
