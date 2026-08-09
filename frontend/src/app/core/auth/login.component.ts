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
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Connexion à SIXPAY CONNECT</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          @if (sessionExpired) {
            <p class="sp-auth-message" role="alert">
              Votre session a expiré. Veuillez vous reconnecter.
            </p>
          }

          @if (authentication.isLocalMode) {
            <p>
              Utilisez votre compte SIXPAY local pour cet environnement
              d’intégration.
            </p>

            <form
              class="sp-local-login-form"
              [formGroup]="form"
              (ngSubmit)="loginLocal()"
            >
              <mat-form-field appearance="outline">
                <mat-label>Nom d’utilisateur</mat-label>
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
          } @else if (authentication.isOidcMode) {
            <p>Authentifiez-vous auprès du fournisseur d’identité SIXPAY.</p>
            <sp-button icon="login" (buttonClick)="loginOidc()">
              Se connecter
            </sp-button>
          } @else {
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

    mat-card-content,
    .sp-local-login-form {
      display: grid;
      gap: var(--sp-space-4);
      padding-top: var(--sp-space-4);
    }

    .sp-local-login-form mat-form-field {
      width: 100%;
    }

    .sp-auth-message,
    .sp-auth-error {
      margin: 0;
    }

    .sp-auth-error {
      color: var(--mat-sys-error);
      font-weight: 600;
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
    if (!this.authentication.isLocalMode || this.form.invalid) {
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
    this.authentication.login(this.returnUrl);
  }
}
