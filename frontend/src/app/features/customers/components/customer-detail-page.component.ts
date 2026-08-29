import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { ObservedCustomerDetail, ObservedCustomerPaymentPage } from '../models/customers';
import { CustomersService } from '../services/customers.service';

@Component({
  selector: 'sp-customer-detail-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    MockContentStateComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/customers">← Retour aux clients observés</a>

      @if (customer(); as currentCustomer) {
        <sp-toolbar
          [title]="currentCustomer.legalName"
          description="Projection ObservedCustomer non autoritative et masquée."
        />

        <div class="sp-grid">
          <sp-card title="Identité observée">
            <dl class="sp-details">
              <div>
                <dt>Observed Customer ID</dt>
                <dd>{{ currentCustomer.observedCustomerId }}</dd>
              </div>
              <div>
                <dt>NIU</dt>
                <dd>{{ currentCustomer.niu.maskedValue }}</dd>
              </div>
              <div>
                <dt>Téléphone</dt>
                <dd>{{ currentCustomer.phone?.maskedValue ?? '—' }}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{{ currentCustomer.email?.maskedValue ?? '—' }}</dd>
              </div>
            </dl>
          </sp-card>

          <sp-card title="Projection">
            <dl class="sp-details">
              <div>
                <dt>Première observation</dt>
                <dd>{{ currentCustomer.firstObservedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
              </div>
              <div>
                <dt>Dernière observation</dt>
                <dd>{{ currentCustomer.lastObservedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
              </div>
              <div>
                <dt>Mise à jour projection</dt>
                <dd>{{ currentCustomer.projectionUpdatedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
              </div>
              <div>
                <dt>Version</dt>
                <dd>{{ currentCustomer.projectionVersion }}</dd>
              </div>
              <div>
                <dt>Watermark</dt>
                <dd>{{ currentCustomer.sourceEventWatermark }}</dd>
              </div>
              <div>
                <dt>Dernier statut</dt>
                <dd>{{ currentCustomer.lastPaymentStatus ?? '—' }}</dd>
              </div>
            </dl>
          </sp-card>
        </div>

        <div class="sp-kpis">
          <sp-card title="Paiements"
            ><strong>{{ currentCustomer.totalPayments }}</strong></sp-card
          >
          <sp-card title="Succès"
            ><strong>{{ currentCustomer.successfulPayments }}</strong></sp-card
          >
          <sp-card title="Échecs"
            ><strong>{{ currentCustomer.failedPayments }}</strong></sp-card
          >
        </div>

        <sp-card title="Institutions observées">
          <div class="sp-institutions">
            @for (
              institution of currentCustomer.institutions;
              track institution.financialInstitutionCode
            ) {
              <section class="sp-institution">
                <div>
                  <strong>{{ institution.financialInstitutionCode }}</strong>
                  <p>
                    {{ institution.firstObservedAt | date: 'dd/MM/yyyy' }}
                    →
                    {{ institution.lastObservedAt | date: 'dd/MM/yyyy' }}
                  </p>
                </div>
                <ul>
                  @for (account of institution.accounts; track account.reference) {
                    <li>{{ account.maskedValue }}</li>
                  }
                </ul>
              </section>
            }
          </div>
        </sp-card>

        <sp-card title="Paiements liés">
          @if (payments(); as paymentPage) {
            @if (paymentPage.items.length === 0) {
              <sp-mock-content-state
                kind="empty"
                title="Aucun paiement lié"
                message="Aucun Payment n'est relié à cette projection."
              />
            } @else {
              <div class="sp-table-scroll">
                <table class="sp-table">
                  <thead>
                    <tr>
                      <th>Référence</th>
                      <th>Institution</th>
                      <th>Montant</th>
                      <th>Statut</th>
                      <th>Reason code</th>
                      <th>Créé</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (payment of paymentPage.items; track payment.paymentId) {
                      <tr>
                        <td>
                          <a [routerLink]="['/payments', payment.paymentId]">
                            {{ payment.paymentReference }}
                          </a>
                        </td>
                        <td>{{ payment.financialInstitutionCode }}</td>
                        <td>
                          @if (payment.amount) {
                            {{
                              payment.amount.amount
                                | currency: payment.amount.currency : 'code' : '1.0-0'
                            }}
                          } @else {
                            —
                          }
                        </td>
                        <td>{{ payment.status }}</td>
                        <td>{{ payment.reasonCode ?? '—' }}</td>
                        <td>{{ payment.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>

              <div class="sp-pagination">
                <sp-button
                  icon="arrow_back"
                  [disabled]="paymentCursorHistory().length === 0"
                  (buttonClick)="previousPayments()"
                >
                  Précédent
                </sp-button>

                <sp-button
                  icon="arrow_forward"
                  [disabled]="!paymentPage.hasMore"
                  (buttonClick)="nextPayments()"
                >
                  Suivant
                </sp-button>
              </div>
            }
          }
        </sp-card>
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Client observé introuvable"
          message="Aucune projection ObservedCustomer ne correspond à cet identifiant."
        />
      } @else {
        <sp-mock-content-state
          kind="loading"
          title="Chargement du client observé"
          message="Chargement de la projection."
        />
      }
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: var(--sp-space-3);
    }
    .sp-kpis {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }
    .sp-kpis strong {
      font-size: 2rem;
    }
    .sp-details {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--sp-space-3);
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
      font-weight: 700;
      overflow-wrap: anywhere;
    }
    .sp-institutions {
      display: grid;
      gap: var(--sp-space-3);
    }
    .sp-institution {
      display: flex;
      justify-content: space-between;
      gap: var(--sp-space-3);
      padding-bottom: var(--sp-space-3);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .sp-institution p {
      margin: 0.25rem 0 0;
      color: var(--mat-sys-on-surface-variant);
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
    .sp-pagination {
      display: flex;
      justify-content: space-between;
      gap: var(--sp-space-3);
      margin-top: var(--sp-space-3);
    }
    @media (max-width: 850px) {
      .sp-grid,
      .sp-kpis,
      .sp-details {
        grid-template-columns: 1fr;
      }
      .sp-institution {
        flex-direction: column;
      }
    }
  `,
})
export class CustomerDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly customers = inject(CustomersService);

  protected readonly customer = signal<ObservedCustomerDetail | null>(null);
  protected readonly payments = signal<ObservedCustomerPaymentPage | null>(null);
  protected readonly notFound = signal(false);
  protected readonly paymentCursorHistory = signal<string[]>([]);

  private readonly observedCustomerId =
    this.route.snapshot.paramMap.get('observedCustomerId') ?? '';

  constructor() {
    this.customers.get(this.observedCustomerId).subscribe((customer) => {
      this.customer.set(customer);
      this.notFound.set(customer === null);

      if (customer) {
        this.loadPayments();
      }
    });
  }

  protected nextPayments(): void {
    const nextCursor = this.payments()?.nextCursor;
    if (!nextCursor) {
      return;
    }

    const currentCursor = this.paymentCursorHistory().at(-1) ?? '';
    this.paymentCursorHistory.update((history) => [...history, currentCursor || '0']);
    this.loadPayments(nextCursor);
  }

  protected previousPayments(): void {
    const history = [...this.paymentCursorHistory()];
    const previousCursor = history.pop();
    this.paymentCursorHistory.set(history);
    this.loadPayments(previousCursor === '0' ? undefined : previousCursor);
  }

  private loadPayments(cursor?: string): void {
    this.customers
      .payments(this.observedCustomerId, {
        size: 2,
        ...(cursor ? { cursor } : {}),
      })
      .subscribe((page) => this.payments.set(page));
  }
}
