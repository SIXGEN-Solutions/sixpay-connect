import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { ErrorService } from '../../../core/errors/error.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpNotificationComponent } from '../../../shared/components/notification/sp-notification.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentDetail } from '../models/payments';
import { PaymentsService } from '../services/payments.service';

@Component({
  selector: 'sp-payment-detail-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    SpCardComponent,
    MockContentStateComponent,
    SpNotificationComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/payments">← Retour aux paiements</a>

      @if (errorService.currentError(); as error) {
        <sp-notification
          status="error"
          [title]="error.title"
          [message]="error.detail"
          [dismissible]="true"
          (dismissed)="errorService.clear()"
        />
      }

      @if (payment(); as currentPayment) {
        <sp-toolbar
          [title]="currentPayment.paymentReference"
          description="Projection Payment masquée et en lecture seule."
        />

        <div class="sp-summary-grid">
          <sp-card title="Paiement">
            <dl class="sp-details">
              <div>
                <dt>Payment ID</dt>
                <dd>{{ currentPayment.paymentId }}</dd>
              </div>
              <div>
                <dt>Statut</dt>
                <dd>{{ currentPayment.status }}</dd>
              </div>
              <div>
                <dt>Montant</dt>
                <dd>
                  {{
                    currentPayment.amount.amount
                      | currency: currentPayment.amount.currency : 'code' : '1.0-0'
                  }}
                </dd>
              </div>
              <div>
                <dt>Institution</dt>
                <dd>{{ currentPayment.financialInstitutionCode }}</dd>
              </div>
              <div>
                <dt>Compte débiteur</dt>
                <dd>{{ currentPayment.debtorAccount?.maskedValue ?? '—' }}</dd>
              </div>
              <div>
                <dt>TresorPay Request</dt>
                <dd>{{ currentPayment.tresorPayRequestId }}</dd>
              </div>
              <div>
                <dt>Observed Customer</dt>
                <dd>{{ currentPayment.observedCustomerId ?? '—' }}</dd>
              </div>
              <div>
                <dt>Reason code</dt>
                <dd>{{ currentPayment.reasonCode ?? '—' }}</dd>
              </div>
              <div>
                <dt>Créé</dt>
                <dd>{{ currentPayment.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
              </div>
              <div>
                <dt>Mis à jour</dt>
                <dd>{{ currentPayment.updatedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
              </div>
              <div>
                <dt>Finalisé</dt>
                <dd>
                  {{
                    currentPayment.finalizedAt
                      ? (currentPayment.finalizedAt | date: 'dd/MM/yyyy HH:mm:ss')
                      : '—'
                  }}
                </dd>
              </div>
              <div>
                <dt>Aggregate version</dt>
                <dd>{{ currentPayment.aggregateVersion }}</dd>
              </div>
              <div>
                <dt>Correlation ID</dt>
                <dd>{{ currentPayment.correlationId }}</dd>
              </div>
            </dl>
          </sp-card>

          <sp-card title="Vérification bancaire">
            @if (currentPayment.bankingVerification; as verification) {
              <dl class="sp-details sp-details--single">
                <div>
                  <dt>Résultat</dt>
                  <dd>{{ verification.outcome }}</dd>
                </div>
                <div>
                  <dt>Verification ID</dt>
                  <dd>{{ verification.verificationId }}</dd>
                </div>
                <div>
                  <dt>Observé</dt>
                  <dd>{{ verification.observedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
                </div>
                <div>
                  <dt>Codes</dt>
                  <dd>{{ verification.reasonCodes.join(', ') || '—' }}</dd>
                </div>
              </dl>
            } @else {
              <p>Aucune vérification bancaire projetée.</p>
            }
          </sp-card>
        </div>

        <div class="sp-operation-grid">
          <sp-card title="Posting">
            @if (currentPayment.posting; as posting) {
              <p>
                <strong>{{ posting.outcome }}</strong>
              </p>
              <p>Référence : {{ posting.bankPostingReference ?? '—' }}</p>
              <p>
                Observé :
                {{ posting.observedAt ? (posting.observedAt | date: 'dd/MM/yyyy HH:mm:ss') : '—' }}
              </p>
            } @else {
              <p>Aucune projection de posting disponible.</p>
            }
          </sp-card>

          <sp-card title="TFJ">
            @if (currentPayment.tfj; as tfj) {
              <p>
                <strong>{{ tfj.status }}</strong>
              </p>
              <p>Business date : {{ tfj.businessDate ?? '—' }}</p>
              <p>
                Confirmé :
                {{ tfj.confirmedAt ? (tfj.confirmedAt | date: 'dd/MM/yyyy HH:mm:ss') : '—' }}
              </p>
            } @else {
              <p>Aucune projection TFJ disponible.</p>
            }
          </sp-card>

          <sp-card title="Reversal">
            @if (currentPayment.reversal; as reversal) {
              <p>
                <strong>{{ reversal.status }}</strong>
              </p>
              <p>Référence : {{ reversal.reversalReference ?? '—' }}</p>
              <p>
                Observé :
                {{
                  reversal.observedAt ? (reversal.observedAt | date: 'dd/MM/yyyy HH:mm:ss') : '—'
                }}
              </p>
            } @else {
              <p>Aucune projection de reversal disponible.</p>
            }
          </sp-card>
        </div>

        <sp-card title="Notifications">
          @if (currentPayment.notifications.length === 0) {
            <sp-mock-content-state
              kind="empty"
              title="Aucune notification"
              message="Aucune projection de notification n'est disponible pour ce Payment."
            />
          } @else {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr>
                    <th>Type</th>
                    <th>Statut</th>
                    <th>Event ID</th>
                    <th>Dernière tentative</th>
                  </tr>
                </thead>
                <tbody>
                  @for (
                    notification of currentPayment.notifications;
                    track notification.eventId ?? notification.type
                  ) {
                    <tr>
                      <td>{{ notification.type }}</td>
                      <td>{{ notification.status }}</td>
                      <td>{{ notification.eventId ?? '—' }}</td>
                      <td>
                        {{
                          notification.lastAttemptAt
                            ? (notification.lastAttemptAt | date: 'dd/MM/yyyy HH:mm:ss')
                            : '—'
                        }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </sp-card>

        @if (canReadAudit()) {
          <sp-card title="Timeline et audit">
            <p>
              La timeline et les preuves d'audit sont exposées par la capacité Reporting dédiée.
            </p>
            <a [routerLink]="['/reporting/payments', currentPayment.paymentId, 'timeline']">
              Ouvrir la timeline Payment
            </a>
          </sp-card>
        }
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Paiement introuvable"
          message="Aucune projection Payment ne correspond à cet identifiant ou n'est visible dans votre périmètre."
        />
      } @else if (loading()) {
        <sp-mock-content-state
          kind="loading"
          title="Chargement du paiement"
          message="Chargement de la projection Payment."
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

    .sp-summary-grid {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: var(--sp-space-3);
    }

    .sp-operation-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    .sp-details {
      margin: 0;
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    .sp-details--single {
      grid-template-columns: 1fr;
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

    @media (max-width: 900px) {
      .sp-summary-grid,
      .sp-operation-grid,
      .sp-details {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class PaymentDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly payments = inject(PaymentsService);
  private readonly auth = inject(AuthenticationService);
  protected readonly errorService = inject(ErrorService);

  protected readonly payment = signal<PaymentDetail | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);

  protected readonly canReadAudit = () =>
    this.auth.hasPermission('payment.audit.read') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('AUDITOR'));

  constructor() {
    const paymentId = this.route.snapshot.paramMap.get('paymentId') ?? '';

    this.errorService.clear();
    this.payments
      .get(paymentId)
      .pipe(
        catchError(() => {
          this.notFound.set(false);
          return EMPTY;
        }),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((payment) => {
        this.payment.set(payment);
        this.notFound.set(payment === null);
      });
  }
}
