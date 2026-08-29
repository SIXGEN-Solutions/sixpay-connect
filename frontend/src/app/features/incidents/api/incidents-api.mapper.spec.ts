import { mapIncidentDetailResponse, mapIncidentSummaryResponse } from './incidents-api.mapper';

describe('Incidents API mapper', () => {
  it('maps summary dates to Date', () => {
    const mapped = mapIncidentSummaryResponse({
      incidentId: 'INC-1',
      severity: 'HIGH',
      component: 'Accounting',
      summary: 'Test',
      status: 'OPEN',
      openedAt: '2026-08-23T10:00:00Z',
      updatedAt: '2026-08-23T10:01:00Z',
    });

    expect(mapped.openedAt).toBeInstanceOf(Date);

    expect(mapped.updatedAt).toBeInstanceOf(Date);
  });

  it('maps detail timeline dates', () => {
    const mapped = mapIncidentDetailResponse({
      incidentId: 'INC-1',
      severity: 'HIGH',
      component: 'Accounting',
      summary: 'Test',
      status: 'OPEN',
      openedAt: '2026-08-23T10:00:00Z',
      updatedAt: '2026-08-23T10:01:00Z',
      description: 'Detail',
      impact: 'Impact',
      accountingBatchId: null,
      paymentId: null,
      paymentReference: null,
      correlationId: null,
      timeline: [
        {
          eventId: 'EVT-1',
          occurredAt: '2026-08-23T10:00:30Z',
          message: 'Detected',
          actor: 'SYSTEM',
        },
      ],
    });

    expect(mapped.timeline[0]?.occurredAt).toBeInstanceOf(Date);
  });
});
