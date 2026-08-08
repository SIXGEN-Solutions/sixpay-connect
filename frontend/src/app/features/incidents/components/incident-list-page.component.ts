import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';
import { SpButtonComponent } from '../../../shared/components/button/sp-button.component';
import { SpCardComponent } from '../../../shared/components/card/sp-card.component';
import { MockContentStateComponent } from '../../../shared/components/mock-content-state/mock-content-state.component';
import { MockStatePanelComponent } from '../../../shared/components/mock-state-panel/mock-state-panel.component';
import { SpToolbarComponent } from '../../../shared/components/toolbar/sp-toolbar.component';
import { IncidentDetail } from '../models/incidents';
import { IncidentsService } from '../services/incidents.service';

@Component({
  selector: 'sp-incident-list-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    RouterLink,
    SpButtonComponent,
    SpCardComponent,
    MockContentStateComponent,
    MockStatePanelComponent,
    SpToolbarComponent,
  ],
  template: `
    <section class="sp-page">
      <sp-toolbar
        title="Incidents"
        description="Vue opérationnelle mockée des anomalies et dégradations."
      />
      <sp-mock-state-panel />

      <sp-card title="Filtres">
        <form class="sp-filter-grid" [formGroup]="form" (ngSubmit)="search()">
          <mat-form-field appearance="outline">
            <mat-label>Sévérité</mat-label>
            <mat-select formControlName="severity">
              <mat-option value="">Toutes</mat-option>
              @for (severity of ['LOW','MEDIUM','HIGH','CRITICAL']; track severity) {
                <mat-option [value]="severity">{{ severity }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Statut</mat-label>
            <mat-select formControlName="status">
              <mat-option value="">Tous</mat-option>
              @for (status of ['OPEN','INVESTIGATING','MONITORING','RESOLVED','CLOSED']; track status) {
                <mat-option [value]="status">{{ status }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Composant</mat-label>
            <input matInput formControlName="component" />
          </mat-form-field>

          <div class="sp-actions">
            <sp-button type="submit" icon="search">Rechercher</sp-button>
            <sp-button type="button" icon="restart_alt" (buttonClick)="reset()">Réinitialiser</sp-button>
          </div>
        </form>
      </sp-card>

      <sp-card title="Incidents">
        @switch (scenario.scenario()) {
          @case ('loading') {
            <sp-mock-content-state kind="loading" title="Chargement des incidents" message="Simulation de consultation en cours." />
          }
          @case ('empty') {
            <sp-mock-content-state kind="empty" title="Aucun incident" message="Aucun incident ne correspond aux critères." />
          }
          @case ('error') {
            <sp-mock-content-state kind="error" title="Incidents indisponibles" message="Erreur simulée sans appel backend." />
          }
          @default {
            @if (incidents().length === 0) {
              <sp-mock-content-state kind="empty" title="Aucun incident" message="Aucun incident ne correspond aux critères." />
            } @else {
              <div class="sp-table-scroll">
                <table class="sp-table">
                  <thead>
                    <tr>
                      <th>ID</th>
                      <th>Sévérité</th>
                      <th>Composant</th>
                      <th>Résumé</th>
                      <th>Ouvert le</th>
                      <th>Mis à jour</th>
                      <th>Statut</th>
                    </tr>
                  </thead>
                  <tbody>
                    @for (incident of incidents(); track incident.incidentId) {
                      <tr>
                        <td><a [routerLink]="[incident.incidentId]">{{ incident.incidentId }}</a></td>
                        <td>{{ incident.severity }}</td>
                        <td>{{ incident.component }}</td>
                        <td>{{ incident.summary }}</td>
                        <td>{{ incident.openedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                        <td>{{ incident.updatedAt | date: 'dd/MM/yyyy HH:mm:ss' }}</td>
                        <td>{{ incident.status }}</td>
                      </tr>
                    }
                  </tbody>
                </table>
              </div>
            }
          }
        }
      </sp-card>
    </section>
  `,
  styles: `
    :host,.sp-page{display:grid;gap:var(--sp-space-4)}
    .sp-filter-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:var(--sp-space-3)}
    .sp-actions{display:flex;gap:var(--sp-space-2);align-items:center}
    .sp-table-scroll{overflow-x:auto}
    .sp-table{width:100%;border-collapse:collapse}
    .sp-table th,.sp-table td{padding:var(--sp-space-2);text-align:left;border-bottom:1px solid var(--mat-sys-outline-variant);white-space:nowrap}
    @media(max-width:850px){.sp-filter-grid{grid-template-columns:1fr}}
  `,
})
export class IncidentListPageComponent {
  protected readonly scenario = inject(MockScenarioService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly service = inject(IncidentsService);

  protected readonly incidents = signal<readonly IncidentDetail[]>([]);

  protected readonly form = this.formBuilder.nonNullable.group({
    severity: [''],
    status: [''],
    component: [''],
  });

  constructor() {
    this.search();
  }

  protected search(): void {
    const value = this.form.getRawValue();

    this.service
      .search({
        ...(value.severity ? { severity: value.severity as IncidentDetail['severity'] } : {}),
        ...(value.status ? { status: value.status as IncidentDetail['status'] } : {}),
        ...(value.component.trim() ? { component: value.component.trim() } : {}),
      })
      .subscribe((incidents) => this.incidents.set(incidents));
  }

  protected reset(): void {
    this.form.reset({ severity: '', status: '', component: '' });
    this.search();
  }
}
