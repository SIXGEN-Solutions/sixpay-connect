import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

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
  readonly history: readonly DynamicSettingHistoryEntry[];
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
                      <dd>{{ item.value?.version ?? 1 }}</dd>
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
                    <label>
                      Nouvelle valeur
                      <input
                        name="value-{{ item.definition.key }}"
                        [ngModel]="draftValue(item.definition.key)"
                        (ngModelChange)="setDraftValue(item.definition.key, $event)"
                        required
                      />
                    </label>

                    <label>
                      Motif
                      <textarea
                        name="reason-{{ item.definition.key }}"
                        [ngModel]="draftReason(item.definition.key)"
                        (ngModelChange)="setDraftReason(item.definition.key, $event)"
                        required
                      ></textarea>
                    </label>

                    <button type="submit" [disabled]="!canSubmit(item.definition.key)">
                      Enregistrer
                    </button>
                  </form>

                  <details>
                    <summary>Historique</summary>

                    @if (item.history.length === 0) {
                      <p>Aucun changement enregistré.</p>
                    } @else {
                      <ul class="sp-history">
                        @for (entry of item.history; track entry.historyId) {
                          <li>
                            <strong>v{{ entry.newVersion }}</strong>
                            — {{ entry.newValue }} — {{ entry.changedBy }} — {{ entry.reason }}

                            <button
                              type="button"
                              (click)="rollback(item, entry.newVersion)"
                              [disabled]="entry.newVersion === item.value?.version"
                            >
                              Restaurer cette version
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

    .sp-form label {
      display: grid;
      gap: 0.25rem;
    }

    .sp-form input,
    .sp-form textarea {
      width: 100%;
      box-sizing: border-box;
    }

    .sp-history {
      display: grid;
      gap: var(--sp-space-2);
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
  protected readonly settings = signal<readonly DynamicSettingView[]>([]);
  private readonly draftValues = signal<Record<string, string>>({});
  private readonly draftReasons = signal<Record<string, string>>({});

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

  protected draftReason(key: string): string {
    return this.draftReasons()[key] ?? '';
  }

  protected setDraftValue(key: string, value: string): void {
    this.draftValues.update((current) => ({ ...current, [key]: value }));
  }

  protected setDraftReason(key: string, reason: string): void {
    this.draftReasons.update((current) => ({ ...current, [key]: reason }));
  }

  protected canSubmit(key: string): boolean {
    return this.draftValue(key).trim().length > 0 && this.draftReason(key).trim().length > 0;
  }

  protected update(item: DynamicSettingView): void {
    if (!this.canSubmit(item.definition.key)) {
      return;
    }

    this.service
      .update(item.definition.key, {
        value: this.draftValue(item.definition.key).trim(),
        reason: this.draftReason(item.definition.key).trim(),
      })
      .subscribe(() => {
        this.setDraftValue(item.definition.key, '');
        this.setDraftReason(item.definition.key, '');
        this.load();
      });
  }

  protected rollback(item: DynamicSettingView, targetVersion: number): void {
    const reason = this.draftReason(item.definition.key).trim();

    if (!reason) {
      return;
    }

    if (!window.confirm(`Restaurer ${item.definition.key} à la version ${targetVersion} ?`)) {
      return;
    }

    this.service
      .rollback(item.definition.key, {
        targetVersion,
        reason,
      })
      .subscribe(() => this.load());
  }

  private load(): void {
    this.loading.set(true);

    this.service.definitions().subscribe((definitions) => {
      if (definitions.length === 0) {
        this.settings.set([]);
        this.loading.set(false);
        return;
      }

      const aggregate = new Map<string, DynamicSettingView>();
      let remaining = definitions.length * 2;

      const completeOne = () => {
        remaining -= 1;
        if (remaining === 0) {
          this.settings.set(
            definitions.map(
              (definition) =>
                aggregate.get(definition.key) ?? {
                  definition,
                  value: null,
                  history: [],
                },
            ),
          );
          this.loading.set(false);
        }
      };

      for (const definition of definitions) {
        aggregate.set(definition.key, {
          definition,
          value: null,
          history: [],
        });

        this.service.value(definition.key).subscribe({
          next: (value) => {
            aggregate.set(definition.key, {
              ...(aggregate.get(definition.key) as DynamicSettingView),
              value,
            });
            completeOne();
          },
          error: completeOne,
        });

        this.service.history(definition.key).subscribe({
          next: (history) => {
            aggregate.set(definition.key, {
              ...(aggregate.get(definition.key) as DynamicSettingView),
              history,
            });
            completeOne();
          },
          error: completeOne,
        });
      }
    });
  }
}
