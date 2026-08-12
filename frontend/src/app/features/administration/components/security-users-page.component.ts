import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { SecurityUserSummary } from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

@Component({
  selector: 'sp-security-users-page',
  imports: [
    RouterLink,
    DatePipe,
    SpButtonComponent,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Utilisateurs & sécurité"
        description="Identités, méthodes d’authentification et autorisations SIXPAY."
      />

      <div class="sp-actions">
        <a routerLink="new">Créer un utilisateur</a>
      </div>

      @if (loading()) {
        <sp-card title="Chargement">
          <p>Chargement des utilisateurs…</p>
        </sp-card>
      } @else if (users().length === 0) {
        <sp-card title="Aucun utilisateur">
          <p>Aucun utilisateur SIXPAY n’est actuellement enregistré.</p>
          <a spCardActions routerLink="new">Créer le premier utilisateur</a>
        </sp-card>
      } @else {
        @for (user of users(); track user.id) {
          <sp-card
            [title]="user.username"
            [subtitle]="user.email ?? 'Email non renseigné'"
          >
            <p>
              Statut: <strong>{{ user.status }}</strong><br />
              Local:
              <strong>{{ user.localEnabled ? 'Activé' : 'Désactivé' }}</strong><br />
              SSO:
              <strong>{{ user.oidcLinked ? 'Lié' : 'Non lié' }}</strong><br />
              Dernière authentification:
              {{
                user.lastAuthenticationAt
                  ? (user.lastAuthenticationAt | date: 'medium')
                  : '—'
              }}
            </p>
            <a spCardActions [routerLink]="[user.id]">Administrer</a>
          </sp-card>
        }
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-actions{display:flex;justify-content:flex-end}
  `,
})
export class SecurityUsersPageComponent {
  private readonly service = inject(SecurityUserAdministrationService);
  protected readonly errorService = inject(ErrorService);
  protected readonly users = signal<readonly SecurityUserSummary[]>([]);
  protected readonly loading = signal(true);

  constructor() {
    this.reload();
  }

  private reload(): void {
    this.errorService.clear();
    this.loading.set(true);

    this.service
      .listUsers()
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((users) => this.users.set(users));
  }
}
