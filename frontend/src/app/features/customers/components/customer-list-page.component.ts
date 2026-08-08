import { DatePipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { CustomersMockService } from '../services/customers-mock.service';

@Component({
  selector: 'sp-customer-list-page',
  imports: [DatePipe, FormsModule, MatFormFieldModule, MatInputModule, RouterLink, SpCardComponent, MockContentStateComponent, MockStatePanelComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <sp-toolbar title="Clients observés" description="Projection non autoritative des clients observés dans les paiements." />
      <sp-mock-state-panel />
      <sp-card title="Recherche">
        <div class="sp-filter-grid">
          <mat-form-field appearance="outline"><mat-label>Nom légal</mat-label><input matInput [(ngModel)]="name" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>NIU</mat-label><input matInput [(ngModel)]="niu" /></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Institution</mat-label><input matInput value="LRB" /></mat-form-field>
        </div>
      </sp-card>

      <sp-card title="Résultats">
        @switch (scenario.scenario()) {
          @case ('loading') { <sp-mock-content-state kind="loading" title="Chargement des clients" message="Simulation d'une consultation en cours." /> }
          @case ('empty') { <sp-mock-content-state kind="empty" title="Aucun client observé" message="Aucun résultat pour les critères sélectionnés." /> }
          @case ('error') { <sp-mock-content-state kind="error" title="Consultation indisponible" message="Erreur simulée sans appel backend." /> }
          @default {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead><tr><th>Nom légal</th><th>NIU</th><th>Institution</th><th>Dernier statut</th><th>Dernière observation</th><th>Paiements</th></tr></thead>
                <tbody>
                  @for (customer of filtered(); track customer.id) {
                    <tr>
                      <td><a [routerLink]="[customer.id]">{{ customer.legalName }}</a></td>
                      <td>{{ customer.niuMasked }}</td><td>{{ customer.institution }}</td><td>{{ customer.lastPaymentStatus }}</td>
                      <td>{{ customer.lastObservedAt | date: 'dd/MM/yyyy HH:mm' }}</td><td>{{ customer.paymentCount }}</td>
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
    :host, .sp-page { display:grid; gap:var(--sp-space-4); }
    .sp-filter-grid { display:grid; grid-template-columns:2fr 1fr 1fr; gap:var(--sp-space-3); }
    .sp-table-scroll { overflow-x:auto; } .sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant);white-space:nowrap}
    @media(max-width:900px){.sp-filter-grid{grid-template-columns:1fr}}
  `,
})
export class CustomerListPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly mocks = inject(CustomersMockService);
  protected name = '';
  protected niu = '';

  protected filtered() {
    return this.mocks.customers.filter((customer) =>
      (!this.name || customer.legalName.toLowerCase().includes(this.name.toLowerCase())) &&
      (!this.niu || customer.niuMasked.toLowerCase().includes(this.niu.toLowerCase())),
    );
  }
}
