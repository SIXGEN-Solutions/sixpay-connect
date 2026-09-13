import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { DynamicSettingsApiClient } from '../api/dynamic-settings-api.client';
import {
  DynamicSettingDefinition,
  DynamicSettingHistoryEntry,
  DynamicSettingRollbackRequest,
  DynamicSettingUpdateRequest,
  DynamicSettingValue,
} from '../models/dynamic-settings';

@Injectable({ providedIn: 'root' })
export class DynamicSettingsService {
  private readonly api = inject(DynamicSettingsApiClient);

  definitions(): Observable<readonly DynamicSettingDefinition[]> {
    return this.api.definitions();
  }

  value(key: string): Observable<DynamicSettingValue> {
    return this.api.value(key);
  }

  update(key: string, request: DynamicSettingUpdateRequest): Observable<DynamicSettingValue> {
    return this.api.update(key, request);
  }

  history(key: string): Observable<readonly DynamicSettingHistoryEntry[]> {
    return this.api.history(key);
  }

  rollback(key: string, request: DynamicSettingRollbackRequest): Observable<DynamicSettingValue> {
    return this.api.rollback(key, request);
  }
}
