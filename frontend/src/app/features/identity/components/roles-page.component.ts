import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-roles-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Rôles SIXPAY" description="Matrice mockée des rôles déjà supportés par le frontend." />
      <nav class="sp-tabs"><a routerLink="/identity/users">Utilisateurs</a><a routerLink="/identity/roles">Rôles</a></nav>
      <div class="sp-grid">
        <sp-card title="ADMIN"><p>Administration, configuration, partenaires et consultation opérationnelle.</p></sp-card>
        <sp-card title="MANAGER"><p>Validation Partner et consultation des opérations.</p></sp-card>
        <sp-card title="AUDITOR"><p>Consultation et preuves d'audit, sans mutation métier.</p></sp-card>
        <sp-card title="PARTNER"><p>Consultation de son propre statut Partner.</p></sp-card>
      </div>
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-tabs{display:flex;gap:var(--sp-space-3)}.sp-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3)}@media(max-width:700px){.sp-grid{grid-template-columns:1fr}}`,
})
export class RolesPageComponent {}
