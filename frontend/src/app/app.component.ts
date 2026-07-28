import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

declare module 'tslib';

@Component({
  selector: 'sp-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  title = 'frontend';
}