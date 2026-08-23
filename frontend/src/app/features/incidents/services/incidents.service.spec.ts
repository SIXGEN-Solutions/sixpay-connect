import {
  HttpErrorResponse,
} from '@angular/common/http';
import {
  TestBed,
} from '@angular/core/testing';
import {
  firstValueFrom,
  of,
  throwError,
} from 'rxjs';
import {
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from 'vitest';

import {
  BackendModeService,
} from '../../../core/backend/backend-mode.service';
import {
  IncidentsApiClient,
} from '../api/incidents-api.client';
import {
  IncidentsMockService,
} from './incidents-mock.service';
import {
  IncidentsService,
} from './incidents.service';

describe(
  'IncidentsService',
  () => {
    const api = {
      search: vi.fn(),
      get: vi.fn(),
    };

    const mock = {
      search: vi.fn(),
      get: vi.fn(),
    };

    function configure(
      usesApi: boolean,
    ): IncidentsService {
      TestBed.configureTestingModule({
        providers: [
          IncidentsService,
          {
            provide:
              BackendModeService,
            useValue: {
              usesApi,
            },
          },
          {
            provide:
              IncidentsApiClient,
            useValue: api,
          },
          {
            provide:
              IncidentsMockService,
            useValue: mock,
          },
        ],
      });

      return TestBed.inject(
        IncidentsService,
      );
    }

    beforeEach(() => {
      vi.clearAllMocks();
      TestBed.resetTestingModule();
    });

    it(
      'uses API in API mode',
      async () => {
        api.search.mockReturnValue(
          of({
            content: [],
            page: 0,
            size: 20,
            totalElements: 0,
            totalPages: 0,
          }),
        );

        const service =
          configure(true);

        await firstValueFrom(
          service.search({}),
        );

        expect(
          api.search,
        ).toHaveBeenCalled();

        expect(
          mock.search,
        ).not.toHaveBeenCalled();
      },
    );

    it(
      'maps API 404 detail to null',
      async () => {
        api.get.mockReturnValue(
          throwError(
            () =>
              new HttpErrorResponse({
                status: 404,
              }),
          ),
        );

        const service =
          configure(true);

        const result =
          await firstValueFrom(
            service.get('MISSING'),
          );

        expect(result).toBeNull();

        expect(
          mock.get,
        ).not.toHaveBeenCalled();
      },
    );

    it(
      'does not hide non-404 API errors',
      async () => {
        api.get.mockReturnValue(
          throwError(
            () =>
              new HttpErrorResponse({
                status: 500,
              }),
          ),
        );

        const service =
          configure(true);

        await expect(
          firstValueFrom(
            service.get('INC-1'),
          ),
        ).rejects.toBeInstanceOf(
          HttpErrorResponse,
        );

        expect(
          mock.get,
        ).not.toHaveBeenCalled();
      },
    );

    it(
      'uses mock only in mock mode',
      async () => {
        mock.search.mockReturnValue(
          of([]),
        );

        const service =
          configure(false);

        await firstValueFrom(
          service.search({}),
        );

        expect(
          mock.search,
        ).toHaveBeenCalled();

        expect(
          api.search,
        ).not.toHaveBeenCalled();
      },
    );

    it(
      'does not fallback to mock when API fails',
      async () => {
        api.search.mockReturnValue(
          throwError(
            () =>
              new HttpErrorResponse({
                status: 500,
              }),
          ),
        );

        const service =
          configure(true);

        await expect(
          firstValueFrom(
            service.search({}),
          ),
        ).rejects.toBeInstanceOf(
          HttpErrorResponse,
        );

        expect(
          api.search,
        ).toHaveBeenCalled();

        expect(
          mock.search,
        ).not.toHaveBeenCalled();
      },
    );
  },
);