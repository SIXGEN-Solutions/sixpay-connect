import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { BankingCustomerPreview } from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-enrollment-wizard',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar
      title="Enrôlement Customer"
      description="Recherchez le client dans Amplitude puis confirmez son enrôlement dans SIXPAY."
    />

    <sp-card title="1. Recherche bancaire">
      <form
        class="customer-enrollment-form"
        [formGroup]="form"
        (ngSubmit)="search()"
        novalidate
      >
        <mat-form-field appearance="outline">
          <mat-label>Institution financière</mat-label>
          <input
            matInput
            formControlName="financialInstitutionCode"
            autocomplete="off"
          />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>NIU</mat-label>
          <input
            matInput
            formControlName="niu"
            autocomplete="off"
          />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Numéro client</mat-label>
          <input
            matInput
            formControlName="customerNumber"
            autocomplete="off"
          />
        </mat-form-field>

        <mat-form-field appearance="outline">
          <mat-label>Référence compte</mat-label>
          <input
            matInput
            formControlName="accountReference"
            autocomplete="off"
          />
        </mat-form-field>

        <div class="customer-enrollment-form__actions">
          <sp-button
            type="submit"
            icon="search"
            [disabled]="searching()"
          >
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
            <span class="customer-preview__label">Compte</span>
            <span>{{ customer.maskedAccountIdentifier }}</span>
          </div>
          <div>
            <span class="customer-preview__label">Devise</span>
            <span>{{ customer.currency }}</span>
          </div>
        </div>

        <p class="customer-preview__notice">
          Le preview ne constitue pas une preuve. La confirmation déclenche
          une vérification bancaire fraîche côté backend avant création.
        </p>

        <div class="customer-enrollment-form__actions">
          <sp-button
            type="button"
            icon="person_add"
            [disabled]="enrolling()"
            (buttonClick)="enroll()"
          >
            {{ enrolling() ? 'Enrôlement…' : 'Confirmer l’enrôlement' }}
          </sp-button>
        </div>
      </sp-card>
    }
  `,
  styles: [`
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

    .customer-preview__notice {
      margin: 1rem 0;
    }

    @media (max-width: 700px) {
      .customer-enrollment-form,
      .customer-preview {
        grid-template-columns: 1fr;
      }
    }
  `],
})
export class CustomerEnrollmentWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CustomerManagementService);
  private readonly router = inject(Router);
  protected readonly errorService = inject(ErrorService);

  protected readonly searching = signal(false);
  protected readonly enrolling = signal(false);
  protected readonly preview = signal<BankingCustomerPreview | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    financialInstitutionCode: [
      '',
      [Validators.required, Validators.maxLength(50)],
    ],
    niu: [''],
    customerNumber: [''],
    accountReference: [
      '',
      [Validators.required, Validators.maxLength(100)],
    ],
  });

  protected search(): void {
    if (this.form.invalid || this.searching()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const niu = value.niu.trim();
    const customerNumber = value.customerNumber.trim();

    this.preview.set(null);
    this.errorService.clear();
    this.searching.set(true);

    this.service
      .bankingPreview({
        financialInstitutionCode:
          value.financialInstitutionCode.trim(),
        accountReference:
          value.accountReference.trim(),
        ...(niu ? { niu } : {}),
        ...(customerNumber
          ? { customerNumber }
          : {}),
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() =>
          this.searching.set(false)
        ),
      )
      .subscribe((preview) =>
        this.preview.set(preview)
      );
  }

  protected enroll(): void {
    if (!this.preview() || this.enrolling()) {
      return;
    }

    const value = this.form.getRawValue();
    const niu = value.niu.trim();
    const customerNumber =
      value.customerNumber.trim();

    this.enrolling.set(true);

    this.service
      .enroll({
        financialInstitutionCode:
          value.financialInstitutionCode.trim(),
        accountReference:
          value.accountReference.trim(),
        ...(niu ? { niu } : {}),
        ...(customerNumber
          ? { customerNumber }
          : {}),
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() =>
          this.enrolling.set(false)
        ),
      )
      .subscribe((customer) => {
        void this.router.navigate(
          ['/customers', customer.id],
          {
            queryParams: {
              enrolled: true,
            },
          },
        );
      });
  }
}
