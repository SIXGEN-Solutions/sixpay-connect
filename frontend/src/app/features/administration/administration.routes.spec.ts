import { describe, expect, it } from 'vitest';

import { ADMINISTRATION_ROUTES } from './administration.routes';

describe('ADMINISTRATION_ROUTES', () => {
  it('keeps legacy settings route and adds separate ADMIN-only dynamic settings route', () => {
    const legacy = ADMINISTRATION_ROUTES.find((route) => route.path === 'settings');
    const dynamic = ADMINISTRATION_ROUTES.find((route) => route.path === 'dynamic-settings');

    expect(legacy).toBeDefined();
    expect(dynamic).toBeDefined();
    expect(dynamic?.data?.['roles']).toEqual(['ADMIN']);
  });
});
