import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { PaymentSearchQuery, PaymentSort } from '../models/payment-query';
import { PAYMENT_STATUSES } from '../models/payments.response';
import { PaymentSearchPage, PaymentSummary } from '../models/payments';
import { PaymentsService } from '../services/payments.service';

@Component({
  selector: 'sp-payment-list-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
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
        title="Paiements"
        description="Consultation des projections Payment masquées de SIXPAY."
      />
      <sp-mock-state-panel />

      <sp-card title="Recherche">
        <form class="sp-filter-grid" [formGroup]="form" (ngSubmit)="search()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Référence Payment</mat-label>
            <input matInput formControlName="paymentReference" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>TresorPay Request ID</mat-label>
            <input matInput formControlName="tresorPayRequestId" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Observed Customer ID</mat-label>
            <input matInput formControlName="observedCustomerId" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Institution</mat-label>
            <input matInput formControlName="financialInstitutionCode" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Tous</mat-option>
              @for (status of statuses; track status) {
                <mat-option [value]="status">{{ status }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Reason code</mat-label>
            <input matInput formControlName="reasonCode" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Montant min.</mat-label>
            <input matInput type="number" min="0" formControlName="amountMin" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Montant max.</mat-label>
            <input matInput type="number" min="0" formControlName="amountMax" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Devise</mat-label>
            <input matInput maxlength="3" formControlName="currency" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Tri</mat-label>
            <mat-select formControlName="sort">
              <mat-option value="CREATED_AT_DESC">Création décroissante</mat-option>
              <mat-option value="CREATED_AT_ASC">Création croissante</mat-option>
              <mat-option value="UPDATED_AT_DESC">Mise à jour décroissante</mat-option>
              <mat-option value="UPDATED_AT_ASC">Mise à jour croissante</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Taille</mat-label>
            <mat-select formControlName="size">
              <mat-option [value]="2">2</mat-option>
              <mat-option [value]="10">10</mat-option>
              <mat-option [value]="50">50</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="sp-filter-actions">
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()"
              >Réinitialiser</sp-button
            >
          </div>
        </form>
      </sp-card>

      <sp-card title="Résultats">
        @switch (scenario.scenario()) {
          @case ('loading') {
            <sp-mock-content-state
              kind="loading"
              title="Chargement des paiements"
              message="Simulation d'une consultation Payment en cours."
            />
          }
          @case ('empty') {
            <sp-mock-content-state
              kind="empty"
              title="Aucun paiement"
              message="Aucun paiement ne correspond aux critères sélectionnés."
            />
          }
          @case ('error') {
            <sp-mock-content-state
              kind="error"
              title="Consultation indisponible"
              message="Erreur simulée. Aucun backend n'est appelé pendant la Phase 7.2."
            />
          }
          @default {
            @if (page(); as currentPage) {
              <div class="sp-result-meta">
                <span>{{ currentPage.size }} résultat(s) sur cette page</span>
                <span>Snapshot {{ currentPage.snapshotAt | date: 'dd/MM/yyyy HH:mm:ss' }}</span>
              </div>

              @if (currentPage.items.length === 0) {
                <sp-mock-content-state
                  kind="empty"
                  title="Aucun paiement"
                  message="Aucun paiement ne correspond aux critères sélectionnés."
                />
              } @else {
                <div class="sp-table-scroll">
                  <table class="sp-table">
                    <thead>
                      <tr>
                        <th>Référence</th>
                        <th>TresorPay</th>
                        <th>Compte</th>
                        <th>Institution</th>
                        <th>Montant</th>
                        <th>Statut</th>
                        <th>Reason code</th>
                        <th>Créé</th>
                      </tr>
                    </thead>
                    <tbody>
                      @for (payment of currentPage.items; track payment.paymentId) {
                        <tr>
                          <td>
                            <a [routerLink]="[payment.paymentId]">{{ payment.paymentReference }}</a>
                          </td>
                          <td>{{ payment.tresorPayRequestId }}</td>
                          <td>{{ payment.debtorAccount?.maskedValue ?? '—' }}</td>
                          <td>{{ payment.financialInstitutionCode }}</td>
                          <td>
                            {{
                              payment.amount.amount
                                | currency: payment.amount.currency : 'code' : '1.0-0'
                            }}
                          </td>
                          <td>
                            <strong>{{ payment.status }}</strong>
                          </td>
                          <td>{{ payment.reasonCode ?? '—' }}</td>
                          <td>{{ payment.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                        </tr>
                      }
                    </tbody>
                  </table>
                </div>

                <div class="sp-pagination">
                  <sp-button
                    type="button"
                    icon="arrow_back"
                    [disabled]="cursorHistory().length === 0"
                    (buttonClick)="previousPage()"
                  >
                    Précédent
                  </sp-button>
                  <sp-button
                    type="button"
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
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }

    .sp-filter-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    .sp-filter-actions {
      display: flex;
      gap: var(--sp-space-2);
      align-items: center;
    }

    .sp-result-meta,
    .sp-pagination {
      display: flex;
      justify-content: space-between;
      gap: var(--sp-space-3);
      align-items: center;
    }

    .sp-result-meta {
      margin-bottom: var(--sp-space-3);
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
    }

    .sp-pagination {
      margin-top: var(--sp-space-3);
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
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      white-space: nowrap;
    }

    @media (max-width: 1000px) {
      .sp-filter-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }

    @media (max-width: 700px) {
      .sp-filter-grid {
        grid-template-columns: 1fr;
      }

      .sp-result-meta {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `,
})
export class PaymentListPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly payments = inject(PaymentsService);

  protected readonly statuses = PAYMENT_STATUSES;
  protected readonly page = signal<PaymentSearchPage | null>(null);
  protected readonly cursorHistory = signal<string[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    paymentReference: [''],
    tresorPayRequestId: [''],
    observedCustomerId: [''],
    financialInstitutionCode: [''],
    status: [''],
    reasonCode: [''],
    amountMin: [null as number | null, [Validators.min(0)]],
    amountMax: [null as number | null, [Validators.min(0)]],
    currency: ['', [Validators.pattern(/^[A-Za-z]{0,3}$/)]],
    sort: ['CREATED_AT_DESC' as PaymentSort],
    size: [2, [Validators.min(1), Validators.max(200)]],
  });

  constructor() {
    this.search();
  }

  protected search(cursor?: string): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const query = this.buildQuery(cursor);
    this.payments.search(query).subscribe((page) => this.page.set(page));
  }

  protected reset(): void {
    this.form.reset({
      paymentReference: '',
      tresorPayRequestId: '',
      observedCustomerId: '',
      financialInstitutionCode: '',
      status: '',
      reasonCode: '',
      amountMin: null,
      amountMax: null,
      currency: '',
      sort: 'CREATED_AT_DESC',
      size: 2,
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

  private buildQuery(cursor?: string): PaymentSearchQuery {
    const value = this.form.getRawValue();

    return {
      ...(value.paymentReference.trim() ? { paymentReference: value.paymentReference.trim() } : {}),

      ...(value.tresorPayRequestId.trim()
        ? { tresorPayRequestId: value.tresorPayRequestId.trim() }
        : {}),

      ...(value.observedCustomerId.trim()
        ? { observedCustomerId: value.observedCustomerId.trim() }
        : {}),

      ...(value.financialInstitutionCode.trim()
        ? { financialInstitutionCode: value.financialInstitutionCode.trim() }
        : {}),

      ...(value.status ? { status: value.status as PaymentSummary['status'] } : {}),

      ...(value.reasonCode.trim() ? { reasonCode: value.reasonCode.trim() } : {}),

      ...(value.amountMin !== null ? { amountMin: value.amountMin } : {}),

      ...(value.amountMax !== null ? { amountMax: value.amountMax } : {}),

      ...(value.currency.trim() ? { currency: value.currency.trim().toUpperCase() } : {}),

      sort: value.sort,
      size: value.size,

      ...(cursor ? { cursor } : {}),
    };
  }
}
