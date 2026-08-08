import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { IncidentDetail } from '../models/incidents';
import { IncidentsService } from '../services/incidents.service';

@Component({
  selector: 'sp-incident-detail-page',
  imports: [DatePipe, RouterLink, SpCardComponent, MockContentStateComponent, SpToolbarComponent],
  template: `
    <section class="sp-page">
      <a routerLink="/incidents">← Retour aux incidents</a>

      @if (incident(); as currentIncident) {
        <sp-toolbar [title]="currentIncident.incidentId" description="Détail d'un incident opérationnel mocké." />

        <div class="sp-grid">
          <sp-card title="Résumé">
            <p><strong>{{ currentIncident.summary }}</strong></p>
            <p>{{ currentIncident.description }}</p>
            <p>{{ currentIncident.severity }} · {{ currentIncident.status }}</p>
          </sp-card>

          <sp-card title="Impact">
            <p>{{ currentIncident.impact }}</p>
            <p>Composant : <strong>{{ currentIncident.component }}</strong></p>
          </sp-card>
        </div>

        <sp-card title="Corrélations">
          <dl class="sp-details">
            <div>
              <dt>Lot comptable</dt>
              <dd>
                @if (currentIncident.accountingBatchId) {
                  <a [routerLink]="['/accounting/batches', currentIncident.accountingBatchId]">
                    {{ currentIncident.accountingBatchId }}
                  </a>
                } @else {
                  —
                }
              </dd>
            </div>
            <div>
              <dt>Payment</dt>
              <dd>
                @if (currentIncident.paymentId) {
                  <a [routerLink]="['/payments', currentIncident.paymentId]">
                    {{ currentIncident.paymentReference ?? currentIncident.paymentId }}
                  </a>
                } @else {
                  —
                }
              </dd>
            </div>
            <div><dt>Correlation ID</dt><dd>{{ currentIncident.correlationId ?? '—' }}</dd></div>
            <div><dt>Ouvert</dt><dd>{{ currentIncident.openedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</dd></div>
          </dl>
        </sp-card>

        <sp-card title="Chronologie">
          <ul class="sp-events">
            @for (event of currentIncident.timeline; track event.eventId) {
              <li>
                <strong>{{ event.occurredAt | date: 'dd/MM/yyyy HH:mm:ss' }}</strong>
                <span>{{ event.message }}</span>
                <small>{{ event.actor }}</small>
              </li>
            }
          </ul>
        </sp-card>
      } @else if (notFound()) {
        <sp-mock-content-state
          kind="empty"
          title="Incident introuvable"
          message="Aucun incident ne correspond à cet identifiant."
        />
      }
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-grid{display:grid;grid-template-columns:1fr 1fr;gap:var(--sp-space-3)}
    .sp-details{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:var(--sp-space-3);margin:0}
    .sp-details div{display:grid;gap:.25rem}
    .sp-details dt{color:var(--mat-sys-on-surface-variant);font-size:.85rem}
    .sp-details dd{margin:0;font-weight:700;overflow-wrap:anywhere}
    .sp-events{display:grid;gap:var(--sp-space-3);padding-left:1.25rem}
    .sp-events li{display:grid;gap:.15rem}
    .sp-events small{color:var(--mat-sys-on-surface-variant)}
    @media(max-width:800px){.sp-grid,.sp-details{grid-template-columns:1fr}}
  `,
})
export class IncidentDetailPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(IncidentsService);

  protected readonly incident = signal<IncidentDetail | null>(null);
  protected readonly notFound = signal(false);

  constructor() {
    const incidentId = this.route.snapshot.paramMap.get('incidentId') ?? '';

    this.service.get(incidentId).subscribe((incident) => {
      this.incident.set(incident);
      this.notFound.set(incident === null);
    });
  }
}
