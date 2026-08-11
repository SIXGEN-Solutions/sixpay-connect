import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AuthenticationSessionResponse,
  LocalLoginRequest,
} from './authentication.model';

const AUTH_API_PATH = '/api/v1/auth';

/**
 * Existing Local credential client plus mechanism-neutral current-session read.
 * The /me response is authoritative for SIXPAY roles and permissions.
 */
@Injectable({ providedIn: 'root' })
export class LocalAuthenticationClient {
  private readonly http = inject(HttpClient);

  login(
    request: LocalLoginRequest,
  ): Observable<AuthenticationSessionResponse> {
    return this.http.post<AuthenticationSessionResponse>(
      `${AUTH_API_PATH}/login`,
      request,
      { withCredentials: true },
    );
  }

  currentUser(): Observable<AuthenticationSessionResponse> {
    return this.http.get<AuthenticationSessionResponse>(
      `${AUTH_API_PATH}/me`,
      { withCredentials: true },
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(
      `${AUTH_API_PATH}/logout`,
      {},
      { withCredentials: true },
    );
  }
}
