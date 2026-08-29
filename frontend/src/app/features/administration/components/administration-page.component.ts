import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { AdministrationOverview } from '../models/administration';
import { AdministrationService } from '../services/administration.service';

@Component({
  selector: 'sp-administration-page',
  imports: [DatePipe, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Administration"
        description="Configuration, utilisateurs et sécurité opérationnelle SIXPAY."
      />

      @if (overview(); as data) {
        <div class="sp-kpis">
          <sp-card title="Zone cutoff comptable">
            <strong>
              {{ data.settings.accountingCutoffZone }}
            </strong>
          </sp-card>

          <sp-card title="Heure cutoff comptable">
            <strong>
              {{ data.settings.accountingCutoffTime }}
            </strong>
          </sp-card>

          <sp-card title="Observation">
            <strong class="sp-date">
              {{ data.observedAt | date: 'dd/MM/yyyy HH:mm:ss' }}
            </strong>
          </sp-card>
        </div>

        <div class="sp-grid">
          <sp-card title="Utilisateurs & sécurité" subtitle="Identités, méthodes et autorisations">
            <p>Administrer Local, SSO, linking OIDC, statut utilisateur et audit.</p>
            <a spCardActions routerLink="users"> Ouvrir </a>
          </sp-card>

          <sp-card title="Paramètres généraux" subtitle="Configuration applicative">
            <p>Configuration opérationnelle réellement exposée par SIXPAY.</p>
            <a spCardActions routerLink="settings"> Ouvrir </a>
          </sp-card>

          <sp-card title="Intégrations" subtitle="État des dépendances">
            <p>
              {{ degradedCount() }} intégration(s) non disponible(s) ou dégradée(s) sur
              {{ data.integrations.length }}.
            </p>
            <a spCardActions routerLink="integrations"> Ouvrir </a>
          </sp-card>
        </div>
      }
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }

    .sp-kpis {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    .sp-kpis strong {
      font-size: 2rem;
    }

    .sp-kpis .sp-date {
      font-size: 1rem;
    }

    .sp-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }

    @media (max-width: 900px) {
      .sp-kpis,
      .sp-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class AdministrationPageComponent {
  private readonly service = inject(AdministrationService);

  protected readonly overview = signal<AdministrationOverview | null>(null);

  constructor() {
    this.service.overview().subscribe((overview) => this.overview.set(overview));
  }

  protected degradedCount(): number {
    return (
      this.overview()?.integrations.filter((integration) => integration.health !== 'AVAILABLE')
        .length ?? 0
    );
  }
}
