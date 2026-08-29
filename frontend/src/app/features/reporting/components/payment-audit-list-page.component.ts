import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentAuditQuery } from '../models/reporting-query';
import { PaymentAuditPage } from '../models/reporting';
import { ReportingService } from '../services/reporting.service';

@Component({
  selector: 'sp-payment-audit-list-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    MockContentStateComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/reporting">← Retour Audit / Reporting</a>
      <sp-toolbar
        title="Preuves d'audit Payment"
        description="Recherche des enregistrements immuables et masqués."
      />

      <sp-card title="Recherche">
        <form class="sp-filter-grid" [formGroup]="form" (ngSubmit)="search()">
          <mat-form-field appearance="outline">
            <mat-label>Payment ID</mat-label>
            <input matInput formControlName="paymentId" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Référence Payment</mat-label>
            <input matInput formControlName="paymentReference" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Actor ID</mat-label>
            <input matInput formControlName="actorId" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Action</mat-label>
            <input matInput formControlName="action" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Résultat</mat-label>
            <mat-select formControlName="result">
              <mat-option value="">Tous</mat-option>
              @for (
                result of ['SUCCESS', 'FAILURE', 'DENIED', 'NO_OP', 'QUARANTINED'];
                track result
              ) {
                <mat-option [value]="result">{{ result }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Source</mat-label>
            <mat-select formControlName="sourceSystem">
              <mat-option value="">Toutes</mat-option>
              <mat-option value="SIXPAY">SIXPAY</mat-option>
              <mat-option value="TRESOR_PAY">TRESOR_PAY</mat-option>
              <mat-option value="AMPLITUDE">AMPLITUDE</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Du (ISO)</mat-label>
            <input matInput formControlName="occurredFrom" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Au (ISO)</mat-label>
            <input matInput formControlName="occurredTo" />
          </mat-form-field>

          <div class="sp-actions">
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()"
              >Réinitialiser</sp-button
            >
          </div>
        </form>
      </sp-card>

      <sp-card title="Résultats">
        @if (page(); as currentPage) {
          @if (currentPage.items.length === 0) {
            <sp-mock-content-state
              kind="empty"
              title="Aucune preuve"
              message="Aucun enregistrement d'audit ne correspond à la période et aux filtres."
            />
          } @else {
            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Action</th>
                    <th>Acteur</th>
                    <th>Cible</th>
                    <th>Résultat</th>
                    <th>Source</th>
                    <th>Référence</th>
                  </tr>
                </thead>
                <tbody>
                  @for (record of currentPage.items; track record.auditId) {
                    <tr>
                      <td>{{ record.occurredAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                      <td>
                        <a [routerLink]="[record.auditId]">{{ record.action }}</a>
                      </td>
                      <td>{{ record.actorType }} / {{ record.actorId }}</td>
                      <td>{{ record.targetType }}</td>
                      <td>{{ record.result }}</td>
                      <td>{{ record.sourceSystem }}</td>
                      <td>{{ record.paymentReference ?? '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-filter-grid {
      display: grid;
      grid-template-columns: repeat(3, minmax(0, 1fr));
      gap: var(--sp-space-3);
    }
    .sp-actions {
      display: flex;
      gap: var(--sp-space-2);
      align-items: center;
    }
    .sp-table-scroll {
      overflow-x: auto;
    }
    .sp-table {
      width: 100%;
      border-collapse: collapse;
    }
    .sp-table th,
    .sp-table td {
      padding: var(--sp-space-2);
      text-align: left;
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      white-space: nowrap;
    }
    @media (max-width: 900px) {
      .sp-filter-grid {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class PaymentAuditListPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly reporting = inject(ReportingService);

  protected readonly page = signal<PaymentAuditPage | null>(null);

  protected readonly form = this.formBuilder.nonNullable.group({
    paymentId: [''],
    paymentReference: [''],
    actorId: [''],
    action: [''],
    result: [''],
    sourceSystem: [''],
    occurredFrom: ['2026-08-08T00:00:00.000Z', Validators.required],
    occurredTo: ['2026-08-09T00:00:00.000Z', Validators.required],
  });

  constructor() {
    this.search();
  }

  protected search(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();

    const query: PaymentAuditQuery = {
      occurredFrom: new Date(value.occurredFrom),
      occurredTo: new Date(value.occurredTo),
      sort: 'OCCURRED_AT_DESC',
      size: 50,
      ...(value.paymentId.trim() ? { paymentId: value.paymentId.trim() } : {}),
      ...(value.paymentReference.trim() ? { paymentReference: value.paymentReference.trim() } : {}),
      ...(value.actorId.trim() ? { actorId: value.actorId.trim() } : {}),
      ...(value.action.trim() ? { action: value.action.trim() } : {}),
      ...(value.result ? { result: value.result as NonNullable<PaymentAuditQuery['result']> } : {}),
      ...(value.sourceSystem
        ? { sourceSystem: value.sourceSystem as NonNullable<PaymentAuditQuery['sourceSystem']> }
        : {}),
    };

    this.reporting.searchAudit(query).subscribe((page) => this.page.set(page));
  }

  protected reset(): void {
    this.form.reset({
      paymentId: '',
      paymentReference: '',
      actorId: '',
      action: '',
      result: '',
      sourceSystem: '',
      occurredFrom: '2026-08-08T00:00:00.000Z',
      occurredTo: '2026-08-09T00:00:00.000Z',
    });
    this.search();
  }
}
