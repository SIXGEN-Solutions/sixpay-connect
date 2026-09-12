import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs';

import { AccountingApiClient } from '../api/accounting-api.client';
import {
  TfjOperationalCategory,
  TfjOperationalResponse,
} from '../models/accounting-tfj-operational';

@Component({
  selector: 'sp-accounting-tfj-operational-page',
  standalone: true,
  imports: [FormsModule],
  template: `
    <section>
      <header>
        <h1>Suivi TFJ / Réconciliation</h1>
        <p>
          Consultation en lecture seule des confirmations TFJ et de leur état de réconciliation.
        </p>
      </header>

      <form (ngSubmit)="search()">
        <label>
          Date comptable
          <input type="date" name="businessDate" [(ngModel)]="businessDate" />
        </label>

        <label>
          Catégorie
          <select name="category" [(ngModel)]="category">
            <option value="">Toutes</option>
            @for (value of categories; track value) {
              <option [value]="value">
                {{ value }}
              </option>
            }
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

        <label>
          Référence bancaire
          <input
            type="text"
            name="bankPostingReference"
            [(ngModel)]="bankPostingReference"
            placeholder="Référence Core Banking"
          />
        </label>

        <button type="submit" [disabled]="loading()">Rechercher</button>
      </form>

      @if (error()) {
        <p role="alert">{{ error() }}</p>
      }
      @if (loading()) {
        <p>Chargement...</p>
      }

      @if (!loading() && items().length > 0) {
        <table>
          <thead>
            <tr>
              <th>Date comptable</th>
              <th>Référence paiement</th>
              <th>Référence bancaire</th>
              <th>Statut TFJ</th>
              <th>Matching</th>
              <th>Catégorie</th>
              <th>Détail</th>
            </tr>
          </thead>
          <tbody>
            @for (item of items(); track item.confirmationId) {
              <tr>
                <td>{{ item.businessDate }}</td>
                <td>{{ item.paymentReference }}</td>
                <td>{{ item.bankPostingReference }}</td>
                <td>{{ item.tfjStatus }}</td>
                <td>{{ item.matchStatus }}</td>
                <td>{{ item.category }}</td>
                <td>
                  <button type="button" (click)="loadDetail(item.confirmationId)">Consulter</button>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }

      @if (!loading() && !error() && items().length === 0) {
        <p>Aucune confirmation TFJ à afficher.</p>
      }

      @if (totalElements() > 0) {
        <footer>{{ totalElements() }} résultat(s)</footer>
      }

      @if (detail(); as selected) {
        <section>
          <h2>Détail de la confirmation</h2>

          <dl>
            <dt>Confirmation</dt>
            <dd>{{ selected.confirmationId }}</dd>

            <dt>Institution financière</dt>
            <dd>{{ selected.financialInstitutionCode }}</dd>

            <dt>Date comptable</dt>
            <dd>{{ selected.businessDate }}</dd>

            <dt>Référence paiement</dt>
            <dd>{{ selected.paymentReference }}</dd>

            <dt>Référence bancaire</dt>
            <dd>{{ selected.bankPostingReference }}</dd>

            <dt>Batch TFJ</dt>
            <dd>{{ selected.tfjBatchReference || '—' }}</dd>

            <dt>Statut TFJ</dt>
            <dd>{{ selected.tfjStatus }}</dd>

            <dt>Canal d'observation</dt>
            <dd>{{ selected.observationChannel }}</dd>

            <dt>Matching</dt>
            <dd>{{ selected.matchStatus }}</dd>

            <dt>Payment associé</dt>
            <dd>{{ selected.matchedPaymentId || '—' }}</dd>

            <dt>Finalité publiée le</dt>
            <dd>{{ selected.finalityPublishedAt || '—' }}</dd>

            <dt>Code d'échec</dt>
            <dd>{{ selected.failureCode || '—' }}</dd>

            <dt>Action de récupération déclarée</dt>
            <dd>{{ selected.recoveryAction || '—' }}</dd>

            <dt>Catégorie opérateur</dt>
            <dd>{{ selected.category }}</dd>

            <dt>Corrélation</dt>
            <dd>{{ selected.correlationId }}</dd>
          </dl>
        </section>
      }
    </section>
  `,
})
export class AccountingTfjOperationalPageComponent {
  private readonly api = inject(AccountingApiClient);

  readonly categories: TfjOperationalCategory[] = [
    'MATCHED',
    'QUARANTINED_UNMATCHED',
    'QUARANTINED_AMBIGUOUS',
    'FAILED',
    'FINALITY_PUBLICATION_PENDING',
    'COMPLETED',
  ];

  readonly items = signal<TfjOperationalResponse[]>([]);
  readonly detail = signal<TfjOperationalResponse | null>(null);
  readonly totalElements = signal(0);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  businessDate = '';
  category: TfjOperationalCategory | '' = '';
  paymentReference = '';
  bankPostingReference = '';

  search(): void {
    this.loading.set(true);
    this.error.set(null);
    this.detail.set(null);

    const query = {
      page: 0,
      size: 20,
      ...(this.businessDate ? { businessDate: this.businessDate } : {}),
      ...(this.category ? { category: this.category } : {}),
      ...(this.paymentReference.trim() ? { paymentReference: this.paymentReference.trim() } : {}),
      ...(this.bankPostingReference.trim()
        ? { bankPostingReference: this.bankPostingReference.trim() }
        : {}),
    };

    this.api
      .searchTfjOperations(query)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (response) => {
          this.items.set(response.content);
          this.totalElements.set(response.totalElements);
        },
        error: () => {
          this.items.set([]);
          this.totalElements.set(0);
          this.error.set('Impossible de charger le suivi TFJ / Réconciliation.');
        },
      });
  }

  loadDetail(confirmationId: string): void {
    this.error.set(null);

    this.api.getTfjOperation(confirmationId).subscribe({
      next: (response) => this.detail.set(response),
      error: () => {
        this.detail.set(null);
        this.error.set('Impossible de charger le détail de la confirmation TFJ.');
      },
    });
  }
}
