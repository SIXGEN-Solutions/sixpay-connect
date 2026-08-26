import { Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type SpButtonVariant = 'primary' | 'secondary' | 'danger';

@Component({
  selector: 'sp-button',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './sp-button.component.html',
  styleUrl: './sp-button.component.scss',
})
export class SpButtonComponent {
  readonly disabled = input(false);
  readonly icon = input<string>();
  readonly type = input<'button' | 'submit'>('button');
  readonly variant = input<SpButtonVariant>('primary');
  readonly accessibleLabel = input<string>();
  readonly buttonClick = output<MouseEvent>();
}
