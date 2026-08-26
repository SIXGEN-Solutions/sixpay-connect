import { Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { Router } from '@angular/router';

import { AuthenticationService } from '../../core/auth/authentication.service';
import { SIXPAY_ROLES, SixpayRole } from '../../core/auth/authentication.model';

@Component({
  selector: 'sp-role-simulator',
  imports: [MatButtonToggleModule],
  template: `
    @if (authentication.isStandaloneMode) {
      <section class="sp-role-simulator" aria-labelledby="role-simulator-title">
        <p id="role-simulator-title">Rôle simulé</p>
        <mat-button-toggle-group
          aria-label="Rôle SIXPAY simulé"
          [value]="activeRole()"
          (change)="changeRole($event.value)"
        >
          @for (role of roles; track role) {
            <mat-button-toggle [value]="role">{{ role }}</mat-button-toggle>
          }
        </mat-button-toggle-group>
      </section>
    }
  `,
  styles: `
    .sp-role-simulator {
      margin: var(--sp-space-3);
      padding-top: var(--sp-space-3);
      border-top: 1px solid var(--mat-sys-outline-variant);
    }

    .sp-role-simulator p {
      margin: 0 0 var(--sp-space-2);
      font-size: 0.75rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    mat-button-toggle-group {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  `,
})
export class RoleSimulatorComponent {
  protected readonly authentication = inject(AuthenticationService);
  private readonly router = inject(Router);

  protected readonly roles = SIXPAY_ROLES;

  protected activeRole(): SixpayRole | undefined {
    return this.roles.find((role) => this.authentication.hasRole(role));
  }

  protected changeRole(role: SixpayRole): void {
    this.authentication.simulateStandaloneRole(role);
    void this.router.navigateByUrl('/');
  }
}
