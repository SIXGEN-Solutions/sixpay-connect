import { createServer } from 'node:http';

const host = '127.0.0.1';
const port = Number(process.env['AMPLITUDE_STUB_PORT'] ?? '18081');

const checks = {
  CUSTOMER_EXISTS: 'PASS',
  FINANCIAL_INSTITUTION_MATCHES: 'PASS',
  NIU_MATCHES: 'PASS',
  IDENTITY_MATCHES: 'PASS',
  ACCOUNT_EXISTS: 'PASS',
  ACCOUNT_BELONGS_TO_CUSTOMER: 'PASS',
  ACCOUNT_IS_ACTIVE: 'PASS',
  ACCOUNT_NOT_BLOCKED: 'PASS',
  ACCOUNT_NOT_OPPOSED: 'PASS',
  REQUIRED_KYC_PRESENT: 'PASS',
  REQUIRED_KYC_VERIFIED: 'PASS',
};

function send(response, status, payload) {
  response.writeHead(status, { 'content-type': 'application/json' });
  response.end(JSON.stringify(payload));
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    let body = '';
    request.setEncoding('utf8');
    request.on('data', (chunk) => {
      body += chunk;
    });
    request.on('end', () => resolve(body));
    request.on('error', reject);
  });
}

const server = createServer(async (request, response) => {
  if (request.method === 'GET' && request.url === '/__health') {
    send(response, 200, { status: 'UP' });
    return;
  }

  if (request.method === 'POST' && request.url === '/api/v1/customer-verifications') {
    if (request.headers.authorization !== 'Bearer cm9-e2e-token') {
      send(response, 401, { code: 'UNAUTHORIZED' });
      return;
    }

    const raw = await readBody(request);
    const payload = raw ? JSON.parse(raw) : {};

    if (!payload.accountReference || !payload.niu) {
      send(response, 400, { code: 'INVALID_REQUEST' });
      return;
    }

    const observedAt = new Date();
    const validUntil = new Date(observedAt.getTime() + 5 * 60 * 1000);

    send(response, 200, {
      code: '00',
      accountFound: true,
      accountStatus: 'ACTIVE',
      accountHolder: 'CM9 Full-stack Customer',
      accountReferenceMasked: '****4321',
      currency: 'XAF',
      availableBalance: 1000000,
      accountBalance: 1000000,
      canDebit: true,
      description: 'CM-9 Amplitude stub verification',
      result: 'SUCCESS',
      observedAt: observedAt.toISOString(),
      validUntil: validUntil.toISOString(),
      checks,
    });
    return;
  }

  send(response, 404, { code: 'NOT_FOUND' });
});

server.listen(port, host, () => {
  console.log(`CM-9 Amplitude stub listening on http://${host}:${port}`);
});

const shutdown = () => server.close(() => process.exit(0));
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
