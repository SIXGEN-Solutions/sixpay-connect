import { Injectable, signal } from '@angular/core';

export type MockScenario = 'success' | 'loading' | 'empty' | 'error';

@Injectable({ providedIn: 'root' })
export class MockScenarioService {
  private readonly scenarioState = signal<MockScenario>('success');

  readonly scenario = this.scenarioState.asReadonly();

  setScenario(scenario: MockScenario): void {
    this.scenarioState.set(scenario);
  }
}
