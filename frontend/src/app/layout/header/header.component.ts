import { Component, EventEmitter, inject, Output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AuthenticationService } from '../../core/auth/authentication.service';

@Component({
  selector: 'sp-header',
  imports: [MatButtonModule, MatIconModule, MatMenuModule, MatToolbarModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss',
})
export class HeaderComponent {
  protected readonly authentication = inject(AuthenticationService);

  @Output() readonly menuToggle = new EventEmitter<void>();

  protected profileInitials(username: string): string {
    const tokens = username
      .trim()
      .split(/[\s._-]+/)
      .filter(Boolean);

    if (tokens.length >= 2) {
      return `${tokens[0]![0] ?? ''}${tokens[1]![0] ?? ''}`.toUpperCase();
    }

    return username.slice(0, 2).toUpperCase();
  }

  protected roleLabel(): string {
    const role = this.authentication.roles().values().next().value;

    switch (role) {
      case 'ADMIN':
        return 'Administrateur';
      case 'MANAGER':
        return 'Gestionnaire';
      case 'AUDITOR':
        return 'Auditeur';
      case 'PARTNER':
        return 'Partenaire';
      default:
        return 'Utilisateur';
    }
  }
}
