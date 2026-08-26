import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  ActivatedRoute,
  Router,
  RouterLink,
} from '@angular/router';
import {
  catchError,
  EMPTY,
  finalize,
} from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  SIXPAY_SECURITY_PERMISSIONS,
  SIXPAY_SECURITY_ROLES,
} from '../models/security-authorization-catalog';
import { SecurityUserDetail } from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

@Component({
  selector: 'sp-security-user-detail-page',
  imports: [
    DatePipe,
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
  templateUrl:
    './security-user-detail-page.component.html',
  styleUrl:
    './security-user-form.scss',
})
export class SecurityUserDetailPageComponent {
  private readonly route =
    inject(ActivatedRoute);

  private readonly router =
    inject(Router);

  private readonly formBuilder =
    inject(FormBuilder);

  private readonly service =
    inject(SecurityUserAdministrationService);

  private readonly userId =
    this.route.snapshot.paramMap.get('userId')!;

  protected readonly errorService =
    inject(ErrorService);

  protected readonly user =
    signal<SecurityUserDetail | null>(null);

  protected readonly saving =
    signal(false);

  protected readonly deleting =
    signal(false);

  protected readonly availableRoles =
    SIXPAY_SECURITY_ROLES;

  protected readonly availablePermissions =
    SIXPAY_SECURITY_PERMISSIONS;

  protected readonly accountForm =
    this.formBuilder.nonNullable.group({
      username: [
        '',
        [
          Validators.required,
          Validators.maxLength(150),
        ],
      ],
      email: [
        '',
        [
          Validators.email,
          Validators.maxLength(320),
        ],
      ],
      roles:
        this.formBuilder.nonNullable.control<
          string[]
        >([]),
      permissions:
        this.formBuilder.nonNullable.control<
          string[]
        >([]),
    });

  protected readonly passwordForm =
    new FormGroup({
      newPassword:
        new FormControl('', {
          nonNullable: true,
          validators: [
            Validators.required,
            Validators.minLength(12),
            Validators.maxLength(200),
          ],
        }),
    });

  protected readonly oidcForm =
    new FormGroup({
      provider:
        new FormControl('', {
          nonNullable: true,
          validators: [
            Validators.required,
          ],
        }),
      providerSubject:
        new FormControl('', {
          nonNullable: true,
          validators: [
            Validators.required,
          ],
        }),
    });

  constructor() {
    this.reload();
  }

  protected saveAccount(): void {
    if (
      this.accountForm.invalid
      || this.saving()
    ) {
      this.accountForm.markAllAsTouched();
      return;
    }

    const value =
      this.accountForm.getRawValue();

    this.errorService.clear();
    this.saving.set(true);

    this.service
      .updateUser(
        this.userId,
        {
          username:
            value.username.trim(),
          email:
            value.email.trim() || null,
          roles:
            value.roles,
          permissions:
            value.permissions,
        },
      )
      .pipe(
        catchError(() => EMPTY),
        finalize(
          () => this.saving.set(false),
        ),
      )
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  protected enableUser(): void {
    this.service
      .enableUser(this.userId)
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  protected toggleLocal(): void {
    const current =
      this.user();

    if (!current) {
      return;
    }

    this.service
      .setLocalEnabled(
        this.userId,
        !current.localEnabled,
      )
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  protected resetPassword(): void {
    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const { newPassword } =
      this.passwordForm.getRawValue();

    this.service
      .resetLocalPassword(
        this.userId,
        newPassword,
      )
      .subscribe((user) => {
        this.passwordForm.reset();
        this.applyUser(user);
      });
  }

  protected linkOidc(): void {
    if (this.oidcForm.invalid) {
      this.oidcForm.markAllAsTouched();
      return;
    }

    const value =
      this.oidcForm.getRawValue();

    this.service
      .linkOidcIdentity(
        this.userId,
        value.provider.trim(),
        value.providerSubject.trim(),
      )
      .subscribe((user) => {
        this.oidcForm.reset();
        this.applyUser(user);
      });
  }

  protected unlinkIdentity(
    identityId: string,
  ): void {
    this.service
      .unlinkIdentity(
        this.userId,
        identityId,
      )
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  protected disableUser(): void {
    this.service
      .disableUser(this.userId)
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  protected deleteUser(): void {
    const current =
      this.user();

    if (
      !current
      || this.deleting()
    ) {
      return;
    }

    if (
      !window.confirm(
        `Supprimer définitivement l’utilisateur "${current.username}" ?`,
      )
    ) {
      return;
    }

    this.deleting.set(true);

    this.service
      .deleteUser(this.userId)
      .pipe(
        catchError(() => EMPTY),
        finalize(
          () => this.deleting.set(false),
        ),
      )
      .subscribe(() => {
        void this.router.navigate(
          ['/administration/users'],
        );
      });
  }

  protected accountFieldError(
    name: keyof typeof this.accountForm.controls,
  ): string | undefined {
    const backendError =
      this.errorService
        .currentError()
        ?.fieldErrors[name];

    if (backendError) {
      return backendError;
    }

    const control =
      this.accountForm.controls[name];

    if (
      !control.touched
      || !control.errors
    ) {
      return undefined;
    }

    if (
      control.hasError('required')
    ) {
      return 'Ce champ est obligatoire.';
    }

    if (
      control.hasError('email')
    ) {
      return 'Saisissez une adresse courriel valide.';
    }

    return 'La valeur saisie dépasse la longueur autorisée.';
  }

  private reload(): void {
    this.service
      .getUser(this.userId)
      .subscribe(
        (user) =>
          this.applyUser(user),
      );
  }

  private applyUser(
    user: SecurityUserDetail,
  ): void {
    this.user.set(user);

    this.accountForm.reset({
      username: user.username,
      email: user.email ?? '',
      roles: [...user.roles],
      permissions: [
        ...user.permissions,
      ],
    });
  }
}
