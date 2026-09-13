import { Component, inject } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MAT_SNACK_BAR_DATA, MatSnackBarRef } from '@angular/material/snack-bar';

export type SpSnackbarStatus = 'info' | 'success' | 'warning' | 'error';

export interface SpSnackbarData {
  title: string;
  message: string;
  status: SpSnackbarStatus;
}

@Component({
  selector: 'sp-snackbar',
  imports: [MatIconModule],
  templateUrl: './sp-snackbar.component.html',
  styleUrl: './sp-snackbar.component.scss',
})
export class SpSnackbarComponent {
  readonly data = inject<SpSnackbarData>(MAT_SNACK_BAR_DATA);
  private readonly snackBarRef = inject(MatSnackBarRef<SpSnackbarComponent>);

  protected dismiss(): void {
    this.snackBarRef.dismiss();
  }

  protected icon(): string {
    return {
      info: 'info',
      success: 'check_circle',
      warning: 'warning',
      error: 'error',
    }[this.data.status];
  }
}
