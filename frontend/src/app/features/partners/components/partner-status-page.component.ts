import { Component, inject, OnInit, signal } from '@angular/core';
import { catchError, EMPTY, finalize } from 'rxjs';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpNotificationComponent } from '../../../shared/components/notification/sp-notification.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PartnerStatusView } from '../models/partners';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'sp-partner-status-page',
  imports: [SpCardComponent, SpLoadingComponent, SpNotificationComponent, SpToolbarComponent],
  template: `
    <sp-toolbar
      title="Mon statut Partner"
      description="Statut et informations de connexion de votre organisation."
    />

    @if (loading()) {
      <sp-loading label="Chargement du statut Partner" />
    } @else if (status(); as currentStatus) {
      <sp-card title="Statut et connexion">
        <dl class="sp-status-details">
          <div>
            <dt>Identifiant</dt>
            <dd>{{ currentStatus.partnerId }}</dd>
          </div>
          <div>
            <dt>Statut courant</dt>
            <dd>{{ currentStatus.status }}</dd>
          </div>
          <div>
            <dt>Motif</dt>
            <dd>{{ currentStatus.statusReason || '—' }}</dd>
          </div>
          <div>
            <dt>Chemin API</dt>
            <dd>{{ currentStatus.connection.apiBasePath }}</dd>
          </div>
          <div>
            <dt>Authentification</dt>
            <dd>{{ currentStatus.connection.supportedAuthenticationMethods.join(', ') }}</dd>
          </div>
          <div>
            <dt>Nouvelles transactions</dt>
            <dd>
              {{ currentStatus.connection.newTransactionsAllowed ? 'Autorisées' : 'Bloquées' }}
            </dd>
          </div>
        </dl>
      </sp-card>
    } @else {
      <sp-notification
        title="Statut indisponible"
        message="Votre statut Partner ne peut pas être chargé actuellement."
        status="error"
      />
    }
  `,
  styles: `
    :host {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-status-details {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: var(--sp-space-4);
      margin: 0;
    }
    dt {
      color: var(--sp-color-text-secondary);
      font-size: 0.875rem;
    }
    dd {
      margin: var(--sp-space-1) 0 0;
      overflow-wrap: anywhere;
    }
    @media (max-width: 768px) {
      .sp-status-details {
        grid-template-columns: 1fr;
      }
    }
  `,
})
export class PartnerStatusPageComponent implements OnInit {
  protected readonly loading = signal(true);
  protected readonly status = signal<PartnerStatusView | null>(null);
  private readonly authentication = inject(AuthenticationService);
  private readonly partners = inject(PartnersService);

  ngOnInit(): void {
    const partnerId = this.authentication.subject();
    if (!partnerId) {
      this.loading.set(false);
      return;
    }
    this.partners
      .getStatus(partnerId)
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((status) => this.status.set(status));
  }
}
