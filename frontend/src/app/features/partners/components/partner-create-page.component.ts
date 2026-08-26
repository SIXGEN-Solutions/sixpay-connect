import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'sp-partner-create-page',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpFormErrorComponent,
    SpToolbarComponent,
  ],
  templateUrl: './partner-create-page.component.html',
  styleUrl: './partner-forms.scss',
})
export class PartnerCreatePageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly partners = inject(PartnersService);
  private readonly router = inject(Router);
  protected readonly errorService = inject(ErrorService);
  protected readonly submitting = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    legalName: ['', [Validators.required, Validators.maxLength(200)]],
    technicalContactName: ['', [Validators.required, Validators.maxLength(150)]],
    technicalContactEmail: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    authorizedTransactionTypes: ['', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const transactionTypes = [
      ...new Set(
        value.authorizedTransactionTypes
          .split(',')
          .map((item) => item.trim())
          .filter(Boolean),
      ),
    ];
    if (transactionTypes.length === 0) {
      this.form.controls.authorizedTransactionTypes.setErrors({ required: true });
      return;
    }

    this.errorService.clear();
    this.submitting.set(true);
    this.partners
      .create({
        legalName: value.legalName.trim(),
        technicalContactName: value.technicalContactName.trim(),
        technicalContactEmail: value.technicalContactEmail.trim(),
        authorizedTransactionTypes: transactionTypes,
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe((partner) => {
        void this.router.navigate(['/partners', partner.id], {
          queryParams: { created: true },
        });
      });
  }

  protected fieldError(name: keyof typeof this.form.controls): string | undefined {
    const backendError = this.errorService.currentError()?.fieldErrors[name];
    if (backendError) {
      return backendError;
    }

    const control = this.form.controls[name];
    if (!control.touched || !control.errors) {
      return undefined;
    }
    if (control.hasError('required')) {
      return 'Ce champ est obligatoire.';
    }
    if (control.hasError('email')) {
      return 'Saisissez une adresse courriel valide.';
    }
    return 'La valeur saisie dépasse la longueur autorisée.';
  }
}
