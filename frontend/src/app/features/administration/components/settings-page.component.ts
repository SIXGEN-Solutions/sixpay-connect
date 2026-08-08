import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { GeneralSettings } from '../models/administration';
import { AdministrationService } from '../services/administration.service';

@Component({
  selector: 'sp-settings-page',
  imports: [DatePipe, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/administration">← Retour à l'administration</a>
      <sp-toolbar
        title="Paramètres généraux"
        description="Configuration mockée en lecture seule — aucune mutation backend."
      />

      @if (settings(); as currentSettings) {
        <sp-card title="Traitement">
          <dl class="sp-details">
            <div><dt>Taille lot comptable</dt><dd>{{ currentSettings.accountingBatchSize }}</dd></div>
            <div><dt>Timeout paiement</dt><dd>{{ currentSettings.paymentTimeoutMs }} ms</dd></div>
            <div><dt>Rétention opérationnelle</dt><dd>{{ currentSettings.operationalRetentionDays }} jours</dd></div>
            <div><dt>Montant maximal mock</dt><dd>{{ currentSettings.maxPaymentAmountXaf }} XAF</dd></div>
            <div><dt>Maintenance</dt><dd>{{ currentSettings.maintenanceMode ? 'ACTIVE' : 'INACTIVE' }}</dd></div>
            <div><dt>Dernière mise à jour</dt><dd>{{ currentSettings.updatedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd></div>
            <div><dt>Mis à jour par</dt><dd>{{ currentSettings.updatedBy }}</dd></div>
          </dl>
        </sp-card>

        <sp-card title="Important">
          <p>
            Phase 7.6 n'implémente aucune sauvegarde réelle. Le contrat de configuration
            et les droits de mutation restent TO_DEFINE.
          </p>
        </sp-card>
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-details{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3);margin:0}
    .sp-details div{display:grid;gap:.25rem}
    .sp-details dt{color:var(--mat-sys-on-surface-variant);font-size:.85rem}
    .sp-details dd{margin:0;font-weight:700;overflow-wrap:anywhere}
    @media(max-width:700px){.sp-details{grid-template-columns:1fr}}
  `,
})
export class SettingsPageComponent {
  private readonly service = inject(AdministrationService);
  protected readonly settings = signal<GeneralSettings | null>(null);

  constructor() {
    this.service.settings().subscribe((settings) => this.settings.set(settings));
  }
}
