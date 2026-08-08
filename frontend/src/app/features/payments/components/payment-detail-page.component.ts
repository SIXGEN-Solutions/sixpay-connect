import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentsMockService } from '../services/payments-mock.service';

@Component({
  selector: 'sp-payment-detail-page',
  imports: [CurrencyPipe, DatePipe, MatIconModule, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar [title]="payment.reference" description="Détail opérationnel mocké du paiement." />
      <a routerLink="/payments">← Retour aux paiements</a>

      <div class="sp-summary-grid">
        <sp-card title="Paiement">
          <dl class="sp-details">
            <div><dt>Statut</dt><dd>{{ payment.status }}</dd></div>
            <div><dt>Montant</dt><dd>{{ payment.amount | currency: payment.currency : 'code' : '1.0-0' }}</dd></div>
            <div><dt>Client</dt><dd>{{ payment.customer }}</dd></div>
            <div><dt>Institution</dt><dd>{{ payment.institution }}</dd></div>
            <div><dt>TresorPay Request</dt><dd>{{ payment.tresorPayRequestId }}</dd></div>
            <div><dt>Créé</dt><dd>{{ payment.createdAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd></div>
          </dl>
        </sp-card>

        <sp-card title="Décision">
          <p><strong>{{ payment.status === 'FAILED' ? 'Paiement refusé' : 'Parcours nominal' }}</strong></p>
          <p>{{ payment.reasonCode ?? 'Toutes les vérifications métier ont abouti.' }}</p>
        </sp-card>
      </div>

      <sp-card title="Timeline">
        <ol class="sp-timeline">
          @for (entry of timeline; track entry.time) {
            <li>
              <mat-icon aria-hidden="true">{{ entry.icon }}</mat-icon>
              <div><strong>{{ entry.label }}</strong><span>{{ entry.detail }}</span></div>
              <time>{{ entry.time }}</time>
            </li>
          }
        </ol>
      </sp-card>

      <sp-card title="Audit">
        <div class="sp-table-scroll">
          <table class="sp-table">
            <thead><tr><th>Heure</th><th>Acteur</th><th>Action</th><th>Résultat</th><th>Correlation ID</th></tr></thead>
            <tbody>
              @for (record of audit; track record.correlation) {
                <tr><td>{{ record.time }}</td><td>{{ record.actor }}</td><td>{{ record.action }}</td><td>{{ record.result }}</td><td>{{ record.correlation }}</td></tr>
              }
            </tbody>
          </table>
        </div>
      </sp-card>
    </section>
  `,
  styles: `
    :host, .sp-page { display: grid; gap: var(--sp-space-4); }
    .sp-summary-grid { display: grid; grid-template-columns: 2fr 1fr; gap: var(--sp-space-3); }
    .sp-details { margin: 0; display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--sp-space-3); }
    .sp-details div { display: grid; gap: .25rem; }
    .sp-details dt { color: var(--mat-sys-on-surface-variant); font-size: .85rem; }
    .sp-details dd { margin: 0; font-weight: 700; }
    .sp-timeline { display: grid; gap: var(--sp-space-3); padding: 0; list-style: none; }
    .sp-timeline li { display: grid; grid-template-columns: auto 1fr auto; gap: var(--sp-space-3); align-items: center; }
    .sp-timeline div { display: grid; gap: .2rem; }
    .sp-timeline span { color: var(--mat-sys-on-surface-variant); }
    .sp-table-scroll { overflow-x: auto; }
    .sp-table { width: 100%; border-collapse: collapse; }
    .sp-table th, .sp-table td { padding: var(--sp-space-2); text-align: left; border-bottom: 1px solid var(--mat-sys-outline-variant); white-space: nowrap; }
    @media (max-width: 800px) { .sp-summary-grid, .sp-details { grid-template-columns: 1fr; } .sp-timeline li { grid-template-columns: auto 1fr; } .sp-timeline time { grid-column: 2; } }
  `,
})
export class PaymentDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly mocks = inject(PaymentsMockService);

  protected readonly payment = this.mocks.find(this.route.snapshot.paramMap.get('paymentId') ?? '');

  protected readonly timeline = [
    { time: '09:47:12.104', icon: 'call_received', label: 'Payment received', detail: 'Demande reçue de TresorPay.' },
    { time: '09:47:12.230', icon: 'verified_user', label: 'Customer verified', detail: 'Identité et compte vérifiés.' },
    { time: '09:47:12.412', icon: 'lock', label: 'Funds reserved', detail: 'Réservation des fonds confirmée.' },
    { time: '09:47:12.803', icon: 'account_balance', label: 'Core banking posting', detail: 'Écriture bancaire enregistrée.' },
    { time: '09:47:13.090', icon: 'send', label: 'TresorPay notified', detail: 'Résultat normalisé retourné.' },
    { time: '09:47:13.218', icon: 'receipt_long', label: 'Accounting candidate created', detail: 'Paiement éligible à la comptabilisation.' },
  ];

  protected readonly audit = [
    { time: '09:47:12', actor: 'TRESOR_PAY', action: 'PAYMENT_REQUESTED', result: 'SUCCESS', correlation: 'e5e41af6-6f71-4cf2-a111-42837d1ea100' },
    { time: '09:47:12', actor: 'SIXPAY', action: 'BANKING_VERIFICATION', result: 'SUCCESS', correlation: 'e5e41af6-6f71-4cf2-a111-42837d1ea100' },
    { time: '09:47:13', actor: 'SIXPAY', action: 'PAYMENT_COMPLETED', result: 'SUCCESS', correlation: 'e5e41af6-6f71-4cf2-a111-42837d1ea100' },
  ];
}
