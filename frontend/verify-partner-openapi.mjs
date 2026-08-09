import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import process from 'node:process';

import ts from 'typescript';
import { parse } from 'yaml';

const frontendRoot = process.cwd();
const openApiPath = resolve(
  frontendRoot,
  '../backend/partner/src/main/resources/openapi/partner-api-v1.yaml',
);
const modelRoot = resolve(frontendRoot, 'src/app/features/partners/models');
const apiClientPath = resolve(frontendRoot, 'src/app/features/partners/api/partners-api.client.ts');
const problemDetailPath = resolve(frontendRoot, 'src/app/core/errors/api-error.model.ts');

const contract = parse(await readFile(openApiPath, 'utf8'));
const schemas = contract.components.schemas;
const failures = [];

const interfaceSources = new Map([
  ['CreatePartnerRequest', resolve(modelRoot, 'create-partners.request.ts')],
  ['PartnerDecisionRequest', resolve(modelRoot, 'create-partners.request.ts')],
  ['SuspendPartnerRequest', resolve(modelRoot, 'create-partners.request.ts')],
  ['ConfigureValidationThresholdRequest', resolve(modelRoot, 'create-partners.request.ts')],
  ['PartnerResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerSummaryResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerPageResponse', resolve(modelRoot, 'partners.response.ts')],
  ['ValidationThresholdResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerStatusResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerConnectionInfoResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerAuditResponse', resolve(modelRoot, 'partners.response.ts')],
  ['PartnerAuditPageResponse', resolve(modelRoot, 'partners.response.ts')],
  ['ProblemDetail', problemDetailPath],
]);

for (const [schemaName, sourcePath] of interfaceSources) {
  const source = await sourceFile(sourcePath);
  const declaration = source.statements.find(
    (statement) => ts.isInterfaceDeclaration(statement) && statement.name.text === schemaName,
  );

  if (!declaration) {
    failures.push(`${schemaName}: interface absente`);
    continue;
  }

  const actual = new Map(
    declaration.members.filter(ts.isPropertySignature).map((member) => [
      propertyName(member.name),
      {
        required: !member.questionToken,
        type: canonicalType(member.type?.getText(source) ?? ''),
      },
    ]),
  );
  const schema = schemas[schemaName];
  const expectedProperties = Object.keys(schema.properties ?? {});
  const required = new Set(schema.required ?? []);

  compareSet(`${schemaName}: propriétés`, new Set(actual.keys()), new Set(expectedProperties));

  for (const property of expectedProperties) {
    const actualProperty = actual.get(property);
    if (actualProperty && actualProperty.required !== required.has(property)) {
      failures.push(
        `${schemaName}.${property}: caractère obligatoire attendu=${required.has(property)}`,
      );
    }
    if (actualProperty) {
      const expectedType = canonicalType(
        expectedTypeOverride(schemaName, property) ?? openApiType(schema.properties[property]),
      );
      if (actualProperty.type !== expectedType) {
        failures.push(
          `${schemaName}.${property}: type attendu=${expectedType}, obtenu=${actualProperty.type}`,
        );
      }
    }
  }
}

await verifyConstEnum(
  resolve(modelRoot, 'partners.response.ts'),
  'PARTNER_STATUSES',
  schemas.PartnerStatus.enum,
);
await verifyConstEnum(
  resolve(modelRoot, 'partners.response.ts'),
  'PARTNER_AUTHENTICATION_METHODS',
  schemas.PartnerConnectionInfoResponse.properties.supportedAuthenticationMethods.items.enum,
);
await verifyConstEnum(
  resolve(modelRoot, 'create-partners.request.ts'),
  'PARTNER_DECISIONS',
  schemas.PartnerDecisionRequest.properties.decision.enum,
);

const operations = Object.values(contract.paths).flatMap((pathItem) =>
  Object.values(pathItem)
    .filter((operation) => operation && typeof operation === 'object' && operation.operationId)
    .map((operation) => operation.operationId),
);
const clientSource = await sourceFile(apiClientPath);
const clientClass = clientSource.statements.find(
  (statement) => ts.isClassDeclaration(statement) && statement.name?.text === 'PartnerApiClient',
);
const actualMethods = new Set(
  (clientClass?.members ?? [])
    .filter(ts.isMethodDeclaration)
    .map((method) => propertyName(method.name))
    .filter((name) => name !== 'partnerPath'),
);
const operationToClientMethod = {
  configurePartnerValidationThreshold: 'configureValidationThreshold',
};
const expectedMethods = new Set(
  operations.map((operation) => operationToClientMethod[operation] ?? operation),
);
compareSet('PartnerApiClient: opérations', actualMethods, expectedMethods);

const clientText = await readFile(apiClientPath, 'utf8');
const requiredPathFragments = [
  "'/api/v1/partners'",
  '/validation`',
  '/suspension`',
  '/reactivation`',
  '/validation-thresholds/',
  '/status`',
  '/audit`',
];
for (const fragment of requiredPathFragments) {
  if (!clientText.includes(fragment)) {
    failures.push(`PartnerApiClient: chemin manquant ${fragment}`);
  }
}

if (failures.length > 0) {
  console.error('Conformité Partner OpenAPI en échec:');
  for (const failure of failures) {
    console.error(`- ${failure}`);
  }
  process.exitCode = 1;
} else {
  console.log(
    `Conformité Partner OpenAPI vérifiée: ${interfaceSources.size} schémas, ` +
      `${expectedMethods.size} opérations et 3 enums.`,
  );
}

async function sourceFile(path) {
  return ts.createSourceFile(
    path,
    await readFile(path, 'utf8'),
    ts.ScriptTarget.Latest,
    true,
    ts.ScriptKind.TS,
  );
}

async function verifyConstEnum(path, constName, expectedValues) {
  const source = await sourceFile(path);
  const statement = source.statements
    .filter(ts.isVariableStatement)
    .flatMap((item) => [...item.declarationList.declarations])
    .find(
      (declaration) => ts.isIdentifier(declaration.name) && declaration.name.text === constName,
    );
  const expression = statement?.initializer;
  const array = expression && ts.isAsExpression(expression) ? expression.expression : expression;
  const actualValues =
    array && ts.isArrayLiteralExpression(array)
      ? array.elements.filter(ts.isStringLiteral).map((element) => element.text)
      : [];
  compareSet(`${constName}: valeurs`, new Set(actualValues), new Set(expectedValues));
}

function propertyName(name) {
  return ts.isIdentifier(name) || ts.isStringLiteral(name) ? name.text : name.getText();
}

function openApiType(schema) {
  if (schema.$ref) {
    return schema.$ref.split('/').at(-1);
  }
  if (Array.isArray(schema.type)) {
    return schema.type
      .map((type) => (type === 'null' ? 'null' : openApiType({ ...schema, type })))
      .join(' | ');
  }
  if (schema.type === 'array') {
    return `readonly ${openApiType(schema.items)}[]`;
  }
  if (schema.type === 'integer' || schema.type === 'number') {
    return 'number';
  }
  if (schema.type === 'object' && schema.additionalProperties) {
    return `Readonly<Record<string, ${openApiType(schema.additionalProperties)}>>`;
  }
  return schema.type;
}

function expectedTypeOverride(schemaName, property) {
  const overrides = {
    'PartnerDecisionRequest.decision': 'PartnerDecision',
    'PartnerConnectionInfoResponse.supportedAuthenticationMethods':
      'readonly PartnerAuthenticationMethod[]',
  };
  return overrides[`${schemaName}.${property}`];
}

function canonicalType(type) {
  return type.replaceAll(/\s+/g, '');
}

function compareSet(label, actual, expected) {
  const missing = [...expected].filter((value) => !actual.has(value));
  const unexpected = [...actual].filter((value) => !expected.has(value));
  if (missing.length || unexpected.length) {
    failures.push(
      `${label}: manquants=[${missing.join(', ')}], inattendus=[${unexpected.join(', ')}]`,
    );
  }
}
