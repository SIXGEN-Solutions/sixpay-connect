import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { CustomersMockService } from '../services/customers-mock.service';

@Component({
  selector: 'sp-customer-detail-page',
  imports: [DatePipe, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar [title]="customer.legalName" description="Fiche mockée d'un ObservedCustomer." />
      <a routerLink="/customers">← Retour aux clients observés</a>
      <div class="sp-grid">
        <sp-card title="Identification">
          <dl class="sp-details">
            <div><dt>NIU</dt><dd>{{ customer.niuMasked }}</dd></div>
            <div><dt>Institution</dt><dd>{{ customer.institution }}</dd></div>
            <div><dt>Compte</dt><dd>{{ customer.accountMasked }}</dd></div>
            <div><dt>Identifiant projection</dt><dd>{{ customer.id }}</dd></div>
          </dl>
        </sp-card>
        <sp-card title="Observation">
          <dl class="sp-details">
            <div><dt>Première observation</dt><dd>{{ customer.firstObservedAt | date: 'dd/MM/yyyy HH:mm' }}</dd></div>
            <div><dt>Dernière observation</dt><dd>{{ customer.lastObservedAt | date: 'dd/MM/yyyy HH:mm' }}</dd></div>
            <div><dt>Nombre de paiements</dt><dd>{{ customer.paymentCount }}</dd></div>
            <div><dt>Dernier statut</dt><dd>{{ customer.lastPaymentStatus }}</dd></div>
          </dl>
        </sp-card>
      </div>
      <sp-card title="Paiements liés">
        <div class="sp-table-scroll">
          <table class="sp-table">
            <thead><tr><th>Référence</th><th>Date</th><th>Montant</th><th>Statut</th></tr></thead>
            <tbody>
              <tr><td><a routerLink="/payments/PAY-2026-0001842">PAY-2026-0001842</a></td><td>08/08/2026 09:47</td><td>125 000 XAF</td><td>SUCCESS</td></tr>
              <tr><td><a routerLink="/payments/PAY-2026-0001776">PAY-2026-0001776</a></td><td>07/08/2026 16:12</td><td>75 000 XAF</td><td>SUCCESS</td></tr>
            </tbody>
          </table>
        </div>
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)} .sp-grid{display:grid;grid-template-columns:1fr 1fr;gap:var(--sp-space-3)}
    .sp-details{display:grid;gap:var(--sp-space-3);margin:0}.sp-details div{display:grid;gap:.25rem}.sp-details dt{color:var(--mat-sys-on-surface-variant);font-size:.85rem}.sp-details dd{margin:0;font-weight:700}
    .sp-table-scroll{overflow-x:auto}.sp-table{width:100%;border-collapse:collapse}.sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant)}
    @media(max-width:800px){.sp-grid{grid-template-columns:1fr}}
  `,
})
export class CustomerDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly mocks = inject(CustomersMockService);
  protected readonly customer = this.mocks.find(this.route.snapshot.paramMap.get('observedCustomerId') ?? '');
}
