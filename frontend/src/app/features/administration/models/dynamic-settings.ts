export type DynamicSettingDomain =
  | 'SECURITY'
  | 'PAYMENT'
  | 'CUSTOMER'
  | 'NOTIFICATION'
  | 'ACCOUNTING'
  | 'REPORTING'
  | 'INTEGRATION'
  | 'GLOBAL';

export type DynamicSettingValueType = 'BOOLEAN' | 'INTEGER' | 'DECIMAL' | 'DURATION' | 'STRING';

export interface DynamicSettingDefinition {
  readonly key: string;
  readonly domain: DynamicSettingDomain;
  readonly type: DynamicSettingValueType;
  readonly defaultValue: string;
  readonly minimumValue: string | null;
  readonly maximumValue: string | null;
  readonly allowedValues: readonly string[];
  readonly description: string;
  readonly dynamic: boolean;
  readonly sensitive: boolean;
  readonly requiresRestart: boolean;
}

export interface DynamicSettingValue {
  readonly key: string;
  readonly domain: DynamicSettingDomain;
  readonly value: string;
  readonly version: number;
  readonly updatedAt: string;
  readonly updatedBy: string;
  readonly reason: string;
}

export interface DynamicSettingHistoryEntry {
  readonly historyId: string;
  readonly key: string;
  readonly domain: DynamicSettingDomain;
  readonly previousValue: string | null;
  readonly newValue: string;
  readonly previousVersion: number;
  readonly newVersion: number;
  readonly changedAt: string;
  readonly changedBy: string;
  readonly reason: string;
  readonly operation: string;
}

export interface DynamicSettingUpdateRequest {
  readonly value: string;
  readonly reason: string;
}

export interface DynamicSettingRollbackRequest {
  readonly targetVersion: number;
  readonly reason: string;
}
