import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import {
  CustomerMaster,
  CustomerSubscription,
} from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-master-detail-page',
  imports: [
    DatePipe,
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
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
              <input
                matInput
                type="email"
                formControlName="email"
              />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Téléphone</mat-label>
              <input
                matInput
                formControlName="phoneNumber"
              />
            </mat-form-field>

            <div class="customer-form__actions">
              <sp-button
                type="submit"
                icon="save"
              >
                Enregistrer
              </sp-button>
            </div>
          </form>
        }

        <div class="customer-section-actions">
          @if (item.status === 'ACTIVE' && canSuspend()) {
            <sp-button
              type="button"
              variant="danger"
              icon="pause_circle"
              (buttonClick)="suspendCustomer()"
            >
              Suspendre
            </sp-button>
          }

          @if (item.status === 'SUSPENDED' && canUpdate()) {
            <sp-button
              type="button"
              icon="play_circle"
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
                    (buttonClick)="makeDefault(account.id)"
                  >
                    Définir par défaut
                  </sp-button>

                  <sp-button
                    type="button"
                    variant="danger"
                    icon="delete"
                    (buttonClick)="removeAccount(account.id)"
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
              <input
                matInput
                formControlName="accountReference"
              />
            </mat-form-field>

            <div class="customer-form__actions">
              <sp-button
                type="submit"
                icon="add_card"
              >
                Vérifier et ajouter
              </sp-button>
            </div>
          </form>
        }
      </sp-card>

      <sp-card title="Subscriptions">
        <div class="customer-rows">
          @for (subscription of subscriptions(); track subscription.id) {
            <div class="customer-row">
              <span>{{ subscription.partnerId }}</span>
              <span>{{ subscription.status }}</span>
              <span>{{ subscription.updatedAt | date: 'short' }}</span>

              <div class="customer-row__actions">
                @if (
                  subscription.status === 'PENDING_ACTIVATION'
                  && canSubscriptionUpdate()
                ) {
                  <sp-button
                    type="button"
                    icon="check_circle"
                    (buttonClick)="activate(subscription.id)"
                  >
                    Activer
                  </sp-button>
                }

                @if (
                  subscription.status === 'ACTIVE'
                  && canSubscriptionSuspend()
                ) {
                  <sp-button
                    type="button"
                    variant="danger"
                    icon="pause_circle"
                    (buttonClick)="suspendSubscription(subscription.id)"
                  >
                    Suspendre
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
              <input
                matInput
                formControlName="partnerId"
              />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Bank account ID</mat-label>
              <input
                matInput
                formControlName="bankAccountId"
              />
            </mat-form-field>

            <div class="customer-form__actions">
              <sp-button
                type="submit"
                icon="add"
              >
                Créer
              </sp-button>
            </div>
          </form>
        }
      </sp-card>
    }
  `,
  styles: [`
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
  `],
})
export class CustomerMasterDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);
  private readonly fb = inject(FormBuilder);

  private readonly customerId =
    this.route.snapshot.paramMap.get('customerId') ?? '';

  protected readonly loading = signal(true);
  protected readonly customer = signal<CustomerMaster | null>(null);
  protected readonly subscriptions = signal<CustomerSubscription[]>([]);

  protected readonly profileForm = this.fb.nonNullable.group({
    legalName: ['', [Validators.required, Validators.maxLength(200)]],
    email: [''],
    phoneNumber: [''],
  });

  protected readonly accountForm = this.fb.nonNullable.group({
    accountReference: ['', Validators.required],
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

  protected readonly canSubscriptionCreate = () =>
    this.auth.hasPermission('subscription.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionUpdate = () =>
    this.auth.hasPermission('subscription.update') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  protected readonly canSubscriptionSuspend = () =>
    this.auth.hasPermission('subscription.suspend') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  constructor() {
    this.reload();
  }

  private reload(): void {
    this.loading.set(true);
    forkJoin({
      customer: this.service.get(this.customerId),
      subscriptions: this.service.subscriptions(this.customerId),
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
        const defaultAccount = customer.bankAccounts.find(
          (account) => account.defaultAccount,
        );
        if (defaultAccount) {
          this.subscriptionForm.controls.bankAccountId.setValue(
            defaultAccount.id,
          );
        }
      });
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid) {
      return;
    }

    const value = this.profileForm.getRawValue();

    this.service
      .update(this.customerId, {
        legalName: value.legalName.trim(),
        email: value.email.trim() || null,
        phoneNumber: value.phoneNumber.trim() || null,
      })
      .subscribe((customer) => this.customer.set(customer));
  }

  protected suspendCustomer(): void {
    const reason = window.prompt('Motif de suspension');
    if (!reason?.trim()) {
      return;
    }

    this.service
      .suspend(this.customerId, { reason: reason.trim() })
      .subscribe((customer) => this.customer.set(customer));
  }

  protected reactivate(): void {
    this.service
      .reactivate(this.customerId)
      .subscribe((customer) => this.customer.set(customer));
  }

  protected addAccount(): void {
    if (this.accountForm.invalid) {
      return;
    }

    const accountReference =
      this.accountForm.getRawValue().accountReference.trim();

    this.service
      .addAccount(this.customerId, { accountReference })
      .subscribe((customer) => {
        this.customer.set(customer);
        this.accountForm.reset();
      });
  }

  protected makeDefault(accountId: string): void {
    this.service
      .makeDefaultAccount(this.customerId, accountId)
      .subscribe((customer) => this.customer.set(customer));
  }

  protected removeAccount(accountId: string): void {
    this.service
      .removeAccount(this.customerId, accountId)
      .subscribe((customer) => this.customer.set(customer));
  }

  protected createSubscription(): void {
    if (this.subscriptionForm.invalid) {
      return;
    }

    const value = this.subscriptionForm.getRawValue();

    this.service
      .createSubscription({
        customerId: this.customerId,
        partnerId: value.partnerId.trim(),
        bankAccountId: value.bankAccountId.trim(),
      })
      .subscribe(() => this.reload());
  }

  protected activate(subscriptionId: string): void {
    this.service
      .activateSubscription(subscriptionId)
      .subscribe(() => this.reload());
  }

  protected suspendSubscription(subscriptionId: string): void {
    const reason = window.prompt(
      'Motif de suspension de la subscription',
    );

    if (!reason?.trim()) {
      return;
    }

    this.service
      .suspendSubscription(
        subscriptionId,
        { reason: reason.trim() },
      )
      .subscribe(() => this.reload());
  }
}
