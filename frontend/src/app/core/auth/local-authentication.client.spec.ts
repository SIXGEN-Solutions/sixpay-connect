import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { LocalAuthenticationClient } from './local-authentication.client';

describe('LocalAuthenticationClient', () => {
  let client: LocalAuthenticationClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    client = TestBed.inject(LocalAuthenticationClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('logs in with credentials and enables browser credentials', () => {
    client
      .login({
        username: 'admin',
        password: 'admin-dev-2026',
      })
      .subscribe();

    const request = controller.expectOne('/api/v1/auth/login');

    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);
    expect(request.request.body).toEqual({
      username: 'admin',
      password: 'admin-dev-2026',
    });

    request.flush({
      subject: 'local-admin',
      username: 'admin',
      roles: ['ADMIN'],
    });
  });

  it('restores the current server-side session', () => {
    client.currentUser().subscribe();

    const request = controller.expectOne('/api/v1/auth/me');

    expect(request.request.method).toBe('GET');
    expect(request.request.withCredentials).toBe(true);

    request.flush({
      subject: 'local-admin',
      username: 'admin',
      roles: ['ADMIN'],
    });
  });

  it('logs out the server-side session', () => {
    client.logout().subscribe();

    const request = controller.expectOne('/api/v1/auth/logout');

    expect(request.request.method).toBe('POST');
    expect(request.request.withCredentials).toBe(true);

    request.flush(null);
  });
});
