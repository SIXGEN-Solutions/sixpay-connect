import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  it('renders an actionable 404 state', () => {
    TestBed.configureTestingModule({
      imports: [NotFoundComponent],
      providers: [provideRouter([])],
    });

    const fixture =
      TestBed.createComponent(NotFoundComponent);

    fixture.detectChanges();

    const text =
      fixture.nativeElement.textContent;

    expect(text).toContain('404');
    expect(text).toContain('Page introuvable');
    expect(text).toContain(
      'Retour au tableau de bord',
    );
  });
});
