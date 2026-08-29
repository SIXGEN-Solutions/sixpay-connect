import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentAuditExportJob } from '../models/reporting';
import { ReportingService } from '../services/reporting.service';

@Component({
  selector: 'sp-payment-audit-export-status-page',
  imports: [DatePipe, RouterLink, SpCardComponent, MockContentStateComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/reporting/exports">← Nouvel export</a>

      @if (job(); as exportJob) {
        <sp-toolbar
          [title]="'Export ' + exportJob.exportId"
          description="Statut du job d'export d'audit."
        />

        <sp-card title="Statut">
          <dl class="sp-details">
            <div>
              <dt>Statut</dt>
              <dd>{{ exportJob.status }}</dd>
            </div>
            <div>
              <dt>Demandé par</dt>
              <dd>{{ exportJob.requestedBy }}</dd>
            </div>
            <div>
              <dt>Demandé le</dt>
              <dd>{{ exportJob.requestedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
            </div>
            <div>
              <dt>Expire le</dt>
              <dd>{{ exportJob.expiresAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd>
            </div>
            <div>
              <dt>Nombre d'enregistrements</dt>
              <dd>{{ exportJob.recordCount ?? '—' }}</dd>
            </div>
            <div>
              <dt>Checksum</dt>
              <dd>{{ exportJob.checksum ?? '—' }}</dd>
            </div>
          </dl>

          <p><strong>Justification :</strong> {{ exportJob.businessPurpose }}</p>

          @if (exportJob.retrievalUri) {
            <p>
              URI de récupération mockée : <code>{{ exportJob.retrievalUri }}</code>
            </p>
          }
        </sp-card>
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Export introuvable"
          message="Ce job d'export n'existe pas dans le jeu de données de démonstration."
        />
      }
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-details {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--sp-space-3);
      margin: 0;
    }
    .sp-details div {
      display: grid;
      gap: 0.2rem;
    }
    .sp-details dt {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.85rem;
    }
    .sp-details dd {
      margin: 0;
      font-weight: 700;
      overflow-wrap: anywhere;
    }
    @media (max-width: 700px) {
      .sp-details {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class PaymentAuditExportStatusPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly reporting = inject(ReportingService);

  protected readonly job = signal<PaymentAuditExportJob | null>(null);
  protected readonly notFound = signal(false);

  constructor() {
    this.reporting
      .getExport(this.route.snapshot.paramMap.get('exportId') ?? '')
      .subscribe((job) => {
        this.job.set(job);
        this.notFound.set(job === null);
      });
  }
}
