import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';

import { IdentityMockService } from './identity-mock.service';

describe('IdentityMockService', () => {
  let service: IdentityMockService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(IdentityMockService);
  });

  it('exposes only supported SIXPAY roles', async () => {
    const roles = await firstValueFrom(service.roles());

    expect(roles.map((role) => role.role)).toEqual([
      'ADMIN',
      'MANAGER',
      'AUDITOR',
      'PARTNER',
    ]);
  });

  it('returns mock identities without mutation capabilities', async () => {
    const users = await firstValueFrom(service.users());

    expect(users).toHaveLength(4);
    expect(users[0]?.roles).toEqual(['ADMIN']);
  });
});
