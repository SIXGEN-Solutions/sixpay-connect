import { HttpErrorResponse } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
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
import {
  DirectoryUserView,
  LdapProvisioningConflictReason,
} from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';

type CreationMode = 'LOCAL' | 'LDAP';

type LdapWorkflowState =
  | 'idle'
  | 'loading'
  | 'found'
  | 'not-found'
  | 'ambiguous'
  | 'inactive'
  | 'already-provisioned'
  | 'username-conflict'
  | 'directory-unavailable'
  | 'forbidden'
  | 'error';

interface ProvisioningProblemDetail {
  readonly reason?: LdapProvisioningConflictReason;
}

@Component({
  selector: 'sp-security-user-create-page',
  imports: [
    MatButtonToggleModule,
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
  protected readonly mode = signal<CreationMode>('LOCAL');
  protected readonly directoryUser = signal<DirectoryUserView | null>(null);
  protected readonly ldapState = signal<LdapWorkflowState>('idle');
  protected readonly ldapMessage = signal<string | null>(null);

  protected readonly availableRoles = SIXPAY_SECURITY_ROLES;
  protected readonly availablePermissions = SIXPAY_SECURITY_PERMISSIONS;

  protected readonly localForm = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.email, Validators.maxLength(320)]],
    roles: this.formBuilder.nonNullable.control<string[]>([]),
    permissions: this.formBuilder.nonNullable.control<string[]>([]),
    localAuthenticationEnabled: [true],
    initialPassword: ['', [Validators.minLength(12), Validators.maxLength(200)]],
  });

  protected readonly ldapForm = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required, Validators.maxLength(150)]],
    roles: this.formBuilder.nonNullable.control<string[]>([]),
    permissions: this.formBuilder.nonNullable.control<string[]>([]),
  });

  protected readonly canProvisionLdap = computed(
    () =>
      this.directoryUser()?.accountStatus === 'ACTIVE' &&
      this.ldapState() === 'found' &&
      !this.submitting(),
  );

  protected selectMode(mode: CreationMode): void {
    if (this.mode() === mode) {
      return;
    }

    this.errorService.clear();
    this.mode.set(mode);
    this.resetLdapLookup();
  }

  protected submitLocal(): void {
    if (this.localForm.invalid || this.submitting()) {
      this.localForm.markAllAsTouched();
      return;
    }

    const value = this.localForm.getRawValue();

    if (value.localAuthenticationEnabled && value.initialPassword.trim().length < 12) {
      this.localForm.controls.initialPassword.setErrors({ minlength: true });
      this.localForm.controls.initialPassword.markAsTouched();
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
        localAuthenticationEnabled: value.localAuthenticationEnabled,
        initialPassword: value.localAuthenticationEnabled ? value.initialPassword : null,
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe((user) => this.navigateToUser(user.id));
  }

  protected lookupDirectoryUser(): void {
    const control = this.ldapForm.controls.username;

    if (control.invalid || this.submitting()) {
      control.markAsTouched();
      return;
    }

    const username = control.value.trim();

    if (!username) {
      control.setErrors({ required: true });
      control.markAsTouched();
      return;
    }

    this.errorService.clear();
    this.directoryUser.set(null);
    this.ldapMessage.set(null);
    this.ldapState.set('loading');

    this.service
      .lookupDirectoryUser(username)
      .pipe(
        catchError((error: unknown) => {
          this.applyLdapError(error, 'lookup');
          return EMPTY;
        }),
      )
      .subscribe((directoryUser) => {
        this.directoryUser.set(directoryUser);

        if (directoryUser.accountStatus === 'ACTIVE') {
          this.ldapState.set('found');
          this.ldapMessage.set(null);
          return;
        }

        this.ldapState.set('inactive');
        this.ldapMessage.set(
          `Le compte Active Directory est ${directoryUser.accountStatus} et ne peut pas être provisionné.`,
        );
      });
  }

  protected provisionLdapUser(): void {
    const directoryUser = this.directoryUser();

    if (!directoryUser || !this.canProvisionLdap() || this.ldapForm.invalid) {
      this.ldapForm.markAllAsTouched();
      return;
    }

    const value = this.ldapForm.getRawValue();

    this.errorService.clear();
    this.submitting.set(true);
    this.ldapMessage.set(null);

    this.service
      .provisionLdapUser(directoryUser.username, {
        roles: value.roles,
        permissions: value.permissions,
      })
      .pipe(
        catchError((error: unknown) => {
          this.applyLdapError(error, 'provision');
          return EMPTY;
        }),
        finalize(() => this.submitting.set(false)),
      )
      .subscribe((user) => this.navigateToUser(user.id));
  }

  protected localFieldError(name: keyof typeof this.localForm.controls): string | undefined {
    const backendError = this.errorService.currentError()?.fieldErrors[name];

    if (backendError) {
      return backendError;
    }

    const control = this.localForm.controls[name];

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

  protected ldapUsernameError(): string | undefined {
    const control = this.ldapForm.controls.username;

    if (!control.touched || !control.errors) {
      return undefined;
    }

    if (control.hasError('required')) {
      return 'L’identifiant Active Directory est obligatoire.';
    }

    if (control.hasError('maxlength')) {
      return 'L’identifiant ne peut pas dépasser 150 caractères.';
    }

    return 'Identifiant invalide.';
  }

  private resetLdapLookup(): void {
    this.directoryUser.set(null);
    this.ldapState.set('idle');
    this.ldapMessage.set(null);
  }

  private applyLdapError(error: unknown, phase: 'lookup' | 'provision'): void {
    if (!(error instanceof HttpErrorResponse)) {
      this.ldapState.set('error');
      this.ldapMessage.set('Une erreur inattendue est survenue.');
      return;
    }

    if (error.status === 403) {
      this.ldapState.set('forbidden');
      this.ldapMessage.set('Vous n’êtes pas autorisé à administrer les utilisateurs SIXPAY.');
      return;
    }

    if (error.status === 404) {
      this.ldapState.set('not-found');
      this.ldapMessage.set('Aucun utilisateur Active Directory ne correspond à cet identifiant.');
      return;
    }

    if (error.status === 503) {
      this.ldapState.set('directory-unavailable');
      this.ldapMessage.set('Le service Active Directory est temporairement indisponible.');
      return;
    }

    if (error.status === 422) {
      this.ldapState.set('inactive');
      this.ldapMessage.set('Ce compte Active Directory ne peut pas être provisionné.');
      return;
    }

    if (error.status === 409) {
      const reason = this.conflictReason(error);

      if (phase === 'lookup' && reason === null) {
        this.ldapState.set('ambiguous');
        this.ldapMessage.set(
          'La recherche Active Directory retourne plusieurs identités pour cet identifiant.',
        );
        return;
      }

      if (reason === 'ALREADY_PROVISIONED') {
        this.ldapState.set('already-provisioned');
        this.ldapMessage.set('Cette identité LDAP est déjà provisionnée dans SIXPAY.');
        return;
      }

      if (reason === 'USERNAME_CONFLICT') {
        this.ldapState.set('username-conflict');
        this.ldapMessage.set(
          'Un utilisateur SIXPAY utilise déjà ce nom. Aucun rattachement automatique ne sera effectué.',
        );
        return;
      }
    }

    this.ldapState.set('error');
    this.ldapMessage.set(
      phase === 'lookup'
        ? 'La recherche Active Directory n’a pas pu être effectuée.'
        : 'Le provisioning LDAP n’a pas pu être effectué.',
    );
  }

  private conflictReason(error: HttpErrorResponse): LdapProvisioningConflictReason | null {
    const candidate = error.error as ProvisioningProblemDetail | null;

    return candidate?.reason === 'ALREADY_PROVISIONED' || candidate?.reason === 'USERNAME_CONFLICT'
      ? candidate.reason
      : null;
  }

  private navigateToUser(userId: string): void {
    void this.router.navigate(['/administration/users', userId], {
      queryParams: { created: true },
    });
  }
}
