import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
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
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar title="Enrôlement Customer" />

    <sp-card title="1. Recherche bancaire">
      <form [formGroup]="form" (ngSubmit)="search()">
        <mat-form-field>
          <mat-label>Institution financière</mat-label>
          <input matInput formControlName="financialInstitutionCode" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>NIU</mat-label>
          <input matInput formControlName="niu" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Numéro client</mat-label>
          <input matInput formControlName="customerNumber" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Référence compte</mat-label>
          <input matInput formControlName="accountReference" />
        </mat-form-field>
        <button sp-button type="submit" [disabled]="searching()">
          Rechercher dans Amplitude
        </button>
      </form>
    </sp-card>

    @if (searching()) {
      <sp-loading label="Recherche bancaire" />
    }

    @if (preview(); as customer) {
      <sp-card title="2. Vérifier les données retournées">
        <p><strong>{{ customer.legalName }}</strong></p>
        <p>NIU : {{ customer.niu }}</p>
        <p>Référence banque : {{ customer.bankingCustomerReference }}</p>
        <p>Compte : {{ customer.maskedAccountIdentifier }}</p>
        <p>Devise : {{ customer.currency }}</p>
        <p>
          Le preview ne constitue pas une preuve. La confirmation déclenche
          une vérification bancaire fraîche côté backend avant création.
        </p>
        <button sp-button type="button" (click)="enroll()" [disabled]="enrolling()">
          Confirmer l’enrôlement
        </button>
      </sp-card>
    }
  `,
  styles: [`
    form { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 1rem; }
    @media (max-width: 700px) { form { grid-template-columns: 1fr; } }
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
    financialInstitutionCode: ['', [Validators.required, Validators.maxLength(50)]],
    niu: [''],
    customerNumber: [''],
    accountReference: ['', [Validators.required, Validators.maxLength(100)]],
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
