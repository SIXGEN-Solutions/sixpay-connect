import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-incident-list-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Incidents" description="Vue opérationnelle mockée des anomalies et dégradations." />
      <sp-card title="Incidents ouverts">
        <table class="sp-table">
          <thead><tr><th>ID</th><th>Sévérité</th><th>Composant</th><th>Résumé</th><th>Ouvert depuis</th><th>Statut</th></tr></thead>
          <tbody>
            <tr><td><a routerLink="/incidents/INC-2026-0084">INC-2026-0084</a></td><td>HIGH</td><td>Accounting</td><td>Écarts de réconciliation</td><td>23 min</td><td>INVESTIGATING</td></tr>
            <tr><td><a routerLink="/incidents/INC-2026-0083">INC-2026-0083</a></td><td>MEDIUM</td><td>Notification</td><td>Latence TresorPay</td><td>41 min</td><td>MONITORING</td></tr>
          </tbody>
        </table>
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant)}
    @media(max-width:800px){.sp-table{display:block;overflow-x:auto}}
  `,
})
export class IncidentListPageComponent {}
