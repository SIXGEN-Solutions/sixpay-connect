export type InstitutionBrandingId =
  | 'SIXPAY'
  | 'AFRILAND_FIRST_BANK'
  | 'LA_REGIONALE'
  | 'BICEC'
  | 'AFRICAN_GOLDEN';

export interface InstitutionBrandingPalette {
  readonly primary: string;
  readonly onPrimary: string;
  readonly primaryContainer: string;
  readonly onPrimaryContainer: string;
  readonly secondary: string;
  readonly onSecondary: string;
  readonly surface: string;
  readonly onSurface: string;
  readonly surfaceContainer: string;
  readonly outline: string;
}

export interface InstitutionBranding {
  readonly id: InstitutionBrandingId;
  readonly institutionName: string;
  readonly productName: string;
  readonly shortName: string;
  readonly palette: InstitutionBrandingPalette;
}
