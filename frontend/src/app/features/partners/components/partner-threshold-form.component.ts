import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { catchError, EMPTY, filter, finalize, switchMap } from 'rxjs';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import {
  SpDialogComponent,
  SpDialogData,
} from '../../../shared/components/dialog/sp-dialog.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { Partner } from '../models/partners';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'sp-partner-threshold-form',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpFormErrorComponent,
  ],
  templateUrl: './partner-threshold-form.component.html',
  styleUrl: './partner-forms.scss',
})
export class PartnerThresholdFormComponent {
  readonly partnerId = input.required<string>();
  readonly transactionTypes = input.required<readonly string[]>();
  readonly changed = output<Partner>();
  protected readonly submitting = signal(false);
  private readonly formBuilder = inject(FormBuilder);
  private readonly partners = inject(PartnersService);
  private readonly dialog = inject(MatDialog);

  protected readonly form = this.formBuilder.nonNullable.group({
    transactionType: ['', Validators.required],
    currency: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    amount: [0, [Validators.required, Validators.min(0.0001)]],
    validationLevels: [1, [Validators.required, Validators.min(1), Validators.max(10)]],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.dialog
      .open<SpDialogComponent, SpDialogData, boolean>(SpDialogComponent, {
        data: {
          title: 'Mettre à jour le seuil',
          message: `Confirmez le seuil de ${value.amount} ${value.currency.toUpperCase()} pour ${value.transactionType}.`,
          confirmLabel: 'Mettre à jour',
        },
      })
      .afterClosed()
      .pipe(
        filter(Boolean),
        switchMap(() => {
          this.submitting.set(true);
          return this.partners.configureValidationThreshold(
            this.partnerId(),
            value.transactionType,
            {
              currency: value.currency.toUpperCase(),
              amount: value.amount,
              validationLevels: value.validationLevels,
            },
          );
        }),
        catchError(() => EMPTY),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe((partner) => this.changed.emit(partner));
  }

  protected error(name: keyof typeof this.form.controls): string | undefined {
    const control = this.form.controls[name];
    if (!control.touched || !control.errors) {
      return undefined;
    }
    if (control.hasError('required')) {
      return 'Ce champ est obligatoire.';
    }
    if (control.hasError('pattern')) {
      return 'La devise doit contenir exactement trois lettres.';
    }
    if (name === 'validationLevels') {
      return 'Le nombre de niveaux doit être compris entre 1 et 10.';
    }
    return 'Le montant doit être supérieur à zéro.';
  }
}
