import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { IdentityUser } from '../models/identity';
import { IdentityService } from '../services/identity.service';

@Component({
  selector: 'sp-users-page',
  imports: [DatePipe, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Gestion des accès"
        description="Projection mockée des identités SIXPAY — aucune mutation IAM."
      />

      <nav class="sp-tabs" aria-label="Gestion des accès">
        <a routerLink="/identity/users">Utilisateurs</a>
        <a routerLink="/identity/roles">Rôles</a>
      </nav>

      <sp-card title="Utilisateurs">
        <div class="sp-table-scroll">
          <table class="sp-table">
            <thead>
              <tr>
                <th>Utilisateur</th>
                <th>Subject</th>
                <th>Type</th>
                <th>Rôles</th>
                <th>Statut</th>
                <th>Dernière connexion</th>
              </tr>
            </thead>
            <tbody>
              @for (user of users(); track user.userId) {
                <tr>
                  <td>{{ user.displayName }}</td>
                  <td>{{ user.subject }}</td>
                  <td>{{ user.type }}</td>
                  <td>{{ user.roles.join(', ') }}</td>
                  <td>{{ user.status }}</td>
                  <td>
                    {{ user.lastLoginAt ? (user.lastLoginAt | date: 'dd/MM/yyyy HH:mm:ss') : '—' }}
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </sp-card>

      <sp-card title="Important">
        <p>
          Cette page ne crée, ne désactive et ne modifie aucun compte. L'Identity Provider reste la
          source d'autorité.
        </p>
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-tabs {
      display: flex;
      gap: var(--sp-space-3);
    }
    .sp-table-scroll {
      overflow-x: auto;
    }
    .sp-table {
      width: 100%;
      border-collapse: collapse;
    }
    .sp-table th,
    .sp-table td {
      padding: var(--sp-space-2);
      text-align: left;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      white-space: nowrap;
    }
  `,
})
export class UsersPageComponent {
  private readonly service = inject(IdentityService);
  protected readonly users = signal<readonly IdentityUser[]>([]);

  constructor() {
    this.service.users().subscribe((users) => this.users.set(users));
  }
}
