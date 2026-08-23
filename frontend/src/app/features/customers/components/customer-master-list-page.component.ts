import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  CustomerMaster,
  CustomerPage,
  CustomerSearchCriteria,
  CustomerStatus,
} from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-master-list-page',
  imports: [
    DatePipe,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
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

    <sp-card title="Recherche Customer">
      <form [formGroup]="searchForm" (ngSubmit)="search()">
        <mat-form-field>
          <mat-label>NIU</mat-label>
          <input matInput formControlName="niu" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Nom</mat-label>
          <input matInput formControlName="legalName" />
        </mat-form-field>

        <mat-form-field>
          <mat-label>Institution</mat-label>
          <input matInput formControlName="financialInstitutionCode" />
        </mat-form-field>

        <label>
          Statut
          <select formControlName="status">
            <option value="">Tous</option>
            <option value="ACTIVE">ACTIVE</option>
            <option value="SUSPENDED">SUSPENDED</option>
            <option value="CLOSED">CLOSED</option>
          </select>
        </label>

        <button type="submit">Rechercher</button>
        <button type="button" (click)="reset()">Réinitialiser</button>
      </form>
    </sp-card>

    <sp-card title="Customers enrôlés">
      @if (loading()) {
        <sp-loading label="Chargement des Customers" />
      } @else if (customers().length === 0) {
        <p>Aucun Customer ne correspond aux critères.</p>
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

        <div class="pagination">
          <button
            type="button"
            [disabled]="page()?.first || loading()"
            (click)="previousPage()"
          >
            Précédent
          </button>

          <span>
            Page {{ currentPage() + 1 }} / {{ displayTotalPages() }}
            — {{ page()?.totalElements ?? 0 }} résultat(s)
          </span>

          <button
            type="button"
            [disabled]="page()?.last || loading()"
            (click)="nextPage()"
          >
            Suivant
          </button>

          <label>
            Taille
            <select [value]="pageSize()" (change)="changePageSize($event)">
              <option value="10">10</option>
              <option value="20">20</option>
              <option value="50">50</option>
              <option value="100">100</option>
            </select>
          </label>
        </div>
      }
    </sp-card>
  `,
  styles: [`
    .actions,
    .pagination {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      align-items: center;
      margin: 1rem 0;
    }

    form {
      display: grid;
      grid-template-columns: repeat(4, minmax(0, 1fr));
      gap: 1rem;
      align-items: center;
    }

    .customer-grid {
      display: grid;
      gap: .75rem;
    }

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
      form,
      .customer-row {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class CustomerMasterListPageComponent {
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);
  private readonly fb = inject(FormBuilder);

  protected readonly loading = signal(true);
  protected readonly page = signal<CustomerPage | null>(null);
  protected readonly customers = signal<CustomerMaster[]>([]);
  protected readonly currentPage = signal(0);
  protected readonly pageSize = signal(20);

  protected readonly searchForm = this.fb.nonNullable.group({
    niu: [''],
    legalName: [''],
    status: [''],
    financialInstitutionCode: [''],
  });

  protected readonly canCreate = () =>
    this.auth.hasPermission('customer.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly displayTotalPages = () =>
    Math.max(this.page()?.totalPages ?? 0, 1);

  constructor() {
    this.load();
  }

  protected search(): void {
    this.currentPage.set(0);
    this.load();
  }

  protected reset(): void {
    this.searchForm.reset({
      niu: '',
      legalName: '',
      status: '',
      financialInstitutionCode: '',
    });
    this.currentPage.set(0);
    this.load();
  }

  protected previousPage(): void {
    if (this.currentPage() === 0) {
      return;
    }
    this.currentPage.update((value) => value - 1);
    this.load();
  }

  protected nextPage(): void {
    if (this.page()?.last) {
      return;
    }
    this.currentPage.update((value) => value + 1);
    this.load();
  }

  protected changePageSize(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.pageSize.set(Number(target.value));
    this.currentPage.set(0);
    this.load();
  }

  private load(): void {
    const value = this.searchForm.getRawValue();

    const niu = value.niu.trim();
    const legalName = value.legalName.trim();
    const financialInstitutionCode =
      value.financialInstitutionCode.trim();
    const status = value.status as CustomerStatus | '';

    const criteria: CustomerSearchCriteria = {
      page: this.currentPage(),
      size: this.pageSize(),
      ...(niu ? { niu } : {}),
      ...(legalName ? { legalName } : {}),
      ...(status ? { status } : {}),
      ...(financialInstitutionCode
        ? { financialInstitutionCode }
        : {}),
    };

    this.loading.set(true);

    this.service
      .search(criteria)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((page) => {
        this.page.set(page);
        this.customers.set(page.content);
        this.currentPage.set(page.page);
      });
  }
}
