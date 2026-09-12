import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { IncidentsApiClient } from '../api/incidents-api.client';
import { IncidentsMockService } from './incidents-mock.service';
import { IncidentsService } from './incidents.service';

describe('IncidentsService', () => {
  const api = {
    search: vi.fn(),
    get: vi.fn(),
  };

  const mock = {
    search: vi.fn(),
    get: vi.fn(),
  };

  function configure(usesApi: boolean): IncidentsService {
    TestBed.configureTestingModule({
      providers: [
        IncidentsService,
        {
          provide: BackendModeService,
          useValue: {
            usesApi,
          },
        },
        {
          provide: IncidentsApiClient,
          useValue: api,
        },
        {
          provide: IncidentsMockService,
          useValue: mock,
        },
      ],
    });

    return TestBed.inject(IncidentsService);
  }

  beforeEach(() => {
    vi.clearAllMocks();
    TestBed.resetTestingModule();
  });

  it('uses API in API mode', async () => {
    api.search.mockReturnValue(
      of({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    );

    const service = configure(true);

    await firstValueFrom(service.search({}));

    expect(api.search).toHaveBeenCalled();

    expect(mock.search).not.toHaveBeenCalled();
  });

  it('maps API 404 detail to null', async () => {
    api.get.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 404,
          }),
      ),
    );

    const service = configure(true);

    const result = await firstValueFrom(service.get('MISSING'));

    expect(result).toBeNull();

    expect(mock.get).not.toHaveBeenCalled();
  });

  it('does not hide non-404 API errors', async () => {
    api.get.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
          }),
      ),
    );

    const service = configure(true);

    await expect(firstValueFrom(service.get('INC-1'))).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(mock.get).not.toHaveBeenCalled();
  });

  it('uses mock only in mock mode and preserves pagination contract', async () => {
    mock.search.mockReturnValue(
      of([
        {
          incidentId: 'INC-1',
          severity: 'HIGH',
          component: 'Accounting',
          summary: 'Delayed batch',
          status: 'OPEN',
          openedAt: new Date('2026-09-12T10:00:00Z'),
          updatedAt: new Date('2026-09-12T10:05:00Z'),
        },
      ]),
    );

    const service = configure(false);

    const result = await firstValueFrom(
      service.search({
        page: 2,
        size: 10,
      }),
    );

    expect(mock.search).toHaveBeenCalled();

    expect(api.search).not.toHaveBeenCalled();

    expect(result.page).toBe(2);
    expect(result.size).toBe(10);
    expect(result.totalElements).toBe(1);
    expect(result.totalPages).toBe(1);
    expect(result.content[0]?.openedAt).toBe('2026-09-12T10:00:00.000Z');
    expect(result.content[0]?.updatedAt).toBe('2026-09-12T10:05:00.000Z');
  });

  it('does not fallback to mock when API fails', async () => {
    api.search.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 500,
          }),
      ),
    );

    const service = configure(true);

    await expect(firstValueFrom(service.search({}))).rejects.toBeInstanceOf(HttpErrorResponse);

    expect(api.search).toHaveBeenCalled();

    expect(mock.search).not.toHaveBeenCalled();
  });
});
