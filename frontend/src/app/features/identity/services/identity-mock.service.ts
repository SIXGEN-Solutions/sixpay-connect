import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

import { IdentityUser, RoleDefinition } from '../models/identity';

const USERS: readonly IdentityUser[] = [
  {
    userId: 'USR-ADMIN-001',
    displayName: 'Administrateur SIXPAY',
    subject: 'admin@sixpay.local',
    type: 'USER',
    roles: ['ADMIN'],
    status: 'ACTIVE',
    lastLoginAt: new Date('2026-08-08T13:54:00Z'),
  },
  {
    userId: 'USR-MANAGER-001',
    displayName: 'Manager Opérations',
    subject: 'manager@sixpay.local',
    type: 'USER',
    roles: ['MANAGER'],
    status: 'ACTIVE',
    lastLoginAt: new Date('2026-08-08T13:31:00Z'),
  },
  {
    userId: 'USR-AUDIT-001',
    displayName: 'Audit Interne',
    subject: 'audit@sixpay.local',
    type: 'USER',
    roles: ['AUDITOR'],
    status: 'ACTIVE',
    lastLoginAt: new Date('2026-08-08T12:48:00Z'),
  },
  {
    userId: 'PARTNER-TRESORPAY',
    displayName: 'TresorPay',
    subject: 'partner-tresorpay',
    type: 'PARTNER',
    roles: ['PARTNER'],
    status: 'ACTIVE',
    lastLoginAt: null,
  },
];

const ROLES: readonly RoleDefinition[] = [
  {
    role: 'ADMIN',
    description: 'Administration et configuration de l’application.',
    capabilities: ['Administration', 'Gestion des accès', 'Consultation opérationnelle'],
    userCount: 1,
  },
  {
    role: 'MANAGER',
    description: 'Pilotage opérationnel et validation Partner.',
    capabilities: ['Consultation opérationnelle', 'Validation Partner'],
    userCount: 1,
  },
  {
    role: 'AUDITOR',
    description: 'Consultation et preuve d’audit sans mutation métier.',
    capabilities: ['Consultation opérationnelle', 'Audit / Reporting'],
    userCount: 1,
  },
  {
    role: 'PARTNER',
    description: 'Consultation limitée au contexte Partner.',
    capabilities: ['Statut Partner'],
    userCount: 1,
  },
];

@Injectable({ providedIn: 'root' })
export class IdentityMockService {
  users(): Observable<readonly IdentityUser[]> {
    return of(USERS);
  }

  roles(): Observable<readonly RoleDefinition[]> {
    return of(ROLES);
  }
}
