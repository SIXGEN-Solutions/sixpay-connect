import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { AccountingBatchDetail } from '../models/accounting';
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
    MockContentStateComponent,
    MockStatePanelComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Comptabilisation"
        description="Vue opérationnelle mockée des lots comptables et de la réconciliation."
      />
      <sp-mock-state-panel />

      <sp-card title="Filtres">
        <form class="sp-filter-grid" [formGroup]="form" (ngSubmit)="search()">
          <mat-form-field appearance="outline">
            <mat-label>Business date</mat-label>
            <input matInput formControlName="businessDate" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Tous</mat-option>
              @for (status of ['READY','SUBMITTED','ACCEPTED','RECONCILING','COMPLETED','FAILED']; track status) {
                <mat-option [value]="status">{{ status }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Réconciliation</mat-label>
            <mat-select formControlName="reconciliationStatus">
              <mat-option value="">Toutes</mat-option>
              <mat-option value="MATCHED">MATCHED</mat-option>
              <mat-option value="PARTIAL_MATCH">PARTIAL_MATCH</mat-option>
              <mat-option value="UNMATCHED">UNMATCHED</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="sp-actions">
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()">Réinitialiser</sp-button>
          </div>
        </form>
      </sp-card>

      @switch (scenario.scenario()) {
        @case ('loading') {
          <sp-mock-content-state
            kind="loading"
            title="Chargement comptable"
            message="Simulation de consultation des lots."
          />
        }
        @case ('empty') {
          <sp-mock-content-state
            kind="empty"
            title="Aucun lot"
            message="Aucun lot ne correspond aux critères."
          />
        }
        @case ('error') {
          <sp-mock-content-state
            kind="error"
            title="Comptabilisation indisponible"
            message="Erreur simulée sans appel backend."
          />
        }
        @default {
          <div class="sp-kpis">
            <sp-card title="Lots visibles">
              <strong>{{ batches().length }}</strong>
            </sp-card>
            <sp-card title="Écritures">
              <strong>{{ totalItems() }}</strong>
              <p>{{ reconciledItems() }} réconciliées</p>
            </sp-card>
            <sp-card title="Écarts">
              <strong>{{ discrepancies() }}</strong>
              <p>À investiguer</p>
            </sp-card>
          </div>

          <sp-card title="Lots">
            @if (batches().length === 0) {
              <sp-mock-content-state
                kind="empty"
                title="Aucun lot"
                message="Aucun résultat ne correspond aux critères."
              />
            } @else {
              <div class="sp-table-scroll">
                <table class="sp-table">
                  <thead>
                    <tr>
                      <th>Lot</th>
                      <th>Business date</th>
                      <th>Fenêtre</th>
                      <th>Items</th>
                      <th>Réconciliés</th>
                      <th>Statut</th>
                      <th>Réconciliation</th>
                      <th>Mis à jour</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (batch of batches(); track batch.batchId) {
                      <tr>
                        <td><a [routerLink]="['batches', batch.batchId]">{{ batch.batchId }}</a></td>
                        <td>{{ batch.businessDate }}</td>
                        <td>{{ batch.windowLabel }}</td>
                        <td>{{ batch.itemCount }}</td>
                        <td>{{ batch.reconciledCount }}</td>
                        <td>{{ batch.status }}</td>
                        <td>{{ batch.reconciliationStatus }}</td>
                        <td>{{ batch.updatedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          </sp-card>
        }
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-filter-grid,.sp-kpis{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-actions{display:flex;gap:var(--sp-space-2);align-items:center}
    .sp-kpis strong{font-size:2rem}
    .sp-table-scroll{overflow-x:auto}
    .sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant);white-space:nowrap}
    @media(max-width:850px){.sp-filter-grid,.sp-kpis{grid-template-columns:1fr}}
  `,
})
export class AccountingOverviewPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly accounting = inject(AccountingService);

  protected readonly batches = signal<readonly AccountingBatchDetail[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    businessDate: ['2026-08-08'],
    status: [''],
    reconciliationStatus: [''],
  });

  constructor() {
    this.search();
  }

  protected search(): void {
    const value = this.form.getRawValue();
    this.accounting
      .search({
        ...(value.businessDate.trim() ? { businessDate: value.businessDate.trim() } : {}),
        ...(value.status ? { status: value.status as AccountingBatchDetail['status'] } : {}),
        ...(value.reconciliationStatus
          ? {
              reconciliationStatus:
                value.reconciliationStatus as AccountingBatchDetail['reconciliationStatus'],
            }
          : {}),
      })
      .subscribe((batches) => this.batches.set(batches));
  }

  protected reset(): void {
    this.form.reset({
      businessDate: '2026-08-08',
      status: '',
      reconciliationStatus: '',
    });
    this.search();
  }

  protected totalItems(): number {
    return this.batches().reduce((total, batch) => total + batch.itemCount, 0);
  }

  protected reconciledItems(): number {
    return this.batches().reduce((total, batch) => total + batch.reconciledCount, 0);
  }

  protected discrepancies(): number {
    return this.batches().reduce((total, batch) => total + batch.discrepancyCount, 0);
  }
}
