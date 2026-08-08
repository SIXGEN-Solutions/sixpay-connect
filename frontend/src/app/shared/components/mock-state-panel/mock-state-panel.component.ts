import { Component, inject } from '@angular/core';
import { MatButtonToggleModule } from '@angular/material/button-toggle';

import { MockScenarioService } from '../../../core/mock/mock-scenario.service';

@Component({
  selector: 'sp-mock-state-panel',
  imports: [MatButtonToggleModule],
  template: `
    <aside class="sp-mock-state-panel" aria-label="Scénario de démonstration">
      <span>Scénario mock</span>
      <mat-button-toggle-group
        [value]="scenario.scenario()"
        (change)="scenario.setScenario($event.value)"
        aria-label="État des données mockées"
      >
        <mat-button-toggle value="success">Succès</mat-button-toggle>
        <mat-button-toggle value="loading">Chargement</mat-button-toggle>
        <mat-button-toggle value="empty">Vide</mat-button-toggle>
        <mat-button-toggle value="error">Erreur</mat-button-toggle>
      </mat-button-toggle-group>
    </aside>
  `,
  styles: `
    .sp-mock-state-panel {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--sp-space-3);
      padding: var(--sp-space-2) var(--sp-space-3);
      border: 1px dashed var(--mat-sys-outline-variant);
      border-radius: 12px;
      background: var(--mat-sys-surface);
    }

    .sp-mock-state-panel span {
      font-weight: 700;
      font-size: 0.85rem;
    }

    @media (max-width: 768px) {
      .sp-mock-state-panel {
        align-items: stretch;
        flex-direction: column;
      }

      mat-button-toggle-group {
        display: grid;
        grid-template-columns: repeat(2, minmax(0, 1fr));
      }
    }
  `,
})
export class MockStatePanelComponent {
  protected readonly scenario = inject(MockScenarioService);
}
