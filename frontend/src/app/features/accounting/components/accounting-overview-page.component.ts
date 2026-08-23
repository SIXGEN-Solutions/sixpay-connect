import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  AccountingBatchStatus,
  AccountingBatchSummary,
} from '../models/accounting';
import { AccountingBatchQuery } from '../models/accounting-query';
import { AccountingService } from '../services/accounting.service';

@Component({
  selector: 'sp-accounting-overview-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Comptabilisation"
        description="Consultation des lots comptables."
      />

      <sp-card title="Filtres">
        <form
          class="sp-filter-grid"
          [formGroup]="form"
          (ngSubmit)="search()"
        >
          <mat-form-field appearance="outline">
            <mat-label>Business date</mat-label>
            <input
              matInput
              formControlName="businessDate"
              placeholder="YYYY-MM-DD"
            />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>

            <mat-select formControlName="status">
              <mat-option value="">Tous</mat-option>

              @for (status of statuses; track status) {
                <mat-option [value]="status">
                  {{ status }}
                </mat-option>
              }
            </mat-select>
          </mat-form-field>

          <div class="sp-actions">
            <sp-button
              type="submit"
              icon="search"
            >
              Rechercher
            </sp-button>

            <sp-button
              type="button"
              icon="restart_alt"
              (buttonClick)="reset()"
            >
              Réinitialiser
            </sp-button>
          </div>
        </form>
      </sp-card>

      <div class="sp-kpis">
        <sp-card title="Lots visibles">
          <strong>{{ batches().length }}</strong>
        </sp-card>

        <sp-card title="Écritures">
          <strong>{{ totalItems() }}</strong>
        </sp-card>
      </div>

      <sp-card title="Lots comptables">
        @if (batches().length === 0) {
          <p class="sp-empty">
            Aucun lot ne correspond aux critères.
          </p>
        } @else {
          <div class="sp-table-scroll">
            <table class="sp-table">
              <thead>
                <tr>
                  <th>Lot</th>
                  <th>Business date</th>
                  <th>Institution</th>
                  <th>Items</th>
                  <th>Statut</th>
                  <th>Créé le</th>
                </tr>
              </thead>

              <tbody>
                @for (batch of batches(); track batch.batchId) {
                  <tr>
                    <td>
                      <a
                        [routerLink]="[
                          'batches',
                          batch.batchId
                        ]"
                      >
                        {{ batch.batchId }}
                      </a>
                    </td>

                    <td>
                      {{ batch.businessDate }}
                    </td>

                    <td>
                      {{ batch.financialInstitutionCode }}
                    </td>

                    <td>
                      {{ batch.itemCount }}
                    </td>

                    <td>
                      {{ batch.status }}
                    </td>

                    <td>
                      {{ batch.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }

    .sp-filter-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
      gap: var(--sp-space-3);
      align-items: start;
    }

    .sp-actions {
      display: flex;
      gap: var(--sp-space-2);
      align-items: center;
      min-height: 56px;
    }

    .sp-kpis {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    .sp-kpis strong {
      font-size: 2rem;
    }

    .sp-table-scroll {
      overflow-x: auto;
    }

    .sp-table {
      width: 100%;
      border-collapse: collapse;
    }

    .sp-table th,
    .sp-table td {
      padding: var(--sp-space-2);
      text-align: left;
      border-bottom:
        1px solid var(--mat-sys-outline-variant);
      white-space: nowrap;
    }

    .sp-empty {
      margin: 0;
    }

    @media (max-width: 850px) {
      .sp-filter-grid,
      .sp-kpis {
        grid-template-columns: 1fr;
      }

      .sp-actions {
        min-height: auto;
      }
    }
  `,
})
export class AccountingOverviewPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly accounting = inject(AccountingService);

  protected readonly batches =
    signal<readonly AccountingBatchSummary[]>([]);

  protected readonly statuses: readonly AccountingBatchStatus[] = [
    'NOT_COMPLETED',
    'COMPLETED'
  ];

  protected readonly form =
    this.formBuilder.nonNullable.group({
      businessDate: [''],
      status: [''],
    });

  constructor() {
    this.search();
  }

  protected search(): void {
    const value = this.form.getRawValue();

    const query: AccountingBatchQuery = {
      ...(value.businessDate.trim()
        ? {
            businessDate: value.businessDate.trim(),
          }
        : {}),
      ...(value.status
        ? {
            status: value.status as AccountingBatchStatus,
          }
        : {}),
    };

    this.accounting
      .search(query)
      .subscribe((batches) => {
        this.batches.set(batches);
      });
  }

  protected reset(): void {
    this.form.reset({
      businessDate: '',
      status: '',
    });

    this.search();
  }

  protected totalItems(): number {
    return this.batches().reduce(
      (total, batch) => total + batch.itemCount,
      0,
    );
  }
}