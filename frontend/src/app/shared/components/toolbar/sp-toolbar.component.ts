import { Component, input } from '@angular/core';

@Component({
  selector: 'sp-toolbar',
  templateUrl: './sp-toolbar.component.html',
  styleUrl: './sp-toolbar.component.scss',
})
export class SpToolbarComponent {
  readonly title = input.required<string>();
  readonly description = input<string>();
}
