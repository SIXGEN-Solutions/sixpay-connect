import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'sp-mock-content-state',
  imports: [MatIconModule, MatProgressSpinnerModule],
  template: `
    <section class="sp-state" role="status">
      @if (kind() === 'loading') {
        <mat-spinner diameter="36" />
      } @else {
        <mat-icon aria-hidden="true">{{ kind() === 'error' ? 'error_outline' : 'inbox' }}</mat-icon>
      }
      <strong>{{ title() }}</strong>
      <span>{{ message() }}</span>
    </section>
  `,
  styles: `
    .sp-state {
      min-height: 12rem;
      display: grid;
      place-items: center;
      align-content: center;
      gap: var(--sp-space-2);
      text-align: center;
    }

    .sp-state mat-icon {
      width: 2rem;
      height: 2rem;
      font-size: 2rem;
    }

    .sp-state span {
      max-width: 38rem;
      color: var(--mat-sys-on-surface-variant);
    }
  `,
})
export class MockContentStateComponent {
  readonly kind = input.required<'loading' | 'empty' | 'error'>();
  readonly title = input.required<string>();
  readonly message = input.required<string>();
}
