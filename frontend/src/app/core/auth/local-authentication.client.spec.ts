import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LocalAuthenticationClient } from './local-authentication.client';

describe('LocalAuthenticationClient', () => {
  let client: LocalAuthenticationClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(LocalAuthenticationClient);

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('establishes an LDAP-backed canonical SIXPAY session', () => {
    client
      .loginLdap({
        username: 'ldap-user',
        password: 'ldap-password',
      })
      .subscribe();

    const request = http.expectOne('/api/v1/auth/login/ldap');

    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({
      username: 'ldap-user',
      password: 'ldap-password',
    });

    request.flush({
      authenticated: true,
      subject: '11111111-1111-4111-8111-111111111111',
      username: 'ldap-user',
      roles: ['ADMIN'],
      permissions: ['customer.read'],
      authenticationMethod: 'LDAP',
      passwordChangeRequired: false,
      capabilities: {
        localEnabled: false,
        oidcEnabled: false,
        ldapEnabled: true,
      },
    });
  });

  it('changes the authenticated LOCAL password through the DA-10 endpoint', () => {
    client
      .changePassword({
        currentPassword: 'Temporary-password-2026',
        newPassword: 'Permanent-password-2026',
      })
      .subscribe();

    const request = http.expectOne('/api/v1/auth/password/change');

    expect(request.request.method).toBe('POST');

    expect(request.request.withCredentials).toBe(true);

    expect(request.request.body).toEqual({
      currentPassword: 'Temporary-password-2026',
      newPassword: 'Permanent-password-2026',
    });

    request.flush(null);
  });
});
