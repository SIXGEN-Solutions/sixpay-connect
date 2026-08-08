import { NavigationItem } from './navigation.model';

export const SIXPAY_NAVIGATION: readonly NavigationItem[] = [
  { label: 'Tableau de bord', icon: 'dashboard', route: '/', exact: true },
  { label: 'Paiements', icon: 'payments', route: '/payments', roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
  { label: 'Clients observés', icon: 'group', route: '/customers', roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
  { label: 'Comptabilisation', icon: 'account_balance', route: '/accounting', roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
  { label: 'Incidents', icon: 'warning', route: '/incidents', roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
  { label: 'Partenaires', icon: 'handshake', route: '/partners', roles: ['ADMIN', 'MANAGER', 'AUDITOR'] },
  { label: 'Administration', icon: 'settings', route: '/administration', roles: ['ADMIN'] },
  { label: 'Gestion des accès', icon: 'admin_panel_settings', route: '/identity', roles: ['ADMIN'] },
  { label: 'Mon statut Partner', icon: 'verified_user', route: '/partners/status', roles: ['PARTNER'] },
  { label: 'Design System', icon: 'palette', route: '/design-system' },
];
