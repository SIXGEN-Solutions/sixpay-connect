import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { catchError, EMPTY, finalize, Observable } from 'rxjs';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import {
  SpDialogComponent,
  SpDialogData,
} from '../../../shared/components/dialog/sp-dialog.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { Partner } from '../models/partners';
import { PartnersService } from '../services/partners.service';
import { PartnerAccessPolicy, PartnerLifecycleAction } from '../security/partner-access.policy';

type PartnerAction = PartnerLifecycleAction;

@Component({
  selector: 'sp-partner-reason-dialog',
  imports: [
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpFormErrorComponent,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <p>{{ data.message }}</p>
      <form [formGroup]="form" id="partner-reason-form" (ngSubmit)="confirm()">
        <mat-form-field appearance="outline">
          <mat-label>Motif</mat-label>
          <textarea matInput formControlName="reason" maxlength="500" rows="4"></textarea>
          <sp-form-error
            errorId="partner-reason-error"
            [message]="
              form.controls.reason.touched && form.controls.reason.invalid
                ? 'Le motif est obligatoire.'
                : undefined
            "
          />
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <sp-button variant="secondary" (buttonClick)="dialogRef.close()">Annuler</sp-button>
      <sp-button (buttonClick)="confirm()">Confirmer</sp-button>
    </mat-dialog-actions>
  `,
})
export class PartnerReasonDialogComponent {
  readonly data = inject<{ title: string; message: string }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<PartnerReasonDialogComponent, string>);
  private readonly formBuilder = inject(FormBuilder);
  readonly form = this.formBuilder.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  confirm(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.dialogRef.close(this.form.controls.reason.value.trim());
  }
}

@Component({
  selector: 'sp-partner-lifecycle-actions',
  imports: [SpButtonComponent],
  template: `
    <div class="sp-partner-actions" aria-label="Actions sur le cycle de vie">
      @if (can('approve')) {
        <sp-button icon="check" [disabled]="busy()" (buttonClick)="run('approve')">
          Approuver
        </sp-button>
      }
      @if (can('reject')) {
        <sp-button variant="danger" icon="close" [disabled]="busy()" (buttonClick)="run('reject')">
          Rejeter
        </sp-button>
      }
      @if (can('suspend')) {
        <sp-button variant="danger" icon="pause" [disabled]="busy()" (buttonClick)="run('suspend')">
          Suspendre
        </sp-button>
      }
      @if (can('reactivate')) {
        <sp-button icon="play_arrow" [disabled]="busy()" (buttonClick)="run('reactivate')">
          Réactiver
        </sp-button>
      }
    </div>
  `,
  styles: `
    .sp-partner-actions {
      display: flex;
      flex-wrap: wrap;
      gap: var(--sp-space-2);
    }
  `,
})
export class PartnerLifecycleActionsComponent {
  readonly partner = input.required<Partner>();
  readonly changed = output<Partner>();
  protected readonly busy = signal(false);
  private readonly partners = inject(PartnersService);
  private readonly dialog = inject(MatDialog);
  private readonly accessPolicy = inject(PartnerAccessPolicy);

  protected can(action: PartnerAction): boolean {
    return this.accessPolicy.canPerformLifecycleAction(action, this.partner().status);
  }

  protected run(action: PartnerAction): void {
    if (this.busy() || !this.can(action)) {
      return;
    }

    const partner = this.partner();
    if (action === 'reject' || action === 'suspend') {
      this.dialog
        .open(PartnerReasonDialogComponent, {
          data: {
            title: action === 'reject' ? 'Rejeter le partenaire' : 'Suspendre le partenaire',
            message: 'Cette opération doit être justifiée.',
          },
        })
        .afterClosed()
        .subscribe((reason) => {
          if (reason) {
            this.execute(action, reason);
          }
        });
      return;
    }

    this.dialog
      .open<SpDialogComponent, SpDialogData, boolean>(SpDialogComponent, {
        data: {
          title: action === 'approve' ? 'Approuver le partenaire' : 'Réactiver le partenaire',
          message: `Confirmez-vous l’action sur ${partner.legalName} ?`,
          confirmLabel: 'Confirmer',
        },
      })
      .afterClosed()
      .subscribe((confirmed) => {
        if (confirmed) {
          this.execute(action, null);
        }
      });
  }

  private execute(action: PartnerAction, reason: string | null): void {
    const partner = this.partner();
    this.busy.set(true);
    let operation: Observable<Partner>;
    if (action === 'approve') {
      operation = this.partners.decide(partner.id, { decision: 'APPROVE', reason: null });
    } else if (action === 'reject') {
      operation = this.partners.decide(partner.id, { decision: 'REJECT', reason });
    } else if (action === 'suspend') {
      operation = this.partners.suspend(partner.id, { reason: reason ?? '' });
    } else {
      operation = this.partners.reactivate(partner.id);
    }

    operation
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.busy.set(false)),
      )
      .subscribe((updated) => this.changed.emit(updated));
  }
}
