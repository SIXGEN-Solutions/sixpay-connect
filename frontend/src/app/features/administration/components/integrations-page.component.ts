import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { IntegrationStatus } from '../models/administration';
import { AdministrationService } from '../services/administration.service';

@Component({
  selector: 'sp-integrations-page',
  imports: [DatePipe, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/administration">← Retour à l'administration</a>
      <sp-toolbar
        title="Intégrations"
        description="Vue de supervision mockée des dépendances externes et internes."
      />

      <div class="sp-grid">
        @for (integration of integrations(); track integration.integrationId) {
          <sp-card [title]="integration.name" [subtitle]="integration.type">
            <p><strong>{{ integration.health }}</strong></p>
            <p>{{ integration.detail }}</p>
            <p>
              Dernier succès :
              {{
                integration.lastSuccessfulAt
                  ? (integration.lastSuccessfulAt | date: 'dd/MM/yyyy HH:mm:ss')
                  : '—'
              }}
            </p>
            <p>Contrôlé : {{ integration.lastCheckedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</p>
          </sp-card>
        }
      </div>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3)}
    @media(max-width:700px){.sp-grid{grid-template-columns:1fr}}
  `,
})
export class IntegrationsPageComponent {
  private readonly service = inject(AdministrationService);
  protected readonly integrations = signal<readonly IntegrationStatus[]>([]);

  constructor() {
    this.service.integrations().subscribe((integrations) => this.integrations.set(integrations));
  }
}
