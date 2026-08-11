import { HttpErrorResponse } from '@angular/common/http';
import { Component, effect, inject, signal } from '@angular/core';
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs';

import { SpButtonComponent } from '../../shared/components/button/sp-button.component';
import { AuthenticationService } from './authentication.service';

@Component({
  selector: 'sp-login',
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    SpButtonComponent,
  ],
  template: `
    <main class="sp-auth-page">
      <mat-card appearance="outlined" class="sp-auth-card">
        <mat-card-header>
          <mat-card-title>SIXPAY CONNECT</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          @if (sessionExpired) {
            <p class="sp-auth-message" role="alert">
              Votre session a expiré. Veuillez vous reconnecter.
            </p>
          }

          @if (authentication.localEnabled) {
            <form
              class="sp-local-login-form"
              [formGroup]="form"
              (ngSubmit)="loginLocal()"
            >
              <mat-form-field appearance="outline">
                <mat-label>Email / Nom d’utilisateur</mat-label>
                <input
                  matInput
                  type="text"
                  autocomplete="username"
                  formControlName="username"
                />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Mot de passe</mat-label>
                <input
                  matInput
                  type="password"
                  autocomplete="current-password"
                  formControlName="password"
                />
              </mat-form-field>

              <p class="sp-forgot-password-hint">
                Mot de passe oublié ? Contactez un administrateur SIXPAY.
              </p>

              @if (invalidCredentials()) {
                <p class="sp-auth-error" role="alert">
                  Nom d’utilisateur ou mot de passe incorrect.
                </p>
              }

              <sp-button
                icon="login"
                type="submit"
                [disabled]="form.invalid || submitting()"
              >
                {{ submitting() ? 'Connexion…' : 'Se connecter' }}
              </sp-button>
            </form>
          }

          @if (authentication.localEnabled && authentication.oidcEnabled) {
            <div class="sp-auth-divider" aria-hidden="true">
              <span></span>
              <strong>OU</strong>
              <span></span>
            </div>
          }

          @if (authentication.oidcEnabled) {
            <section class="sp-sso-login">
              <sp-button
                icon="login"
                variant="secondary"
                (buttonClick)="loginOidc()"
              >
                Se connecter avec SSO
              </sp-button>
            </section>
          }

          @if (
            !authentication.localEnabled &&
            !authentication.oidcEnabled &&
            authentication.isStandaloneMode
          ) {
            <p>La session de démonstration SIXPAY est active.</p>
          }
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    .sp-auth-page {
      max-width: 40rem;
      margin: var(--sp-space-6) auto;
      padding: var(--sp-space-4);
    }

    .sp-auth-card {
      width: 100%;
    }

    mat-card-content,
    .sp-local-login-form,
    .sp-sso-login {
      display: grid;
      gap: var(--sp-space-4);
      padding-top: var(--sp-space-4);
    }

    .sp-local-login-form mat-form-field {
      width: 100%;
    }

    .sp-auth-message,
    .sp-auth-error,
    .sp-forgot-password-hint {
      margin: 0;
    }

    .sp-auth-error {
      color: var(--mat-sys-error);
      font-weight: 600;
    }

    .sp-forgot-password-hint {
      text-align: right;
      font-size: 0.875rem;
    }

    .sp-auth-divider {
      display: grid;
      grid-template-columns: 1fr auto 1fr;
      align-items: center;
      gap: var(--sp-space-3);
      margin-top: var(--sp-space-4);
    }

    .sp-auth-divider span {
      height: 1px;
      background: var(--mat-sys-outline-variant);
    }

    .sp-auth-divider strong {
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class LoginComponent {
  protected readonly authentication = inject(AuthenticationService);
  private readonly route = inject(ActivatedRoute);

  protected readonly submitting = signal(false);
  protected readonly invalidCredentials = signal(false);

  protected readonly sessionExpired =
    this.route.snapshot.queryParamMap.get('sessionExpired') === 'true';

  private readonly returnUrl =
    this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';

  protected readonly form = new FormGroup({
    username: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    password: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  constructor() {
    effect(() => {
      if (this.authentication.isAuthenticated()) {
        this.authentication.completeLoginNavigation();
      }
    });
  }

  protected loginLocal(): void {
    if (!this.authentication.localEnabled || this.form.invalid) {
      return;
    }

    this.invalidCredentials.set(false);
    this.submitting.set(true);

    this.authentication
      .loginLocal(this.form.getRawValue(), this.returnUrl)
      .pipe(finalize(() => this.submitting.set(false)))
      .subscribe({
        error: (error: unknown) => {
          if (
            error instanceof HttpErrorResponse &&
            error.status === 401
          ) {
            this.invalidCredentials.set(true);
          }
        },
      });
  }

  protected loginOidc(): void {
    if (!this.authentication.oidcEnabled) {
      return;
    }

    this.authentication.loginOidc(this.returnUrl);
  }
}
