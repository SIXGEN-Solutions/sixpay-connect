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
  of,
  throwError,
} from 'rxjs';
import {
  HttpErrorResponse,
} from '@angular/common/http';

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

  it('submits current and new password through authentication service', () => {
    const component =
      fixture.componentInstance as any;

    const changePassword =
      vi.spyOn(
        authentication,
        'changeLocalPassword',
      )
        .mockReturnValue(
          of(undefined),
        );

    component.form.setValue({
      currentPassword:
        'Temporary-password-2026',
      newPassword:
        'Permanent-password-2027',
      confirmation:
        'Permanent-password-2027',
    });

    component.submit();

    expect(
      changePassword,
    ).toHaveBeenCalledOnce();

    expect(
      changePassword,
    ).toHaveBeenCalledWith({
      currentPassword:
        'Temporary-password-2026',
      newPassword:
        'Permanent-password-2027',
    });

    expect(
      component.serverError(),
    ).toBeNull();
  });

  it('surfaces backend lifecycle rejection detail to the user', () => {
    const component =
      fixture.componentInstance as any;

    vi.spyOn(
      authentication,
      'changeLocalPassword',
    )
      .mockReturnValue(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 400,
              error: {
                detail:
                  'Password must not reuse a recent password',
              },
            }),
        ),
      );

    component.form.setValue({
      currentPassword:
        'Temporary-password-2026',
      newPassword:
        'Historical-password-2026',
      confirmation:
        'Historical-password-2026',
    });

    component.submit();

    expect(
      component.serverError(),
    ).toBe(
      'Password must not reuse a recent password',
    );
  });
});
