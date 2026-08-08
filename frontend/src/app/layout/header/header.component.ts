import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AuthenticationService } from '../../core/auth/authentication.service';
import { BrandingService } from '../../core/branding/branding.service';
import { BrandingSwitcherComponent } from '../../core/branding/branding-switcher.component';

@Component({
  selector: 'sp-header',
  imports: [BrandingSwitcherComponent, MatIconModule, MatToolbarModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly authentication = inject(AuthenticationService);
  protected readonly branding = inject(BrandingService);
}
