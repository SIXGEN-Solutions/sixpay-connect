import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  LocalLoginRequest,
  LocalSessionResponse,
} from './authentication.model';

const LOCAL_AUTH_API_PATH = '/api/v1/auth';

@Injectable({ providedIn: 'root' })
export class LocalAuthenticationClient {
  private readonly http = inject(HttpClient);

  login(request: LocalLoginRequest): Observable<LocalSessionResponse> {
    return this.http.post<LocalSessionResponse>(
      `${LOCAL_AUTH_API_PATH}/login`,
      request,
      { withCredentials: true },
    );
  }

  currentUser(): Observable<LocalSessionResponse> {
    return this.http.get<LocalSessionResponse>(
      `${LOCAL_AUTH_API_PATH}/me`,
      { withCredentials: true },
    );
  }

  logout(): Observable<void> {
    return this.http.post<void>(
      `${LOCAL_AUTH_API_PATH}/logout`,
      {},
      { withCredentials: true },
    );
  }
}
