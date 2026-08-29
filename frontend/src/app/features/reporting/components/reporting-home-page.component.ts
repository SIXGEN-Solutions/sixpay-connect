import { Component, inject } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-reporting-home-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Audit / Reporting"
        description="Consultation privilégiée des preuves Payment et exports contrôlés."
      />

      <div class="sp-grid">
        <sp-card title="Timeline Payment">
          <p>Consulter la chronologie normalisée d'un paiement.</p>
          <label for="timeline-payment-id">Payment ID</label>
          <input id="timeline-payment-id" [formControl]="paymentId" />
          <div spCardActions>
            <sp-button icon="timeline" (buttonClick)="openTimeline()">Ouvrir la timeline</sp-button>
          </div>
        </sp-card>

        <sp-card title="Preuves d'audit">
          <p>Rechercher les enregistrements immuables Payment et intégration.</p>
          <a spCardActions routerLink="/reporting/audit-records">Rechercher</a>
        </sp-card>

        <sp-card title="Export contrôlé">
          <p>Créer un export borné avec justification métier.</p>
          <a spCardActions routerLink="/reporting/exports">Créer un export</a>
        </sp-card>
      </div>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }
    input {
      width: 100%;
      box-sizing: border-box;
      padding: 0.75rem;
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
    }
    label {
      display: block;
      margin-bottom: 0.35rem;
      font-weight: 700;
    }
    @media (max-width: 900px) {
      .sp-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class ReportingHomePageComponent {
  private readonly router = inject(Router);

  protected readonly paymentId = new FormControl('7fa85f64-5717-4562-b3fc-2c963f66afa1', {
    nonNullable: true,
  });

  protected openTimeline(): void {
    const paymentId = this.paymentId.value.trim();
    if (paymentId) {
      void this.router.navigate(['/reporting/payments', paymentId, 'timeline']);
    }
  }
}
