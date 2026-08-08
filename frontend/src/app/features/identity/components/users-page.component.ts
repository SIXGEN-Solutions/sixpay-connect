import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-users-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Gestion des accès" description="Utilisateurs et rôles mockés pour la Phase 7.0." />
      <nav class="sp-tabs"><a routerLink="/identity/users">Utilisateurs</a><a routerLink="/identity/roles">Rôles</a></nav>
      <sp-card title="Utilisateurs">
        <table class="sp-table">
          <thead><tr><th>Utilisateur</th><th>Identifiant</th><th>Rôles</th><th>Statut</th></tr></thead>
          <tbody>
            <tr><td>Administrateur SIXPAY</td><td>admin@sixpay.local</td><td>ADMIN</td><td>ACTIVE</td></tr>
            <tr><td>Manager Opérations</td><td>manager@sixpay.local</td><td>MANAGER</td><td>ACTIVE</td></tr>
            <tr><td>Audit Interne</td><td>audit@sixpay.local</td><td>AUDITOR</td><td>ACTIVE</td></tr>
            <tr><td>TresorPay</td><td>partner-tresorpay</td><td>PARTNER</td><td>ACTIVE</td></tr>
          </tbody>
        </table>
      </sp-card>
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-tabs{display:flex;gap:var(--sp-space-3)}.sp-table{width:100%;border-collapse:collapse}.sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant)}@media(max-width:800px){.sp-table{display:block;overflow-x:auto}}`,
})
export class UsersPageComponent {}
