import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PartnerAccessPolicy } from '../security/partner-access.policy';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

@Component({
  selector: 'sp-partner-access-page',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpFormErrorComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar
      title="Partenaires"
      description="Accédez à une fiche Partner à partir de son identifiant."
    >
      @if (partnerAccess.canCreate()) {
        <a routerLink="/partners/create" class="sp-link-action">Créer un partenaire</a>
      }
    </sp-toolbar>

    <sp-card title="Consulter une fiche">
      <form class="sp-access-form" [formGroup]="form" (ngSubmit)="open()" novalidate>
        <mat-form-field appearance="outline">
          <mat-label>Identifiant Partner</mat-label>
          <input matInput formControlName="partnerId" autocomplete="off" />
          <sp-form-error
            errorId="partner-id-error"
            [message]="
              form.controls.partnerId.touched && form.controls.partnerId.invalid
                ? 'Saisissez un identifiant UUID valide.'
                : undefined
            "
          />
        </mat-form-field>
        <sp-button type="submit" icon="search">Consulter</sp-button>
      </form>
    </sp-card>
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--sp-space-4);
    }

    .sp-access-form {
      display: flex;
      align-items: center;
      gap: var(--sp-space-3);
    }

    .sp-access-form mat-form-field {
      flex: 1;
    }

    .sp-link-action {
      color: var(--sp-color-primary);
      font-weight: 600;
    }

    @media (max-width: 768px) {
      .sp-access-form {
        align-items: stretch;
        flex-direction: column;
      }
    }
  `,
})
export class PartnerAccessPageComponent {
  protected readonly partnerAccess = inject(PartnerAccessPolicy);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);
  protected readonly form = this.formBuilder.nonNullable.group({
    partnerId: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
  });

  protected open(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    void this.router.navigate(['/partners', this.form.controls.partnerId.value]);
  }
}
