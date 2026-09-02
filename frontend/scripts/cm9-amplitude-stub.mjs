import { randomUUID } from 'node:crypto';
import { createServer } from 'node:http';

const host = '127.0.0.1';
const port = Number(process.env['AMPLITUDE_STUB_PORT'] ?? '18081');

const requiredCheckTypes = [
  'CUSTOMER_EXISTS',
  'FINANCIAL_INSTITUTION_MATCHES',
  'NIU_MATCHES',
  'IDENTITY_MATCHES',
  'ACCOUNT_EXISTS',
  'ACCOUNT_BELONGS_TO_CUSTOMER',
  'ACCOUNT_IS_ACTIVE',
  'ACCOUNT_NOT_BLOCKED',
  'ACCOUNT_NOT_OPPOSED',
  'REQUIRED_KYC_PRESENT',
  'REQUIRED_KYC_VERIFIED',
];

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

function validVerificationRequest(payload) {
  return Boolean(
    payload &&
    payload.financialInstitutionCode &&
    payload.customer?.niu &&
    payload.customer?.legalName &&
    payload.account?.accountReference &&
    Array.isArray(payload.requiredKycFields) &&
    payload.requiredKycFields.length >= 4 &&
    payload.requestedAt,
  );
}

function verifiedResponse(payload) {
  const verifiedAt = new Date().toISOString();
  const customerReference = 'AMPLITUDE-CUSTOMER-CM9';
  const accountReference = payload.account.accountReference;
  const financialInstitutionCode = payload.financialInstitutionCode;

  const checks = requiredCheckTypes.map((type) => ({
    type,
    result: 'PASS',
    reasonCode: null,
    checkedAt: verifiedAt,
  }));

  const kycFields = [
    {
      code: 'niu',
      value: payload.customer.niu,
      present: true,
      verified: true,
      verifiedAt,
    },
    {
      code: 'legalName',
      value: payload.customer.legalName,
      present: true,
      verified: true,
      verifiedAt,
    },
    {
      code: 'phoneNumber',
      value: '+237600000009',
      present: true,
      verified: true,
      verifiedAt,
    },
    {
      code: 'email',
      value: 'cm9.customer@sixpay.test',
      present: true,
      verified: true,
      verifiedAt,
    },
  ];

  return {
    verificationId: randomUUID(),
    verifiedAt,
    source: 'AMPLITUDE',
    outcome: 'VERIFIED',
    customerReference,
    accountReference,
    checks,
    identity: {
      customerReference,
      customerNumber: 'CM9-000001',
      financialInstitutionCode,
      niu: payload.customer.niu,
      legalName: payload.customer.legalName,
      phoneNumber: '+237600000009',
      email: 'cm9.customer@sixpay.test',
      kycStatus: 'COMPLETE',
      kycFields,
      kycLastUpdatedAt: verifiedAt,
      source: 'AMPLITUDE',
      retrievedAt: verifiedAt,
    },
    account: {
      accountReference,
      customerReference,
      financialInstitutionCode,
      maskedAccountIdentifier: '****4321',
      currency: 'XAF',
      accountType: 'CURRENT',
      status: 'ACTIVE',
      restrictions: [],
      source: 'AMPLITUDE',
      retrievedAt: verifiedAt,
    },
  };
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

    if (!validVerificationRequest(payload)) {
      send(response, 400, { code: 'INVALID_REQUEST' });
      return;
    }

    send(response, 200, verifiedResponse(payload));
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
