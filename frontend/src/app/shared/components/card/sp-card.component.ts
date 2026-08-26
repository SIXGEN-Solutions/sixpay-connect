import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'sp-card',
  imports: [MatCardModule],
  templateUrl: './sp-card.component.html',
  styleUrl: './sp-card.component.scss',
})
export class SpCardComponent {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
