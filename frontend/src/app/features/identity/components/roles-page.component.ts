import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { RoleDefinition } from '../models/identity';
import { IdentityService } from '../services/identity.service';

@Component({
  selector: 'sp-roles-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Rôles SIXPAY"
        description="Rôles effectivement supportés par le frontend et core/auth."
      />

      <nav class="sp-tabs" aria-label="Gestion des accès">
        <a routerLink="/identity/users">Utilisateurs</a>
        <a routerLink="/identity/roles">Rôles</a>
      </nav>

      <div class="sp-grid">
        @for (role of roles(); track role.role) {
          <sp-card [title]="role.role" [subtitle]="role.userCount + ' identité(s) mockée(s)'">
            <p>{{ role.description }}</p>
            <ul>
              @for (capability of role.capabilities; track capability) {
                <li>{{ capability }}</li>
              }
            </ul>
          </sp-card>
        }
      </div>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-tabs{display:flex;gap:var(--sp-space-3)}
    .sp-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3)}
    @media(max-width:700px){.sp-grid{grid-template-columns:1fr}}
  `,
})
export class RolesPageComponent {
  private readonly service = inject(IdentityService);
  protected readonly roles = signal<readonly RoleDefinition[]>([]);

  constructor() {
    this.service.roles().subscribe((roles) => this.roles.set(roles));
  }
}
