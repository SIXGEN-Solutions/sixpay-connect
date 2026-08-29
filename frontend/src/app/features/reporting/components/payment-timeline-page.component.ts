import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PaymentTimelineQuery } from '../models/reporting-query';
import { TIMELINE_CATEGORIES, TimelineCategoryResponse } from '../models/reporting.response';
import { PaymentTimelinePage } from '../models/reporting';
import { ReportingService } from '../services/reporting.service';

@Component({
  selector: 'sp-payment-timeline-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    MockContentStateComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <a routerLink="/reporting">← Retour Audit / Reporting</a>
      <sp-toolbar
        [title]="'Timeline ' + paymentId"
        description="Lifecycle Payment ordonné et normalisé."
      />

      <sp-card title="Filtres">
        <form class="sp-filter" [formGroup]="form" (ngSubmit)="search()">
          <mat-form-field appearance="outline">
            <mat-label>Catégorie</mat-label>
            <mat-select formControlName="category">
              <mat-option value="">Toutes</mat-option>
              @for (category of categories; track category) {
                <mat-option [value]="category">{{ category }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <sp-button type="submit" icon="search">Filtrer</sp-button>
        </form>
      </sp-card>

      <sp-card title="Événements">
        @if (page(); as currentPage) {
          @if (currentPage.items.length === 0) {
            <sp-mock-content-state
              kind="empty"
              title="Aucun événement"
              message="Aucune timeline n'est disponible pour ce Payment ID."
            />
          } @else {
            <ol class="sp-timeline">
              @for (entry of currentPage.items; track entry.timelineEntryId) {
                <li>
                  <div class="sp-marker"></div>
                  <div>
                    <div class="sp-row">
                      <strong>{{ entry.eventType }}</strong>
                      <span>{{ entry.category }}</span>
                    </div>
                    <p>{{ entry.fromState ?? '—' }} → {{ entry.toState ?? '—' }}</p>
                    <p>
                      {{ entry.sourceSystem }} · {{ entry.result ?? '—' }} · v{{
                        entry.aggregateVersion
                      }}
                    </p>
                    <small>{{ entry.occurredAt | date: 'dd/MM/yyyy HH:mm:ss.SSS' }}</small>
                  </div>
                </li>
              }
            </ol>

            <div class="sp-pagination">
              <sp-button
                icon="arrow_back"
                [disabled]="cursorHistory().length === 0"
                (buttonClick)="previous()"
              >
                Précédent
              </sp-button>
              <span>Snapshot {{ currentPage.snapshotAt | date: 'dd/MM/yyyy HH:mm:ss' }}</span>
              <sp-button
                icon="arrow_forward"
                [disabled]="!currentPage.hasMore"
                (buttonClick)="next()"
              >
                Suivant
              </sp-button>
            </div>
          }
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }
    .sp-filter {
      display: flex;
      gap: var(--sp-space-3);
      align-items: center;
      flex-wrap: wrap;
    }
    .sp-timeline {
      list-style: none;
      padding: 0;
      display: grid;
      gap: var(--sp-space-3);
    }
    .sp-timeline li {
      display: grid;
      grid-template-columns: auto 1fr;
      gap: var(--sp-space-3);
    }
    .sp-marker {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--mat-sys-primary);
      margin-top: 0.35rem;
    }
    .sp-row {
      display: flex;
      justify-content: space-between;
      gap: var(--sp-space-3);
    }
    .sp-timeline p {
      margin: 0.25rem 0;
      color: var(--mat-sys-on-surface-variant);
    }
    .sp-pagination {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: var(--sp-space-3);
      margin-top: var(--sp-space-3);
    }
    @media (max-width: 700px) {
      .sp-row,
      .sp-pagination {
        align-items: flex-start;
        flex-direction: column;
      }
    }
  `,
})
export class PaymentTimelinePageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder);
  private readonly reporting = inject(ReportingService);

  protected readonly paymentId = this.route.snapshot.paramMap.get('paymentId') ?? '';
  protected readonly categories = TIMELINE_CATEGORIES;
  protected readonly page = signal<PaymentTimelinePage | null>(null);
  protected readonly cursorHistory = signal<string[]>([]);
  protected readonly form = this.formBuilder.nonNullable.group({ category: [''] });

  constructor() {
    this.search();
  }

  protected search(cursor?: string): void {
    const category = this.form.getRawValue().category;
    const query: PaymentTimelineQuery = {
      size: 3,
      ...(category ? { category: category as TimelineCategoryResponse } : {}),
      ...(cursor ? { cursor } : {}),
    };
    this.reporting.timeline(this.paymentId, query).subscribe((page) => this.page.set(page));
  }

  protected next(): void {
    const cursor = this.page()?.nextCursor;
    if (!cursor) {
      return;
    }
    this.cursorHistory.update((history) => [...history, '0']);
    this.search(cursor);
  }

  protected previous(): void {
    if (this.cursorHistory().length === 0) {
      return;
    }
    this.cursorHistory.set([]);
    this.search();
  }
}
