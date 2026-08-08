import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-accounting-overview-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Comptabilisation" description="Pilotage mocké des lots et de la réconciliation." />
      <div class="sp-kpis">
        <sp-card title="Lots du jour"><strong>4</strong><p>3 terminés, 1 en contrôle</p></sp-card>
        <sp-card title="Écritures"><strong>1 219</strong><p>1 211 réconciliées</p></sp-card>
        <sp-card title="Écarts"><strong>8</strong><p>À investiguer</p></sp-card>
      </div>
      <sp-card title="Lots récents">
        <table class="sp-table">
          <thead><tr><th>Lot</th><th>Fenêtre</th><th>Items</th><th>Statut</th><th>Écarts</th></tr></thead>
          <tbody>
            <tr><td><a routerLink="/accounting/batches/ACC-20260808-03">ACC-20260808-03</a></td><td>12:00–14:00</td><td>384</td><td>RECONCILING</td><td>8</td></tr>
            <tr><td><a routerLink="/accounting/batches/ACC-20260808-02">ACC-20260808-02</a></td><td>10:00–12:00</td><td>421</td><td>COMPLETED</td><td>0</td></tr>
            <tr><td><a routerLink="/accounting/batches/ACC-20260808-01">ACC-20260808-01</a></td><td>08:00–10:00</td><td>414</td><td>COMPLETED</td><td>0</td></tr>
          </tbody>
        </table>
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-kpis{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-kpis strong{font-size:2rem}.sp-table{width:100%;border-collapse:collapse}.sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant)}
    @media(max-width:800px){.sp-kpis{grid-template-columns:1fr}.sp-table{display:block;overflow-x:auto}}
  `,
})
export class AccountingOverviewPageComponent {}
