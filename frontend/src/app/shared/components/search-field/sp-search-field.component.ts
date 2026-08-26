import { Component, input, output, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'sp-search-field',
  imports: [MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  templateUrl: './sp-search-field.component.html',
  styleUrl: './sp-search-field.component.scss',
})
export class SpSearchFieldComponent {
  readonly label = input('Rechercher');
  readonly placeholder = input('');
  readonly searchChange = output<string>();
  protected readonly value = signal('');

  protected update(value: string): void {
    this.value.set(value);
    this.searchChange.emit(value);
  }

  protected clear(inputElement: HTMLInputElement): void {
    inputElement.value = '';
    inputElement.focus();
    this.update('');
  }
}
