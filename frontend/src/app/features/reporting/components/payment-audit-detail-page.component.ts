import { DatePipe, KeyValuePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentAuditRecord } from '../models/reporting';
import { ReportingService } from '../services/reporting.service';

@Component({
  selector: 'sp-payment-audit-detail-page',
  imports: [
    DatePipe,
    KeyValuePipe,
    RouterLink,
    SpCardComponent,
    MockContentStateComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/reporting/audit-records">← Retour aux preuves d'audit</a>

      @if (record(); as audit) {
        <sp-toolbar [title]="audit.action" description="Enregistrement d'audit immuable." />

        <div class="sp-grid">
          <sp-card title="Événement">
            <dl class="sp-details">
              <div><dt>Audit ID</dt><dd>{{ audit.auditId }}</dd></div>
              <div><dt>Date</dt><dd>{{ audit.occurredAt | date: 'dd/MM/yyyy HH:mm:ss.SSS' }}</dd></div>
              <div><dt>Résultat</dt><dd>{{ audit.result }}</dd></div>
              <div><dt>Reason code</dt><dd>{{ audit.reasonCode }}</dd></div>
              <div><dt>Source</dt><dd>{{ audit.sourceSystem }}</dd></div>
              <div><dt>Correlation ID</dt><dd>{{ audit.correlationId }}</dd></div>
            </dl>
          </sp-card>

          <sp-card title="Acteur / cible">
            <dl class="sp-details">
              <div><dt>Acteur</dt><dd>{{ audit.actorType }} / {{ audit.actorId }}</dd></div>
              <div><dt>Rôles</dt><dd>{{ audit.actorRoles.join(', ') || '—' }}</dd></div>
              <div><dt>Cible</dt><dd>{{ audit.targetType }} / {{ audit.targetId }}</dd></div>
              <div><dt>Payment</dt><dd>{{ audit.paymentReference ?? audit.paymentId ?? '—' }}</dd></div>
              <div><dt>Avant</dt><dd>{{ audit.beforeState ?? '—' }}</dd></div>
              <div><dt>Après</dt><dd>{{ audit.afterState ?? '—' }}</dd></div>
            </dl>
          </sp-card>
        </div>

        <sp-card title="Métadonnées minimisées">
          <dl class="sp-details">
            @for (item of audit.metadata | keyvalue; track item.key) {
              <div><dt>{{ item.key }}</dt><dd>{{ item.value }}</dd></div>
            }
          </dl>
        </sp-card>

        <sp-card title="Preuve d'intégrité">
          <p><strong>{{ audit.integrityScheme }}</strong></p>
          <p class="sp-wrap">{{ audit.integrityValue }}</p>
        </sp-card>
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Preuve introuvable"
          message="Cet auditId n'existe pas dans le jeu de données de démonstration."
        />
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-grid{display:grid;grid-template-columns:1fr 1fr;gap:var(--sp-space-3)}
    .sp-details{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3);margin:0}
    .sp-details div{display:grid;gap:.2rem}
    .sp-details dt{color:var(--mat-sys-on-surface-variant);font-size:.85rem}
    .sp-details dd{margin:0;font-weight:700;overflow-wrap:anywhere}
    .sp-wrap{overflow-wrap:anywhere}
    @media(max-width:800px){.sp-grid,.sp-details{grid-template-columns:1fr}}
  `,
})
export class PaymentAuditDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly reporting = inject(ReportingService);

  protected readonly record = signal<PaymentAuditRecord | null>(null);
  protected readonly notFound = signal(false);

  constructor() {
    this.reporting
      .getAudit(this.route.snapshot.paramMap.get('auditId') ?? '')
      .subscribe((record) => {
        this.record.set(record);
        this.notFound.set(record === null);
      });
  }
}
