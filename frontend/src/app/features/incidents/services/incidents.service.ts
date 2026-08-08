import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { IncidentQuery } from '../models/incident-query';
import { IncidentDetail } from '../models/incidents';
import { IncidentsMockService } from './incidents-mock.service';

@Injectable({ providedIn: 'root' })
export class IncidentsService {
  private readonly mock = inject(IncidentsMockService);

  search(query: IncidentQuery): Observable<readonly IncidentDetail[]> {
    return this.mock.search(query);
  }

  get(incidentId: string): Observable<IncidentDetail | null> {
    return this.mock.get(incidentId);
  }
}
