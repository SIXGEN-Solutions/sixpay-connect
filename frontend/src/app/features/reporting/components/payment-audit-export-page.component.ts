import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentAuditExportRequest } from '../models/reporting.response';
import { ReportingService } from '../services/reporting.service';

@Component({
  selector: 'sp-payment-audit-export-page',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/reporting">← Retour Audit / Reporting</a>
      <sp-toolbar title="Export d'audit" description="Demande contrôlée, bornée et justifiée." />

      <sp-card title="Nouvel export">
        <form class="sp-form" [formGroup]="form" (ngSubmit)="submit()">
          <mat-form-field appearance="outline">
            <mat-label>Du (ISO)</mat-label>
            <input matInput formControlName="occurredFrom" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Au (ISO)</mat-label>
            <input matInput formControlName="occurredTo" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Format</mat-label>
            <mat-select formControlName="format">
              <mat-option value="CSV">CSV</mat-option>
              <mat-option value="JSONL">JSONL</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline" class="sp-purpose">
            <mat-label>Justification métier</mat-label>
            <textarea matInput rows="5" formControlName="businessPurpose"></textarea>
            <mat-hint>10 à 500 caractères</mat-hint>
          </mat-form-field>

          <sp-button type="submit" icon="download" [disabled]="form.invalid || submitting()">
            Demander l'export
          </sp-button>
        </form>
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-form {
      display: grid;
      grid-template-columns: 1fr 1fr 1fr;
      gap: var(--sp-space-3);
    }
    .sp-purpose {
      grid-column: 1/-1;
    }
    @media (max-width: 800px) {
      .sp-form {
        grid-template-columns: 1fr;
      }
      .sp-purpose {
        grid-column: auto;
      }
    }
  `,
})
export class PaymentAuditExportPageComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly reporting = inject(ReportingService);
  private readonly router = inject(Router);

  protected readonly submitting = signal(false);

  protected readonly form = this.formBuilder.nonNullable.group({
    occurredFrom: ['2026-08-08T00:00:00.000Z', Validators.required],
    occurredTo: ['2026-08-09T00:00:00.000Z', Validators.required],
    businessPurpose: [
      'Contrôle interne quotidien des opérations SIXPAY.',
      [Validators.required, Validators.minLength(10), Validators.maxLength(500)],
    ],
    format: ['CSV' as 'CSV' | 'JSONL', Validators.required],
  });

  protected submit(): void {
    if (this.form.invalid || this.submitting()) {
      return;
    }

    this.submitting.set(true);
    const value = this.form.getRawValue();

    const request: PaymentAuditExportRequest = {
      occurredFrom: value.occurredFrom,
      occurredTo: value.occurredTo,
      businessPurpose: value.businessPurpose.trim(),
      format: value.format,
    };

    this.reporting.requestExport(request).subscribe((job) => {
      this.submitting.set(false);
      void this.router.navigate(['/reporting/exports', job.exportId]);
    });
  }
}
