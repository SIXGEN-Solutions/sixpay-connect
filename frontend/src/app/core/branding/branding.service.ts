import { DOCUMENT } from '@angular/common';
import { computed, inject, Injectable, signal } from '@angular/core';

import {
  DEFAULT_INSTITUTION_BRANDING,
  INSTITUTION_BRANDINGS,
} from './branding.config';
import {
  InstitutionBranding,
  InstitutionBrandingId,
  InstitutionBrandingPalette,
} from './branding.model';

const BRANDING_STORAGE_KEY = 'sixpay.ui.institution-branding';

@Injectable({ providedIn: 'root' })
export class BrandingService {
  private readonly document = inject(DOCUMENT);

  private readonly activeBrandingState = signal<InstitutionBranding>(
    this.resolveInitialBranding(),
  );

  readonly availableBrandings = INSTITUTION_BRANDINGS;
  readonly activeBranding = this.activeBrandingState.asReadonly();
  readonly institutionName = computed(() => this.activeBrandingState().institutionName);
  readonly productName = computed(() => this.activeBrandingState().productName);
  readonly activeBrandingId = computed(() => this.activeBrandingState().id);

  constructor() {
    this.applyBranding(this.activeBrandingState());
  }

  selectBranding(id: InstitutionBrandingId): void {
    const branding =
      INSTITUTION_BRANDINGS.find((candidate) => candidate.id === id) ??
      INSTITUTION_BRANDINGS[0];

    if (!branding) {
      return;
    }

    this.activeBrandingState.set(branding);
    this.storage?.setItem(BRANDING_STORAGE_KEY, branding.id);
    this.applyBranding(branding);
  }

  private resolveInitialBranding(): InstitutionBranding {
    const storedId = this.storage?.getItem(
      BRANDING_STORAGE_KEY,
    ) as InstitutionBrandingId | null;

    return (
      INSTITUTION_BRANDINGS.find((branding) => branding.id === storedId) ??
      INSTITUTION_BRANDINGS.find(
        (branding) => branding.id === DEFAULT_INSTITUTION_BRANDING,
      ) ??
      INSTITUTION_BRANDINGS[0]!
    );
  }

  private applyBranding(branding: InstitutionBranding): void {
    const root = this.document.documentElement;
    root.dataset['institutionBranding'] = branding.id;
    this.document.title = `${branding.productName} — ${branding.institutionName}`;

    const variables = this.toCssVariables(branding.palette);
    for (const [name, value] of Object.entries(variables)) {
      root.style.setProperty(name, value);
    }
  }

  private toCssVariables(
    palette: InstitutionBrandingPalette,
  ): Readonly<Record<string, string>> {
    return {
      '--sp-brand-primary': palette.primary,
      '--sp-brand-on-primary': palette.onPrimary,
      '--sp-brand-primary-container': palette.primaryContainer,
      '--sp-brand-on-primary-container': palette.onPrimaryContainer,
      '--sp-brand-secondary': palette.secondary,
      '--sp-brand-on-secondary': palette.onSecondary,
      '--sp-brand-surface': palette.surface,
      '--sp-brand-on-surface': palette.onSurface,
      '--sp-brand-surface-container': palette.surfaceContainer,
      '--sp-brand-outline': palette.outline,

      '--sp-color-primary': palette.primary,
      '--sp-color-primary-container': palette.primaryContainer,
      '--sp-color-border': palette.outline,

      '--mat-sys-primary': palette.primary,
      '--mat-sys-on-primary': palette.onPrimary,
      '--mat-sys-primary-container': palette.primaryContainer,
      '--mat-sys-on-primary-container': palette.onPrimaryContainer,
      '--mat-sys-secondary': palette.secondary,
      '--mat-sys-on-secondary': palette.onSecondary,
      '--mat-sys-surface': palette.surface,
      '--mat-sys-on-surface': palette.onSurface,
      '--mat-sys-surface-container': palette.surfaceContainer,
      '--mat-sys-outline': palette.outline,
      '--mat-sys-outline-variant': palette.outline,
    };
  }

  private get storage(): Storage | undefined {
    return this.document.defaultView?.localStorage;
  }
}
