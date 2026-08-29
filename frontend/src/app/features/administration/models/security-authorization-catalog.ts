export const SIXPAY_SECURITY_ROLES = [
  'ADMIN',
  'OPS',
  'SUPPORT',
  'MANAGER',
  'AUDITOR',
  'READ_ONLY',
  'PARTNER',
] as const;

export type SecurityAdministrationRole = (typeof SIXPAY_SECURITY_ROLES)[number];

export const SIXPAY_SECURITY_PERMISSIONS = [
  'observed-customer.read',
  'payment.read',
  'payment.write',
  'payment.audit',
  'payment.reconcile',
  'payment.reverse',
  'payment.audit.read',
  'payment.audit.export',
] as const;

export type SecurityAdministrationPermission = (typeof SIXPAY_SECURITY_PERMISSIONS)[number];
