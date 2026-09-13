import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin, of } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import {
  SpDialogComponent,
  SpDialogData,
} from '../../../shared/components/dialog/sp-dialog.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpNotificationComponent } from '../../../shared/components/notification/sp-notification.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { CustomerMaster, CustomerSubscription } from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

type Feedback = {
  readonly title: string;
  readonly message: string;
};

interface CustomerReasonDialogData {
  readonly title: string;
  readonly confirmLabel: string;
  readonly destructive: boolean;
}

@Component({
  selector: 'sp-customer-reason-dialog',
  imports: [
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <mat-form-field appearance="outline" style="width: 100%">
          <mat-label>Motif</mat-label>
          <textarea matInput rows="4" formControlName="reason"></textarea>
          @if (form.controls.reason.hasError('required') && form.controls.reason.touched) {
            <mat-error>Le motif est obligatoire.</mat-error>
          }
          @if (form.controls.reason.hasError('maxlength')) {
            <mat-error>Le motif ne doit pas dépasser 500 caractères.</mat-error>
          }
        </mat-form-field>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <sp-button variant="secondary" (buttonClick)="close()"> Annuler </sp-button>
      <sp-button [variant]="data.destructive ? 'danger' : 'primary'" (buttonClick)="submit()">
        {{ data.confirmLabel }}
      </sp-button>
    </mat-dialog-actions>
  `,
})
class CustomerReasonDialogComponent {
  protected readonly data = inject<CustomerReasonDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<CustomerReasonDialogComponent, string | null>);
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.maxLength(500)]],
  });

  protected close(): void {
    this.dialogRef.close(null);
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const reason = this.form.getRawValue().reason.trim();
    if (!reason) {
      this.form.controls.reason.setErrors({ required: true });
      this.form.controls.reason.markAsTouched();
      return;
    }

    this.dialogRef.close(reason);
  }
}

@Component({
  selector: 'sp-customer-master-detail-page',
  imports: [
    DatePipe,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpNotificationComponent,
    SpToolbarComponent,
  ],
  template: `
    @if (loading()) {
      <sp-loading label="Chargement du Customer" />
    } @else if (customer(); as item) {
      <sp-toolbar
        [title]="item.legalName"
        description="Consultez et administrez le Customer, ses comptes et ses subscriptions."
      />

      @if (feedback(); as notice) {
        <sp-notification
          status="success"
          [title]="notice.title"
          [message]="notice.message"
          [dismissible]="true"
          (dismissed)="feedback.set(null)"
        />
      }

      @if (errorService.currentError(); as error) {
        <sp-notification
          status="error"
          [title]="error.title"
          [message]="error.detail"
          [dismissible]="true"
          (dismissed)="errorService.clear()"
        />
      }

      <sp-card title="Identité">
        <div class="customer-summary">
          <div>
            <span class="customer-summary__label">Statut</span>
            <strong>{{ item.status }}</strong>
          </div>
          <div>
            <span class="customer-summary__label">NIU</span>
            <span>{{ item.niu || '—' }}</span>
          </div>
          <div>
            <span class="customer-summary__label">Référence bancaire</span>
            <span>{{ item.bankingCustomerReference }}</span>
          </div>
        </div>

        @if (canUpdate()) {
          <form
            class="customer-form"
            [formGroup]="profileForm"
            (ngSubmit)="saveProfile()"
            novalidate
          >
            <mat-form-field appearance="outline">
              <mat-label>Nom légal</mat-label>
              <input matInput formControlName="legalName" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Courriel</mat-label>
              <input matInput type="email" formControlName="email" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Téléphone</mat-label>
              <input matInput formControlName="phoneNumber" />
            </mat-form-field>

            <div class="customer-form__actions">
              <sp-button type="submit" icon="save" [disabled]="mutating()"> Enregistrer </sp-button>
            </div>
          </form>
        }

        <div class="customer-section-actions">
          @if (item.status === 'ACTIVE' && canSuspend()) {
            <sp-button
              type="button"
              variant="danger"
              icon="pause_circle"
              [disabled]="mutating()"
              (buttonClick)="openCustomerSuspension()"
            >
              Suspendre
            </sp-button>
          }

          @if (item.status === 'SUSPENDED' && canUpdate()) {
            <sp-button
              type="button"
              icon="play_circle"
              [disabled]="mutating()"
              (buttonClick)="reactivate()"
            >
              Réactiver
            </sp-button>
          }
        </div>
      </sp-card>

      <sp-card title="Comptes">
        <div class="customer-rows">
          @for (account of item.bankAccounts; track account.id) {
            <div class="customer-row">
              <span>{{ account.maskedAccountIdentifier }}</span>
              <span>{{ account.currency }}</span>
              <span>{{ account.accountType || '—' }}</span>
              <span>{{ account.defaultAccount ? 'Défaut' : '' }}</span>

              @if (!account.defaultAccount && canUpdate()) {
                <div class="customer-row__actions">
                  <sp-button
                    type="button"
                    variant="secondary"
                    icon="star"
                    [disabled]="mutating()"
                    (buttonClick)="makeDefault(account.id)"
                  >
                    Définir par défaut
                  </sp-button>

                  <sp-button
                    type="button"
                    variant="danger"
                    icon="delete"
                    [disabled]="mutating()"
                    (buttonClick)="confirmRemoveAccount(account.id)"
                  >
                    Retirer
                  </sp-button>
                </div>
              }
            </div>
          }
        </div>

        @if (canUpdate()) {
          <form
            class="customer-form customer-form--compact"
            [formGroup]="accountForm"
            (ngSubmit)="addAccount()"
            novalidate
          >
            <mat-form-field appearance="outline">
              <mat-label>Référence du nouveau compte</mat-label>
              <input matInput formControlName="accountReference" />
            </mat-form-field>

            <div class="customer-form__actions">
              <sp-button type="submit" icon="add_card" [disabled]="mutating()">
                Vérifier et ajouter
              </sp-button>
            </div>
          </form>
        }
      </sp-card>

      @if (canSubscriptionRead()) {
        <sp-card title="Subscriptions">
          <div class="customer-rows">
            @for (subscription of subscriptions(); track subscription.id) {
              <div class="customer-row">
                <span>{{ subscription.partnerId }}</span>
                <span>{{ subscription.status }}</span>
                <span>{{ subscription.updatedAt | date: 'short' }}</span>

                <div class="customer-row__actions">
                  @if (subscription.status === 'PENDING_ACTIVATION' && canSubscriptionUpdate()) {
                    <sp-button
                      type="button"
                      icon="check_circle"
                      [disabled]="mutating()"
                      (buttonClick)="activate(subscription.id)"
                    >
                      Activer
                    </sp-button>
                  }

                  @if (subscription.status === 'ACTIVE' && canSubscriptionSuspend()) {
                    <sp-button
                      type="button"
                      variant="danger"
                      icon="pause_circle"
                      [disabled]="mutating()"
                      (buttonClick)="openSubscriptionSuspension(subscription.id)"
                    >
                      Suspendre
                    </sp-button>
                  }

                  @if (subscription.status !== 'CLOSED' && canSubscriptionClose()) {
                    <sp-button
                      type="button"
                      variant="danger"
                      icon="cancel"
                      [disabled]="mutating()"
                      (buttonClick)="openSubscriptionClose(subscription.id)"
                    >
                      Fermer
                    </sp-button>
                  }
                </div>
              </div>
            }
          </div>

          @if (canSubscriptionCreate()) {
            <form
              class="customer-form"
              [formGroup]="subscriptionForm"
              (ngSubmit)="createSubscription()"
              novalidate
            >
              <mat-form-field appearance="outline">
                <mat-label>Partner ID</mat-label>
                <input matInput formControlName="partnerId" />
              </mat-form-field>

              <mat-form-field appearance="outline">
                <mat-label>Bank account ID</mat-label>
                <input matInput formControlName="bankAccountId" />
              </mat-form-field>

              <div class="customer-form__actions">
                <sp-button type="submit" icon="add" [disabled]="mutating()"> Créer </sp-button>
              </div>
            </form>
          }
        </sp-card>
      }
    }
  `,
  styles: [
    `
      sp-notification {
        display: block;
        margin: 1rem 0;
      }

      .customer-summary {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1rem;
        margin-bottom: 1.5rem;
      }

      .customer-summary > div {
        display: grid;
        gap: 0.25rem;
      }

      .customer-summary__label {
        font-size: 0.875rem;
        opacity: 0.75;
      }

      .customer-form {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 1rem;
        align-items: start;
        margin-top: 1rem;
      }

      .customer-form--compact {
        grid-template-columns: minmax(0, 2fr) auto;
      }

      .customer-form__actions {
        grid-column: 1 / -1;
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
      }

      .customer-section-actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.75rem;
        margin-top: 1rem;
      }

      .customer-rows {
        display: grid;
        gap: 0.75rem;
      }

      .customer-row {
        display: flex;
        flex-wrap: wrap;
        gap: 1rem;
        align-items: center;
        padding: 0.75rem 0;
        border-bottom: 1px solid var(--mat-sys-outline-variant, #ddd);
      }

      .customer-row:last-child {
        border-bottom: 0;
      }

      .customer-row__actions {
        display: flex;
        flex-wrap: wrap;
        gap: 0.5rem;
        margin-left: auto;
      }

      @media (max-width: 900px) {
        .customer-summary,
        .customer-form,
        .customer-form--compact {
          grid-template-columns: 1fr;
        }

        .customer-row__actions {
          width: 100%;
          margin-left: 0;
        }
      }
    `,
  ],
})
export class CustomerMasterDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);
  private readonly fb = inject(FormBuilder);
  private readonly dialog = inject(MatDialog);
  protected readonly errorService = inject(ErrorService);

  private readonly customerId = this.route.snapshot.paramMap.get('customerId') ?? '';

  protected readonly loading = signal(true);
  protected readonly mutating = signal(false);
  protected readonly customer = signal<CustomerMaster | null>(null);
  protected readonly subscriptions = signal<CustomerSubscription[]>([]);
  protected readonly feedback = signal<Feedback | null>(null);

  protected readonly profileForm = this.fb.nonNullable.group({
    legalName: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    phoneNumber: ['', Validators.maxLength(50)],
  });

  protected readonly accountForm = this.fb.nonNullable.group({
    accountReference: ['', [Validators.required, Validators.maxLength(100)]],
  });

  protected readonly subscriptionForm = this.fb.nonNullable.group({
    partnerId: ['', Validators.required],
    bankAccountId: ['', Validators.required],
  });

  protected readonly canUpdate = () =>
    this.auth.hasPermission('customer.update') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSuspend = () =>
    this.auth.hasPermission('customer.suspend') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionRead = () =>
    this.auth.hasPermission('subscription.read') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionCreate = () =>
    this.auth.hasPermission('subscription.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionUpdate = () =>
    this.auth.hasPermission('subscription.update') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionSuspend = () =>
    this.auth.hasPermission('subscription.suspend') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionClose = () =>
    this.auth.hasPermission('subscription.close') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  constructor() {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    this.errorService.clear();

    const subscriptions$ = this.canSubscriptionRead()
      ? this.service.subscriptions(this.customerId)
      : of<CustomerSubscription[]>([]);

    forkJoin({
      customer: this.service.get(this.customerId),
      subscriptions: subscriptions$,
    })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe(({ customer, subscriptions }) => {
        this.customer.set(customer);
        this.subscriptions.set(subscriptions);
        this.profileForm.patchValue({
          legalName: customer.legalName,
          email: customer.email ?? '',
          phoneNumber: customer.phoneNumber ?? '',
        });
        const defaultAccount = customer.bankAccounts.find((account) => account.defaultAccount);
        if (defaultAccount) {
          this.subscriptionForm.controls.bankAccountId.setValue(defaultAccount.id);
        }
      });
  }

  protected saveProfile(): void {
    if (!this.canUpdate() || this.profileForm.invalid || this.mutating()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    const value = this.profileForm.getRawValue();
    this.runMutation(
      this.service.update(this.customerId, {
        legalName: value.legalName.trim(),
        email: value.email.trim() || null,
        phoneNumber: value.phoneNumber.trim() || null,
      }),
      'Customer mis à jour',
      'Les informations du Customer ont été enregistrées.',
      (customer) => this.customer.set(customer),
    );
  }

  protected openCustomerSuspension(): void {
    if (!this.canSuspend() || this.customer()?.status !== 'ACTIVE' || this.mutating()) {
      return;
    }

    this.openReasonDialog({
      title: 'Suspendre le Customer',
      confirmLabel: 'Suspendre',
      destructive: true,
    }).subscribe((reason) => {
      if (!reason) return;

      this.runMutation(
        this.service.suspend(this.customerId, { reason }),
        'Customer suspendu',
        'Le Customer a été suspendu.',
        (customer) => this.customer.set(customer),
      );
    });
  }

  protected reactivate(): void {
    if (!this.canUpdate() || this.customer()?.status !== 'SUSPENDED' || this.mutating()) {
      return;
    }

    this.confirm({
      title: 'Réactiver le Customer',
      message: 'Confirmer la réactivation de ce Customer ?',
      confirmLabel: 'Réactiver',
    }).subscribe((confirmed) => {
      if (!confirmed) return;

      this.runMutation(
        this.service.reactivate(this.customerId),
        'Customer réactivé',
        'Le Customer est de nouveau actif.',
        (customer) => this.customer.set(customer),
      );
    });
  }

  protected addAccount(): void {
    if (!this.canUpdate() || this.accountForm.invalid || this.mutating()) {
      this.accountForm.markAllAsTouched();
      return;
    }

    const accountReference = this.accountForm.getRawValue().accountReference.trim();
    if (!accountReference) return;

    this.runMutation(
      this.service.addAccount(this.customerId, { accountReference }),
      'Compte ajouté',
      'Le compte bancaire vérifié a été rattaché au Customer.',
      (customer) => {
        this.customer.set(customer);
        this.accountForm.reset();
      },
    );
  }

  protected makeDefault(accountId: string): void {
    if (!this.canUpdate() || this.mutating()) return;

    this.runMutation(
      this.service.makeDefaultAccount(this.customerId, accountId),
      'Compte par défaut mis à jour',
      'Le compte sélectionné est désormais le compte par défaut.',
      (customer) => this.customer.set(customer),
    );
  }

  protected confirmRemoveAccount(accountId: string): void {
    if (!this.canUpdate() || this.mutating()) return;

    this.confirm({
      title: 'Retirer le compte',
      message: 'Confirmer le retrait de ce compte bancaire du Customer ?',
      confirmLabel: 'Retirer',
      destructive: true,
    }).subscribe((confirmed) => {
      if (!confirmed) return;

      this.runMutation(
        this.service.removeAccount(this.customerId, accountId),
        'Compte retiré',
        'Le compte bancaire a été retiré du Customer.',
        (customer) => this.customer.set(customer),
      );
    });
  }

  protected createSubscription(): void {
    if (!this.canSubscriptionCreate() || this.subscriptionForm.invalid || this.mutating()) {
      this.subscriptionForm.markAllAsTouched();
      return;
    }

    const value = this.subscriptionForm.getRawValue();

    this.runMutation(
      this.service.createSubscription({
        customerId: this.customerId,
        partnerId: value.partnerId.trim(),
        bankAccountId: value.bankAccountId.trim(),
      }),
      'Subscription créée',
      'La CustomerSubscription a été créée en attente d’activation.',
      () => this.reloadAfterMutation(),
    );
  }

  protected activate(subscriptionId: string): void {
    const subscription = this.findSubscription(subscriptionId);
    if (
      !this.canSubscriptionUpdate() ||
      subscription?.status !== 'PENDING_ACTIVATION' ||
      this.mutating()
    ) {
      return;
    }

    this.runMutation(
      this.service.activateSubscription(subscriptionId),
      'Subscription activée',
      'La CustomerSubscription est active.',
      () => this.reloadAfterMutation(),
    );
  }

  protected openSubscriptionSuspension(subscriptionId: string): void {
    const subscription = this.findSubscription(subscriptionId);
    if (!this.canSubscriptionSuspend() || subscription?.status !== 'ACTIVE' || this.mutating()) {
      return;
    }

    this.openReasonDialog({
      title: 'Suspendre la subscription',
      confirmLabel: 'Suspendre',
      destructive: true,
    }).subscribe((reason) => {
      if (!reason) return;

      this.runMutation(
        this.service.suspendSubscription(subscriptionId, { reason }),
        'Subscription suspendue',
        'La CustomerSubscription a été suspendue.',
        () => this.reloadAfterMutation(),
      );
    });
  }

  protected openSubscriptionClose(subscriptionId: string): void {
    const subscription = this.findSubscription(subscriptionId);
    if (
      !this.canSubscriptionClose() ||
      !subscription ||
      subscription.status === 'CLOSED' ||
      this.mutating()
    ) {
      return;
    }

    this.openReasonDialog({
      title: 'Fermer la subscription',
      confirmLabel: 'Fermer',
      destructive: true,
    }).subscribe((reason) => {
      if (!reason) return;

      this.runMutation(
        this.service.closeSubscription(subscriptionId, { reason }),
        'Subscription fermée',
        'La CustomerSubscription est fermée et son historique est conservé.',
        () => this.reloadAfterMutation(),
      );
    });
  }

  private findSubscription(subscriptionId: string): CustomerSubscription | undefined {
    return this.subscriptions().find((subscription) => subscription.id === subscriptionId);
  }

  private confirm(data: SpDialogData) {
    return this.dialog
      .open<SpDialogComponent, SpDialogData, boolean>(SpDialogComponent, { data })
      .afterClosed();
  }

  private openReasonDialog(config: {
    readonly title: string;
    readonly confirmLabel: string;
    readonly destructive: boolean;
  }) {
    return this.dialog
      .open<CustomerReasonDialogComponent, CustomerReasonDialogData, string | null>(
        CustomerReasonDialogComponent,
        {
          data: {
            title: config.title,
            confirmLabel: config.confirmLabel,
            destructive: config.destructive,
          },
        },
      )
      .afterClosed();
  }

  private runMutation<T>(
    operation: import('rxjs').Observable<T>,
    title: string,
    message: string,
    onSuccess: (value: T) => void,
  ): void {
    if (this.mutating()) return;

    this.feedback.set(null);
    this.errorService.clear();
    this.mutating.set(true);

    operation
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.mutating.set(false)),
      )
      .subscribe((value) => {
        onSuccess(value);
        this.feedback.set({ title, message });
      });
  }

  private reloadAfterMutation(): void {
    const feedback = this.feedback();
    this.reload();
    if (feedback) {
      this.feedback.set(feedback);
    }
  }
}
