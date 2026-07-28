import { Component, computed, input } from '@angular/core';
import { MatTableModule } from '@angular/material/table';

export interface SpDataTableColumn {
  readonly key: string;
  readonly label: string;
}

export type SpDataTableRow = Readonly<Record<string, unknown>>;

@Component({
  selector: 'sp-data-table',
  imports: [MatTableModule],
  templateUrl: './sp-data-table.component.html',
  styleUrl: './sp-data-table.component.scss',
})
export class SpDataTableComponent {
  readonly caption = input.required<string>();
  readonly columns = input.required<readonly SpDataTableColumn[]>();
  readonly data = input.required<readonly SpDataTableRow[]>();
  readonly emptyMessage = input('Aucune donnée disponible');
  protected readonly displayedColumns = computed(() => this.columns().map((column) => column.key));

  protected cellValue(row: SpDataTableRow, key: string): string {
    const value = row[key];
    return value === null || value === undefined ? '—' : String(value);
  }
}
