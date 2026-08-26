import { Component } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-dashboard-page',
  imports: [MatIconModule, RouterLink, SpCardComponent, SpToolbarComponent],
  templateUrl: './dashboard-page.component.html',
  styleUrl: './dashboard-page.component.scss',
})
export class DashboardPageComponent {
  protected readonly recentPayments = [
    { reference: 'PAY-2026-0001842', customer: 'CAMEROUN SERVICES SARL', amount: '125 000 XAF', status: 'SUCCESS' },
    { reference: 'PAY-2026-0001841', customer: 'ETS MBARGA & FILS', amount: '62 500 XAF', status: 'PROCESSING' },
    { reference: 'PAY-2026-0001840', customer: 'AFRICA LOGISTICS SA', amount: '350 000 XAF', status: 'FAILED' },
  ];
}
