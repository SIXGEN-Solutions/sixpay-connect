import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
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
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar
      title="Customers SIXPAY"
      description="Recherchez et administrez les Customers enrôlés dans SIXPAY."
    />

    <div class="customer-actions">
      @if (canCreate()) {
        <a mat-flat-button routerLink="/customers/enroll"> Enrôler un Customer </a>
      }

      <a mat-stroked-button routerLink="/customers/observed"> Clients observés </a>
    </div>

    <div class="customer-content">
      <sp-card title="Recherche Customer">
        <form
          class="customer-search-form"
          [formGroup]="searchForm"
          (ngSubmit)="search()"
          novalidate
        >
          <mat-form-field appearance="outline">
            <mat-label>NIU</mat-label>
            <input matInput formControlName="niu" autocomplete="off" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Nom</mat-label>
            <input matInput formControlName="legalName" autocomplete="off" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Institution</mat-label>
            <input matInput formControlName="financialInstitutionCode" autocomplete="off" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Tous</mat-option>
              <mat-option value="ACTIVE">ACTIVE</mat-option>
              <mat-option value="SUSPENDED">SUSPENDED</mat-option>
              <mat-option value="CLOSED">CLOSED</mat-option>
            </mat-select>
          </mat-form-field>

          <div class="customer-search-form__actions">
            <sp-button type="submit" icon="search" [disabled]="loading()"> Rechercher </sp-button>

            <sp-button
              type="button"
              variant="secondary"
              icon="restart_alt"
              [disabled]="loading()"
              (buttonClick)="reset()"
            >
              Réinitialiser
            </sp-button>
          </div>
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
            <sp-button
              type="button"
              variant="secondary"
              icon="chevron_left"
              [disabled]="page()?.first || loading()"
              (buttonClick)="previousPage()"
            >
              Précédent
            </sp-button>

            <span class="pagination__summary">
              Page {{ currentPage() + 1 }} / {{ displayTotalPages() }} —
              {{ page()?.totalElements ?? 0 }} résultat(s)
            </span>

            <sp-button
              type="button"
              variant="secondary"
              icon="chevron_right"
              [disabled]="page()?.last || loading()"
              (buttonClick)="nextPage()"
            >
              Suivant
            </sp-button>

            <mat-form-field appearance="outline" class="pagination__size">
              <mat-label>Taille</mat-label>
              <mat-select [value]="pageSize()" (selectionChange)="changePageSize($event.value)">
                <mat-option [value]="10">10</mat-option>
                <mat-option [value]="20">20</mat-option>
                <mat-option [value]="50">50</mat-option>
                <mat-option [value]="100">100</mat-option>
              </mat-select>
            </mat-form-field>
          </div>
        }
      </sp-card>
    </div>
  `,
  styles: [
    `
      .customer-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        margin: 1rem 0;
      }

      .customer-search-form {
        display: grid;
        grid-template-columns: repeat(4, minmax(0, 1fr));
        gap: 1rem;
        align-items: start;
      }

      .customer-search-form__actions {
        grid-column: 1 / -1;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      .customer-grid {
        display: grid;
        gap: 0.75rem;
      }

      .customer-row {
        display: grid;
        grid-template-columns: 2fr 1.2fr 1fr 0.8fr 1fr;
        gap: 1rem;
        padding: 1rem;
        border: 1px solid var(--mat-sys-outline-variant, #ddd);
        border-radius: 0.5rem;
        text-decoration: none;
        color: inherit;
      }

      .customer-row:hover {
        background: var(--mat-sys-surface-container-low, #f7f7f7);
      }

      .pagination {
        display: flex;
        flex-wrap: wrap;
        gap: 1rem;
        align-items: center;
        margin-top: 1rem;
      }

      .pagination__summary {
        min-width: 14rem;
      }

      .pagination__size {
        width: 8rem;
        margin-left: auto;
      }

      .customer-content {
        display: grid;
        gap: 1.5rem;
      }

      @media (max-width: 1000px) {
        .customer-search-form {
          grid-template-columns: repeat(2, minmax(0, 1fr));
        }

        .customer-row {
          grid-template-columns: 1fr 1fr;
        }
      }

      @media (max-width: 700px) {
        .customer-search-form,
        .customer-row {
          grid-template-columns: 1fr;
        }

        .pagination__size {
          margin-left: 0;
        }
      }
    `,
  ],
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

  protected readonly displayTotalPages = () => Math.max(this.page()?.totalPages ?? 0, 1);

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

  protected changePageSize(size: number): void {
    this.pageSize.set(size);
    this.currentPage.set(0);
    this.load();
  }

  private load(): void {
    const value = this.searchForm.getRawValue();

    const niu = value.niu.trim();
    const legalName = value.legalName.trim();
    const financialInstitutionCode = value.financialInstitutionCode.trim();
    const status = value.status as CustomerStatus | '';

    const criteria: CustomerSearchCriteria = {
      page: this.currentPage(),
      size: this.pageSize(),
      ...(niu ? { niu } : {}),
      ...(legalName ? { legalName } : {}),
      ...(status ? { status } : {}),
      ...(financialInstitutionCode ? { financialInstitutionCode } : {}),
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
