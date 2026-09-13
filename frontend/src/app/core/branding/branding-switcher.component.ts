import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { environment } from '../../../environments/environment';

import { InstitutionBrandingId } from './branding.model';
import { BrandingService } from './branding.service';

@Component({
  selector: 'sp-branding-switcher',
  imports: [FormsModule],
  template: `
    @if (switcherEnabled) {
      <label class="sp-branding-switcher">
        <span class="sp-branding-switcher__label">Banque active</span>
        <span class="sp-branding-switcher__control">
          <span class="sp-branding-switcher__dot" aria-hidden="true"></span>
          <select
            [ngModel]="branding.activeBrandingId()"
            (ngModelChange)="select($event)"
            aria-label="Banque active"
          >
            @for (profile of branding.availableBrandings; track profile.id) {
              <option [value]="profile.id">{{ profile.institutionName }}</option>
            }
          </select>
          <span class="sp-branding-switcher__chevron" aria-hidden="true">⌄</span>
        </span>
      </label>
    }
  `,
  styles: `
    :host {
      display: block;
    }

    .sp-branding-switcher {
      display: grid;
      gap: 0.4rem;
    }

    .sp-branding-switcher__label {
      color: var(--sp-color-text-muted);
      font-size: 0.68rem;
      font-weight: 800;
      letter-spacing: 0.07em;
      text-transform: uppercase;
    }

    .sp-branding-switcher__control {
      position: relative;
      display: grid;
      grid-template-columns: auto minmax(0, 1fr) auto;
      align-items: center;
      gap: 0.55rem;
      min-height: 2.75rem;
      padding: 0 0.75rem;
      border: 1px solid var(--sp-brand-outline, var(--sp-color-border));
      border-radius: 0.7rem;
      background: #fff;
    }

    .sp-branding-switcher__control:focus-within {
      border-color: var(--sp-brand-primary, var(--sp-color-primary));
      box-shadow: 0 0 0 3px
        color-mix(in srgb, var(--sp-brand-primary, var(--sp-color-primary)) 14%, transparent);
    }

    .sp-branding-switcher__dot {
      width: 0.55rem;
      height: 0.55rem;
      border-radius: 50%;
      background: var(--sp-brand-primary, var(--sp-color-primary));
    }

    select {
      width: 100%;
      min-width: 0;
      appearance: none;
      border: 0;
      outline: 0;
      background: transparent;
      color: var(--sp-color-text);
      font: inherit;
      font-size: 0.82rem;
      font-weight: 650;
      cursor: pointer;
    }

    select option {
      color: #1b1b1b;
      background: #fff;
    }

    .sp-branding-switcher__chevron {
      color: var(--sp-color-text-muted);
      font-size: 1rem;
      pointer-events: none;
    }
  `,
})
export class BrandingSwitcherComponent {
  protected readonly switcherEnabled = environment.branding.switcherEnabled;
  protected readonly branding = inject(BrandingService);

  protected select(id: InstitutionBrandingId): void {
    this.branding.selectBranding(id);
  }
}
