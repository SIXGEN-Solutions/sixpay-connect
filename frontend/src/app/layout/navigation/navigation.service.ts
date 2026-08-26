import { computed, inject, Injectable } from '@angular/core';

import { AuthenticationService } from '../../core/auth/authentication.service';
import { SIXPAY_NAVIGATION } from './navigation.config';
import { canSeeNavigationItem } from './navigation.policy';

@Injectable({ providedIn: 'root' })
export class NavigationService {
  private readonly authentication = inject(AuthenticationService);

  readonly items = computed(() => {
    const roles = this.authentication.roles();
    return SIXPAY_NAVIGATION.filter((item) => canSeeNavigationItem(item, roles));
  });
}
