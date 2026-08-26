import { Component, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'sp-form-error',
  imports: [MatIconModule],
  template: `
    @if (message()) {
      <p class="sp-form-error" [id]="errorId()" role="alert" aria-live="polite">
        <mat-icon aria-hidden="true">error</mat-icon>
        <span>{{ message() }}</span>
      </p>
    }
  `,
  styles: `
    .sp-form-error {
      display: flex;
      align-items: center;
      gap: var(--sp-space-1);
      margin: var(--sp-space-1) 0 0;
      color: var(--sp-color-error);
      font-size: var(--sp-font-size-sm);
    }

    mat-icon {
      width: 1rem;
      height: 1rem;
      font-size: 1rem;
    }
  `,
})
export class SpFormErrorComponent {
  readonly errorId = input.required<string>();
  readonly message = input<string>();
}
