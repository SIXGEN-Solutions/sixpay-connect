import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-administration-page',
  imports: [RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Administration" description="Paramètres et configuration mockés de SIXPAY." />
      <div class="sp-grid">
        <sp-card title="Paramètres généraux" subtitle="Configuration applicative">
          <p>Seuils opérationnels, fenêtres de traitement et options fonctionnelles.</p>
          <a spCardActions routerLink="settings">Ouvrir</a>
        </sp-card>
        <sp-card title="Intégrations" subtitle="État et configuration">
          <p>TresorPay, Amplitude, Accounting API et notifications.</p>
          <a spCardActions routerLink="integrations">Ouvrir</a>
        </sp-card>
      </div>
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3)}@media(max-width:700px){.sp-grid{grid-template-columns:1fr}}`,
})
export class AdministrationPageComponent {}
