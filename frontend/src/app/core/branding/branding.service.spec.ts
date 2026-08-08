import { DOCUMENT } from '@angular/common';
import { TestBed } from '@angular/core/testing';

import { BrandingService } from './branding.service';

describe('BrandingService', () => {
  let service: BrandingService;
  let document: Document;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(BrandingService);
    document = TestBed.inject(DOCUMENT);
  });

  afterEach(() => {
    document.defaultView?.localStorage.removeItem('sixpay.ui.institution-branding');
  });

  it('starts with a supported branding profile', () => {
    expect(service.availableBrandings.length).toBe(5);
    expect(service.activeBranding()).toBeTruthy();
  });

  it('applies La Regionale branding tokens', () => {
    service.selectBranding('LA_REGIONALE');

    expect(service.activeBrandingId()).toBe('LA_REGIONALE');
    expect(document.documentElement.style.getPropertyValue('--sp-brand-primary')).toBe(
      '#159BD7',
    );
    expect(document.title).toContain('La Régionale Bank');
  });

  it('persists the selected institution for the demo', () => {
    service.selectBranding('BICEC');

    expect(
      document.defaultView?.localStorage.getItem('sixpay.ui.institution-branding'),
    ).toBe('BICEC');
  });
});
