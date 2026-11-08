import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PaymentsApiClient } from '../api/payments-api.client';
import { PaymentsMockService } from './payments-mock.service';
import { PaymentsService } from './payments.service';

describe('PaymentsService R4 T0 manual reconciliation', () => {
  function setup(usesApi: boolean) {
    const api = { reconcileT0: vi.fn(() => of({ results: [] })) };
    TestBed.configureTestingModule({ providers: [
      PaymentsService,
      { provide: BackendModeService, useValue: { usesApi } },
      { provide: PaymentsApiClient, useValue: api },
      { provide: PaymentsMockService, useValue: {} },
    ] });
    return { service: TestBed.inject(PaymentsService), api };
  }

  it('delegates only to the approved internal API in API mode', async () => {
    const { service, api } = setup(true);
    await firstValueFrom(service.reconcileT0(['payment-id']));
    expect(api.reconcileT0).toHaveBeenCalledWith(['payment-id']);
  });

  it('does not send the real command in mock mode', async () => {
    const { service, api } = setup(false);
    await expect(firstValueFrom(service.reconcileT0(['payment-id']))).rejects.toThrow();
    expect(api.reconcileT0).not.toHaveBeenCalled();
  });
});
