import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { catchError, EMPTY, finalize } from 'rxjs';

import {
  SpDataTableColumn,
  SpDataTableComponent,
  SpDataTableRow,
} from '../../../shared/components/data-table/sp-data-table.component';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpLoadingComponent } from '../../../shared/components/loading/sp-loading.component';
import { PartnersService } from '../services/partners.service';

@Component({
  selector: 'sp-partner-audit',
  imports: [
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    ReactiveFormsModule,
    SpButtonComponent,
    SpDataTableComponent,
    SpLoadingComponent,
  ],
  templateUrl: './partner-audit.component.html',
  styleUrl: './partner-audit.component.scss',
})
export class PartnerAuditComponent implements OnInit {
  readonly partnerId = input.required<string>();
  protected readonly loading = signal(false);
  protected readonly page = signal(0);
  protected readonly size = signal(25);
  protected readonly totalElements = signal(0);
  protected readonly entries = signal<readonly SpDataTableRow[]>([]);
  protected readonly columns: readonly SpDataTableColumn[] = [
    { key: 'action', label: 'Action' },
    { key: 'result', label: 'Résultat' },
    { key: 'actor', label: 'Acteur' },
    { key: 'correlationId', label: 'Correlation ID' },
    { key: 'details', label: 'Détails' },
    { key: 'occurredAt', label: 'Date de l’événement' },
  ];

  private readonly formBuilder = inject(FormBuilder);
  private readonly partners = inject(PartnersService);
  private readonly today = new Date();
  private readonly monthAgo = new Date(
    this.today.getFullYear(),
    this.today.getMonth() - 1,
    this.today.getDate(),
  );

  protected readonly form = this.formBuilder.nonNullable.group({
    from: [this.toDateInput(this.monthAgo), Validators.required],
    to: [this.toDateInput(this.today), Validators.required],
  });
  protected invalidPeriod(): boolean {
    const { from, to } = this.form.getRawValue();
    return Boolean(from && to && from > to);
  }

  ngOnInit(): void {
    this.load();
  }

  protected search(): void {
    if (this.form.invalid || this.invalidPeriod()) {
      this.form.markAllAsTouched();
      return;
    }
    this.page.set(0);
    this.load();
  }

  protected changePage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.size.set(event.pageSize);
    this.load();
  }

  private load(): void {
    const { from, to } = this.form.getRawValue();
    this.loading.set(true);
    this.partners
      .getAuditTrail(this.partnerId(), {
        from: new Date(`${from}T00:00:00.000`).toISOString(),
        to: new Date(`${to}T23:59:59.999`).toISOString(),
        page: this.page(),
        size: this.size(),
      })
      .pipe(
        catchError(() => EMPTY),
        finalize(() => this.loading.set(false)),
      )
      .subscribe((auditPage) => {
        this.totalElements.set(auditPage.totalElements);
        this.entries.set(
          auditPage.items.map((item) => ({
            action: item.action,
            result: item.result,
            actor: item.actorId,
            correlationId: item.correlationId,
            details: item.details,
            occurredAt: item.occurredAt.toLocaleString('fr-CA'),
          })),
        );
      });
  }

  private toDateInput(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
