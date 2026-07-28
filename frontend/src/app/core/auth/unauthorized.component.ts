import { Component } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'sp-unauthorized',
  imports: [MatCardModule],
  template: `
    <main class="sp-unauthorized">
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Accès non autorisé</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p>Une authentification valide est nécessaire pour accéder à cette ressource.</p>
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
export class UnauthorizedComponent {}
