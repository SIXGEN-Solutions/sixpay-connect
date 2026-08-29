import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';

import { SpButtonComponent } from '../../shared/components/button/sp-button.component';
import { AuthenticationService } from './authentication.service';

const PASSWORD_MIN_LENGTH = 12;
const PASSWORD_MAX_LENGTH = 200;

const matchingPasswordsValidator: ValidatorFn = (
  control: AbstractControl,
): ValidationErrors | null => {
  const newPassword = control.get('newPassword')?.value as string | undefined;

  const confirmation = control.get('confirmation')?.value as string | undefined;

  if (!newPassword || !confirmation) {
    return null;
  }

  return newPassword === confirmation ? null : { passwordsMismatch: true };
};

@Component({
  selector: 'sp-password-change',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    SpButtonComponent,
  ],
  template: `
    <main class="sp-password-change-page">
      <mat-card appearance="outlined" class="sp-password-change-card">
        <mat-card-header>
          <mat-card-title> Changer votre mot de passe </mat-card-title>
          <mat-card-subtitle>
            @if (authentication.passwordChangeRequired()) {
              Votre mot de passe temporaire ou expiré doit être remplacé avant de continuer dans
              SIXPAY CONNECT.
            } @else {
              Modifiez votre mot de passe Local SIXPAY.
            }
          </mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form class="sp-password-change-form" [formGroup]="form" (ngSubmit)="submit()" novalidate>
            <mat-form-field appearance="outline">
              <mat-label> Mot de passe actuel </mat-label>
              <input
                matInput
                type="password"
                autocomplete="current-password"
                formControlName="currentPassword"
              />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label> Nouveau mot de passe </mat-label>
              <input
                matInput
                type="password"
                autocomplete="new-password"
                formControlName="newPassword"
                [attr.maxlength]="passwordMaxLength"
              />
              <mat-hint> {{ passwordMinLength }} caractères minimum. </mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label> Confirmation </mat-label>
              <input
                matInput
                type="password"
                autocomplete="new-password"
                formControlName="confirmation"
                [attr.maxlength]="passwordMaxLength"
              />
            </mat-form-field>

            @if (form.hasError('passwordsMismatch') && form.controls.confirmation.touched) {
              <p class="sp-password-change-error" role="alert">
                La confirmation ne correspond pas au nouveau mot de passe.
              </p>
            }

            <section class="sp-password-rules" aria-label="Règles du mot de passe">
              <strong>Règles</strong>
              <ul>
                <li>{{ passwordMinLength }} caractères minimum</li>
                <li>différent du mot de passe actuel et des derniers mots de passe</li>
              </ul>
            </section>

            @if (serverError()) {
              <p class="sp-password-change-error" role="alert">
                {{ serverError() }}
              </p>
            }

            <sp-button type="submit" [disabled]="form.invalid || submitting()">
              {{ submitting() ? 'Modification…' : 'Modifier mon mot de passe' }}
            </sp-button>

            @if (authentication.passwordChangeRequired()) {
              <p class="sp-password-change-restriction">
                Tant que le mot de passe n’est pas modifié, seul ce parcours et la déconnexion
                restent disponibles.
              </p>
            }
          </form>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    .sp-password-change-page {
      max-width: 42rem;
      margin: var(--sp-space-6) auto;
      padding: var(--sp-space-4);
    }

    .sp-password-change-card {
      width: 100%;
    }

    mat-card-content,
    .sp-password-change-form {
      display: grid;
      gap: var(--sp-space-4);
      padding-top: var(--sp-space-4);
    }

    .sp-password-change-form mat-form-field {
      width: 100%;
    }

    .sp-password-rules {
      padding: var(--sp-space-3);
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: var(--sp-radius-md);
    }

    .sp-password-rules ul {
      margin-bottom: 0;
    }

    .sp-password-change-error {
      margin: 0;
      color: var(--mat-sys-error);
      font-weight: 600;
    }

    .sp-password-change-restriction {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
    }
  `,
})
export class PasswordChangeComponent {
  protected readonly authentication = inject(AuthenticationService);

  protected readonly passwordMinLength = PASSWORD_MIN_LENGTH;

  protected readonly passwordMaxLength = PASSWORD_MAX_LENGTH;

  protected readonly submitting = signal(false);

  protected readonly serverError = signal<string | null>(null);

  protected readonly form = new FormGroup(
    {
      currentPassword: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
      newPassword: new FormControl('', {
        nonNullable: true,
        validators: [
          Validators.required,
          Validators.minLength(PASSWORD_MIN_LENGTH),
          Validators.maxLength(PASSWORD_MAX_LENGTH),
        ],
      }),
      confirmation: new FormControl('', {
        nonNullable: true,
        validators: [Validators.required],
      }),
    },
    {
      validators: [matchingPasswordsValidator],
    },
  );

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    this.serverError.set(null);
    this.submitting.set(true);

    this.authentication
      .changeLocalPassword({
        currentPassword: value.currentPassword,
        newPassword: value.newPassword,
      })
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        error: (error: unknown) => {
          this.serverError.set(passwordChangeErrorMessage(error));
        },
      });
  }
}

function passwordChangeErrorMessage(error: unknown): string {
  if (!(error instanceof HttpErrorResponse)) {
    return 'Impossible de modifier le mot de passe. Réessayez.';
  }

  if (error.status === 400 && typeof error.error?.detail === 'string') {
    return error.error.detail;
  }

  if (error.status === 409) {
    return typeof error.error?.detail === 'string'
      ? error.error.detail
      : 'Le mot de passe Local ne peut pas être modifié pour cette session.';
  }

  return 'Impossible de modifier le mot de passe. Réessayez.';
}
