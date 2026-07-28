import { Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

export type SpNotificationStatus = 'info' | 'success' | 'warning' | 'error';

@Component({
  selector: 'sp-notification',
  imports: [MatIconModule],
  templateUrl: './sp-notification.component.html',
  styleUrl: './sp-notification.component.scss',
})
export class SpNotificationComponent {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly status = input<SpNotificationStatus>('info');
  readonly dismissible = input(false);
  readonly dismissed = output<void>();

  protected icon(): string {
    return {
      info: 'info',
      success: 'check_circle',
      warning: 'warning',
      error: 'error',
    }[this.status()];
  }
}
