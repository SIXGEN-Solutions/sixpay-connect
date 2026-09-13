import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DynamicSettingsApiClient } from '../api/dynamic-settings-api.client';
import { DynamicSettingsService } from './dynamic-settings.service';

describe('DynamicSettingsService', () => {
  const api = {
    definitions: vi.fn(() => of([])),
    value: vi.fn(() => of(null)),
    update: vi.fn(() => of(null)),
    history: vi.fn(() => of([])),
    rollback: vi.fn(() => of(null)),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        DynamicSettingsService,
        {
          provide: DynamicSettingsApiClient,
          useValue: api,
        },
      ],
    });
  });

  it('delegates through the dedicated DS client instead of AdministrationService', () => {
    const service = TestBed.inject(DynamicSettingsService);

    service.definitions().subscribe();
    service.history('payment.callback.max-attempts').subscribe();

    expect(api.definitions).toHaveBeenCalledOnce();
    expect(api.history).toHaveBeenCalledWith('payment.callback.max-attempts');
  });
});
