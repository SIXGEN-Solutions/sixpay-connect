import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  DynamicSettingDefinition,
  DynamicSettingDomain,
  DynamicSettingHistoryEntry,
  DynamicSettingValue,
} from '../models/dynamic-settings';
import { DynamicSettingsService } from '../services/dynamic-settings.service';

interface DynamicSettingView {
  readonly definition: DynamicSettingDefinition;
  readonly value: DynamicSettingValue | null;
  readonly history: readonly DynamicSettingHistoryEntry[] | null;
}

@Component({
  selector: 'sp-dynamic-settings-page',
  imports: [CommonModule, FormsModule, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/administration"> ← Retour à l'administration </a>

      <sp-toolbar
        title="Paramètres dynamiques"
        description="Catalogue des paramètres opérationnels modifiables à chaud. Les paramètres généraux historiques restent en lecture seule."
      />

      @if (error()) {
        <p class="sp-error" role="alert">{{ error() }}</p>
      }

      @if (loading()) {
        <p>Chargement des paramètres dynamiques…</p>
      } @else {
        @for (group of groupedSettings(); track group.domain) {
          <section class="sp-domain">
            <h2>{{ group.domain }}</h2>

            <div class="sp-grid">
              @for (item of group.items; track item.definition.key) {
                <sp-card [title]="item.definition.key" [subtitle]="item.definition.description">
                  <dl class="sp-details">
                    <div>
                      <dt>Type</dt>
                      <dd>{{ item.definition.type }}</dd>
                    </div>
                    <div>
                      <dt>Valeur</dt>
                      <dd>{{ item.value?.value ?? item.definition.defaultValue }}</dd>
                    </div>
                    <div>
                      <dt>Version</dt>
                      <dd>{{ item.value?.version ?? '—' }}</dd>
                    </div>
                    <div>
                      <dt>Minimum</dt>
                      <dd>{{ item.definition.minimumValue ?? '—' }}</dd>
                    </div>
                    <div>
                      <dt>Maximum</dt>
                      <dd>{{ item.definition.maximumValue ?? '—' }}</dd>
                    </div>
                    <div>
                      <dt>Valeurs autorisées</dt>
                      <dd>
                        {{
                          item.definition.allowedValues.length > 0
                            ? item.definition.allowedValues.join(', ')
                            : '—'
                        }}
                      </dd>
                    </div>
                  </dl>

                  <form class="sp-form" (ngSubmit)="update(item)">
                    <label [attr.for]="'value-' + item.definition.key">Nouvelle valeur</label>

                    @if (item.definition.allowedValues.length > 0) {
                      <select
                        [id]="'value-' + item.definition.key"
                        name="value-{{ item.definition.key }}"
                        [ngModel]="draftValue(item.definition.key)"
                        (ngModelChange)="setDraftValue(item.definition.key, $event)"
                        [disabled]="isSaving(item.definition.key)"
                        required
                      >
                        <option value="">Sélectionner une valeur</option>
                        @for (allowed of item.definition.allowedValues; track allowed) {
                          <option [value]="allowed">{{ allowed }}</option>
                        }
                      </select>
                    } @else if (item.definition.type === 'BOOLEAN') {
                      <select
                        [id]="'value-' + item.definition.key"
                        name="value-{{ item.definition.key }}"
                        [ngModel]="draftValue(item.definition.key)"
                        (ngModelChange)="setDraftValue(item.definition.key, $event)"
                        [disabled]="isSaving(item.definition.key)"
                        required
                      >
                        <option value="">Sélectionner une valeur</option>
                        <option value="true">true</option>
                        <option value="false">false</option>
                      </select>
                    } @else {
                      <input
                        [id]="'value-' + item.definition.key"
                        name="value-{{ item.definition.key }}"
                        [type]="isNumeric(item.definition) ? 'number' : 'text'"
                        [attr.min]="
                          isNumeric(item.definition) ? item.definition.minimumValue : null
                        "
                        [attr.max]="
                          isNumeric(item.definition) ? item.definition.maximumValue : null
                        "
                        [attr.maxlength]="isNumeric(item.definition) ? null : 2048"
                        [ngModel]="draftValue(item.definition.key)"
                        (ngModelChange)="setDraftValue(item.definition.key, $event)"
                        [disabled]="isSaving(item.definition.key)"
                        required
                      />
                    }

                    <label [attr.for]="'update-reason-' + item.definition.key">
                      Motif de modification
                    </label>
                    <textarea
                      [id]="'update-reason-' + item.definition.key"
                      name="update-reason-{{ item.definition.key }}"
                      [ngModel]="updateReason(item.definition.key)"
                      (ngModelChange)="setUpdateReason(item.definition.key, $event)"
                      [disabled]="isSaving(item.definition.key)"
                      maxlength="1024"
                      required
                    ></textarea>

                    @if (operationError(item.definition.key)) {
                      <p class="sp-error" role="alert">
                        {{ operationError(item.definition.key) }}
                      </p>
                    }

                    <button
                      type="submit"
                      [disabled]="!canSubmit(item) || isSaving(item.definition.key)"
                    >
                      {{ isSaving(item.definition.key) ? 'Enregistrement…' : 'Enregistrer' }}
                    </button>
                  </form>

                  <details (toggle)="historyToggled(item, $event)">
                    <summary>Historique</summary>

                    @if (isHistoryLoading(item.definition.key)) {
                      <p>Chargement de l'historique…</p>
                    } @else if (historyError(item.definition.key)) {
                      <p class="sp-error" role="alert">{{ historyError(item.definition.key) }}</p>
                    } @else if (item.history === null) {
                      <p>Ouvrez cette section pour charger l'historique.</p>
                    } @else if (item.history.length === 0) {
                      <p>Aucun changement enregistré.</p>
                    } @else {
                      <ul class="sp-history">
                        @for (entry of item.history; track entry.historyId) {
                          <li>
                            <strong>v{{ entry.newVersion }}</strong>
                            — {{ entry.newValue }} — {{ entry.changedBy }} — {{ entry.reason }}

                            <label
                              [attr.for]="
                                'rollback-reason-' + item.definition.key + '-' + entry.newVersion
                              "
                            >
                              Motif de restauration
                            </label>
                            <textarea
                              [id]="
                                'rollback-reason-' + item.definition.key + '-' + entry.newVersion
                              "
                              name="rollback-reason-{{ item.definition.key }}-{{
                                entry.newVersion
                              }}"
                              [ngModel]="rollbackReason(item.definition.key)"
                              (ngModelChange)="setRollbackReason(item.definition.key, $event)"
                              [disabled]="isRollingBack(item.definition.key)"
                              maxlength="1024"
                            ></textarea>

                            <button
                              type="button"
                              (click)="rollback(item, entry.newVersion)"
                              [disabled]="
                                entry.newVersion === item.value?.version ||
                                !canRollback(item.definition.key) ||
                                isRollingBack(item.definition.key)
                              "
                            >
                              {{
                                isRollingBack(item.definition.key)
                                  ? 'Restauration…'
                                  : 'Restaurer cette version'
                              }}
                            </button>
                          </li>
                        }
                      </ul>
                    }
                  </details>
                </sp-card>
              }
            </div>
          </section>
        }
      }
    </section>
  `,
  styles: `
    :host,
    .sp-page,
    .sp-domain,
    .sp-grid,
    .sp-form,
    .sp-details {
      display: grid;
      gap: var(--sp-space-3);
    }

    .sp-page {
      gap: var(--sp-space-4);
    }

    .sp-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .sp-details {
      grid-template-columns: repeat(2, minmax(0, 1fr));
      margin: 0;
    }

    .sp-details div {
      display: grid;
      gap: 0.25rem;
    }

    .sp-details dt {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
    }

    .sp-details dd {
      margin: 0;
      overflow-wrap: anywhere;
    }

    .sp-form label,
    .sp-history label {
      display: grid;
      gap: 0.25rem;
    }

    .sp-form input,
    .sp-form select,
    .sp-form textarea,
    .sp-history textarea {
      width: 100%;
      box-sizing: border-box;
    }

    .sp-error {
      color: var(--mat-sys-error);
    }

    .sp-history {
      display: grid;
      gap: var(--sp-space-3);
      padding-left: 1.25rem;
    }

    @media (max-width: 900px) {
      .sp-grid,
      .sp-details {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class DynamicSettingsPageComponent {
  private readonly service = inject(DynamicSettingsService);

  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);
  protected readonly settings = signal<readonly DynamicSettingView[]>([]);

  private readonly draftValues = signal<Record<string, string>>({});
  private readonly updateReasons = signal<Record<string, string>>({});
  private readonly rollbackReasons = signal<Record<string, string>>({});
  private readonly saving = signal<Record<string, boolean>>({});
  private readonly rollingBack = signal<Record<string, boolean>>({});
  private readonly historyLoading = signal<Record<string, boolean>>({});
  private readonly historyErrors = signal<Record<string, string | null>>({});
  private readonly operationErrors = signal<Record<string, string | null>>({});

  protected readonly groupedSettings = computed(() => {
    const byDomain = new Map<DynamicSettingDomain, DynamicSettingView[]>();

    for (const item of this.settings()) {
      const group = byDomain.get(item.definition.domain) ?? [];
      group.push(item);
      byDomain.set(item.definition.domain, group);
    }

    return [...byDomain.entries()]
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([domain, items]) => ({
        domain,
        items: [...items].sort((left, right) =>
          left.definition.key.localeCompare(right.definition.key),
        ),
      }));
  });

  constructor() {
    this.load();
  }

  protected draftValue(key: string): string {
    return this.draftValues()[key] ?? '';
  }

  protected updateReason(key: string): string {
    return this.updateReasons()[key] ?? '';
  }

  protected rollbackReason(key: string): string {
    return this.rollbackReasons()[key] ?? '';
  }

  protected setDraftValue(key: string, value: string): void {
    this.draftValues.update((current) => ({ ...current, [key]: value }));
  }

  protected setUpdateReason(key: string, reason: string): void {
    this.updateReasons.update((current) => ({ ...current, [key]: reason }));
  }

  protected setRollbackReason(key: string, reason: string): void {
    this.rollbackReasons.update((current) => ({ ...current, [key]: reason }));
  }

  protected isSaving(key: string): boolean {
    return this.saving()[key] === true;
  }

  protected isRollingBack(key: string): boolean {
    return this.rollingBack()[key] === true;
  }

  protected isHistoryLoading(key: string): boolean {
    return this.historyLoading()[key] === true;
  }

  protected historyError(key: string): string | null {
    return this.historyErrors()[key] ?? null;
  }

  protected operationError(key: string): string | null {
    return this.operationErrors()[key] ?? null;
  }

  protected isNumeric(definition: DynamicSettingDefinition): boolean {
    return definition.type === 'INTEGER' || definition.type === 'DECIMAL';
  }

  protected canSubmit(item: DynamicSettingView): boolean {
    return (
      this.draftValue(item.definition.key).trim().length > 0 &&
      this.updateReason(item.definition.key).trim().length > 0 &&
      this.isDraftCompatibleWithDefinition(item.definition, this.draftValue(item.definition.key))
    );
  }

  protected canRollback(key: string): boolean {
    return this.rollbackReason(key).trim().length > 0;
  }

  protected update(item: DynamicSettingView): void {
    const key = item.definition.key;
    if (!this.canSubmit(item) || this.isSaving(key)) {
      return;
    }

    this.setOperationError(key, null);
    this.setFlag(this.saving, key, true);

    this.service
      .update(key, {
        value: this.draftValue(key).trim(),
        reason: this.updateReason(key).trim(),
      })
      .subscribe({
        next: (value) => {
          this.replaceItem(key, { ...item, value });
          this.setDraftValue(key, '');
          this.setUpdateReason(key, '');
          this.setFlag(this.saving, key, false);
        },
        error: (cause) => {
          this.setOperationError(key, this.describeError(cause, 'Modification impossible.'));
          this.setFlag(this.saving, key, false);
        },
      });
  }

  protected rollback(item: DynamicSettingView, targetVersion: number): void {
    const key = item.definition.key;
    const reason = this.rollbackReason(key).trim();

    if (!reason || this.isRollingBack(key)) {
      return;
    }

    if (!window.confirm(`Restaurer ${key} à la version ${targetVersion} ?`)) {
      return;
    }

    this.setOperationError(key, null);
    this.setFlag(this.rollingBack, key, true);

    this.service
      .rollback(key, {
        targetVersion,
        reason,
      })
      .subscribe({
        next: (value) => {
          this.replaceItem(key, { ...item, value });
          this.setRollbackReason(key, '');
          this.setFlag(this.rollingBack, key, false);
          this.loadHistory(key);
        },
        error: (cause) => {
          this.setOperationError(key, this.describeError(cause, 'Restauration impossible.'));
          this.setFlag(this.rollingBack, key, false);
        },
      });
  }

  protected historyToggled(item: DynamicSettingView, event: Event): void {
    const details = event.currentTarget as HTMLDetailsElement;
    if (details.open && item.history === null && !this.isHistoryLoading(item.definition.key)) {
      this.loadHistory(item.definition.key);
    }
  }

  private load(): void {
    this.loading.set(true);
    this.error.set(null);

    this.service.definitions().subscribe({
      next: (definitions) => {
        if (definitions.length === 0) {
          this.settings.set([]);
          this.loading.set(false);
          return;
        }

        forkJoin(definitions.map((definition) => this.service.value(definition.key))).subscribe({
          next: (values) => {
            const valuesByKey = new Map(values.map((value) => [value.key, value]));
            this.settings.set(
              definitions.map((definition) => ({
                definition,
                value: valuesByKey.get(definition.key) ?? null,
                history: null,
              })),
            );
            this.loading.set(false);
          },
          error: (cause) => {
            this.error.set(this.describeError(cause, 'Chargement des valeurs impossible.'));
            this.settings.set(
              definitions.map((definition) => ({
                definition,
                value: null,
                history: null,
              })),
            );
            this.loading.set(false);
          },
        });
      },
      error: (cause) => {
        this.error.set(this.describeError(cause, 'Chargement du catalogue impossible.'));
        this.settings.set([]);
        this.loading.set(false);
      },
    });
  }

  private loadHistory(key: string): void {
    this.setHistoryError(key, null);
    this.setFlag(this.historyLoading, key, true);

    this.service.history(key).subscribe({
      next: (history) => {
        const current = this.settings().find((item) => item.definition.key === key);
        if (current) {
          this.replaceItem(key, { ...current, history });
        }
        this.setFlag(this.historyLoading, key, false);
      },
      error: (cause) => {
        this.setHistoryError(
          key,
          this.describeError(cause, "Chargement de l'historique impossible."),
        );
        this.setFlag(this.historyLoading, key, false);
      },
    });
  }

  private replaceItem(key: string, replacement: DynamicSettingView): void {
    this.settings.update((current) =>
      current.map((item) => (item.definition.key === key ? replacement : item)),
    );
  }

  private isDraftCompatibleWithDefinition(
    definition: DynamicSettingDefinition,
    rawValue: string,
  ): boolean {
    const value = rawValue.trim();

    if (definition.allowedValues.length > 0) {
      return definition.allowedValues.includes(value);
    }

    if (definition.type === 'BOOLEAN') {
      return value === 'true' || value === 'false';
    }

    if (definition.type === 'INTEGER') {
      if (!/^-?\d+$/.test(value)) {
        return false;
      }
      return this.withinNumericBounds(Number(value), definition);
    }

    if (definition.type === 'DECIMAL') {
      const number = Number(value);
      return Number.isFinite(number) && this.withinNumericBounds(number, definition);
    }

    return value.length > 0;
  }

  private withinNumericBounds(value: number, definition: DynamicSettingDefinition): boolean {
    const minimum = definition.minimumValue === null ? null : Number(definition.minimumValue);
    const maximum = definition.maximumValue === null ? null : Number(definition.maximumValue);

    return (minimum === null || value >= minimum) && (maximum === null || value <= maximum);
  }

  private describeError(cause: unknown, fallback: string): string {
    if (cause instanceof HttpErrorResponse) {
      if (cause.status === 400) {
        return `${fallback} Requête invalide.`;
      }
      if (cause.status === 401) {
        return `${fallback} Authentification requise.`;
      }
      if (cause.status === 403) {
        return `${fallback} Accès administrateur requis.`;
      }
      if (cause.status === 404) {
        return `${fallback} Paramètre ou version introuvable.`;
      }
    }

    return fallback;
  }

  private setFlag(
    target: {
      update: (updater: (current: Record<string, boolean>) => Record<string, boolean>) => void;
    },
    key: string,
    value: boolean,
  ): void {
    target.update((current) => ({ ...current, [key]: value }));
  }

  private setHistoryError(key: string, value: string | null): void {
    this.historyErrors.update((current) => ({ ...current, [key]: value }));
  }

  private setOperationError(key: string, value: string | null): void {
    this.operationErrors.update((current) => ({ ...current, [key]: value }));
  }
}
