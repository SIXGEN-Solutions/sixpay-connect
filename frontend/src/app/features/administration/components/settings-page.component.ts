import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-settings-page',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, RouterLink, SpCardComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Paramètres généraux" description="Formulaire mocké — aucune sauvegarde backend." />
      <a routerLink="/administration">← Retour à l'administration</a>
      <sp-card title="Traitement">
        <div class="sp-grid">
          <mat-form-field appearance="outline"><mat-label>Taille lot comptable</mat-label><input matInput type="number" [(ngModel)]="batchSize" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Timeout paiement (ms)</mat-label><input matInput type="number" [(ngModel)]="paymentTimeout" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Rétention opérationnelle (jours)</mat-label><input matInput type="number" [(ngModel)]="retentionDays" /></mat-form-field>
        </div>
        <p>Les valeurs sont locales à la maquette et disparaissent au rechargement.</p>
      </sp-card>
    </section>
  `,
  styles: `:host,.sp-page{display:grid;gap:var(--sp-space-4)}.sp-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}@media(max-width:800px){.sp-grid{grid-template-columns:1fr}}`,
})
export class SettingsPageComponent {
  protected batchSize = 500;
  protected paymentTimeout = 5000;
  protected retentionDays = 90;
}
