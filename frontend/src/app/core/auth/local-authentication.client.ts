import { HttpClient, HttpHeaders } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AuthenticationSessionResponse,
  LocalLoginRequest,
  LocalPasswordChangeRequest,
} from './authentication.model';

const AUTH_API_PATH = '/api/v1/auth';

/**
 * Authentication backend client.
 *
 * <p>The class name is retained for source compatibility, but DA-8 makes
 * `/me`, `/logout`, and OIDC session establishment mechanism-neutral.</p>
 */
@Injectable({ providedIn: 'root' })
export class LocalAuthenticationClient {
  private readonly http = inject(HttpClient);

  login(request: LocalLoginRequest): Observable<AuthenticationSessionResponse> {
    return this.http.post<AuthenticationSessionResponse>(`${AUTH_API_PATH}/login`, request, {
      withCredentials: true,
    });
  }

  currentUser(): Observable<AuthenticationSessionResponse> {
    return this.http.get<AuthenticationSessionResponse>(`${AUTH_API_PATH}/me`, {
      withCredentials: true,
    });
  }

  changePassword(request: LocalPasswordChangeRequest): Observable<void> {
    return this.http.post<void>(`${AUTH_API_PATH}/password/change`, request, {
      withCredentials: true,
    });
  }

  establishOidcSession(accessToken: string): Observable<AuthenticationSessionResponse> {
    return this.http.post<AuthenticationSessionResponse>(
      `${AUTH_API_PATH}/session/oidc`,
      {},
      {
        withCredentials: true,
        headers: new HttpHeaders({
          Authorization: `Bearer ${accessToken}`,
        }),
      },
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${AUTH_API_PATH}/logout`, {}, { withCredentials: true });
  }
}
