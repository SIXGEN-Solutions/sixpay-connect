import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { AccountingBatchDetail } from '../models/accounting';
import { AccountingService } from '../services/accounting.service';

@Component({
  selector: 'sp-accounting-batch-detail-page',
  imports: [CurrencyPipe, DatePipe, RouterLink, SpCardComponent, MockContentStateComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/accounting">← Retour à la comptabilisation</a>

      @if (batch(); as currentBatch) {
        <sp-toolbar [title]="currentBatch.batchId" description="Détail du lot comptable mocké." />

        <div class="sp-grid">
          <sp-card title="Synthèse">
            <p><strong>{{ currentBatch.itemCount }} items</strong></p>
            <p>{{ currentBatch.reconciledCount }} réconciliés · {{ currentBatch.discrepancyCount }} écarts</p>
          </sp-card>

          <sp-card title="Soumission">
            <p>{{ currentBatch.submissionStatus }}</p>
            <p>{{ currentBatch.providerReference ?? '—' }}</p>
          </sp-card>

          <sp-card title="Réconciliation">
            <p><strong>{{ currentBatch.reconciliationStatus }}</strong></p>
            <p>
              {{ currentBatch.lastReconciledAt ? (currentBatch.lastReconciledAt | date: 'dd/MM/yyyy HH:mm:ss') : '—' }}
            </p>
          </sp-card>
        </div>

        <sp-card title="Écarts à investiguer">
          @if (currentBatch.discrepancies.length === 0) {
            <sp-mock-content-state
              kind="empty"
              title="Aucun écart"
              message="Ce lot est entièrement réconcilié."
            />
          } @else {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr>
                    <th>Paiement</th>
                    <th>Type</th>
                    <th>Attendu</th>
                    <th>Constaté</th>
                    <th>Reason code</th>
                    <th>Détecté</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of currentBatch.discrepancies; track item.discrepancyId) {
                    <tr>
                      <td>
                        <a [routerLink]="['/payments', item.paymentId]">{{ item.paymentReference }}</a>
                      </td>
                      <td>{{ item.type }}</td>
                      <td>
                        {{ item.expectedAmount === null ? '—' : (item.expectedAmount | currency: item.currency : 'code' : '1.0-0') }}
                      </td>
                      <td>
                        {{ item.observedAmount === null ? '—' : (item.observedAmount | currency: item.currency : 'code' : '1.0-0') }}
                      </td>
                      <td>{{ item.reasonCode }}</td>
                      <td>{{ item.detectedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </sp-card>
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Lot introuvable"
          message="Aucun lot comptable ne correspond à cet identifiant."
        />
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-table-scroll{overflow-x:auto}
    .sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant);white-space:nowrap}
    @media(max-width:800px){.sp-grid{grid-template-columns:1fr}}
  `,
})
export class AccountingBatchDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly accounting = inject(AccountingService);

  protected readonly batch = signal<AccountingBatchDetail | null>(null);
  protected readonly notFound = signal(false);

  constructor() {
    const batchId = this.route.snapshot.paramMap.get('batchId') ?? '';
    this.accounting.get(batchId).subscribe((batch) => {
      this.batch.set(batch);
      this.notFound.set(batch === null);
    });
  }
}
