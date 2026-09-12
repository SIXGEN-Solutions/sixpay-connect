import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';

import { DynamicSettingsApiClient } from './dynamic-settings-api.client';

const API = '/internal/api/v1/administration/dynamic-settings';
const KEY = 'payment.callback.max-attempts';

describe('DynamicSettingsApiClient', () => {
  let client: DynamicSettingsApiClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    client = TestBed.inject(DynamicSettingsApiClient);
    http = TestBed.inject(HttpTestingController);
  });

  it('loads definitions and current value', () => {
    client.definitions().subscribe();
    const definitions = http.expectOne(API);
    expect(definitions.request.method).toBe('GET');
    definitions.flush([]);

    client.value(KEY).subscribe();
    const value = http.expectOne(`${API}/${encodeURIComponent(KEY)}`);
    expect(value.request.method).toBe('GET');
    value.flush({
      key: KEY,
      domain: 'PAYMENT',
      value: '8',
      version: 1,
      updatedAt: '1970-01-01T00:00:00Z',
      updatedBy: 'SYSTEM_DEFAULT',
      reason: 'Catalog default',
    });

    http.verify();
  });

  it('updates with a mandatory reason', () => {
    client.update(KEY, { value: '10', reason: 'Operational tuning' }).subscribe();

    const call = http.expectOne(`${API}/${encodeURIComponent(KEY)}`);
    expect(call.request.method).toBe('PUT');
    expect(call.request.body).toEqual({
      value: '10',
      reason: 'Operational tuning',
    });

    call.flush({
      key: KEY,
      domain: 'PAYMENT',
      value: '10',
      version: 2,
      updatedAt: '2026-09-12T17:00:00Z',
      updatedBy: 'admin',
      reason: 'Operational tuning',
    });

    http.verify();
  });

  it('loads history and rolls back explicitly', () => {
    client.history(KEY).subscribe();
    const history = http.expectOne(`${API}/${encodeURIComponent(KEY)}/history`);
    expect(history.request.method).toBe('GET');
    history.flush([]);

    client.rollback(KEY, { targetVersion: 2, reason: 'Restore stable value' }).subscribe();
    const rollback = http.expectOne(`${API}/${encodeURIComponent(KEY)}/rollback`);
    expect(rollback.request.method).toBe('POST');
    expect(rollback.request.body).toEqual({
      targetVersion: 2,
      reason: 'Restore stable value',
    });
    rollback.flush({
      key: KEY,
      domain: 'PAYMENT',
      value: '10',
      version: 4,
      updatedAt: '2026-09-12T17:00:00Z',
      updatedBy: 'admin',
      reason: 'Restore stable value',
    });

    http.verify();
  });
});
