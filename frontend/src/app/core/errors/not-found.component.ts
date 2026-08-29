import { Location } from '@angular/common';
import { Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../shared/components/button/sp-button.component';

@Component({
  selector: 'sp-not-found',
  imports: [MatCardModule, MatIconModule, RouterLink, SpButtonComponent],
  template: `
    <main class="sp-not-found">
      <mat-card appearance="outlined">
        <mat-card-content>
          <mat-icon aria-hidden="true">search_off</mat-icon>

          <p class="sp-not-found__code">404</p>

          <h1>Page introuvable</h1>

          <p>La page demandée n’existe pas ou son adresse a changé.</p>

          <div class="sp-not-found__actions">
            <sp-button type="button" icon="arrow_back" variant="secondary" (buttonClick)="goBack()">
              Page précédente
            </sp-button>

            <a routerLink="/"> Retour au tableau de bord </a>
          </div>
        </mat-card-content>
      </mat-card>
    </main>
  `,
  styles: `
    .sp-not-found {
      display: grid;
      place-items: center;
      min-height: 70vh;
      padding: var(--sp-space-4);
    }

    mat-card {
      width: min(100%, 42rem);
    }

    mat-card-content {
      display: grid;
      justify-items: center;
      gap: var(--sp-space-3);
      padding: var(--sp-space-6);
      text-align: center;
    }

    mat-icon {
      width: 3rem;
      height: 3rem;
      color: var(--mat-sys-on-surface-variant);
      font-size: 3rem;
    }

    .sp-not-found__code,
    h1,
    p {
      margin: 0;
    }

    .sp-not-found__code {
      font-size: 3rem;
      font-weight: 800;
      line-height: 1;
    }

    .sp-not-found__actions {
      display: flex;
      align-items: center;
      justify-content: center;
      flex-wrap: wrap;
      gap: var(--sp-space-3);
      margin-top: var(--sp-space-2);
    }
  `,
})
export class NotFoundComponent {
  private readonly location = inject(Location);

  protected goBack(): void {
    this.location.back();
  }
}
