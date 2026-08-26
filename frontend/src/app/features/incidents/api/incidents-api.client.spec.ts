import {
  provideHttpClient,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import {
  TestBed,
} from '@angular/core/testing';
import {
  firstValueFrom,
} from 'rxjs';

import {
  IncidentsApiClient,
} from './incidents-api.client';

describe(
  'IncidentsApiClient',
  () => {
    let client:
      IncidentsApiClient;

    let http:
      HttpTestingController;

    beforeEach(() => {
      TestBed.configureTestingModule({
        providers: [
          provideHttpClient(),
          provideHttpClientTesting(),
        ],
      });

      client =
        TestBed.inject(
          IncidentsApiClient,
        );

      http =
        TestBed.inject(
          HttpTestingController,
        );
    });

    afterEach(() => {
      http.verify();
    });

    it(
      'translates IncidentQuery to HttpParams',
      async () => {
        const promise =
          firstValueFrom(
            client.search({
              severity: 'HIGH',
              status: 'OPEN',
              component:
                ' Accounting ',
              page: 2,
              size: 50,
            }),
          );

        const request =
          http.expectOne(
            (candidate) =>
              candidate.url
                === '/internal/api/v1/incidents'
              && candidate.params.get(
                'severity',
              ) === 'HIGH'
              && candidate.params.get(
                'status',
              ) === 'OPEN'
              && candidate.params.get(
                'component',
              ) === 'Accounting'
              && candidate.params.get(
                'page',
              ) === '2'
              && candidate.params.get(
                'size',
              ) === '50',
          );

        expect(
          request.request.method,
        ).toBe('GET');

        request.flush({
          content: [],
          page: 2,
          size: 50,
          totalElements: 0,
          totalPages: 0,
        });

        await promise;
      },
    );

    it(
      'uses contract pagination defaults',
      async () => {
        const promise =
          firstValueFrom(
            client.search({}),
          );

        const request =
          http.expectOne(
            (candidate) =>
              candidate.url
                === '/internal/api/v1/incidents'
              && candidate.params.get(
                'page',
              ) === '0'
              && candidate.params.get(
                'size',
              ) === '20',
          );

        request.flush({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        });

        await promise;
      },
    );

    it(
      'calls incident detail endpoint',
      async () => {
        const promise =
          firstValueFrom(
            client.get(
              'INC-2026-0084',
            ),
          );

        const request =
          http.expectOne(
            '/internal/api/v1/incidents/INC-2026-0084',
          );

        expect(
          request.request.method,
        ).toBe('GET');

        request.flush({
          incidentId:
            'INC-2026-0084',
          severity: 'HIGH',
          component:
            'Accounting',
          summary: 'Test',
          status: 'OPEN',
          openedAt:
            '2026-08-23T10:00:00Z',
          updatedAt:
            '2026-08-23T10:01:00Z',
          description: 'Test',
          impact: 'Test',
          accountingBatchId: null,
          paymentId: null,
          paymentReference: null,
          correlationId: null,
          timeline: [],
        });

        await promise;
      },
    );
  },
);
