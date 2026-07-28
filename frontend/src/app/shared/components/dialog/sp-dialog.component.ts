import { Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';

import { SpButtonComponent } from '../button/sp-button.component';

export interface SpDialogData {
  readonly title: string;
  readonly message: string;
  readonly confirmLabel?: string;
  readonly cancelLabel?: string;
  readonly destructive?: boolean;
}

@Component({
  selector: 'sp-dialog',
  imports: [MatDialogModule, MatIconModule, SpButtonComponent],
  templateUrl: './sp-dialog.component.html',
  styleUrl: './sp-dialog.component.scss',
})
export class SpDialogComponent {
  protected readonly data = inject<SpDialogData>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<SpDialogComponent, boolean>);

  protected close(result: boolean): void {
    this.dialogRef.close(result);
  }
}
