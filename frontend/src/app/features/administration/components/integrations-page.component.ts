import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-integrations-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Intégrations" description="Vue mockée des dépendances externes." />
      <a routerLink="/administration">← Retour à l'administration</a>
      <div class="sp-grid">
        @for (integration of integrations; track integration.name) {
          <sp-card [title]="integration.name" [subtitle]="integration.type">
            <p><strong>{{ integration.status }}</strong></p>
            <p>{{ integration.detail }}</p>
          </sp-card>
        }
      </div>
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3)}@media(max-width:700px){.sp-grid{grid-template-columns:1fr}}`,
})
export class IntegrationsPageComponent {
  protected readonly integrations = [
    { name: 'TresorPay', type: 'REST / mTLS', status: 'AVAILABLE', detail: 'Dernier succès il y a 18 secondes.' },
    { name: 'Amplitude', type: 'Core Banking', status: 'AVAILABLE', detail: 'Réservation et posting opérationnels.' },
    { name: 'Accounting API', type: 'REST / OAuth2', status: 'DEGRADED', detail: 'Réconciliation partielle sur le lot courant.' },
    { name: 'Notifications', type: 'Operational', status: 'AVAILABLE', detail: 'File d’attente nominale.' },
  ];
}
