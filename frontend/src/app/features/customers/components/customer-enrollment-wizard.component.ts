import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { Router } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { BankingCustomerPreview } from '../models/customer-management';
import { BankingCustomerPreviewRequest } from '../models/customer-management.requests';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-enrollment-wizard',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatRadioModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar
      title="Enrôlement Client"
      description="Recherchez le client dans Amplitude puis confirmez son enrôlement dans SIXPAY."
    />

    <sp-card title="1. Recherche bancaire">
      <form class="customer-enrollment-form" [formGroup]="form" (ngSubmit)="search()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Institution financière</mat-label>
          <input matInput formControlName="financialInstitutionCode" autocomplete="off" />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Numéro client</mat-label>
          <input matInput formControlName="customerNumber" autocomplete="off" required />
        </mat-form-field>

        <div class="customer-enrollment-form__actions">
          <sp-button type="submit" icon="search" [disabled]="searching()">
            {{ searching() ? 'Recherche…' : 'Rechercher dans Amplitude' }}
          </sp-button>
        </div>
      </form>
    </sp-card>

    @if (searching()) {
      <sp-loading label="Recherche bancaire" />
    }

    @if (preview(); as customer) {
      <sp-card title="2. Vérifier les données retournées">
        <div class="customer-preview">
          <div>
            <span class="customer-preview__label">Nom légal</span>
            <strong>{{ customer.legalName }}</strong>
          </div>
          <div>
            <span class="customer-preview__label">NIU</span>
            <span>{{ customer.niu }}</span>
          </div>
          <div>
            <span class="customer-preview__label">Référence banque</span>
            <span>{{ customer.bankingCustomerReference }}</span>
          </div>
          <div>
            <span class="customer-preview__label">Numéro client</span>
            <span>{{ customer.customerNumber }}</span>
          </div>
        </div>

        <div class="customer-preview__accounts">
          <strong>3. Choisir le compte bancaire par défaut</strong>
          @if (customer.accounts.length === 0) {
            <p>Aucun compte bancaire n'a été retourné pour ce client.</p>
          } @else {
            <mat-radio-group
              aria-label="Sélectionner le compte bancaire par défaut"
              [value]="selectedAccountReference()"
              (change)="selectedAccountReference.set($event.value)"
            >
              @for (account of customer.accounts; track account.accountReference) {
                <mat-radio-button
                  [value]="account.accountReference"
                  [disabled]="account.status !== 'ACTIVE'"
                >
                  {{ account.maskedAccountIdentifier }} — {{ account.status || 'Statut non renseigné' }}
                </mat-radio-button>
              }
            </mat-radio-group>
          }
        </div>

        <p class="customer-preview__notice">
          La liste des comptes est fournie par la banque, mais ne constitue pas
          une preuve de vérification. Une vérification bancaire fraîche du compte
          sélectionné est effectuée à la confirmation avant toute création.
        </p>

        <div class="customer-enrollment-form__actions">
          <sp-button
            type="button"
            icon="person_add"
            [disabled]="enrolling() || !selectedAccountReference()"
            (buttonClick)="enroll()"
          >
            {{ enrolling() ? 'Enrôlement…' : 'Confirmer l’enrôlement' }}
          </sp-button>
        </div>
      </sp-card>
    }
  `,
  styles: [
    `
      .customer-enrollment-form {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
        align-items: start;
      }

      .customer-enrollment-form__actions {
        grid-column: 1 / -1;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      .customer-preview {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
        gap: 1rem;
        margin-bottom: 1rem;
      }

      .customer-preview > div {
        display: grid;
        gap: 0.25rem;
      }

      .customer-preview__label {
        font-size: 0.875rem;
        opacity: 0.75;
      }

      .customer-preview__accounts {
        grid-column: 1 / -1;
        display: grid;
        gap: 0.8rem;
      }

      .customer-preview__accounts mat-radio-group {
        display: grid;
        gap: 0.5rem;
      }

      .customer-preview__notice {
        margin: 1rem 0;
      }

      @media (max-width: 700px) {
        .customer-enrollment-form,
        .customer-preview {
          grid-template-columns: 1fr;
        }
      }
    `,
  ],
})
export class CustomerEnrollmentWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CustomerManagementService);
  private readonly router = inject(Router);
  protected readonly errorService = inject(ErrorService);

  protected readonly searching = signal(false);
  protected readonly enrolling = signal(false);
  protected readonly preview = signal<BankingCustomerPreview | null>(null);

  protected readonly selectedAccountReference = signal<string | null>(null);
  protected readonly reviewedRequest = signal<BankingCustomerPreviewRequest | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    financialInstitutionCode: ['REGIONAL', [Validators.required, Validators.maxLength(50)]],
    customerNumber: ['', [Validators.required, Validators.maxLength(50)]],
  });

  constructor() {
    // Prevent using an obsolete preview when search criteria are changed.
    this.form.valueChanges.subscribe(() => {
      this.preview.set(null);
      this.reviewedRequest.set(null);
      this.selectedAccountReference.set(null);
    });
  }

  protected search(): void {
    if (this.form.invalid || this.searching()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request: BankingCustomerPreviewRequest = {
      financialInstitutionCode: value.financialInstitutionCode.trim(),
      customerNumber: value.customerNumber.trim(),
    };
    if (!request.financialInstitutionCode || !request.customerNumber) {
      this.form.markAllAsTouched();
      return;
    }

    this.preview.set(null);
    this.reviewedRequest.set(null);
    this.selectedAccountReference.set(null);
    this.errorService.clear();
    this.searching.set(true);

    this.service
      .bankingPreview(request)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.searching.set(false)),
      )
      .subscribe((preview) => {
        const current = this.form.getRawValue();
        if (current.financialInstitutionCode.trim() !== request.financialInstitutionCode
            || current.customerNumber.trim() !== request.customerNumber) {
          return; // Ignore a completed search after the operator changed the form.
        }
        this.reviewedRequest.set(request);
        this.preview.set(preview);
      });
  }

  protected enroll(): void {
    const request = this.reviewedRequest();
    const selectedAccount = this.selectedAccountReference();
    const preview = this.preview();

    if (
      !request ||
      !selectedAccount ||
      !preview ||
      this.enrolling() ||
      !preview.accounts.some(
        (account) =>
          account.accountReference === selectedAccount &&
          account.status === 'ACTIVE'
      )
    ) {
      return;
    }

    const niu = preview.niu?.trim();

    const enrollmentRequest = {
      ...request,
      ...(niu ? { niu } : {}),
      accountReference: selectedAccount,
    };

    this.enrolling.set(true);

    this.service
      .enroll(enrollmentRequest)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.enrolling.set(false)),
      )
      .subscribe((customer) => {
        void this.router.navigate(['/customers', customer.id], {
          queryParams: { enrolled: true },
        });
      });
  }
}
