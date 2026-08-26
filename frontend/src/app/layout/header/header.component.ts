import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AuthenticationService } from '../../core/auth/authentication.service';

@Component({
  selector: 'sp-header',
  imports: [MatIconModule, MatToolbarModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly authentication = inject(AuthenticationService);
}
