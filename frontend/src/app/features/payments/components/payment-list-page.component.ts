import { CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthenticationService } from '../../../core/auth/authentication.service';
import { BackendModeService } from '../../../core/backend/backend-mode.service';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentSearchQuery, PaymentSort } from '../models/payment-query';
import { PaymentT0ManualResult } from '../models/payment-t0-manual-reconciliation';
import { isT0RecoveryCandidate, validT0Selection } from '../models/payment-t0-reconciliation-selection';
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
            @if (canReconcile()) {
              <sp-button
                type="button"
                icon="filter_alt"
                (buttonClick)="filterUncertain()"
              >
                Résultats T0 incertains
              </sp-button>
            }
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()"
              >Réinitialiser</sp-button
            >
          </div>
        </form>
      </sp-card>

      @if (canReconcile()) {
        <sp-card title="Réconciliation manuelle T0">
          <p>Sélectionnez les paiements en statut POSTING_PENDING ou POSTING_OUTCOME_UNKNOWN sur la page courante (50 maximum).</p>
          <div class="sp-filter-actions">
            <span>{{ selectedCount() }} paiement(s) sélectionné(s)</span>
            <sp-button type="button" icon="sync" [disabled]="reconciling() || selectedCount() === 0"
              (buttonClick)="reconcileSelected()">Réconcilier</sp-button>
          </div>
          @if (reconciling()) { <p role="status">Réconciliation en cours…</p> }
          @if (reconcileError()) { <p role="alert">{{ reconcileError() }}</p> }
          @if (reconcileResults().length > 0) {
            <div role="status" aria-live="polite">
              <p>Résultats de la dernière réconciliation :</p>
              <ul>
                @for (item of reconcileResults(); track item.paymentId) {
                  <li>{{ item.paymentId }} : {{ item.outcome }} — {{ item.paymentStatus ?? 'statut indisponible' }}</li>
                }
              </ul>
            </div>
          }
        </sp-card>
      }

      @if (searchError()) { <p role="alert">{{ searchError() }}</p> }
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
                        @if (canReconcile()) { <th>Sélection T0</th> }
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
                          @if (canReconcile()) {
                            <td>
                              @if (isCandidate(payment)) {
                                <input type="checkbox" [checked]="isSelected(payment.paymentId)"
                                  [disabled]="reconciling() || (selectedCount() >= 50 && !isSelected(payment.paymentId))"
                                  [attr.aria-label]="'Sélectionner le paiement ' + payment.paymentReference"
                                  (change)="toggleSelection(payment.paymentId, $any($event.target).checked)" />
                              } @else { — }
                            </td>
                          }
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
  private readonly authentication = inject(AuthenticationService);
  private readonly backendMode = inject(BackendModeService);

  // Frontend role catalog currently omits OPS. Do not alter security configuration in R4.
  protected readonly canReconcile = computed(() => this.backendMode.usesApi
    && this.authentication.hasRole('ADMIN')
    && this.authentication.hasPermission('payment.reconcile'));
  protected readonly searchError = signal<string | null>(null);
  protected readonly selected = signal<readonly string[]>([]);
  protected readonly selectedCount = computed(() => this.selected().length);
  protected readonly reconciling = signal(false);
  protected readonly reconcileResults = signal<readonly PaymentT0ManualResult[]>([]);
  protected readonly reconcileError = signal<string | null>(null);
  protected readonly isCandidate = isT0RecoveryCandidate;

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

    this.selected.set([]);
    this.searchError.set(null);
    const query = this.buildQuery(cursor);
    this.payments.search(query).subscribe({
      next: (page) => this.page.set(page),
      error: () => {
        this.page.set(null);
        this.searchError.set('Impossible de charger les paiements.');
      },
    });
  }

  protected filterUncertain(): void {
    // Backend Payment query supports one status at a time. Both uncertain states remain
    // available in the existing status selector; this shortcut focuses on UNKNOWN.
    this.form.controls.status.setValue('POSTING_OUTCOME_UNKNOWN');
    this.cursorHistory.set([]);
    this.search();
  }

  protected isSelected(paymentId: string): boolean {
    return this.selected().includes(paymentId);
  }

  protected toggleSelection(paymentId: string, checked: boolean): void {
    if (!this.canReconcile() || this.reconciling()) return;
    const visible = this.page()?.items.find((item) => item.paymentId === paymentId);
    if (!visible || !isT0RecoveryCandidate(visible)) return;
    if (checked && !this.isSelected(paymentId) && this.selectedCount() < 50) {
      this.selected.update((ids) => [...ids, paymentId]);
    } else if (!checked) {
      this.selected.update((ids) => ids.filter((id) => id !== paymentId));
    }
  }

  protected reconcileSelected(): void {
    const ids = this.selected();
    if (!this.canReconcile() || this.reconciling() || !validT0Selection(ids)) return;
    this.reconciling.set(true);
    this.reconcileError.set(null);
    this.reconcileResults.set([]);
    this.payments.reconcileT0(ids).pipe(finalize(() => this.reconciling.set(false))).subscribe({
      next: (response) => {
        this.reconcileResults.set(response.results);
        this.selected.set([]);
        this.search();
      },
      error: (error: unknown) => {
        const status = error instanceof HttpErrorResponse ? error.status : 0;
        this.reconcileError.set(status === 401 ? 'Session non authentifiée.'
          : status === 403 ? 'Permission de réconciliation insuffisante.'
          : status === 400 ? 'Sélection de paiements invalide.'
          : status === 503 ? 'Service de réconciliation indisponible.'
          : 'La réconciliation a échoué. Vérifiez les états avant de relancer.');
      },
    });
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
    this.reconcileResults.set([]);
    this.reconcileError.set(null);
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
