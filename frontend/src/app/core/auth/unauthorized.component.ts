/*import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'sp-forbidden',
  imports: [MatCardModule],
  template: `
    <main class="sp-unauthorized">
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Accès non autorisé</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Votre compte est authentifié, mais votre rôle ne permet pas cette opération.</p>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    .sp-unauthorized {
      max-width: 40rem;
      margin: var(--sp-space-6) auto;
      padding: var(--sp-space-4);
    }
  `,
})
export class ForbiddenComponent {}
*/

import { Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../shared/components/button/sp-button.component';

@Component({
  selector: 'sp-forbidden',
  imports: [
    MatCardModule,
    RouterLink,
    SpButtonComponent,
  ],
  template: `
    <main class="sp-unauthorized">
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Accès non autorisé</mat-card-title>
        </mat-card-header>

        <mat-card-content>
          <p>
            Votre compte est authentifié, mais votre rôle ne permet pas
            cette opération.
          </p>
        </mat-card-content>

        <mat-card-actions>
          <sp-button
            icon="arrow_back"
            (buttonClick)="goBack()"
          >
            Page précédente
          </sp-button>

          <a routerLink="/">
            Retour au tableau de bord
          </a>
        </mat-card-actions>
      </mat-card>
    </main>
  `,
  styles: `
    .sp-unauthorized {
      max-width: 40rem;
      margin: var(--sp-space-6) auto;
      padding: var(--sp-space-4);
    }

    mat-card-actions {
      display: flex;
      gap: var(--sp-space-3);
      align-items: center;
      flex-wrap: wrap;
    }
  `,
})
export class ForbiddenComponent {
  private readonly location = inject(Location);

  protected goBack(): void {
    this.location.back();
  }
}