import {
  CurrencyPipe,
  DatePipe,
} from '@angular/common';
import {
  Component,
  inject,
  signal,
} from '@angular/core';
import {
  ActivatedRoute,
  RouterLink,
} from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { AccountingBatchDetail } from '../models/accounting';
import { AccountingService } from '../services/accounting.service';

@Component({
  selector: 'sp-accounting-batch-detail-page',
  imports: [
    CurrencyPipe,
    DatePipe,
    RouterLink,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/accounting">
        ← Retour à la comptabilisation
      </a>

      @if (batch(); as currentBatch) {
        <sp-toolbar
          [title]="currentBatch.batchId"
          description="Détail du lot comptable SIXPAY."
        />

        <div class="sp-grid">
          <sp-card title="Synthèse">
            <p>
              <strong>
                {{ currentBatch.itemCount }}
                items
              </strong>
            </p>

            <p>
              Statut :
              {{ currentBatch.status }}
            </p>
          </sp-card>

          <sp-card title="Institution">
            <p>
              {{
                currentBatch
                  .financialInstitutionCode
              }}
            </p>

            <p>
              Business date :
              {{ currentBatch.businessDate }}
            </p>
          </sp-card>

          <sp-card title="Métadonnées">
            <p>
              Idempotency key :
              {{ currentBatch.idempotencyKey }}
            </p>

            <p>
              Créé le :
              {{
                currentBatch.createdAt
                  | date:
                    'dd/MM/yyyy HH:mm:ss'
              }}
            </p>
          </sp-card>
        </div>

        <sp-card title="Écritures">
          @if (
            currentBatch.items.length === 0
          ) {
            <p>
              Aucune écriture dans ce lot.
            </p>
          } @else {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr>
                    <th>Paiement</th>
                    <th>Partenaire</th>
                    <th>Montant</th>
                    <th>Posting</th>
                    <th>TresorPay</th>
                    <th>Statut</th>
                  </tr>
                </thead>

                <tbody>
                  @for (
                    item of currentBatch.items;
                    track item.paymentId
                  ) {
                    <tr>
                      <td>
                        <a
                          [routerLink]="[
                            '/payments',
                            item.paymentId
                          ]"
                        >
                          {{
                            item
                              .publicPaymentReference
                          }}
                        </a>
                      </td>

                      <td>
                        {{ item.partnerId }}
                      </td>

                      <td>
                        {{
                          item.amount
                            | currency:
                              item.currency:
                              'code':
                              '1.0-2'
                        }}
                      </td>

                      <td>
                        {{
                          item
                            .bankPostingReference
                            ?? '—'
                        }}
                      </td>

                      <td>
                        {{
                          item.tresorPayStatus
                        }}
                      </td>

                      <td>
                        {{ item.status }}
                      </td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </sp-card>
      } @else if (notFound()) {
        <sp-card title="Lot introuvable">
          Aucun lot comptable ne correspond
          à cet identifiant.
        </sp-card>
      }
    </section>
  `,
  styles: `
    :host,.sp-page{
      display:grid;
      gap:var(--sp-space-4)
    }
    .sp-grid{
      display:grid;
      grid-template-columns:
        repeat(3,minmax(0,1fr));
      gap:var(--sp-space-3)
    }
    .sp-table-scroll{
      overflow-x:auto
    }
    .sp-table{
      width:100%;
      border-collapse:collapse
    }
    .sp-table th,
    .sp-table td{
      padding:var(--sp-space-2);
      text-align:left;
      border-bottom:
        1px solid
        var(--mat-sys-outline-variant);
      white-space:nowrap
    }
    @media(max-width:800px){
      .sp-grid{
        grid-template-columns:1fr
      }
    }
  `,
})
export class AccountingBatchDetailPageComponent {
  private readonly route =
    inject(ActivatedRoute);

  private readonly accounting =
    inject(AccountingService);

  protected readonly batch =
    signal<AccountingBatchDetail | null>(
      null,
    );

  protected readonly notFound =
    signal(false);

  constructor() {
    const batchId =
      this.route.snapshot.paramMap
        .get('batchId')
      ?? '';

    this.accounting
      .get(batchId)
      .subscribe((batch) => {
        this.batch.set(batch);
        this.notFound.set(
          batch === null,
        );
      });
  }
}
