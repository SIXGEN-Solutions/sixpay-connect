import { Component } from '@angular/core';
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
