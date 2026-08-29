import { DatePipe } from '@angular/common';
import { Component, computed, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpFormErrorComponent } from '../../../shared/components/sp-form-error.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { PartnerPage, PartnerSummary } from '../models/partners';
import { PartnerAccessPolicy } from '../security/partner-access.policy';
import { PartnersService } from '../services/partners.service';

const UUID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

const DEFAULT_PAGE_SIZE = 20;

@Component({
  selector: 'sp-partner-access-page',
  imports: [
    DatePipe,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    ReactiveFormsModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    MockContentStateComponent,
    MockStatePanelComponent,
    SpFormErrorComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Partenaires"
        description="Consultez et administrez les partenaires SIXPAY."
      >
        @if (partnerAccess.canCreate()) {
          <a routerLink="/partners/create" class="sp-link-action"> Créer un partenaire </a>
        }
      </sp-toolbar>

      @if (backendMode.usesMock) {
        <sp-mock-state-panel />
      }

      <sp-card title="Catalogue des partenaires">
        <div class="sp-catalog-toolbar">
          <p class="sp-catalog-description">
            Sélectionnez un partenaire pour ouvrir sa fiche complète.
          </p>

          <mat-form-field appearance="outline" class="sp-page-size">
            <mat-label>Éléments par page</mat-label>
            <mat-select [value]="pageSize()" (selectionChange)="changePageSize($event.value)">
              <mat-option [value]="10">10</mat-option>
              <mat-option [value]="20">20</mat-option>
              <mat-option [value]="50">50</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        @if (loading()) {
          <sp-mock-content-state
            kind="loading"
            title="Chargement des partenaires"
            message="Le catalogue Partner est en cours de chargement."
          />
        } @else if (error()) {
          <sp-mock-content-state
            kind="error"
            title="Catalogue indisponible"
            message="Impossible de charger les partenaires pour le moment."
          />
          <div class="sp-state-action">
            <sp-button type="button" icon="refresh" (buttonClick)="reload()"> Réessayer </sp-button>
          </div>
        } @else if (page(); as currentPage) {
          @if (currentPage.items.length === 0) {
            <sp-mock-content-state
              kind="empty"
              title="Aucun partenaire"
              message="Aucun partenaire n'est disponible dans le catalogue."
            />
          } @else {
            <div class="sp-result-meta">
              <span>
                {{ firstVisibleItem() }}–{{ lastVisibleItem() }} sur {{ currentPage.totalElements }}
              </span>
              <span> Page {{ currentPage.page + 1 }} sur {{ currentPage.totalPages }} </span>
            </div>

            <div class="sp-table-scroll">
              <table class="sp-table">
                <thead>
                  <tr>
                    <th>Nom</th>
                    <th>Statut</th>
                    <th>Transactions</th>
                    <th>Contact technique</th>
                    <th>Créé le</th>
                    <th>Mis à jour</th>
                  </tr>
                </thead>
                <tbody>
                  @for (partner of currentPage.items; track partner.id) {
                    <tr
                      class="sp-partner-row"
                      tabindex="0"
                      role="link"
                      [attr.aria-label]="'Ouvrir ' + partner.legalName"
                      (click)="openPartner(partner)"
                      (keydown.enter)="openPartner(partner)"
                    >
                      <td>
                        <strong>{{ partner.legalName }}</strong>
                      </td>
                      <td>
                        <span class="sp-status" [attr.data-status]="partner.status">
                          {{ partner.status }}
                        </span>
                      </td>
                      <td>
                        {{ partner.authorizedTransactionTypes.join(', ') }}
                      </td>
                      <td>
                        <span>{{ partner.technicalContactName }}</span>
                        <small>{{ partner.technicalContactEmail }}</small>
                      </td>
                      <td>{{ partner.createdAt | date: 'dd/MM/yyyy' }}</td>
                      <td>{{ partner.updatedAt | date: 'dd/MM/yyyy HH:mm' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>

            <div class="sp-pagination">
              <sp-button
                type="button"
                icon="arrow_back"
                variant="secondary"
                [disabled]="!canPreviousPage()"
                (buttonClick)="previousPage()"
              >
                Précédent
              </sp-button>

              <span class="sp-page-indicator">
                {{ currentPage.page + 1 }} / {{ currentPage.totalPages }}
              </span>

              <sp-button
                type="button"
                icon="arrow_forward"
                variant="secondary"
                [disabled]="!canNextPage()"
                (buttonClick)="nextPage()"
              >
                Suivant
              </sp-button>
            </div>
          }
        }
      </sp-card>

      <sp-card title="Accès direct par identifiant">
        <p class="sp-support-copy">
          Utilisez cet accès secondaire pour le support ou le diagnostic lorsqu'un identifiant
          Partner est déjà connu.
        </p>

        <form class="sp-access-form" [formGroup]="form" (ngSubmit)="openById()" novalidate>
          <mat-form-field appearance="outline">
            <mat-label>Identifiant Partner</mat-label>
            <input matInput formControlName="partnerId" autocomplete="off" />
            <sp-form-error
              errorId="partner-id-error"
              [message]="
                form.controls.partnerId.touched && form.controls.partnerId.invalid
                  ? 'Saisissez un identifiant UUID valide.'
                  : undefined
              "
            />
          </mat-form-field>

          <sp-button type="submit" icon="search"> Consulter </sp-button>
        </form>
      </sp-card>
    </section>
  `,
  styles: `
    :host,
    .sp-page {
      display: grid;
      gap: var(--sp-space-4);
    }

    .sp-link-action {
      color: var(--sp-color-primary);
      font-weight: 600;
    }

    .sp-catalog-toolbar,
    .sp-result-meta,
    .sp-pagination,
    .sp-access-form {
      display: flex;
      align-items: center;
      gap: var(--sp-space-3);
    }

    .sp-catalog-toolbar,
    .sp-result-meta,
    .sp-pagination {
      justify-content: space-between;
    }

    .sp-catalog-description,
    .sp-support-copy {
      margin: 0;
      color: var(--mat-sys-on-surface-variant);
    }

    .sp-page-size {
      width: 11rem;
    }

    .sp-result-meta {
      margin-bottom: var(--sp-space-3);
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
    }

    .sp-table-scroll {
      overflow-x: auto;
    }

    .sp-table {
      width: 100%;
      border-collapse: collapse;
      min-width: 58rem;
    }

    .sp-table th,
    .sp-table td {
      padding: var(--sp-space-3);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      text-align: left;
      vertical-align: middle;
    }

    .sp-table th {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.8rem;
      font-weight: 700;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .sp-partner-row {
      cursor: pointer;
    }

    .sp-partner-row:hover,
    .sp-partner-row:focus-visible {
      background: var(--mat-sys-surface-container-low);
      outline: none;
    }

    .sp-partner-row td:nth-child(4) {
      display: grid;
      gap: 0.2rem;
    }

    .sp-partner-row small {
      color: var(--mat-sys-on-surface-variant);
    }

    .sp-status {
      display: inline-flex;
      align-items: center;
      min-height: 1.75rem;
      padding: 0 var(--sp-space-2);
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 999px;
      font-size: 0.75rem;
      font-weight: 700;
      white-space: nowrap;
    }

    .sp-status[data-status='ACTIVE'] {
      background: var(--mat-sys-secondary-container);
      color: var(--mat-sys-on-secondary-container);
    }

    .sp-status[data-status='PENDING_VALIDATION'] {
      background: var(--mat-sys-tertiary-container);
      color: var(--mat-sys-on-tertiary-container);
    }

    .sp-status[data-status='SUSPENDED'],
    .sp-status[data-status='REJECTED'] {
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
    }

    .sp-pagination {
      margin-top: var(--sp-space-3);
    }

    .sp-page-indicator {
      color: var(--mat-sys-on-surface-variant);
      font-size: 0.875rem;
      font-weight: 600;
    }

    .sp-state-action {
      display: flex;
      justify-content: center;
      margin-top: var(--sp-space-3);
    }

    .sp-access-form {
      margin-top: var(--sp-space-3);
    }

    .sp-access-form mat-form-field {
      flex: 1;
    }

    @media (max-width: 768px) {
      .sp-catalog-toolbar,
      .sp-result-meta,
      .sp-access-form {
        align-items: stretch;
        flex-direction: column;
      }

      .sp-page-size {
        width: 100%;
      }

      .sp-pagination {
        flex-wrap: wrap;
      }
    }
  `,
})
export class PartnerAccessPageComponent {
  protected readonly partnerAccess = inject(PartnerAccessPolicy);
  protected readonly backendMode = inject(BackendModeService);

  private readonly partners = inject(PartnersService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly formBuilder = inject(FormBuilder);
  private readonly router = inject(Router);

  protected readonly loading = signal(true);
  protected readonly error = signal(false);
  protected readonly page = signal<PartnerPage | null>(null);
  protected readonly pageIndex = signal(0);
  protected readonly pageSize = signal(DEFAULT_PAGE_SIZE);

  protected readonly firstVisibleItem = computed(() => {
    const currentPage = this.page();
    if (!currentPage || currentPage.totalElements === 0) {
      return 0;
    }

    return currentPage.page * currentPage.size + 1;
  });

  protected readonly lastVisibleItem = computed(() => {
    const currentPage = this.page();
    if (!currentPage) {
      return 0;
    }

    return Math.min((currentPage.page + 1) * currentPage.size, currentPage.totalElements);
  });

  protected readonly canPreviousPage = computed(() => (this.page()?.page ?? 0) > 0);

  protected readonly canNextPage = computed(() => {
    const currentPage = this.page();
    return currentPage ? currentPage.page + 1 < currentPage.totalPages : false;
  });

  protected readonly form = this.formBuilder.nonNullable.group({
    partnerId: ['', [Validators.required, Validators.pattern(UUID_PATTERN)]],
  });

  constructor() {
    this.loadPage();
  }

  protected reload(): void {
    this.loadPage();
  }

  protected previousPage(): void {
    if (!this.canPreviousPage()) {
      return;
    }

    this.pageIndex.update((page) => page - 1);
    this.loadPage();
  }

  protected nextPage(): void {
    if (!this.canNextPage()) {
      return;
    }

    this.pageIndex.update((page) => page + 1);
    this.loadPage();
  }

  protected changePageSize(size: number): void {
    this.pageSize.set(size);
    this.pageIndex.set(0);
    this.loadPage();
  }

  protected openPartner(partner: PartnerSummary): void {
    void this.router.navigate(['/partners', partner.id]);
  }

  protected openById(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    void this.router.navigate(['/partners', this.form.controls.partnerId.value]);
  }

  private loadPage(): void {
    this.loading.set(true);
    this.error.set(false);

    this.partners
      .search({
        page: this.pageIndex(),
        size: this.pageSize(),
      })
      .pipe(
        finalize(() => this.loading.set(false)),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (page) => {
          this.page.set(page);

          if (page.totalPages > 0 && page.page >= page.totalPages) {
            this.pageIndex.set(page.totalPages - 1);
          }
        },
        error: () => {
          this.page.set(null);
          this.error.set(true);
        },
      });
  }
}
