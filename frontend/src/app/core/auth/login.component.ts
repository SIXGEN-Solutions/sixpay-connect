import { Component, effect, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { ActivatedRoute } from '@angular/router';

import { SpButtonComponent } from '../../shared/components/button/sp-button.component';
import { AuthenticationService } from './authentication.service';

@Component({
  selector: 'sp-login',
  imports: [MatCardModule, SpButtonComponent],
  template: `
    <main class="sp-auth-page">
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Connexion à SIXPAY CONNECT</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          @if (sessionExpired) {
            <p role="alert">Votre session a expiré. Veuillez vous reconnecter.</p>
          } @else {
            <p>Authentifiez-vous auprès du fournisseur d’identité SIXPAY.</p>
          }
          <sp-button icon="login" (buttonClick)="login()">Se connecter</sp-button>
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
    mat-card-content {
      display: grid;
      gap: var(--sp-space-4);
      padding-top: var(--sp-space-4);
    }
  `,
})
export class LoginComponent {
  private readonly authentication = inject(AuthenticationService);
  private readonly route = inject(ActivatedRoute);
  protected readonly sessionExpired =
    this.route.snapshot.queryParamMap.get('sessionExpired') === 'true';
  private readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? '/';

  constructor() {
    effect(() => {
      if (this.authentication.isAuthenticated()) {
        this.authentication.completeLoginNavigation();
      }
    });
  }

  protected login(): void {
    this.authentication.login(this.returnUrl);
  }
}
