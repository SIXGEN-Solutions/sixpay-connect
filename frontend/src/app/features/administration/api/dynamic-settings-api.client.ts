import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  DynamicSettingDefinition,
  DynamicSettingHistoryEntry,
  DynamicSettingRollbackRequest,
  DynamicSettingUpdateRequest,
  DynamicSettingValue,
} from '../models/dynamic-settings';

const API = '/internal/api/v1/administration/dynamic-settings';

@Injectable({ providedIn: 'root' })
export class DynamicSettingsApiClient {
  private readonly http = inject(HttpClient);

  definitions(): Observable<readonly DynamicSettingDefinition[]> {
    return this.http.get<readonly DynamicSettingDefinition[]>(API);
  }

  value(key: string): Observable<DynamicSettingValue> {
    return this.http.get<DynamicSettingValue>(`${API}/${encodeURIComponent(key)}`);
  }

  update(key: string, request: DynamicSettingUpdateRequest): Observable<DynamicSettingValue> {
    return this.http.put<DynamicSettingValue>(`${API}/${encodeURIComponent(key)}`, request);
  }

  history(key: string): Observable<readonly DynamicSettingHistoryEntry[]> {
    return this.http.get<readonly DynamicSettingHistoryEntry[]>(
      `${API}/${encodeURIComponent(key)}/history`,
    );
  }

  rollback(key: string, request: DynamicSettingRollbackRequest): Observable<DynamicSettingValue> {
    return this.http.post<DynamicSettingValue>(
      `${API}/${encodeURIComponent(key)}/rollback`,
      request,
    );
  }
}
