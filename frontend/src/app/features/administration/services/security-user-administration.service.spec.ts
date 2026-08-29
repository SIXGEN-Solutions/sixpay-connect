import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import {
  CreateSecurityUserRequest,
  SecurityUserDetail,
  UpdateSecurityUserRequest,
} from '../models/security-user-administration';
import { SecurityUserAdministrationService } from './security-user-administration.service';

const API = '/internal/api/v1/administration/users';
const USER: SecurityUserDetail = {
  id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  username: 'admin',
  email: 'admin@sixpay.local',
  status: 'ACTIVE',
  localEnabled: true,
  oidcLinked: false,
  roles: ['ADMIN'],
  permissions: [],
  identities: [],
  recentAuthenticationEvents: [],
};

describe('SecurityUserAdministrationService', () => {
  let service: SecurityUserAdministrationService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SecurityUserAdministrationService);
    http = TestBed.inject(HttpTestingController);
  });

  it('creates a canonical SIXPAY user', () => {
    const request: CreateSecurityUserRequest = {
      username: 'admin',
      email: 'admin@sixpay.local',
      roles: ['ADMIN'],
      permissions: ['payment.read'],
      localAuthenticationEnabled: true,
      initialPassword: 'Admin-dev-2026',
    };
    service.createUser(request).subscribe((user) => expect(user).toEqual(USER));
    const call = http.expectOne(API);
    expect(call.request.method).toBe('POST');
    expect(call.request.body).toEqual(request);
    call.flush(USER);
    http.verify();
  });

  it('lists and reads users', () => {
    service.listUsers().subscribe((users) => expect(users).toEqual([USER]));
    const listCall = http.expectOne(API);
    expect(listCall.request.method).toBe('GET');
    listCall.flush([USER]);

    service.getUser(USER.id).subscribe((user) => expect(user).toEqual(USER));
    const detailCall = http.expectOne(`${API}/${USER.id}`);
    expect(detailCall.request.method).toBe('GET');
    detailCall.flush(USER);
    http.verify();
  });

  it('updates canonical account and SIXPAY authorization', () => {
    const request: UpdateSecurityUserRequest = {
      username: 'ops-admin',
      email: 'ops-admin@sixpay.local',
      roles: ['ADMIN', 'AUDITOR'],
      permissions: ['reporting.read'],
    };
    service.updateUser(USER.id, request).subscribe((user) => expect(user.id).toBe(USER.id));
    const call = http.expectOne(`${API}/${USER.id}`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual(request);
    call.flush({ ...USER, ...request });
    http.verify();
  });

  it('enables, disables and deletes a user', () => {
    service.enableUser(USER.id).subscribe();
    const enableCall = http.expectOne(`${API}/${USER.id}/enable`);
    expect(enableCall.request.method).toBe('POST');
    enableCall.flush(USER);

    service.disableUser(USER.id).subscribe();
    const disableCall = http.expectOne(`${API}/${USER.id}/disable`);
    expect(disableCall.request.method).toBe('POST');
    disableCall.flush({ ...USER, status: 'DISABLED' });

    service.deleteUser(USER.id).subscribe();
    const deleteCall = http.expectOne(`${API}/${USER.id}`);
    expect(deleteCall.request.method).toBe('DELETE');
    deleteCall.flush(null);
    http.verify();
  });

  it('preserves existing Local and OIDC administration endpoints', () => {
    service.setLocalEnabled(USER.id, false).subscribe();
    const localCall = http.expectOne(`${API}/${USER.id}/authentication-methods/local`);
    expect(localCall.request.method).toBe('PUT');
    expect(localCall.request.body).toEqual({ enabled: false });
    localCall.flush({ ...USER, localEnabled: false });

    service.resetLocalPassword(USER.id, 'Temporary-2026').subscribe();
    const passwordCall = http.expectOne(`${API}/${USER.id}/local-password-reset`);
    expect(passwordCall.request.method).toBe('POST');
    passwordCall.flush(USER);

    service.linkOidcIdentity(USER.id, 'ENTRA', 'provider-subject').subscribe();
    const linkCall = http.expectOne(`${API}/${USER.id}/identities/oidc`);
    expect(linkCall.request.method).toBe('POST');
    linkCall.flush(USER);

    service.unlinkIdentity(USER.id, 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb').subscribe();
    const unlinkCall = http.expectOne(
      `${API}/${USER.id}/identities/bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb`,
    );
    expect(unlinkCall.request.method).toBe('DELETE');
    unlinkCall.flush(USER);
    http.verify();
  });
});
