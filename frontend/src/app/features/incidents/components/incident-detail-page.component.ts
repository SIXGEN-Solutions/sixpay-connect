import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-incident-detail-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar [title]="incidentId" description="Détail mocké d'un incident opérationnel." />
      <a routerLink="/incidents">← Retour aux incidents</a>
      <div class="sp-grid">
        <sp-card title="Résumé"><p><strong>Écarts de réconciliation Accounting</strong></p><p>8 paiements non parfaitement rapprochés.</p></sp-card>
        <sp-card title="Impact"><p>Pas de perte financière confirmée.</p><p>Comptabilisation maintenue en contrôle.</p></sp-card>
      </div>
      <sp-card title="Chronologie">
        <ul class="sp-events">
          <li><strong>13:46</strong> Alerte de rapprochement déclenchée.</li>
          <li><strong>13:49</strong> Incident créé automatiquement.</li>
          <li><strong>13:55</strong> Analyse opérateur démarrée.</li>
          <li><strong>14:07</strong> 6/8 écarts catégorisés.</li>
        </ul>
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-grid{display:grid;grid-template-columns:1fr 1fr;gap:var(--sp-space-3)}
    .sp-events{display:grid;gap:var(--sp-space-2)}@media(max-width:800px){.sp-grid{grid-template-columns:1fr}}
  `,
})
export class IncidentDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  protected readonly incidentId = this.route.snapshot.paramMap.get('incidentId') ?? 'INC-UNKNOWN';
}
