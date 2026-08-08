import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AdministrationOverview,
  GeneralSettings,
  IntegrationStatus,
} from '../models/administration';
import { AdministrationMockService } from './administration-mock.service';

@Injectable({ providedIn: 'root' })
export class AdministrationService {
  private readonly mock = inject(AdministrationMockService);

  overview(): Observable<AdministrationOverview> {
    return this.mock.overview();
  }

  settings(): Observable<GeneralSettings> {
    return this.mock.settings();
  }

  integrations(): Observable<readonly IntegrationStatus[]> {
    return this.mock.integrations();
  }
}
