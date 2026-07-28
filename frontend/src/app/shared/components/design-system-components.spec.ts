import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SpDataTableComponent } from './data-table/sp-data-table.component';
import { SpNotificationComponent } from './notification/sp-notification.component';
import { SpSearchFieldComponent } from './search-field/sp-search-field.component';

describe('Design System components', () => {
  it('renders an accessible notification', async () => {
    const fixture = await createComponent(SpNotificationComponent);
    fixture.componentRef.setInput('title', 'Erreur');
    fixture.componentRef.setInput('message', 'La demande a échoué.');
    fixture.componentRef.setInput('status', 'error');
    fixture.detectChanges();

    const notification = fixture.nativeElement.querySelector('section') as HTMLElement;
    expect(notification.getAttribute('role')).toBe('alert');
    expect(notification.textContent).toContain('La demande a échoué.');
  });

  it('emits search changes and provides an accessible clear button', async () => {
    const fixture = await createComponent(SpSearchFieldComponent);
    const values: string[] = [];
    fixture.componentInstance.searchChange.subscribe((value) => values.push(value));
    fixture.detectChanges();

    const input = fixture.nativeElement.querySelector('input') as HTMLInputElement;
    input.value = 'partner';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    const clearButton = fixture.nativeElement.querySelector(
      'button[aria-label="Effacer la recherche"]',
    );
    expect(values).toEqual(['partner']);
    expect(clearButton).not.toBeNull();
  });

  it('renders the table caption and data', async () => {
    const fixture = await createComponent(SpDataTableComponent);
    fixture.componentRef.setInput('caption', 'Audit Partner');
    fixture.componentRef.setInput('columns', [{ key: 'action', label: 'Action' }]);
    fixture.componentRef.setInput('data', [{ action: 'PARTNER_CREATED' }]);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('caption').textContent).toContain('Audit Partner');
    expect(fixture.nativeElement.textContent).toContain('PARTNER_CREATED');
  });
});

async function createComponent<T>(type: Type<T>): Promise<ComponentFixture<T>> {
  await TestBed.configureTestingModule({ imports: [type] }).compileComponents();
  return TestBed.createComponent(type);
}
