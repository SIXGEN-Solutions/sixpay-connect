#!/usr/bin/env python3
from pathlib import Path
import subprocess

ROOT = Path.cwd()
BRANCH = "feat/sixpay-customer-management-baseline"

def run(*args):
    return subprocess.run(args, cwd=ROOT, text=True, capture_output=True)

def guard():
    r = run("git", "rev-parse", "--show-toplevel")
    if r.returncode != 0:
        raise SystemExit("Run inside sixpay-connect.")
    if Path(r.stdout.strip()).resolve() != ROOT.resolve():
        raise SystemExit("Run from repository root.")
    branch = run("git", "branch", "--show-current").stdout.strip()
    if branch != BRANCH:
        raise SystemExit(f"Expected branch {BRANCH}, got {branch}")

def create(rel, text):
    p = ROOT / rel
    if p.exists():
        if p.read_text(encoding="utf-8") == text:
            print(f"[skip] {rel}")
            return
        raise SystemExit(f"[stop] {rel} already exists with different content")
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[create] {rel}")

def overwrite(rel, text):
    p = ROOT / rel
    if not p.exists():
        raise SystemExit(f"[stop] missing {rel}")
    p.write_text(text, encoding="utf-8", newline="\n")
    print(f"[write] {rel}")

guard()

F = "frontend/src/app/features/customers"
MODELS = F + "/models"
API = F + "/api"
SERVICES = F + "/services"
COMP = F + "/components"
CORE_AUTH = "frontend/src/app/core/auth"

create(MODELS + "/customer-management.ts", '''export type CustomerStatus = 'ACTIVE' | 'SUSPENDED' | 'CLOSED';

export type SubscriptionStatus =
  | 'PENDING_ACTIVATION'
  | 'ACTIVE'
  | 'SUSPENDED'
  | 'CLOSED';

export interface CustomerBankAccount {
  id: string;
  bankingAccountReference: string;
  accountBindingFingerprint: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  defaultAccount: boolean;
  verifiedAt: Date;
}

export interface CustomerMaster {
  id: string;
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string | null;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  status: CustomerStatus;
  statusReason: string | null;
  createdAt: Date;
  updatedAt: Date;
  bankAccounts: CustomerBankAccount[];
}

export interface BankingCustomerPreview {
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  accountReference: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  retrievedAt: Date;
}

export interface CustomerSubscription {
  id: string;
  customerId: string;
  partnerId: string;
  bankAccountId: string;
  status: SubscriptionStatus;
  statusReason: string | null;
  createdAt: Date;
  activatedAt: Date | null;
  updatedAt: Date;
  closedAt: Date | null;
}
''')

create(MODELS + "/customer-management.requests.ts", '''export interface BankingCustomerPreviewRequest {
  financialInstitutionCode: string;
  niu?: string;
  customerNumber?: string;
  accountReference: string;
}

export interface EnrollCustomerRequest {
  financialInstitutionCode: string;
  niu?: string;
  customerNumber?: string;
  accountReference: string;
}

export interface UpdateCustomerRequest {
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
}

export interface ReasonRequest {
  reason: string;
}

export interface AddBankAccountRequest {
  accountReference: string;
}

export interface CreateSubscriptionRequest {
  customerId: string;
  partnerId: string;
  bankAccountId: string;
}
''')

create(MODELS + "/customer-management.response.ts", '''export interface CustomerBankAccountResponse {
  id: string;
  bankingAccountReference: string;
  accountBindingFingerprint: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  defaultAccount: boolean;
  verifiedAt: string;
}

export interface CustomerMasterResponse {
  id: string;
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string | null;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  statusReason: string | null;
  createdAt: string;
  updatedAt: string;
  bankAccounts: CustomerBankAccountResponse[];
}

export interface BankingCustomerPreviewResponse {
  financialInstitutionCode: string;
  bankingCustomerReference: string;
  customerNumber: string | null;
  niu: string;
  legalName: string;
  email: string | null;
  phoneNumber: string | null;
  accountReference: string;
  maskedAccountIdentifier: string;
  currency: string;
  accountType: string | null;
  retrievedAt: string;
}

export interface CustomerSubscriptionResponse {
  id: string;
  customerId: string;
  partnerId: string;
  bankAccountId: string;
  status: 'PENDING_ACTIVATION' | 'ACTIVE' | 'SUSPENDED' | 'CLOSED';
  statusReason: string | null;
  createdAt: string;
  activatedAt: string | null;
  updatedAt: string;
  closedAt: string | null;
}
''')

create(API + "/customer-management-api.mapper.ts", '''import {
  BankingCustomerPreview,
  CustomerMaster,
  CustomerSubscription,
} from '../models/customer-management';
import {
  BankingCustomerPreviewResponse,
  CustomerMasterResponse,
  CustomerSubscriptionResponse,
} from '../models/customer-management.response';

export function mapCustomerMaster(
  response: CustomerMasterResponse,
): CustomerMaster {
  return {
    ...response,
    createdAt: new Date(response.createdAt),
    updatedAt: new Date(response.updatedAt),
    bankAccounts: response.bankAccounts.map((account) => ({
      ...account,
      verifiedAt: new Date(account.verifiedAt),
    })),
  };
}

export function mapBankingPreview(
  response: BankingCustomerPreviewResponse,
): BankingCustomerPreview {
  return {
    ...response,
    retrievedAt: new Date(response.retrievedAt),
  };
}

export function mapCustomerSubscription(
  response: CustomerSubscriptionResponse,
): CustomerSubscription {
  return {
    ...response,
    createdAt: new Date(response.createdAt),
    activatedAt: response.activatedAt ? new Date(response.activatedAt) : null,
    updatedAt: new Date(response.updatedAt),
    closedAt: response.closedAt ? new Date(response.closedAt) : null,
  };
}
''')

create(API + "/customer-management-api.client.ts", '''import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AddBankAccountRequest,
  BankingCustomerPreviewRequest,
  CreateSubscriptionRequest,
  EnrollCustomerRequest,
  ReasonRequest,
  UpdateCustomerRequest,
} from '../models/customer-management.requests';
import {
  BankingCustomerPreviewResponse,
  CustomerMasterResponse,
  CustomerSubscriptionResponse,
} from '../models/customer-management.response';

const CUSTOMERS = '/internal/api/v1/customers';
const SUBSCRIPTIONS = '/internal/api/v1/subscriptions';

@Injectable({ providedIn: 'root' })
export class CustomerManagementApiClient {
  private readonly http = inject(HttpClient);

  list(): Observable<CustomerMasterResponse[]> {
    return this.http.get<CustomerMasterResponse[]>(CUSTOMERS);
  }

  get(customerId: string): Observable<CustomerMasterResponse> {
    return this.http.get<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}`,
    );
  }

  bankingPreview(
    request: BankingCustomerPreviewRequest,
  ): Observable<BankingCustomerPreviewResponse> {
    return this.http.post<BankingCustomerPreviewResponse>(
      `${CUSTOMERS}/banking-preview`,
      request,
    );
  }

  enroll(request: EnrollCustomerRequest): Observable<CustomerMasterResponse> {
    let params = new HttpParams()
      .set('financialInstitutionCode', request.financialInstitutionCode)
      .set('accountReference', request.accountReference);

    if (request.niu) params = params.set('niu', request.niu);
    if (request.customerNumber) {
      params = params.set('customerNumber', request.customerNumber);
    }

    return this.http.post<CustomerMasterResponse>(CUSTOMERS, null, { params });
  }

  update(
    customerId: string,
    request: UpdateCustomerRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.put<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}`,
      request,
    );
  }

  suspend(
    customerId: string,
    request: ReasonRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/suspension`,
      request,
    );
  }

  reactivate(customerId: string): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/reactivation`,
      null,
    );
  }

  addAccount(
    customerId: string,
    request: AddBankAccountRequest,
  ): Observable<CustomerMasterResponse> {
    return this.http.post<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts`,
      request,
    );
  }

  makeDefaultAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMasterResponse> {
    return this.http.put<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts/${encodeURIComponent(accountId)}/default`,
      null,
    );
  }

  removeAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMasterResponse> {
    return this.http.delete<CustomerMasterResponse>(
      `${CUSTOMERS}/${encodeURIComponent(customerId)}/accounts/${encodeURIComponent(accountId)}`,
    );
  }

  subscriptions(customerId: string): Observable<CustomerSubscriptionResponse[]> {
    return this.http.get<CustomerSubscriptionResponse[]>(SUBSCRIPTIONS, {
      params: new HttpParams().set('customerId', customerId),
    });
  }

  createSubscription(
    request: CreateSubscriptionRequest,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(SUBSCRIPTIONS, request);
  }

  activateSubscription(
    subscriptionId: string,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}/activation`,
      null,
    );
  }

  suspendSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<CustomerSubscriptionResponse> {
    return this.http.post<CustomerSubscriptionResponse>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}/suspension`,
      request,
    );
  }

  closeSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<void> {
    return this.http.delete<void>(
      `${SUBSCRIPTIONS}/${encodeURIComponent(subscriptionId)}`,
      { body: request },
    );
  }
}
''')

create(SERVICES + "/customer-management.service.ts", '''import { inject, Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

import { CustomerManagementApiClient } from '../api/customer-management-api.client';
import {
  mapBankingPreview,
  mapCustomerMaster,
  mapCustomerSubscription,
} from '../api/customer-management-api.mapper';
import {
  BankingCustomerPreview,
  CustomerMaster,
  CustomerSubscription,
} from '../models/customer-management';
import {
  AddBankAccountRequest,
  BankingCustomerPreviewRequest,
  CreateSubscriptionRequest,
  EnrollCustomerRequest,
  ReasonRequest,
  UpdateCustomerRequest,
} from '../models/customer-management.requests';

@Injectable({ providedIn: 'root' })
export class CustomerManagementService {
  private readonly api = inject(CustomerManagementApiClient);

  list(): Observable<CustomerMaster[]> {
    return this.api.list().pipe(map((items) => items.map(mapCustomerMaster)));
  }

  get(customerId: string): Observable<CustomerMaster> {
    return this.api.get(customerId).pipe(map(mapCustomerMaster));
  }

  bankingPreview(
    request: BankingCustomerPreviewRequest,
  ): Observable<BankingCustomerPreview> {
    return this.api.bankingPreview(request).pipe(map(mapBankingPreview));
  }

  enroll(request: EnrollCustomerRequest): Observable<CustomerMaster> {
    return this.api.enroll(request).pipe(map(mapCustomerMaster));
  }

  update(
    customerId: string,
    request: UpdateCustomerRequest,
  ): Observable<CustomerMaster> {
    return this.api.update(customerId, request).pipe(map(mapCustomerMaster));
  }

  suspend(customerId: string, request: ReasonRequest): Observable<CustomerMaster> {
    return this.api.suspend(customerId, request).pipe(map(mapCustomerMaster));
  }

  reactivate(customerId: string): Observable<CustomerMaster> {
    return this.api.reactivate(customerId).pipe(map(mapCustomerMaster));
  }

  addAccount(
    customerId: string,
    request: AddBankAccountRequest,
  ): Observable<CustomerMaster> {
    return this.api.addAccount(customerId, request).pipe(map(mapCustomerMaster));
  }

  makeDefaultAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .makeDefaultAccount(customerId, accountId)
      .pipe(map(mapCustomerMaster));
  }

  removeAccount(
    customerId: string,
    accountId: string,
  ): Observable<CustomerMaster> {
    return this.api
      .removeAccount(customerId, accountId)
      .pipe(map(mapCustomerMaster));
  }

  subscriptions(customerId: string): Observable<CustomerSubscription[]> {
    return this.api
      .subscriptions(customerId)
      .pipe(map((items) => items.map(mapCustomerSubscription)));
  }

  createSubscription(
    request: CreateSubscriptionRequest,
  ): Observable<CustomerSubscription> {
    return this.api
      .createSubscription(request)
      .pipe(map(mapCustomerSubscription));
  }

  activateSubscription(subscriptionId: string): Observable<CustomerSubscription> {
    return this.api
      .activateSubscription(subscriptionId)
      .pipe(map(mapCustomerSubscription));
  }

  suspendSubscription(
    subscriptionId: string,
    request: ReasonRequest,
  ): Observable<CustomerSubscription> {
    return this.api
      .suspendSubscription(subscriptionId, request)
      .pipe(map(mapCustomerSubscription));
  }
}
''')

create(CORE_AUTH + "/permission.guard.ts", '''import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthenticationService } from './authentication.service';
import { SixpayRole } from './authentication.model';

export const permissionGuard: CanActivateFn = (route) => {
  const authentication = inject(AuthenticationService);
  const router = inject(Router);

  const permissions =
    (route.data['permissions'] as readonly string[] | undefined) ?? [];
  const fallbackRoles =
    (route.data['fallbackRoles'] as readonly SixpayRole[] | undefined) ?? [];

  const authorized =
    permissions.length === 0 ||
    permissions.some((permission) => authentication.hasPermission(permission)) ||
    (authentication.isStandaloneMode &&
      fallbackRoles.some((role) => authentication.hasRole(role)));

  return authorized ? true : router.createUrlTree(['/forbidden']);
};
''')

create(COMP + "/customer-master-list-page.component.ts", '''import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { CustomerMaster } from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-master-list-page',
  imports: [
    DatePipe,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar title="Customers SIXPAY" />

    <div class="actions">
      @if (canCreate()) {
        <a routerLink="/customers/enroll">Enrôler un Customer</a>
      }
      <a routerLink="/customers/observed">Observed Customers</a>
    </div>

    <sp-card>
      @if (loading()) {
        <sp-loading label="Chargement des Customers" />
      } @else if (customers().length === 0) {
        <p>Aucun Customer master enrôlé.</p>
      } @else {
        <div class="customer-grid">
          @for (customer of customers(); track customer.id) {
            <a class="customer-row" [routerLink]="['/customers', customer.id]">
              <strong>{{ customer.legalName }}</strong>
              <span>{{ customer.niu || 'NIU non renseigné' }}</span>
              <span>{{ customer.financialInstitutionCode }}</span>
              <span>{{ customer.status }}</span>
              <span>{{ customer.updatedAt | date: 'short' }}</span>
            </a>
          }
        </div>
      }
    </sp-card>
  `,
  styles: [`
    .actions { display: flex; gap: 1rem; margin: 1rem 0; }
    .customer-grid { display: grid; gap: .75rem; }
    .customer-row {
      display: grid;
      grid-template-columns: 2fr 1.2fr 1fr .8fr 1fr;
      gap: 1rem;
      padding: 1rem;
      border: 1px solid #ddd;
      border-radius: .5rem;
      text-decoration: none;
      color: inherit;
    }
    @media (max-width: 900px) {
      .customer-row { grid-template-columns: 1fr; }
    }
  `],
})
export class CustomerMasterListPageComponent {
  private readonly service = inject(CustomerManagementService);
  private readonly auth = inject(AuthenticationService);

  protected readonly loading = signal(true);
  protected readonly customers = signal<CustomerMaster[]>([]);

  protected readonly canCreate = () =>
    this.auth.hasPermission('customer.create') ||
    (this.auth.isStandaloneMode && this.auth.hasRole('ADMIN'));

  constructor() {
    this.service
      .list()
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((items) => this.customers.set(items));
  }
}
''')

create(COMP + "/customer-enrollment-wizard.component.ts", '''import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';
import { catchError, EMPTY, finalize } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { BankingCustomerPreview } from '../models/customer-management';
import { CustomerManagementService } from '../services/customer-management.service';

@Component({
  selector: 'sp-customer-enrollment-wizard',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpToolbarComponent,
  ],
  template: `
    <sp-toolbar title="Enrôlement Customer" />

    <sp-card>
      <h2>1. Recherche bancaire</h2>
      <form [formGroup]="form" (ngSubmit)="search()">
        <mat-form-field>
          <mat-label>Institution financière</mat-label>
          <input matInput formControlName="financialInstitutionCode" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>NIU</mat-label>
          <input matInput formControlName="niu" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Numéro client</mat-label>
          <input matInput formControlName="customerNumber" />
        </mat-form-field>
        <mat-form-field>
          <mat-label>Référence compte</mat-label>
          <input matInput formControlName="accountReference" />
        </mat-form-field>
        <button sp-button type="submit" [disabled]="searching()">
          Rechercher dans Amplitude
        </button>
      </form>
    </sp-card>

    @if (searching()) {
      <sp-loading label="Recherche bancaire" />
    }

    @if (preview(); as customer) {
      <sp-card>
        <h2>2. Vérifier les données retournées</h2>
        <p><strong>{{ customer.legalName }}</strong></p>
        <p>NIU : {{ customer.niu }}</p>
        <p>Référence banque : {{ customer.bankingCustomerReference }}</p>
        <p>Compte : {{ customer.maskedAccountIdentifier }}</p>
        <p>Devise : {{ customer.currency }}</p>
        <p>
          Le preview ne constitue pas une preuve. La confirmation déclenche
          une vérification bancaire fraîche côté backend avant création.
        </p>
        <button sp-button type="button" (click)="enroll()" [disabled]="enrolling()">
          Confirmer l’enrôlement
        </button>
      </sp-card>
    }
  `,
  styles: [`
    form { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 1rem; }
    @media (max-width: 700px) { form { grid-template-columns: 1fr; } }
  `],
})
export class CustomerEnrollmentWizardComponent {
  private readonly fb = inject(FormBuilder);
  private readonly service = inject(CustomerManagementService);
  private readonly router = inject(Router);
  protected readonly errorService = inject(ErrorService);

  protected readonly searching = signal(false);
  protected readonly enrolling = signal(false);
  protected readonly preview = signal<BankingCustomerPreview | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    financialInstitutionCode: ['', [Validators.required, Validators.maxLength(50)]],
    niu: [''],
    customerNumber: [''],
    accountReference: ['', [Validators.required, Validators.maxLength(100)]],
  });

  protected search(): void {
    if (this.form.invalid || this.searching()) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.preview.set(null);
    this.errorService.clear();
    this.searching.set(true);

    this.service
      .bankingPreview({
        financialInstitutionCode: value.financialInstitutionCode.trim(),
        niu: value.niu.trim() || undefined,
        customerNumber: value.customerNumber.trim() || undefined,
        accountReference: value.accountReference.trim(),
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.searching.set(false)),
      )
      .subscribe((preview) => this.preview.set(preview));
  }

  protected enroll(): void {
    if (!this.preview() || this.enrolling()) return;

    const value = this.form.getRawValue();
    this.enrolling.set(true);

    this.service
      .enroll({
        financialInstitutionCode: value.financialInstitutionCode.trim(),
        niu: value.niu.trim() || undefined,
        customerNumber: value.customerNumber.trim() || undefined,
        accountReference: value.accountReference.trim(),
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.enrolling.set(false)),
      )
      .subscribe((customer) => {
        void this.router.navigate(['/customers', customer.id], {
          queryParams: { enrolled: true },
        });
      });
  }
}
''')

create(COMP + "/customer-master-detail-page.component.ts", '''import { DatePipe } from '@angular/common';
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
      <sp-toolbar [title]="item.legalName" />

      <sp-card>
        <h2>Identité</h2>
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

      <sp-card>
        <h2>Comptes</h2>
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

      <sp-card>
        <h2>Subscriptions</h2>
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
''')

overwrite(F + "/customers.routes.ts", '''import { Routes } from '@angular/router';

import { permissionGuard } from '../../core/auth/permission.guard';
import { roleGuard } from '../../core/auth/role.guard';

const CUSTOMER_READ_ROLES = ['ADMIN', 'MANAGER', 'AUDITOR'] as const;

export const CUSTOMER_ROUTES: Routes = [
  {
    path: '',
    canActivate: [permissionGuard],
    data: {
      permissions: ['customer.read'],
      fallbackRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-master-list-page.component').then(
        (component) => component.CustomerMasterListPageComponent,
      ),
  },
  {
    path: 'enroll',
    canActivate: [permissionGuard],
    data: {
      permissions: ['customer.create'],
      fallbackRoles: ['ADMIN'],
    },
    loadComponent: () =>
      import('./components/customer-enrollment-wizard.component').then(
        (component) => component.CustomerEnrollmentWizardComponent,
      ),
  },
  {
    path: 'observed',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-list-page.component').then(
        (component) => component.CustomerListPageComponent,
      ),
  },
  {
    path: 'observed/:observedCustomerId',
    canActivate: [roleGuard],
    data: { roles: CUSTOMER_READ_ROLES },
    loadComponent: () =>
      import('./components/customer-detail-page.component').then(
        (component) => component.CustomerDetailPageComponent,
      ),
  },
  {
    path: ':customerId',
    canActivate: [permissionGuard],
    data: {
      permissions: ['customer.read'],
      fallbackRoles: CUSTOMER_READ_ROLES,
    },
    loadComponent: () =>
      import('./components/customer-master-detail-page.component').then(
        (component) => component.CustomerMasterDetailPageComponent,
      ),
  },
];
''')

print("CM-8 Angular Customer Management applied.")
