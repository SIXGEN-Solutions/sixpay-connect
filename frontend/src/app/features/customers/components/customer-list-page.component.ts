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
import { ObservedCustomerSearchQuery, ObservedCustomerSort } from '../models/customer-query';
import {
  OBSERVED_CUSTOMER_PAYMENT_STATUSES,
  ObservedCustomerPaymentStatusResponse,
} from '../models/customers.response';
import { ObservedCustomerSearchPage } from '../models/customers';
import { CustomersService } from '../services/customers.service';

@Component({
  selector: 'sp-customer-list-page',
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
        title="Clients observés"
        description="Projection CQRS non autoritative, masquée et en lecture seule."
      />

      <sp-mock-state-panel />

      <sp-card title="Recherche">
        <form class="sp-filter-grid" [formGroup]="form" (ngSubmit)="search()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>NIU</mat-label>
            <input matInput formControlName="niu" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Nom légal</mat-label>
            <input matInput formControlName="legalName" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Institution</mat-label>
            <input matInput formControlName="financialInstitutionCode" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Dernier statut Payment</mat-label>
            <mat-select formControlName="lastPaymentStatus">
              <mat-option value="">Tous</mat-option>
              @for (status of statuses; track status) {
                <mat-option [value]="status">{{ status }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Dernier reason code</mat-label>
            <input matInput formControlName="lastFailureReasonCode" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Tri</mat-label>
            <mat-select formControlName="sort">
              <mat-option value="LAST_OBSERVED_AT_DESC">Dernière observation décroissante</mat-option>
              <mat-option value="LAST_OBSERVED_AT_ASC">Dernière observation croissante</mat-option>
              <mat-option value="FIRST_OBSERVED_AT_DESC">Première observation décroissante</mat-option>
              <mat-option value="FIRST_OBSERVED_AT_ASC">Première observation croissante</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Taille</mat-label>
            <mat-select formControlName="size">
              <mat-option [value]="1">1</mat-option>
              <mat-option [value]="10">10</mat-option>
              <mat-option [value]="50">50</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="sp-actions">
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()">Réinitialiser</sp-button>
          </div>
        </form>
      </sp-card>

      <sp-card title="Résultats">
        @switch (scenario.scenario()) {
          @case ('loading') {
            <sp-mock-content-state
              kind="loading"
              title="Chargement des clients observés"
              message="Simulation d'une consultation ObservedCustomer."
            />
          }
          @case ('empty') {
            <sp-mock-content-state
              kind="empty"
              title="Aucun client observé"
              message="Aucun résultat ne correspond aux critères sélectionnés."
            />
          }
          @case ('error') {
            <sp-mock-content-state
              kind="error"
              title="Consultation indisponible"
              message="Erreur simulée. Aucun backend n'est appelé pendant la Phase 7.4."
            />
          }
          @default {
            @if (page(); as currentPage) {
              <div class="sp-meta">
                <span>{{ currentPage.size }} résultat(s)</span>
                <span>Snapshot {{ currentPage.snapshotAt | date: 'dd/MM/yyyy HH:mm:ss' }}</span>
              </div>

              @if (currentPage.items.length === 0) {
                <sp-mock-content-state
                  kind="empty"
                  title="Aucun client observé"
                  message="Aucun résultat ne correspond aux critères sélectionnés."
                />
              } @else {
                <div class="sp-table-scroll">
                  <table class="sp-table">
                    <thead>
                      <tr>
                        <th>Nom légal</th>
                        <th>NIU</th>
                        <th>Dernier statut</th>
                        <th>Paiements</th>
                        <th>Succès</th>
                        <th>Échecs</th>
                        <th>Dernière observation</th>
                        <th>Projection</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (customer of currentPage.items; track customer.observedCustomerId) {
                        <tr>
                          <td>
                            <a [routerLink]="[customer.observedCustomerId]">
                              {{ customer.legalName }}
                            </a>
                          </td>
                          <td>{{ customer.niu.maskedValue }}</td>
                          <td>{{ customer.lastPaymentStatus ?? '—' }}</td>
                          <td>{{ customer.totalPayments }}</td>
                          <td>{{ customer.successfulPayments }}</td>
                          <td>{{ customer.failedPayments }}</td>
                          <td>{{ customer.lastObservedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                          <td>v{{ customer.projectionVersion }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>

                <div class="sp-pagination">
                  <sp-button
                    icon="arrow_back"
                    [disabled]="cursorHistory().length === 0"
                    (buttonClick)="previousPage()"
                  >
                    Précédent
                  </sp-button>

                  <sp-button
                    icon="arrow_forward"
                    [disabled]="!currentPage.hasMore"
                    (buttonClick)="nextPage()"
                  >
                    Suivant
                  </sp-button>
                </div>
              }
            }
          }
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-filter-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-actions{display:flex;gap:var(--sp-space-2);align-items:center}
    .sp-meta,.sp-pagination{display:flex;justify-content:space-between;gap:var(--sp-space-3);align-items:center}
    .sp-meta{margin-bottom:var(--sp-space-3);color:var(--mat-sys-on-surface-variant);font-size:.875rem}
    .sp-pagination{margin-top:var(--sp-space-3)}
    .sp-table-scroll{overflow-x:auto}
    .sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{
      padding:var(--sp-space-2);
      text-align:left;
      border-bottom:1px solid var(--mat-sys-outline-variant);
      white-space:nowrap
    }
    @media(max-width:1000px){.sp-filter-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}
    @media(max-width:700px){
      .sp-filter-grid{grid-template-columns:1fr}
      .sp-meta{align-items:flex-start;flex-direction:column}
    }
  `,
})
export class CustomerListPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly customers = inject(CustomersService);

  protected readonly statuses = OBSERVED_CUSTOMER_PAYMENT_STATUSES;
  protected readonly page = signal<ObservedCustomerSearchPage | null>(null);
  protected readonly cursorHistory = signal<string[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    niu: [''],
    legalName: [''],
    financialInstitutionCode: [''],
    lastPaymentStatus: [''],
    lastFailureReasonCode: [''],
    sort: ['LAST_OBSERVED_AT_DESC' as ObservedCustomerSort],
    size: [1],
  });

  constructor() {
    this.search();
  }

  protected search(cursor?: string): void {
    const value = this.form.getRawValue();

    const query: ObservedCustomerSearchQuery = {
      sort: value.sort,
      size: value.size,
      ...(value.niu.trim() ? { niu: value.niu.trim() } : {}),
      ...(value.legalName.trim() ? { legalName: value.legalName.trim() } : {}),
      ...(value.financialInstitutionCode.trim()
        ? { financialInstitutionCode: value.financialInstitutionCode.trim() }
        : {}),
      ...(value.lastPaymentStatus
        ? {
            lastPaymentStatus:
              value.lastPaymentStatus as ObservedCustomerPaymentStatusResponse,
          }
        : {}),
      ...(value.lastFailureReasonCode.trim()
        ? { lastFailureReasonCode: value.lastFailureReasonCode.trim() }
        : {}),
      ...(cursor ? { cursor } : {}),
    };

    this.customers.search(query).subscribe((page) => this.page.set(page));
  }

  protected reset(): void {
    this.form.reset({
      niu: '',
      legalName: '',
      financialInstitutionCode: '',
      lastPaymentStatus: '',
      lastFailureReasonCode: '',
      sort: 'LAST_OBSERVED_AT_DESC',
      size: 1,
    });
    this.cursorHistory.set([]);
    this.search();
  }

  protected nextPage(): void {
    const nextCursor = this.page()?.nextCursor;
    if (!nextCursor) {
      return;
    }

    const currentCursor = this.cursorHistory().at(-1) ?? '';
    this.cursorHistory.update((history) => [...history, currentCursor || '0']);
    this.search(nextCursor);
  }

  protected previousPage(): void {
    const history = [...this.cursorHistory()];
    const previousCursor = history.pop();
    this.cursorHistory.set(history);
    this.search(previousCursor === '0' ? undefined : previousCursor);
  }
}
