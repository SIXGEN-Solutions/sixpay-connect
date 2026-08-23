import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { CustomerMaster } from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-master-list-page',
  imports: [
    DatePipe,
    RouterLink,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar title="Customers SIXPAY" />

    <div class="actions">
      @if (canCreate()) {
        <a routerLink="/customers/enroll">Enrôler un Customer</a>
      }
      <a routerLink="/customers/observed">Observed Customers</a>
    </div>

    <sp-card title="Customers enrôlés">
      @if (loading()) {
        <sp-loading label="Chargement des Customers" />
      } @else if (customers().length === 0) {
        <p>Aucun Customer master enrôlé.</p>
      } @else {
        <div class="customer-grid">
          @for (customer of customers(); track customer.id) {
            <a class="customer-row" [routerLink]="['/customers', customer.id]">
              <strong>{{ customer.legalName }}</strong>
              <span>{{ customer.niu || 'NIU non renseigné' }}</span>
              <span>{{ customer.financialInstitutionCode }}</span>
              <span>{{ customer.status }}</span>
              <span>{{ customer.updatedAt | date: 'short' }}</span>
            </a>
          }
        </div>
      }
    </sp-card>
  `,
  styles: [`
    .actions { display: flex; gap: 1rem; margin: 1rem 0; }
    .customer-grid { display: grid; gap: .75rem; }
    .customer-row {
      display: grid;
      grid-template-columns: 2fr 1.2fr 1fr .8fr 1fr;
      gap: 1rem;
      padding: 1rem;
      border: 1px solid #ddd;
      border-radius: .5rem;
      text-decoration: none;
      color: inherit;
    }
    @media (max-width: 900px) {
      .customer-row { grid-template-columns: 1fr; }
    }
  `],
})
export class CustomerMasterListPageComponent {
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);

  protected readonly loading = signal(true);
  protected readonly customers = signal<CustomerMaster[]>([]);

  protected readonly canCreate = () =>
    this.auth.hasPermission('customer.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  constructor() {
    this.service
      .list()
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((items) => this.customers.set(items));
  }
}
