import { TestBed } from '@angular/core/testing';

import { BackendModeService } from './backend-mode.service';

describe('BackendModeService', () => {
  it('exposes exactly one active backend mode', () => {
    const service = TestBed.inject(BackendModeService);

    expect(service.usesApi === service.usesMock).toBe(false);
    expect(['api', 'mock']).toContain(service.mode);
  });
});
