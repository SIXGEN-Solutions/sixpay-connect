import {
  TestBed,
} from '@angular/core/testing';
import {
  firstValueFrom,
} from 'rxjs';

import {
  AdministrationMockService,
} from './administration-mock.service';

describe(
  'AdministrationMockService',
  () => {
    let service:
      AdministrationMockService;

    beforeEach(() => {
      TestBed.configureTestingModule({});
      service = TestBed.inject(
        AdministrationMockService,
      );
    });

    it(
      'returns overview',
      async () => {
        const overview =
          await firstValueFrom(
            service.overview(),
          );

        expect(
          overview.integrations.length,
        ).toBeGreaterThan(0);

        expect(
          overview.settings.accountingCutoffZone,
        ).toBe('Africa/Douala');

        expect(
          overview.observedAt,
        ).toBeInstanceOf(Date);
      },
    );
  },
);
