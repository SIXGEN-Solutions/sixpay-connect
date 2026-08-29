import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  CreateSecurityUserRequest,
  SecurityUserDetail,
  SecurityUserSummary,
  UpdateSecurityUserRequest,
} from '../models/security-user-administration';

const API = '/internal/api/v1/administration/users';

@Injectable({ providedIn: 'root' })
export class SecurityUserAdministrationService {
  private readonly http = inject(HttpClient);

  createUser(request: CreateSecurityUserRequest): Observable<SecurityUserDetail> {
    return this.http.post<SecurityUserDetail>(API, request);
  }

  listUsers(): Observable<readonly SecurityUserSummary[]> {
    return this.http.get<readonly SecurityUserSummary[]>(API);
  }

  getUser(userId: string): Observable<SecurityUserDetail> {
    return this.http.get<SecurityUserDetail>(`${API}/${userId}`);
  }

  updateUser(userId: string, request: UpdateSecurityUserRequest): Observable<SecurityUserDetail> {
    return this.http.put<SecurityUserDetail>(`${API}/${userId}`, request);
  }

  enableUser(userId: string): Observable<SecurityUserDetail> {
    return this.http.post<SecurityUserDetail>(`${API}/${userId}/enable`, {});
  }

  setLocalEnabled(userId: string, enabled: boolean): Observable<SecurityUserDetail> {
    return this.http.put<SecurityUserDetail>(`${API}/${userId}/authentication-methods/local`, {
      enabled,
    });
  }

  resetLocalPassword(userId: string, newPassword: string): Observable<SecurityUserDetail> {
    return this.http.post<SecurityUserDetail>(`${API}/${userId}/local-password-reset`, {
      newPassword,
    });
  }

  linkOidcIdentity(
    userId: string,
    provider: string,
    providerSubject: string,
  ): Observable<SecurityUserDetail> {
    return this.http.post<SecurityUserDetail>(`${API}/${userId}/identities/oidc`, {
      provider,
      providerSubject,
    });
  }

  unlinkIdentity(userId: string, identityId: string): Observable<SecurityUserDetail> {
    return this.http.delete<SecurityUserDetail>(`${API}/${userId}/identities/${identityId}`);
  }

  disableUser(userId: string): Observable<SecurityUserDetail> {
    return this.http.post<SecurityUserDetail>(`${API}/${userId}/disable`, {});
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${API}/${userId}`);
  }
}
