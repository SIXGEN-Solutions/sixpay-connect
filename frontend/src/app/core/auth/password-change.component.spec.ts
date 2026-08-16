import {
  provideHttpClient,
} from '@angular/common/http';
import {
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import {
  ComponentFixture,
  TestBed,
} from '@angular/core/testing';
import {
  provideRouter,
} from '@angular/router';

import {
  AuthenticationService,
} from './authentication.service';
import {
  PasswordChangeComponent,
} from './password-change.component';

describe('PasswordChangeComponent', () => {
  let fixture:
    ComponentFixture<PasswordChangeComponent>;
  let authentication:
    AuthenticationService;

  beforeEach(async () => {
    await TestBed
      .configureTestingModule({
        imports: [
          PasswordChangeComponent,
        ],
        providers: [
          provideRouter([]),
          provideHttpClient(),
          provideHttpClientTesting(),
        ],
      })
      .compileComponents();

    fixture =
      TestBed.createComponent(
        PasswordChangeComponent,
      );

    authentication =
      TestBed.inject(
        AuthenticationService,
      );
  });

  it('requires matching new-password confirmation', () => {
    const component =
      fixture.componentInstance as any;

    component.form.setValue({
      currentPassword:
        'Temporary-password-2026',
      newPassword:
        'Permanent-password-2026',
      confirmation:
        'Different-password-2026',
    });

    expect(
      component.form.hasError(
        'passwordsMismatch',
      ),
    ).toBe(true);

    expect(component.form.valid)
      .toBe(false);
  });

  it('requires at least 12 characters for the new password', () => {
    const component =
      fixture.componentInstance as any;

    component.form.setValue({
      currentPassword:
        'Temporary-password-2026',
      newPassword:
        'short',
      confirmation:
        'short',
    });

    expect(
      component.form.controls
        .newPassword
        .hasError('minlength'),
    ).toBe(true);
  });
});
