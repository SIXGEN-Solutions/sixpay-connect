import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, FormGroup } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';

import { ErrorService } from '../../../core/errors/error.service';
import { Partner } from '../models/partners';
import { PartnersService } from '../services/partners.service';
import { PartnerCreatePageComponent } from './partner-create-page.component';

describe('PartnerCreatePageComponent', () => {
  type TestableComponent = PartnerCreatePageComponent & {
    form: FormGroup<{
      legalName: FormControl<string>;
      technicalContactName: FormControl<string>;
      technicalContactEmail: FormControl<string>;
      authorizedTransactionTypes: FormControl<string>;
    }>;
    submit(): void;
    fieldError(
      name:
        | 'legalName'
        | 'technicalContactName'
        | 'technicalContactEmail'
        | 'authorizedTransactionTypes',
    ): string | undefined;
  };
  const creation = new Subject<Partner>();
  const partners = { create: vi.fn(() => creation.asObservable()) };
  const router = { navigate: vi.fn().mockResolvedValue(true) };
  let fixture: ComponentFixture<PartnerCreatePageComponent>;
  let component: TestableComponent;

  beforeEach(async () => {
    partners.create.mockClear();
    router.navigate.mockClear();
    await TestBed.configureTestingModule({
      imports: [PartnerCreatePageComponent],
      providers: [
        ErrorService,
        { provide: PartnersService, useValue: partners },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(PartnerCreatePageComponent);
    component = fixture.componentInstance as TestableComponent;
    fixture.detectChanges();
  });

  it('affiche les erreurs du formulaire invalide sans appeler le backend', () => {
    component.submit();
    fixture.detectChanges();

    expect(partners.create).not.toHaveBeenCalled();
    expect(fixture.nativeElement.textContent).toContain('Ce champ est obligatoire.');
  });

  it('normalise les données, bloque la double soumission et redirige après succès', () => {
    component.form.setValue({
      legalName: '  Golden Partner ',
      technicalContactName: ' Alice ',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ' PAYMENT, PAYMENT, REFUND ',
    });

    component.submit();
    component.submit();

    expect(partners.create).toHaveBeenCalledOnce();
    expect(partners.create).toHaveBeenCalledWith({
      legalName: 'Golden Partner',
      technicalContactName: 'Alice',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ['PAYMENT', 'REFUND'],
    });

    creation.next({
      id: 'partner-id',
      legalName: 'Golden Partner',
      technicalContactName: 'Alice',
      technicalContactEmail: 'alice@example.test',
      authorizedTransactionTypes: ['PAYMENT', 'REFUND'],
      status: 'PENDING_VALIDATION',
      statusReason: null,
      validationThresholds: [],
      createdAt: new Date(),
      updatedAt: new Date(),
    });
    expect(router.navigate).toHaveBeenCalledWith(['/partners', 'partner-id'], {
      queryParams: { created: true },
    });
  });

  it('rend une erreur backend de champ prioritaire', () => {
    TestBed.inject(ErrorService).publish({
      status: 400,
      title: 'Invalid request',
      detail: 'Invalid data',
      fieldErrors: { legalName: 'Raison sociale déjà utilisée.' },
      correlationId: 'corr-1',
    });

    expect(component.fieldError('legalName')).toBe('Raison sociale déjà utilisée.');
  });
});
