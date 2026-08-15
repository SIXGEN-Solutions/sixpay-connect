import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  SIXPAY_SECURITY_PERMISSIONS,
  SIXPAY_SECURITY_ROLES,
} from '../models/security-authorization-catalog';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

@Component({
  selector: 'sp-security-user-create-page',
  imports: [
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpFormErrorComponent,
    SpToolbarComponent,
  ],
  templateUrl: './security-user-create-page.component.html',
  styleUrl: './security-user-form.scss',
})
export class SecurityUserCreatePageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly service = inject(SecurityUserAdministrationService);
  private readonly router = inject(Router);

  protected readonly errorService = inject(ErrorService);
  protected readonly submitting = signal(false);

  protected readonly availableRoles = SIXPAY_SECURITY_ROLES;
  protected readonly availablePermissions = SIXPAY_SECURITY_PERMISSIONS;

  protected readonly form = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.email, Validators.maxLength(320)]],
    roles: this.formBuilder.nonNullable.control<string[]>([]),
    permissions: this.formBuilder.nonNullable.control<string[]>([]),
    localAuthenticationEnabled: [true],
    initialPassword: [
      '',
      [
        Validators.minLength(12),
        Validators.maxLength(200),
      ],
    ],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    if (
      value.localAuthenticationEnabled &&
      value.initialPassword.trim().length < 12
    ) {
      this.form.controls.initialPassword.setErrors({
        minlength: true,
      });

      this.form.controls.initialPassword.markAsTouched();
      return;
    }

    this.errorService.clear();
    this.submitting.set(true);

    this.service
      .createUser({
        username: value.username.trim(),
        email: value.email.trim() || null,
        roles: value.roles,
        permissions: value.permissions,
        localAuthenticationEnabled:
          value.localAuthenticationEnabled,
        initialPassword:
          value.localAuthenticationEnabled
            ? value.initialPassword
            : null,
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe((user) => {
        void this.router.navigate(
          ['/administration/users', user.id],
          {
            queryParams: {
              created: true,
            },
          },
        );
      });
  }

  protected fieldError(
    name: keyof typeof this.form.controls,
  ): string | undefined {
    const backendError =
      this.errorService.currentError()?.fieldErrors[name];

    if (backendError) {
      return backendError;
    }

    const control =
      this.form.controls[name];

    if (!control.touched || !control.errors) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'Ce champ est obligatoire.';
    }

    if (control.hasError('email')) {
      return 'Saisissez une adresse courriel valide.';
    }

    if (control.hasError('minlength')) {
      return 'Le mot de passe doit contenir au moins 12 caractères.';
    }

    if (control.hasError('maxlength')) {
      return 'La valeur saisie dépasse la longueur autorisée.';
    }

    return 'Valeur invalide.';
  }
}
