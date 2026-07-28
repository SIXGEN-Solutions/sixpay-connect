import { Component, computed, signal } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import {
  SpDataTableColumn,
  SpDataTableComponent,
  SpDataTableRow,
} from '../../../shared/components/data-table/sp-data-table.component';
import {
  SpDialogComponent,
  SpDialogData,
} from '../../../shared/components/dialog/sp-dialog.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { SpNotificationComponent } from '../../../shared/components/notification/sp-notification.component';
import { SpSearchFieldComponent } from '../../../shared/components/search-field/sp-search-field.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';

@Component({
  selector: 'sp-design-system-catalog',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    SpButtonComponent,
    SpCardComponent,
    SpDataTableComponent,
    SpFormErrorComponent,
    SpLoadingComponent,
    SpNotificationComponent,
    SpSearchFieldComponent,
    SpToolbarComponent,
  ],
  templateUrl: './design-system-catalog.component.html',
  styleUrl: './design-system-catalog.component.scss',
})
export class DesignSystemCatalogComponent {
  private readonly dialog = new MatDialog();
  protected readonly searchTerm = signal('');
  protected readonly showSuccess = signal(true);
  protected readonly showRequiredError = signal(true);

  protected readonly auditColumns: readonly SpDataTableColumn[] = [
    { key: 'action', label: 'Action' },
    { key: 'result', label: 'Résultat' },
    { key: 'actor', label: 'Acteur' },
    { key: 'occurredAt', label: 'Date' },
  ];

  private readonly auditRows: readonly SpDataTableRow[] = [
    {
      action: 'PARTNER_CREATED',
      result: 'SUCCESS',
      actor: 'admin@sixpay',
      occurredAt: '28 juillet 2026, 14:32',
    },
    {
      action: 'PARTNER_VALIDATED',
      result: 'SUCCESS',
      actor: 'validator@sixpay',
      occurredAt: '28 juillet 2026, 15:06',
    },
    {
      action: 'THRESHOLD_UPDATED',
      result: 'WARNING',
      actor: 'admin@sixpay',
      occurredAt: '28 juillet 2026, 15:24',
    },
  ];

  protected readonly filteredAuditRows = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase();
    return term
      ? this.auditRows.filter((row) =>
          Object.values(row).some((value) => String(value).toLocaleLowerCase().includes(term)),
        )
      : this.auditRows;
  });

  protected openConfirmation(): void {
    const data: SpDialogData = {
      title: 'Confirmer la validation',
      message: 'Voulez-vous valider ce partenaire ? Cette action sera journalisée.',
      confirmLabel: 'Valider',
    };

    this.dialog.open(SpDialogComponent, {
      data,
      maxWidth: '32rem',
      width: 'calc(100% - 2rem)',
      restoreFocus: true,
    });
  }
}
