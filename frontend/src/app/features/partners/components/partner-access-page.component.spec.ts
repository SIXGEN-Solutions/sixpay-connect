import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PartnerAccessPolicy } from '../security/partner-access.policy';
import { PartnersService } from '../services/partners.service';
import { PartnerAccessPageComponent } from './partner-access-page.component';
import { PartnerSearchQuery } from '../models/partner-query';
import { PartnerPage } from '../models/partners';

describe('PartnerAccessPageComponent hardening', () => {
  let fixture: ComponentFixture<PartnerAccessPageComponent>;

  const page: PartnerPage = {
    items: [
      {
        id: '10000000-0000-4000-8000-000000000001',
        legalName: 'TresorPay',
        technicalContactName: 'TresorPay Operations',
        technicalContactEmail: 'operations1@example.test',
        authorizedTransactionTypes: ['PAYMENT'],
        status: 'ACTIVE',
        createdAt: new Date('2026-07-02T09:00:00Z'),
        updatedAt: new Date('2026-08-02T11:30:00Z'),
      },
    ],
    page: 0,
    size: 20,
    totalElements: 28,
    totalPages: 2,
};

  function configure(
    searchImplementation: (
      query: PartnerSearchQuery,
    ) => Observable<PartnerPage> = vi.fn(() => of(page)),
    usesMock = true,
  ) {
    const partners = {
      search: searchImplementation,
    };

    TestBed.configureTestingModule({
      imports: [PartnerAccessPageComponent],
      providers: [
        provideRouter([]),
        {
          provide: PartnersService,
          useValue: partners,
        },
        {
          provide: BackendModeService,
          useValue: {
            usesMock,
            usesApi: !usesMock,
          },
        },
        {
          provide: PartnerAccessPolicy,
          useValue: {
            canCreate: () => true,
          },
        },
      ],
    });

    fixture = TestBed.createComponent(
      PartnerAccessPageComponent,
    );
    fixture.detectChanges();

    return partners;
  }

  it('loads page zero with the contract default size', () => {
    const partners = configure();

    expect(partners.search).toHaveBeenCalledWith({
      page: 0,
      size: 20,
    });
    expect(fixture.nativeElement.textContent)
      .toContain('TresorPay');
  });

  it('requests page one when next is selected', () => {
    const search = vi.fn((query: { page?: number; size?: number }) =>
      of({
        ...page,
        page: query.page ?? 0,
        size: query.size ?? 20,
      }),
    );

    configure(search);

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];

    const next = buttons.find(
      (button) => button.textContent?.includes('Suivant'),
    );

    next?.click();
    fixture.detectChanges();

    expect(search).toHaveBeenLastCalledWith({
      page: 1,
      size: 20,
    });
  });

  it('opens the existing Partner detail route', () => {
    configure();

    const router = TestBed.inject(Router);
    const navigate = vi.spyOn(router, 'navigate');

    const row = fixture.nativeElement.querySelector(
      '.sp-partner-row',
    ) as HTMLTableRowElement;

    row.click();

    expect(navigate).toHaveBeenCalledWith([
      '/partners',
      '10000000-0000-4000-8000-000000000001',
    ]);
  });

  it('shows empty and error states without inventing data', () => {
    TestBed.resetTestingModule();

    configure(
      vi.fn(() =>
        of({
          items: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }),
      ),
    );

    expect(fixture.nativeElement.textContent)
      .toContain('Aucun partenaire');

    TestBed.resetTestingModule();

    configure(
      vi.fn(() =>
        throwError(
          () => new Error('Partner catalog unavailable'),
        ),
      ),
    );

    expect(fixture.nativeElement.textContent)
      .toContain('Catalogue indisponible');
  });

  it('hides the mock state panel in API mode', () => {
    configure(vi.fn(() => of(page)), false);

    expect(
      fixture.nativeElement.querySelector(
        'sp-mock-state-panel',
      ),
    ).toBeNull();
  });
});
