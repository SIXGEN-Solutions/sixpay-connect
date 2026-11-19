import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { AuthenticationService } from '../../../core/auth/authentication.service';
import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PaymentsService } from '../../payments/services/payments.service';
import { PaymentSummary } from '../../payments/models/payments';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';

@Component({
  selector: 'sp-dashboard-page',
  imports: [CurrencyPipe, MatIconModule, RouterLink, SpCardComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent implements OnInit {
  protected readonly authentication = inject(AuthenticationService);

  private readonly backendMode = inject(BackendModeService);
  private readonly paymentsService = inject(PaymentsService);

  protected readonly recentPayments = signal<readonly PaymentSummary[]>([]);
  protected readonly loadingPayments = signal(true);
  protected readonly paymentsError = signal(false);

  ngOnInit(): void {
    // Dashboard opérationnel : données API uniquement.
    if (!this.backendMode.usesApi) {
      this.loadingPayments.set(false);
      return;
    }

    this.paymentsService.search({
      sort: 'CREATED_AT_DESC',
      size: 10,
    }).subscribe({
      next: (page) => {
        this.recentPayments.set(page.items);
        this.paymentsError.set(false);
        this.loadingPayments.set(false);
      },
      error: () => {
        this.recentPayments.set([]);
        this.paymentsError.set(true);
        this.loadingPayments.set(false);
      },
    });
  }
}