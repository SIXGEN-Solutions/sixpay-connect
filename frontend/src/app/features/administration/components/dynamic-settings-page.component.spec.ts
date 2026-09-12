import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DynamicSettingsService } from '../services/dynamic-settings.service';
import { DynamicSettingsPageComponent } from './dynamic-settings-page.component';

describe('DynamicSettingsPageComponent', () => {
  let fixture: ComponentFixture<DynamicSettingsPageComponent>;

  const service = {
    definitions: vi.fn(() =>
      of([
        {
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
        },
      ]),
    ),
    value: vi.fn(() =>
      of({
        key: 'payment.callback.max-attempts',
        domain: 'PAYMENT',
        value: '8',
        version: 1,
        updatedAt: '1970-01-01T00:00:00Z',
        updatedBy: 'SYSTEM_DEFAULT',
        reason: 'Catalog default',
      }),
    ),
    history: vi.fn(() => of([])),
    update: vi.fn(),
    rollback: vi.fn(),
  };

  beforeEach(async () => {
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
  });

  it('renders a dedicated mutable dynamic-settings page', () => {
    const text = fixture.nativeElement.textContent as string;

    expect(text).toContain('Paramètres dynamiques');
    expect(text).toContain('payment.callback.max-attempts');
    expect(text).toContain('Version');
    expect(text).toContain('Motif');
  });
});
