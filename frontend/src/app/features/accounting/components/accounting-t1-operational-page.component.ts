import { NgFor, NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccountingApiClient } from '../api/accounting-api.client';
import {
  AccountingT1OperationalCandidateStatus,
  AccountingT1OperationalResponse,
} from '../models/accounting-t1-operational';

@Component({
  selector: 'app-accounting-t1-operational-page',
  standalone: true,
  imports: [FormsModule, NgIf, NgFor],
  template: `
    <section>
      <header>
        <h1>Suivi opérationnel T1</h1>
        <p>Consultation en lecture seule des candidats Accounting du traitement T1.</p>
      </header>

      <form (ngSubmit)="search()" #filterForm="ngForm">
        <label>
          Date comptable
          <input type="date" name="businessDate" [(ngModel)]="businessDate" />
        </label>

        <label>
          Statut
          <select name="status" [(ngModel)]="status">
            <option value="">Tous</option>
            <option *ngFor="let value of statuses" [value]="value">{{ value }}</option>
          </select>
        </label>

        <label>
          Référence paiement
          <input
            type="text"
            name="paymentReference"
            [(ngModel)]="paymentReference"
            placeholder="PAY-..."
          />
        </label>

        <button type="submit" [disabled]="loading()">Rechercher</button>
      </form>

      <p *ngIf="error()" role="alert">{{ error() }}</p>
      <p *ngIf="loading()">Chargement...</p>

      <table *ngIf="!loading() && items().length > 0">
        <thead>
          <tr>
            <th>Référence paiement</th>
            <th>Date comptable</th>
            <th>Statut</th>
            <th>TRESOR PAY</th>
            <th>Motif</th>
            <th>Incident technique</th>
            <th>Batch</th>
          </tr>
        </thead>
        <tbody>
          <tr *ngFor="let item of items()">
            <td>{{ item.publicPaymentReference }}</td>
            <td>{{ item.accountingBusinessDate }}</td>
            <td>{{ item.status }}</td>
            <td>{{ item.tresorPayProviderStatus || '—' }}</td>
            <td>{{ item.eligibilityReason }}</td>
            <td>{{ item.technicalIssue }}</td>
            <td>{{ item.batchId || '—' }}</td>
          </tr>
        </tbody>
      </table>

      <p *ngIf="!loading() && !error() && items().length === 0">
        Aucun candidat opérationnel T1.
      </p>

      <footer *ngIf="totalElements() > 0">
        {{ totalElements() }} résultat(s)
      </footer>
    </section>
  `,
})
export class AccountingT1OperationalPageComponent {
  private readonly api = inject(AccountingApiClient);

  readonly statuses: AccountingT1OperationalCandidateStatus[] = [
    'AWAITING_TRESORPAY_VERIFICATION',
    'ELIGIBLE_FOR_BATCH',
    'INELIGIBLE_FOR_CURRENT_SELECTION',
    'ASSIGNED_TO_BATCH',
  ];

  readonly items = signal<AccountingT1OperationalResponse[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  businessDate = '';
  status: AccountingT1OperationalCandidateStatus | '' = '';
  paymentReference = '';

  search(): void {
    this.loading.set(true);
    this.error.set(null);

    const query = {
      page: 0,
      size: 20,
      ...(this.businessDate ? { businessDate: this.businessDate } : {}),
      ...(this.status ? { status: this.status } : {}),
      ...(this.paymentReference.trim()
        ? { paymentReference: this.paymentReference.trim() }
        : {}),
    };

    this.api
      .searchT1Operations(query)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.items.set(response.content);
          this.totalElements.set(response.totalElements);
        },
        error: () => {
          this.items.set([]);
          this.totalElements.set(0);
          this.error.set('Impossible de charger le suivi opérationnel T1.');
        },
      });
  }
}
