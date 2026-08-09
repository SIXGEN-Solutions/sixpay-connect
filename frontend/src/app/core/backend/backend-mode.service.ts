import { Injectable } from '@angular/core';

import { environment } from '../../../environments/environment';
import { BackendMode } from '../../../environments/environment.model';

@Injectable({ providedIn: 'root' })
export class BackendModeService {
  readonly mode: BackendMode = environment.backend.mode;

  get usesApi(): boolean {
    return this.mode === 'api';
  }

  get usesMock(): boolean {
    return this.mode === 'mock';
  }
}
