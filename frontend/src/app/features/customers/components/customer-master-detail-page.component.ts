import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
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
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    @if (loading()) {
      <sp-loading label="Chargement du Customer" />
    } @else if (customer(); as item) {
      <sp-toolbar [title]="item.legalName" />

      <sp-card title="Identité">
        <p>Statut : <strong>{{ item.status }}</strong></p>
        <p>NIU : {{ item.niu || '—' }}</p>
        <p>Référence bancaire : {{ item.bankingCustomerReference }}</p>

        @if (canUpdate()) {
          <form [formGroup]="profileForm" (ngSubmit)="saveProfile()">
            <mat-form-field>
              <mat-label>Nom légal</mat-label>
              <input matInput formControlName="legalName" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Courriel</mat-label>
              <input matInput formControlName="email" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Téléphone</mat-label>
              <input matInput formControlName="phoneNumber" />
            </mat-form-field>
            <button sp-button type="submit">Enregistrer</button>
          </form>
        }

        @if (item.status === 'ACTIVE' && canSuspend()) {
          <button sp-button type="button" (click)="suspendCustomer()">Suspendre</button>
        }
        @if (item.status === 'SUSPENDED' && canUpdate()) {
          <button sp-button type="button" (click)="reactivate()">Réactiver</button>
        }
      </sp-card>

      <sp-card title="Comptes">
        @for (account of item.bankAccounts; track account.id) {
          <div class="row">
            <span>{{ account.maskedAccountIdentifier }}</span>
            <span>{{ account.currency }}</span>
            <span>{{ account.accountType || '—' }}</span>
            <span>{{ account.defaultAccount ? 'Défaut' : '' }}</span>
            @if (!account.defaultAccount && canUpdate()) {
              <button sp-button type="button" (click)="makeDefault(account.id)">
                Définir par défaut
              </button>
              <button sp-button type="button" (click)="removeAccount(account.id)">
                Retirer
              </button>
            }
          </div>
        }

        @if (canUpdate()) {
          <form [formGroup]="accountForm" (ngSubmit)="addAccount()">
            <mat-form-field>
              <mat-label>Référence du nouveau compte</mat-label>
              <input matInput formControlName="accountReference" />
            </mat-form-field>
            <button sp-button type="submit">Vérifier et ajouter</button>
          </form>
        }
      </sp-card>

      <sp-card title="Subscriptions">
        @for (subscription of subscriptions(); track subscription.id) {
          <div class="row">
            <span>{{ subscription.partnerId }}</span>
            <span>{{ subscription.status }}</span>
            <span>{{ subscription.updatedAt | date: 'short' }}</span>
            @if (subscription.status === 'PENDING_ACTIVATION' && canSubscriptionUpdate()) {
              <button sp-button type="button" (click)="activate(subscription.id)">
                Activer
              </button>
            }
            @if (subscription.status === 'ACTIVE' && canSubscriptionSuspend()) {
              <button sp-button type="button" (click)="suspendSubscription(subscription.id)">
                Suspendre
              </button>
            }
          </div>
        }

        @if (canSubscriptionCreate()) {
          <form [formGroup]="subscriptionForm" (ngSubmit)="createSubscription()">
            <mat-form-field>
              <mat-label>Partner ID</mat-label>
              <input matInput formControlName="partnerId" />
            </mat-form-field>
            <mat-form-field>
              <mat-label>Bank account ID</mat-label>
              <input matInput formControlName="bankAccountId" />
            </mat-form-field>
            <button sp-button type="submit">Créer</button>
          </form>
        }
      </sp-card>
    }
  `,
  styles: [`
    form, .row { display: flex; flex-wrap: wrap; gap: 1rem; align-items: center; }
    .row { padding: .75rem 0; }
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
          this.subscriptionForm.controls.bankAccountId.setValue(defaultAccount.id);
        }
      });
  }

  protected saveProfile(): void {
    if (this.profileForm.invalid) return;
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
    if (!reason?.trim()) return;
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
    if (this.accountForm.invalid) return;
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
    if (this.subscriptionForm.invalid) return;
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
    const reason = window.prompt('Motif de suspension de la subscription');
    if (!reason?.trim()) return;
    this.service
      .suspendSubscription(subscriptionId, { reason: reason.trim() })
      .subscribe(() => this.reload());
  }
}
