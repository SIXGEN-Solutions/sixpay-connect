import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { SecurityUserSummary } from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

@Component({
  selector: 'sp-security-users-page',
  imports: [RouterLink, DatePipe, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Utilisateurs & sécurité"
        description="Identités, méthodes d’authentification et autorisations SIXPAY."
      />

      @for (user of users(); track user.id) {
        <sp-card
          [title]="user.username"
          [subtitle]="user.email ?? 'Email non renseigné'"
        >
          <p>
            Statut: <strong>{{ user.status }}</strong><br />
            Local: <strong>{{ user.localEnabled ? 'Activé' : 'Désactivé' }}</strong><br />
            SSO: <strong>{{ user.oidcLinked ? 'Lié' : 'Non lié' }}</strong><br />
            Dernière authentification:
            {{ user.lastAuthenticationAt ? (user.lastAuthenticationAt | date:'medium') : '—' }}
          </p>
          <a spCardActions [routerLink]="[user.id]">Administrer</a>
        </sp-card>
      }
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}`,
})
export class SecurityUsersPageComponent {
  private readonly service = inject(SecurityUserAdministrationService);
  protected readonly users = signal<readonly SecurityUserSummary[]>([]);

  constructor() {
    this.service.listUsers().subscribe((users) => this.users.set(users));
  }
}
