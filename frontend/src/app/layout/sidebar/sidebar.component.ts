import { Component, inject, input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { BrandingSwitcherComponent } from '../../core/branding/branding-switcher.component';
import { BrandingService } from '../../core/branding/branding.service';
import { NavigationService } from '../navigation/navigation.service';
import { RoleSimulatorComponent } from '../role-simulator/role-simulator.component';

@Component({
  selector: 'sp-sidebar',
  imports: [
    BrandingSwitcherComponent,
    MatIconModule,
    MatListModule,
    RouterLink,
    RouterLinkActive,
    RoleSimulatorComponent,
  ],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  readonly collapsed = input(false);

  protected readonly navigation = inject(NavigationService);
  protected readonly branding = inject(BrandingService);

  protected sectionFor(route: string): string {
    if (route === '/') {
      return 'Vue d’ensemble';
    }

    if (route.startsWith('/payments') || route.startsWith('/reporting')) {
      return 'Paiements';
    }

    if (route.startsWith('/customers') || route.startsWith('/partners')) {
      return 'Référentiels';
    }

    if (route.startsWith('/accounting') || route.startsWith('/incidents')) {
      return 'Opérations';
    }

    return 'Système';
  }

  protected showSection(index: number): boolean {
    const items = this.navigation.items();

    if (index === 0) {
      return true;
    }

    return this.sectionFor(items[index]!.route) !== this.sectionFor(items[index - 1]!.route);
  }
}
