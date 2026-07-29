import { MatDialog } from '@angular/material/dialog';
import { TestBed } from '@angular/core/testing';

import { Partner } from '../models/partners';
import { PartnerLifecycleActionsComponent } from './partner-lifecycle-actions.component';

describe('PartnerLifecycleActionsComponent', () => {
  it.each([
    ['PENDING_VALIDATION', ['Approuver', 'Rejeter']],
    ['ACTIVE', ['Suspendre']],
    ['SUSPENDED', ['Réactiver']],
    ['REJECTED', []],
  ] as const)('shows only allowed actions for %s', (status, labels) => {
    TestBed.configureTestingModule({
      imports: [PartnerLifecycleActionsComponent],
      providers: [{ provide: MatDialog, useValue: {} }],
    });
    const fixture = TestBed.createComponent(PartnerLifecycleActionsComponent);
    fixture.componentRef.setInput('partner', partner(status));
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent as string;
    for (const label of ['Approuver', 'Rejeter', 'Suspendre', 'Réactiver']) {
      expect(text.includes(label)).toBe(labels.includes(label as never));
    }
  });

  function partner(status: Partner['status']): Partner {
    return {
      id: '8ec6a427-406f-4f93-b271-cbc819a4c1dd',
      legalName: 'Acme Payments',
      technicalContactName: 'Alice Ops',
      technicalContactEmail: 'alice@example.com',
      authorizedTransactionTypes: ['PAYMENT'],
      status,
      statusReason: null,
      validationThresholds: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    };
  }
});
