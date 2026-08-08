import { InstitutionBranding, InstitutionBrandingId } from './branding.model';

/**
 * Deployment default.
 *
 * The standalone mock frame can override this at runtime with the branding switcher.
 * A client-specific production deployment can later replace this value through the
 * deployment/environment strategy without changing business features.
 */
export const DEFAULT_INSTITUTION_BRANDING: InstitutionBrandingId = 'SIXPAY';

export const INSTITUTION_BRANDINGS: readonly InstitutionBranding[] = [
  {
    id: 'SIXPAY',
    institutionName: 'SIXPAY',
    productName: 'SIXPAY CONNECT',
    shortName: 'SIXPAY',
    palette: {
      primary: '#1F5A7A',
      onPrimary: '#FFFFFF',
      primaryContainer: '#D7ECF7',
      onPrimaryContainer: '#0B2F43',
      secondary: '#53636D',
      onSecondary: '#FFFFFF',
      surface: '#FFFFFF',
      onSurface: '#182025',
      surfaceContainer: '#F3F6F8',
      outline: '#C5CDD2',
    },
  },
  {
    id: 'AFRILAND_FIRST_BANK',
    institutionName: 'Afriland First Bank',
    productName: 'SIXPAY CONNECT',
    shortName: 'AFRILAND',
    palette: {
      primary: '#B5121B',
      onPrimary: '#FFFFFF',
      primaryContainer: '#FBE7E9',
      onPrimaryContainer: '#52070C',
      secondary: '#6B7280',
      onSecondary: '#FFFFFF',
      surface: '#FFFFFF',
      onSurface: '#202124',
      surfaceContainer: '#F4F4F5',
      outline: '#C8C9CC',
    },
  },
  {
    id: 'LA_REGIONALE',
    institutionName: 'La Régionale Bank',
    productName: 'SIXPAY CONNECT',
    shortName: 'LA RÉGIONALE',
    palette: {
      primary: '#159BD7',
      onPrimary: '#FFFFFF',
      primaryContainer: '#DDF3FD',
      onPrimaryContainer: '#063B55',
      secondary: '#5C7F91',
      onSecondary: '#FFFFFF',
      surface: '#FFFFFF',
      onSurface: '#15303D',
      surfaceContainer: '#F1F9FC',
      outline: '#B8D7E5',
    },
  },
  {
    id: 'BICEC',
    institutionName: 'BICEC',
    productName: 'SIXPAY CONNECT',
    shortName: 'BICEC',
    palette: {
      primary: '#F28C00',
      onPrimary: '#111111',
      primaryContainer: '#FFF0D8',
      onPrimaryContainer: '#4A2900',
      secondary: '#202020',
      onSecondary: '#FFFFFF',
      surface: '#FFFFFF',
      onSurface: '#191919',
      surfaceContainer: '#F7F7F7',
      outline: '#C8C8C8',
    },
  },
  {
    id: 'AFRICAN_GOLDEN',
    institutionName: 'African Golden',
    productName: 'SIXPAY CONNECT',
    shortName: 'AFRICAN GOLDEN',
    palette: {
      primary: '#B68A1F',
      onPrimary: '#FFFFFF',
      primaryContainer: '#F8EDC8',
      onPrimaryContainer: '#3B2A00',
      secondary: '#333333',
      onSecondary: '#FFFFFF',
      surface: '#FFFFFF',
      onSurface: '#24221C',
      surfaceContainer: '#FAF8F1',
      outline: '#D1C49B',
    },
  },
];
