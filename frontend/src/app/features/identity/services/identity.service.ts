import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { IdentityUser, RoleDefinition } from '../models/identity';
import { IdentityMockService } from './identity-mock.service';

@Injectable({ providedIn: 'root' })
export class IdentityService {
  private readonly mock = inject(IdentityMockService);

  users(): Observable<readonly IdentityUser[]> {
    return this.mock.users();
  }

  roles(): Observable<readonly RoleDefinition[]> {
    return this.mock.roles();
  }
}
