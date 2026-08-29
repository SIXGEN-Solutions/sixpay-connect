import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { AdministrationApiClient } from './administration-api.client';

describe('AdministrationApiClient', () => {
  let client: AdministrationApiClient;

  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });

    client = TestBed.inject(AdministrationApiClient);

    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
  });

  it('calls overview endpoint', async () => {
    const promise = firstValueFrom(client.overview());

    const request = http.expectOne('/internal/api/v1/administration/overview');

    expect(request.request.method).toBe('GET');

    request.flush({
      settings: {
        accountingCutoffZone: 'Africa/Douala',
        accountingCutoffTime: '23:59',
      },
      integrations: [],
      observedAt: '2026-08-23T10:00:00Z',
    });

    await promise;
  });

  it('calls settings endpoint', async () => {
    const promise = firstValueFrom(client.settings());

    const request = http.expectOne('/internal/api/v1/administration/settings');

    expect(request.request.method).toBe('GET');

    request.flush({
      accountingCutoffZone: 'Africa/Douala',
      accountingCutoffTime: '23:59',
    });

    await promise;
  });

  it('calls integrations endpoint', async () => {
    const promise = firstValueFrom(client.integrations());

    const request = http.expectOne('/internal/api/v1/administration/integrations');

    expect(request.request.method).toBe('GET');

    request.flush([]);

    await promise;
  });
});
