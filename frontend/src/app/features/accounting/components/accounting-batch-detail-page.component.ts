import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-accounting-batch-detail-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar [title]="batchId" description="Détail mocké d'un lot comptable." />
      <a routerLink="/accounting">← Retour à la comptabilisation</a>
      <div class="sp-grid">
        <sp-card title="Synthèse"><p><strong>384 items</strong></p><p>376 réconciliés · 8 écarts</p></sp-card>
        <sp-card title="Soumission"><p>Accounting API</p><p><strong>ACCEPTED</strong></p></sp-card>
        <sp-card title="Réconciliation"><p>Dernier contrôle 14:07</p><p><strong>PARTIAL_MATCH</strong></p></sp-card>
      </div>
      <sp-card title="Écarts à investiguer">
        <table class="sp-table">
          <thead><tr><th>Paiement</th><th>Attendu</th><th>Constaté</th><th>Écart</th><th>État</th></tr></thead>
          <tbody>
            <tr><td>PAY-2026-0001801</td><td>75 000 XAF</td><td>—</td><td>75 000 XAF</td><td>MISSING_PROVIDER_ITEM</td></tr>
            <tr><td>PAY-2026-0001794</td><td>18 500 XAF</td><td>18 000 XAF</td><td>500 XAF</td><td>AMOUNT_MISMATCH</td></tr>
          </tbody>
        </table>
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-table{width:100%;border-collapse:collapse}.sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant)}
    @media(max-width:800px){.sp-grid{grid-template-columns:1fr}.sp-table{display:block;overflow-x:auto}}
  `,
})
export class AccountingBatchDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly batchId = this.route.snapshot.paramMap.get('batchId') ?? 'ACC-UNKNOWN';
}
