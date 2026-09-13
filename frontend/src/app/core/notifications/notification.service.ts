import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  SpSnackbarComponent,
  SpSnackbarData,
  SpSnackbarStatus,
} from '../../shared/components/snackbar/sp-snackbar.component';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly snackBar = inject(MatSnackBar);

  info(message: string, title = 'Information'): void {
    this.open({ title, message, status: 'info' });
  }

  success(message: string, title = 'Succès'): void {
    this.open({ title, message, status: 'success' });
  }

  warning(message: string, title = 'Attention'): void {
    this.open({ title, message, status: 'warning' });
  }

  error(message: string, title = 'Erreur'): void {
    this.open({ title, message, status: 'error' });
  }

  open(data: SpSnackbarData): void {
    this.snackBar.openFromComponent(SpSnackbarComponent, {
      data,
      duration: durationFor(data.status),
      horizontalPosition: 'end',
      verticalPosition: 'top',
      panelClass: ['sp-snackbar-panel', `sp-snackbar-panel--${data.status}`],
    });
  }
}

function durationFor(status: SpSnackbarStatus): number {
  return status === 'error' || status === 'warning' ? 7000 : 4500;
}
