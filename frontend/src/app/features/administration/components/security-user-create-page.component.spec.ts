import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ErrorService } from '../../../core/errors/error.service';
import { DirectoryUserView, SecurityUserDetail } from '../models/security-user-administration';
import { SecurityUserAdministrationService } from '../services/security-user-administration.service';
import { SecurityUserCreatePageComponent } from './security-user-create-page.component';

const DIRECTORY_USER: DirectoryUserView = {
  username: 'jane.doe',
  displayName: 'Jane Doe',
  email: 'jane.doe@example.com',
  accountStatus: 'ACTIVE',
  stableSubject: 'hidden-object-guid',
};

const CREATED_USER: SecurityUserDetail = {
  id: 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa',
  username: 'jane.doe',
  email: 'jane.doe@example.com',
  status: 'ACTIVE',
  localEnabled: false,
  oidcLinked: false,
  roles: ['OPS'],
  permissions: ['payment.read'],
  identities: [],
  recentAuthenticationEvents: [],
};

class SecurityUserAdministrationServiceStub {
  lookupDirectoryUser = vi.fn<(username: string) => Observable<DirectoryUserView>>(() =>
    of(DIRECTORY_USER),
  );

  provisionLdapUser = vi.fn(() => of(CREATED_USER));

  createUser = vi.fn(() => of(CREATED_USER));
}

describe('SecurityUserCreatePageComponent', () => {
  let fixture: ComponentFixture<SecurityUserCreatePageComponent>;
  let service: SecurityUserAdministrationServiceStub;

  beforeEach(async () => {
    service = new SecurityUserAdministrationServiceStub();

    await TestBed.configureTestingModule({
      imports: [SecurityUserCreatePageComponent],
      providers: [
        provideRouter([]),
        ErrorService,
        {
          provide: SecurityUserAdministrationService,
          useValue: service,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SecurityUserCreatePageComponent);
    fixture.detectChanges();
  });

  it('supports LDAP as an explicit authentication identity type', () => {
    const ldapType: import('../models/security-user-administration').AuthenticationIdentityType =
      'LDAP';

    expect(ldapType).toBe('LDAP');
  });

  it('keeps LOCAL as the default creation workflow', () => {
    expect(fixture.nativeElement.textContent).toContain('Créer l’utilisateur Local');
    expect(fixture.nativeElement.textContent).not.toContain('Identité Active Directory trouvée');
  });

  it('looks up an LDAP identity and never renders stableSubject/objectGUID', () => {
    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    expect(service.lookupDirectoryUser).toHaveBeenCalledWith('jane.doe');
    expect(fixture.nativeElement.textContent).toContain('Jane Doe');
    expect(fixture.nativeElement.textContent).toContain('jane.doe@example.com');
    expect(fixture.nativeElement.textContent).toContain('ACTIVE');
    expect(fixture.nativeElement.textContent).not.toContain('hidden-object-guid');
  });

  it('blocks provisioning when the directory account is not ACTIVE', () => {
    service.lookupDirectoryUser.mockReturnValue(
      of({ ...DIRECTORY_USER, accountStatus: 'DISABLED' }),
    );

    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    expect(findButton('Provisionner dans SIXPAY').disabled).toBe(true);
    expect(fixture.nativeElement.textContent).toContain('DISABLED');
  });

  it('shows not found and directory unavailable states', () => {
    service.lookupDirectoryUser.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 404, error: {} })),
    );

    selectLdapMode();
    setLdapUsername('missing');
    submitDirectoryLookup();
    expect(fixture.nativeElement.textContent).toContain('Aucun utilisateur Active Directory');

    service.lookupDirectoryUser.mockReturnValueOnce(
      throwError(() => new HttpErrorResponse({ status: 503, error: {} })),
    );

    setLdapUsername('jane.doe');
    submitDirectoryLookup();
    expect(fixture.nativeElement.textContent).toContain('temporairement indisponible');
  });

  it('shows ambiguous lookup on a 409 without provisioning reason', () => {
    service.lookupDirectoryUser.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 409, error: {} })),
    );

    selectLdapMode();
    setLdapUsername('duplicate');
    submitDirectoryLookup();

    expect(fixture.nativeElement.textContent).toContain('plusieurs identités');
  });

  it('shows an already-provisioned conflict', () => {
    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    service.provisionLdapUser.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { reason: 'ALREADY_PROVISIONED' },
          }),
      ),
    );

    clickButton('Provisionner dans SIXPAY');

    expect(fixture.nativeElement.textContent).toContain('déjà provisionnée');
  });

  it('shows a username conflict without automatic linking', () => {
    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    service.provisionLdapUser.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 409,
            error: { reason: 'USERNAME_CONFLICT' },
          }),
      ),
    );

    clickButton('Provisionner dans SIXPAY');

    expect(fixture.nativeElement.textContent).toContain('Aucun rattachement automatique');
  });

  it('renders SIXPAY roles and permissions selectors in LDAP mode', () => {
    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    const roleSelect = fixture.nativeElement.querySelector(
      'mat-select[formControlName="roles"]',
    ) as HTMLElement | null;
    const permissionSelect = fixture.nativeElement.querySelector(
      'mat-select[formControlName="permissions"]',
    ) as HTMLElement | null;

    expect(roleSelect).not.toBeNull();
    expect(permissionSelect).not.toBeNull();
  });

  it('shows a generic API failure without exposing directory internals', () => {
    service.lookupDirectoryUser.mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500, error: {} })),
    );

    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();

    expect(fixture.nativeElement.textContent).toContain(
      'La recherche Active Directory n’a pas pu être effectuée.',
    );
    expect(fixture.nativeElement.textContent).not.toContain('serviceAccount');
    expect(fixture.nativeElement.textContent).not.toContain('objectGUID');
  });

  it('navigates to the canonical user detail after LDAP provisioning', () => {
    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    selectLdapMode();
    setLdapUsername('jane.doe');
    submitDirectoryLookup();
    clickButton('Provisionner dans SIXPAY');

    expect(navigate).toHaveBeenCalledWith(['/administration/users', CREATED_USER.id], {
      queryParams: { created: true },
    });
  });

  function selectLdapMode(): void {
    const toggle = [...fixture.nativeElement.querySelectorAll('mat-button-toggle')].find(
      (candidate: Element) => candidate.textContent?.includes('LDAP / Active Directory'),
    ) as HTMLElement | undefined;
    const button = toggle?.querySelector('button') as HTMLButtonElement | null;

    if (!button) {
      throw new Error('LDAP / Active Directory toggle button not found');
    }

    button.click();
    fixture.detectChanges();
  }

  function setLdapUsername(value: string): void {
    const input = fixture.nativeElement.querySelector(
      'input[formControlName="username"]',
    ) as HTMLInputElement | null;

    if (!input) {
      throw new Error('Active Directory username input not found');
    }

    input.value = value;
    input.dispatchEvent(new Event('input', { bubbles: true }));
    fixture.detectChanges();
  }

  function submitDirectoryLookup(): void {
    const input = fixture.nativeElement.querySelector(
      'input[formControlName="username"]',
    ) as HTMLInputElement | null;
    const form = input?.closest('form') as HTMLFormElement | null;

    if (!form) {
      throw new Error('Active Directory lookup form not found');
    }

    form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  function clickButton(label: string): void {
    const button = findButton(label);
    button.click();
    fixture.detectChanges();
  }

  function findButton(label: string): HTMLButtonElement {
    const host = [...fixture.nativeElement.querySelectorAll('sp-button')].find(
      (candidate: Element) => candidate.textContent?.includes(label),
    ) as HTMLElement | undefined;
    const button = host?.querySelector('button') as HTMLButtonElement | null;

    if (!button) {
      throw new Error(`Button not found: ${label}`);
    }

    return button;
  }
});
