import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentsMockService } from '../services/payments-mock.service';

@Component({
  selector: 'sp-payment-list-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    FormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    SpCardComponent,
    MockContentStateComponent,
    MockStatePanelComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Paiements" description="Recherche et consultation des paiements SIXPAY." />
      <sp-mock-state-panel />

      <sp-card title="Filtres">
        <div class="sp-filter-grid">
          <mat-form-field appearance="outline">
            <mat-label>Référence</mat-label>
            <input matInput [(ngModel)]="reference" />
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>
            <mat-select [(ngModel)]="status">
              <mat-option value="">Tous</mat-option>
              <mat-option value="SUCCESS">SUCCESS</mat-option>
              <mat-option value="PROCESSING">PROCESSING</mat-option>
              <mat-option value="FAILED">FAILED</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Institution</mat-label>
            <mat-select [(ngModel)]="institution">
              <mat-option value="">Toutes</mat-option>
              <mat-option value="LRB">La Régionale Bank</mat-option>
            </mat-select>
          </mat-form-field>
        </div>
      </sp-card>

      <sp-card title="Résultats">
        @switch (scenario.scenario()) {
          @case ('loading') {
            <sp-mock-content-state kind="loading" title="Chargement des paiements" message="Simulation d'un appel de consultation en cours." />
          }
          @case ('empty') {
            <sp-mock-content-state kind="empty" title="Aucun paiement" message="Aucun paiement ne correspond aux critères sélectionnés." />
          }
          @case ('error') {
            <sp-mock-content-state kind="error" title="Consultation indisponible" message="Simulation d'une erreur de consultation. Aucun backend n'est appelé." />
          }
          @default {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr><th>Référence</th><th>Client</th><th>Institution</th><th>Montant</th><th>Statut</th><th>Créé</th></tr>
                </thead>
                <tbody>
                  @for (payment of visiblePayments(); track payment.id) {
                    <tr>
                      <td><a [routerLink]="[payment.id]">{{ payment.reference }}</a></td>
                      <td>{{ payment.customer }}</td>
                      <td>{{ payment.institution }}</td>
                      <td>{{ payment.amount | currency: payment.currency : 'code' : '1.0-0' }}</td>
                      <td><strong>{{ payment.status }}</strong></td>
                      <td>{{ payment.createdAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host, .sp-page { display: grid; gap: var(--sp-space-4); }
    .sp-filter-grid { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: var(--sp-space-3); }
    .sp-table-scroll { overflow-x: auto; }
    .sp-table { width: 100%; border-collapse: collapse; }
    .sp-table th, .sp-table td { padding: var(--sp-space-2); text-align: left; border-bottom: 1px solid var(--mat-sys-outline-variant); white-space: nowrap; }
    @media (max-width: 900px) { .sp-filter-grid { grid-template-columns: 1fr; } }
  `,
})
export class PaymentListPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly mocks = inject(PaymentsMockService);

  protected reference = '';
  protected status = '';
  protected institution = '';

  protected visiblePayments() {
    return this.mocks.payments.filter((payment) =>
      (!this.reference || payment.reference.toLowerCase().includes(this.reference.toLowerCase())) &&
      (!this.status || payment.status === this.status) &&
      (!this.institution || payment.institution === this.institution),
    );
  }
}
