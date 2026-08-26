import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { catchError, EMPTY, finalize, forkJoin } from 'rxjs';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpNotificationComponent } from '../../../shared/components/notification/sp-notification.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { Partner, PartnerStatusView } from '../models/partners';
import { PartnersService } from '../services/partners.service';
import { PartnerAccessPolicy } from '../security/partner-access.policy';
import { PartnerAuditComponent } from './partner-audit.component';
import { PartnerLifecycleActionsComponent } from './partner-lifecycle-actions.component';
import { PartnerThresholdFormComponent } from './partner-threshold-form.component';

type DetailState = 'loading' | 'success' | 'not-found' | 'forbidden' | 'error';

@Component({
  selector: 'sp-partner-detail-page',
  imports: [
    DatePipe,
    PartnerAuditComponent,
    PartnerLifecycleActionsComponent,
    PartnerThresholdFormComponent,
    SpCardComponent,
    SpLoadingComponent,
    SpNotificationComponent,
    SpToolbarComponent,
  ],
  templateUrl: './partner-detail-page.component.html',
  styleUrl: './partner-detail-page.component.scss',
})
export class PartnerDetailPageComponent implements OnInit {
  protected readonly partnerAccess = inject(PartnerAccessPolicy);
  protected readonly state = signal<DetailState>('loading');
  protected readonly partner = signal<Partner | null>(null);
  protected readonly status = signal<PartnerStatusView | null>(null);
  protected readonly created = signal(false);
  protected readonly actionSucceeded = signal(false);
  private readonly route = inject(ActivatedRoute);
  private readonly partners = inject(PartnersService);
  private readonly partnerId = this.route.snapshot.paramMap.get('partnerId') ?? '';

  ngOnInit(): void {
    this.created.set(this.route.snapshot.queryParamMap.get('created') === 'true');
    this.load();
  }

  protected refresh(updated?: Partner): void {
    if (updated) {
      this.partner.set(updated);
      this.actionSucceeded.set(true);
    }
    this.load();
  }

  private load(): void {
    this.state.set('loading');
    forkJoin({
      partner: this.partners.get(this.partnerId),
      status: this.partners.getStatus(this.partnerId),
    })
      .pipe(
        catchError((error: unknown) => {
          const status = error instanceof HttpErrorResponse ? error.status : 0;
          this.state.set(status === 404 ? 'not-found' : status === 403 ? 'forbidden' : 'error');
          return EMPTY;
        }),
        finalize(() => {
          if (this.partner() && this.status()) {
            this.state.set('success');
          }
        }),
      )
      .subscribe(({ partner, status }) => {
        this.partner.set(partner);
        this.status.set(status);
        this.state.set('success');
      });
  }
}
