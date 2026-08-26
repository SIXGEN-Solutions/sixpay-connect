import {
  Component,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import {
  SpCardComponent,
} from '../../../shared/components/card/sp-card.component';
import {
  SpToolbarComponent,
} from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  GeneralSettings,
} from '../models/administration';
import {
  AdministrationService,
} from '../services/administration.service';

@Component({
  selector: 'sp-settings-page',
  imports: [
    RouterLink,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/administration">
        ← Retour à l'administration
      </a>

      <sp-toolbar
        title="Paramètres généraux"
        description="Configuration opérationnelle SIXPAY en lecture seule."
      />

      @if (settings(); as currentSettings) {
        <sp-card title="Cutoff comptable">
          <dl class="sp-details">
            <div>
              <dt>Zone</dt>
              <dd>
                {{ currentSettings.accountingCutoffZone }}
              </dd>
            </div>

            <div>
              <dt>Heure</dt>
              <dd>
                {{ currentSettings.accountingCutoffTime }}
              </dd>
            </div>
          </dl>
        </sp-card>

        <sp-card title="Important">
          <p>
            Cette boundary est volontairement
            read-only. Les mutations de configuration
            ne font pas partie du contrat FS-1.4.
          </p>
        </sp-card>
      }
    </section>
  `,
  styles: `
    :host,.sp-page{
      display:grid;
      gap:var(--sp-space-4)
    }

    .sp-details{
      display:grid;
      grid-template-columns:repeat(2,minmax(0,1fr));
      gap:var(--sp-space-3);
      margin:0
    }

    .sp-details div{
      display:grid;
      gap:.25rem
    }

    .sp-details dt{
      color:var(--mat-sys-on-surface-variant);
      font-size:.85rem
    }

    .sp-details dd{
      margin:0;
      font-weight:700;
      overflow-wrap:anywhere
    }

    @media(max-width:700px){
      .sp-details{
        grid-template-columns:1fr
      }
    }
  `,
})
export class SettingsPageComponent {
  private readonly service =
    inject(AdministrationService);

  protected readonly settings =
    signal<GeneralSettings | null>(null);

  constructor() {
    this.service
      .settings()
      .subscribe(
        (settings) =>
          this.settings.set(settings),
      );
  }
}
