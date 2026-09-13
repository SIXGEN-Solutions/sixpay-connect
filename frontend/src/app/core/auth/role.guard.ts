import { authorizationGuard } from './authorization.guard';

/**
 * @deprecated Use authorizationGuard for new or permission-aware routes.
 * Retained as a compatibility alias for role-only feature routes.
 */
export const roleGuard = authorizationGuard;
