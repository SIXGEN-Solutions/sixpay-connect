import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

import { BackendModeService } from '../../../core/backend/backend-mode.service';
import { PartnerAccessPolicy } from '../security/partner-access.policy';
import { PartnersService } from '../services/partners.service';
import { PartnerAccessPageComponent } from './partner-access-page.component';

describe('PartnerAccessPageComponent', () => {
  let fixture: ComponentFixture<PartnerAccessPageComponent>;

  const page = {
    items: [
      {
        id: '10000000-0000-4000-8000-000000000001',
        legalName: 'TresorPay',
        technicalContactName: 'TresorPay Operations',
        technicalContactEmail: 'operations1@example.test',
        authorizedTransactionTypes: ['PAYMENT'],
        status: 'ACTIVE' as const,
        createdAt: new Date('2026-07-02T09:00:00Z'),
        updatedAt: new Date('2026-08-02T11:30:00Z'),
      },
    ],
    page: 0,
    size: 20,
    totalElements: 28,
    totalPages: 2,
  };

  function configure(searchResult = of(page)) {
    const partners = {
      search: vi.fn(() => searchResult),
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
            usesMock: true,
            usesApi: false,
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

    fixture = TestBed.createComponent(PartnerAccessPageComponent);
    fixture.detectChanges();

    return partners;
  }

  it('loads and renders the Partner catalog', () => {
    const partners = configure();

    expect(partners.search).toHaveBeenCalledWith({
      page: 0,
      size: 20,
    });

    expect(
      fixture.nativeElement.textContent,
    ).toContain('TresorPay');

    expect(
      fixture.nativeElement.textContent,
    ).toContain('1–20 sur 28');
  });

  it('navigates to the existing detail route when a row is selected', () => {
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

  it('renders an empty state', () => {
    configure(
      of({
        items: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    );

    expect(
      fixture.nativeElement.textContent,
    ).toContain('Aucun partenaire');
  });

  it('renders an error state when catalog loading fails', () => {
    configure(
      throwError(() => new Error('Partner catalog unavailable')),
    );

    expect(
      fixture.nativeElement.textContent,
    ).toContain('Catalogue indisponible');
  });
});
