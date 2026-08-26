import { Component, input } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'sp-loading',
  imports: [MatProgressSpinnerModule],
  templateUrl: './sp-loading.component.html',
  styleUrl: './sp-loading.component.scss',
})
export class SpLoadingComponent {
  readonly label = input('Chargement en cours');
  readonly diameter = input(40);
}
