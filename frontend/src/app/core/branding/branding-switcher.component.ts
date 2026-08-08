import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AuthenticationService } from '../auth/authentication.service';
import { InstitutionBrandingId } from './branding.model';
import { BrandingService } from './branding.service';

@Component({
  selector: 'sp-branding-switcher',
  imports: [FormsModule],
  template: `
    @if (authentication.isStandaloneMode) {
      <label class="sp-branding-switcher">
        <span>Institution</span>
        <select
          [ngModel]="branding.activeBrandingId()"
          (ngModelChange)="select($event)"
          aria-label="Institution de démonstration"
        >
          @for (profile of branding.availableBrandings; track profile.id) {
            <option [value]="profile.id">{{ profile.institutionName }}</option>
          }
        </select>
      </label>
    }
  `,
  styles: `
    :host{display:block}
    .sp-branding-switcher{display:flex;align-items:center;gap:.5rem;font-size:.8rem}
    .sp-branding-switcher span{font-weight:600}
    select{
      min-width:10rem;
      padding:.4rem .6rem;
      border:1px solid color-mix(in srgb,var(--sp-brand-on-primary,#fff) 55%,transparent);
      border-radius:.5rem;
      background:color-mix(in srgb,var(--sp-brand-on-primary,#fff) 12%,transparent);
      color:inherit;
      font:inherit
    }
    select option{color:#1b1b1b;background:#fff}
    @media(max-width:58rem){.sp-branding-switcher span{display:none}select{min-width:8rem}}
  `,
})
export class BrandingSwitcherComponent {
  protected readonly authentication = inject(AuthenticationService);
  protected readonly branding = inject(BrandingService);

  protected select(id: InstitutionBrandingId): void {
    this.branding.selectBranding(id);
  }
}
