import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  DynamicSettingDefinition,
  DynamicSettingHistoryEntry,
  DynamicSettingValue,
} from '../models/dynamic-settings';
import { DynamicSettingsService } from '../services/dynamic-settings.service';
import { DynamicSettingsPageComponent } from './dynamic-settings-page.component';

const DEFINITION: DynamicSettingDefinition = {
  key: 'payment.callback.max-attempts',
  domain: 'PAYMENT',
  type: 'INTEGER',
  defaultValue: '8',
  minimumValue: '1',
  maximumValue: '100',
  allowedValues: [],
  description: 'Maximum callback delivery attempts',
  dynamic: true,
  sensitive: false,
  requiresRestart: false,
};

const BOOLEAN_DEFINITION: DynamicSettingDefinition = {
  ...DEFINITION,
  key: 'payment.callback.enabled',
  type: 'BOOLEAN',
  defaultValue: 'true',
  minimumValue: null,
  maximumValue: null,
};

const ALLOWED_DEFINITION: DynamicSettingDefinition = {
  ...DEFINITION,
  key: 'payment.callback.mode',
  type: 'STRING',
  defaultValue: 'SAFE',
  minimumValue: null,
  maximumValue: null,
  allowedValues: ['SAFE', 'FAST'],
};

const VALUE: DynamicSettingValue = {
  key: DEFINITION.key,
  domain: 'PAYMENT',
  value: '8',
  version: 1,
  updatedAt: '1970-01-01T00:00:00Z',
  updatedBy: 'SYSTEM_DEFAULT',
  reason: 'Catalog default',
};

const HISTORY_ENTRY: DynamicSettingHistoryEntry = {
  historyId: 'history-1',
  key: DEFINITION.key,
  domain: 'PAYMENT',
  previousValue: '8',
  newValue: '10',
  previousVersion: 1,
  newVersion: 2,
  changedAt: '2026-09-12T17:00:00Z',
  changedBy: 'admin',
  reason: 'Operational tuning',
  operation: 'UPDATE',
};

const HISTORY: DynamicSettingHistoryEntry[] = [HISTORY_ENTRY];

describe('DynamicSettingsPageComponent', () => {
  let fixture: ComponentFixture<DynamicSettingsPageComponent>;

  let service: {
    definitions: ReturnType<typeof vi.fn>;
    value: ReturnType<typeof vi.fn>;
    history: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    rollback: ReturnType<typeof vi.fn>;
  };

  async function create(
    definitions: readonly DynamicSettingDefinition[] = [DEFINITION],
  ): Promise<void> {
    service = {
      definitions: vi.fn(() => of(definitions)),
      value: vi.fn((key: string) =>
        of({
          ...VALUE,
          key,
          value: definitions.find((definition) => definition.key === key)?.defaultValue ?? '8',
        }),
      ),
      history: vi.fn(() => of(HISTORY)),
      update: vi.fn(() => of({ ...VALUE, value: '10', version: 2 })),
      rollback: vi.fn(() => of({ ...VALUE, value: '8', version: 3 })),
    };

    await TestBed.configureTestingModule({
      imports: [DynamicSettingsPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: DynamicSettingsService,
          useValue: service,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DynamicSettingsPageComponent);
    fixture.detectChanges();
  }

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  function controlById<T extends HTMLElement>(id: string): T {
    const match = fixture.debugElement.query(By.css(`[id="${id}"]`));

    if (!match) {
      throw new Error(`Control not found: #${id}`);
    }

    return match.nativeElement as T;
  }

  function setFormValue(id: string, value: string): void {
    const control = controlById<HTMLInputElement | HTMLTextAreaElement>(id);
    control.value = value;
    control.dispatchEvent(new Event('input'));
    fixture.detectChanges();
  }

  function submitUpdateForm(): void {
    fixture.debugElement.query(By.css('form')).triggerEventHandler('ngSubmit', new Event('submit'));
    fixture.detectChanges();
  }

  function openHistory(): void {
    const details = fixture.debugElement.query(By.css('details'));
    details.nativeElement.open = true;
    details.triggerEventHandler('toggle', { currentTarget: details.nativeElement });
    fixture.detectChanges();
  }

  it('loads definitions and values without loading history eagerly', async () => {
    await create();

    expect(service.definitions).toHaveBeenCalledTimes(1);
    expect(service.value).toHaveBeenCalledWith(DEFINITION.key);
    expect(service.history).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Paramètres dynamiques');
    expect(fixture.nativeElement.textContent).toContain(DEFINITION.key);
  });

  it('loads history lazily when the details section is opened', async () => {
    await create();

    const details = fixture.debugElement.query(By.css('details'));
    details.nativeElement.open = true;
    details.triggerEventHandler('toggle', { currentTarget: details.nativeElement });
    fixture.detectChanges();

    expect(service.history).toHaveBeenCalledTimes(1);
    expect(service.history).toHaveBeenCalledWith(DEFINITION.key);
    expect(fixture.nativeElement.textContent).toContain('Operational tuning');
  });

  it('renders contract-backed controls for numeric, boolean and allowed values', async () => {
    await create([DEFINITION, BOOLEAN_DEFINITION, ALLOWED_DEFINITION]);

    const numberInput = controlById<HTMLInputElement>(`value-${DEFINITION.key}`);
    expect(numberInput.type).toBe('number');
    expect(numberInput.min).toBe('1');
    expect(numberInput.max).toBe('100');

    const booleanSelect = controlById<HTMLSelectElement>(`value-${BOOLEAN_DEFINITION.key}`);
    expect([...booleanSelect.options].map((option) => option.value)).toEqual(['', 'true', 'false']);

    const allowedSelect = controlById<HTMLSelectElement>(`value-${ALLOWED_DEFINITION.key}`);
    expect([...allowedSelect.options].map((option) => option.value)).toEqual(['', 'SAFE', 'FAST']);
  });

  it('uses a separate update reason and exposes saving state', async () => {
    const pending = new Subject<DynamicSettingValue>();
    await create();
    service.update.mockReturnValue(pending.asObservable());

    setFormValue(`value-${DEFINITION.key}`, '10');
    setFormValue(`update-reason-${DEFINITION.key}`, 'Operational tuning');
    submitUpdateForm();

    expect(service.update).toHaveBeenCalledWith(DEFINITION.key, {
      value: '10',
      reason: 'Operational tuning',
    });
    expect(fixture.nativeElement.textContent).toContain('Enregistrement…');

    pending.next({ ...VALUE, value: '10', version: 2 });
    pending.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Enregistrement…');
  });

  it('uses a separate rollback reason and exposes rolling-back state', async () => {
    const pending = new Subject<DynamicSettingValue>();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    await create();
    service.rollback.mockReturnValue(pending.asObservable());

    openHistory();
    setFormValue(
      `rollback-reason-${DEFINITION.key}-${HISTORY_ENTRY.newVersion}`,
      'Restore stable value',
    );

    fixture.debugElement.query(By.css('button[type="button"]')).triggerEventHandler('click');
    fixture.detectChanges();

    expect(service.rollback).toHaveBeenCalledWith(DEFINITION.key, {
      targetVersion: HISTORY_ENTRY.newVersion,
      reason: 'Restore stable value',
    });
    expect(fixture.nativeElement.textContent).toContain('Restauration…');

    pending.next({ ...VALUE, version: 3 });
    pending.complete();
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).not.toContain('Restauration…');
  });

  it('surfaces a forbidden backend response instead of swallowing it', async () => {
    await create();
    service.update.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 403,
            statusText: 'Forbidden',
          }),
      ),
    );

    setFormValue(`value-${DEFINITION.key}`, '10');
    setFormValue(`update-reason-${DEFINITION.key}`, 'Operational tuning');
    submitUpdateForm();

    expect(fixture.nativeElement.textContent).toContain('Accès administrateur requis');
  });

  it('surfaces a catalogue loading error', async () => {
    service = {
      definitions: vi.fn(() =>
        throwError(
          () =>
            new HttpErrorResponse({
              status: 500,
              statusText: 'Server Error',
            }),
        ),
      ),
      value: vi.fn(),
      history: vi.fn(),
      update: vi.fn(),
      rollback: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [DynamicSettingsPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: DynamicSettingsService,
          useValue: service,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DynamicSettingsPageComponent);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Chargement du catalogue impossible');
  });
});
