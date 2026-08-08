import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { AuthenticationService } from '../../core/auth/authentication.service';
import { RoleSimulatorComponent } from '../role-simulator/role-simulator.component';

@Component({
  selector: 'sp-sidebar',
  imports: [MatIconModule, MatListModule, RouterLink, RouterLinkActive, RoleSimulatorComponent],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss',
})
export class SidebarComponent {
  protected readonly authentication = inject(AuthenticationService);
}
