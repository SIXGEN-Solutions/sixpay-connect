from pathlib import Path
import sys

ROOT = Path.cwd()

ADMIN = ROOT / "documentation/contracts/internal/administration-query-api-v1.yaml"
INCIDENT = ROOT / "documentation/contracts/internal/incident-query-api-v1.yaml"
MERGED = ROOT / "documentation/contracts/internal/administration-operational-api-v1.yaml"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
FULLSTACK_GATE = ROOT / "frontend/scripts/verify-full-stack-conformance.mjs"
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"

EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

MERGED_CONTRACT = r'''openapi: 3.1.0

info:
  title: SIXPAY CONNECT - Internal Administration Operational API
  version: 1.0.0
  description: |
    Read-only internal API for Administration-owned operational projections.

    This physical API contract consolidates two distinct registered capabilities:
    ADMINISTRATION_OPERATIONAL_QUERY and OPERATIONAL_INCIDENT_QUERY.

    The Administration operational capability exposes runtime-backed settings,
    integration-health projections and the bounded operational overview.

    The Operational Incident capability exposes incident search, incident detail
    and the safe incident timeline.

    Physical contract consolidation does not merge capability ownership or
    authorization semantics. Security User Administration remains exposed through
    its dedicated endpoints under /internal/api/v1/administration/users and is
    intentionally outside this contract.

    This contract does not define settings mutation, integration mutation,
    incident commands, Security user mutation, Payment audit evidence mutation,
    provider commands or any fallback to mock/demo data as a system of record.

  license:
    name: SIXGEN Solutions Proprietary
    url: https://github.com/SIXGEN-Solutions/sixpay-connect/blob/main/LICENSE.md

  x-sixpay-contract:
    registryIds:
      - administration-query-api-v1
      - incident-query-api-v1
    gates:
      - FS-1.4
    phaseSteps:
      - FS-1.4.1
      - FS-1.4.2
    lifecycleStatus: ACTIVE_MVP
    approvalStatus: PENDING_APPROVAL
    generationPolicy: REFERENCE_ONLY
    codeGenerationAllowed: false
    domain: administration
    businessOwner: administration
    securityOwner: security
    capabilities:
      - ADMINISTRATION_OPERATIONAL_QUERY
      - OPERATIONAL_INCIDENT_QUERY
    direction: INTERNAL_CLIENT_TO_SIXPAY
    dataClassification: INTERNAL
    readOnly: true
    authorization:
      model: ROLE_BASED
      operationSpecific: true
    limitations:
      - NO_SETTINGS_MUTATION
      - NO_INTEGRATION_MUTATION
      - NO_SECURITY_USER_MUTATION
      - NO_IDENTITY_MUTATION
      - NO_ROLE_OR_PERMISSION_MUTATION
      - NO_MOCK_OR_DEMO_VALUES_AS_SYSTEM_OF_RECORD
      - NO_INCIDENT_CREATION
      - NO_INCIDENT_MUTATION
      - NO_INCIDENT_STATUS_TRANSITION
      - NO_INCIDENT_ASSIGNMENT
      - NO_INCIDENT_DELETION
      - NO_PAYMENT_AUDIT_PAYLOAD
      - NO_RAW_PROVIDER_PAYLOAD
      - NO_SECRETS_OR_CREDENTIALS

servers:
  - url: https://sixpay-api.example
    description: Environment-specific internal SIXPAY endpoint

security:
  - bearerAuth: []

tags:
  - name: Administration
    description: Read-only operational Administration projections.
  - name: Incidents
    description: Read-only operational Incident projections.

paths:
  /internal/api/v1/administration/overview:
    get:
      tags: [Administration]
      operationId: getAdministrationOverview
      summary: Get the operational Administration overview
      security:
        - bearerAuth: []
      x-sixpay-authorization:
        requiredRoles: [ADMIN]
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
      responses:
        '200':
          description: Administration overview
          headers:
            X-Correlation-ID:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdministrationOverviewResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/AdministrationForbidden'
        '500':
          $ref: '#/components/responses/AdministrationInternalServerError'
        '503':
          $ref: '#/components/responses/AdministrationServiceUnavailable'

  /internal/api/v1/administration/settings:
    get:
      tags: [Administration]
      operationId: getAdministrationSettings
      summary: Get the effective operational settings projection
      security:
        - bearerAuth: []
      x-sixpay-authorization:
        requiredRoles: [ADMIN]
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
      responses:
        '200':
          description: Effective operational settings
          headers:
            X-Correlation-ID:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/AdministrationSettingsResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/AdministrationForbidden'
        '500':
          $ref: '#/components/responses/AdministrationInternalServerError'
        '503':
          $ref: '#/components/responses/AdministrationServiceUnavailable'

  /internal/api/v1/administration/integrations:
    get:
      tags: [Administration]
      operationId: getAdministrationIntegrations
      summary: Get observable integration-health projections
      security:
        - bearerAuth: []
      x-sixpay-authorization:
        requiredRoles: [ADMIN]
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
      responses:
        '200':
          description: Observable integration-health projections
          headers:
            X-Correlation-ID:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json:
              schema:
                type: array
                items:
                  $ref: '#/components/schemas/IntegrationStatusResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/AdministrationForbidden'
        '500':
          $ref: '#/components/responses/AdministrationInternalServerError'
        '503':
          $ref: '#/components/responses/AdministrationServiceUnavailable'

  /internal/api/v1/incidents:
    get:
      tags: [Incidents]
      operationId: searchIncidents
      summary: Search operational incidents
      security:
        - bearerAuth: []
      x-sixpay-authorization:
        requiredRoles:
          - ADMIN
          - MANAGER
          - AUDITOR
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - name: severity
          in: query
          required: false
          schema:
            $ref: '#/components/schemas/IncidentSeverity'
        - name: status
          in: query
          required: false
          schema:
            $ref: '#/components/schemas/IncidentStatus'
        - name: component
          in: query
          required: false
          schema:
            type: string
            minLength: 1
            maxLength: 128
        - $ref: '#/components/parameters/Page'
        - $ref: '#/components/parameters/PageSize'
      responses:
        '200':
          description: Page of matching operational incidents
          headers:
            X-Correlation-ID:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/IncidentPageResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/IncidentForbidden'
        '500':
          $ref: '#/components/responses/IncidentInternalServerError'
        '503':
          $ref: '#/components/responses/IncidentServiceUnavailable'

  /internal/api/v1/incidents/{incidentId}:
    get:
      tags: [Incidents]
      operationId: getIncident
      summary: Get operational Incident detail
      security:
        - bearerAuth: []
      x-sixpay-authorization:
        requiredRoles:
          - ADMIN
          - MANAGER
          - AUDITOR
      parameters:
        - $ref: '#/components/parameters/CorrelationId'
        - $ref: '#/components/parameters/IncidentId'
      responses:
        '200':
          description: Operational Incident detail
          headers:
            X-Correlation-ID:
              $ref: '#/components/headers/CorrelationId'
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/IncidentDetailResponse'
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          $ref: '#/components/responses/Unauthorized'
        '403':
          $ref: '#/components/responses/IncidentForbidden'
        '404':
          $ref: '#/components/responses/NotFound'
        '500':
          $ref: '#/components/responses/IncidentInternalServerError'
        '503':
          $ref: '#/components/responses/IncidentServiceUnavailable'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  parameters:
    CorrelationId:
      name: X-Correlation-ID
      in: header
      required: true
      schema:
        type: string
        format: uuid

    IncidentId:
      name: incidentId
      in: path
      required: true
      schema:
        type: string
        minLength: 1
        maxLength: 64

    Page:
      name: page
      in: query
      required: false
      schema:
        type: integer
        minimum: 0
        default: 0

    PageSize:
      name: size
      in: query
      required: false
      schema:
        type: integer
        minimum: 1
        maximum: 200
        default: 20

  headers:
    CorrelationId:
      description: Echo of the request correlation identifier
      schema:
        type: string
        format: uuid

  responses:
    BadRequest:
      description: Invalid filter, pagination value or malformed request
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    Unauthorized:
      description: Authentication is required
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    AdministrationForbidden:
      description: Administration overview, settings and integrations require ROLE_ADMIN.
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    IncidentForbidden:
      description: Incident queries require ROLE_ADMIN, ROLE_MANAGER or ROLE_AUDITOR.
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    NotFound:
      description: Incident does not exist or is not visible to the caller
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    AdministrationInternalServerError:
      description: Internal Administration query failure
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    IncidentInternalServerError:
      description: Internal Incident query failure
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    AdministrationServiceUnavailable:
      description: Runtime Administration projection source is temporarily unavailable.
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

    IncidentServiceUnavailable:
      description: Incident query projection is temporarily unavailable
      content:
        application/problem+json:
          schema:
            $ref: '#/components/schemas/ProblemDetail'

  schemas:
    AdministrationOverviewResponse:
      type: object
      additionalProperties: false
      required: [settings, integrations, observedAt]
      properties:
        settings:
          $ref: '#/components/schemas/AdministrationSettingsResponse'
        integrations:
          type: array
          items:
            $ref: '#/components/schemas/IntegrationStatusResponse'
        observedAt:
          type: string
          format: date-time

    AdministrationSettingsResponse:
      type: object
      additionalProperties: false
      required: [accountingCutoffZone, accountingCutoffTime]
      properties:
        accountingCutoffZone:
          type: string
          minLength: 1
          maxLength: 64
          examples: [Africa/Douala]
        accountingCutoffTime:
          type: string
          pattern: '^([01]\d|2[0-3]):[0-5]\d$'
          examples: ["23:00"]

    IntegrationHealth:
      type: string
      enum: [AVAILABLE, DEGRADED, UNAVAILABLE, UNKNOWN]

    IntegrationStatusResponse:
      type: object
      additionalProperties: false
      required: [integrationId, name, type, health, lastCheckedAt]
      properties:
        integrationId:
          type: string
          minLength: 1
          maxLength: 64
        name:
          type: string
          minLength: 1
          maxLength: 128
        type:
          type: string
          minLength: 1
          maxLength: 64
        health:
          $ref: '#/components/schemas/IntegrationHealth'
        detail:
          type: [string, 'null']
          maxLength: 512
        lastSuccessfulAt:
          type: [string, 'null']
          format: date-time
        lastCheckedAt:
          type: string
          format: date-time

    IncidentSeverity:
      type: string
      enum: [LOW, MEDIUM, HIGH, CRITICAL]

    IncidentStatus:
      type: string
      enum: [OPEN, INVESTIGATING, MONITORING, RESOLVED, CLOSED]

    IncidentSummary:
      type: object
      additionalProperties: false
      required: [incidentId, severity, component, summary, status, openedAt, updatedAt]
      properties:
        incidentId:
          type: string
          minLength: 1
          maxLength: 64
        severity:
          $ref: '#/components/schemas/IncidentSeverity'
        component:
          type: string
          minLength: 1
          maxLength: 128
        summary:
          type: string
          minLength: 1
          maxLength: 256
        status:
          $ref: '#/components/schemas/IncidentStatus'
        openedAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    IncidentTimelineEntry:
      type: object
      additionalProperties: false
      required: [eventId, occurredAt, message, actor]
      properties:
        eventId:
          type: string
          minLength: 1
          maxLength: 64
        occurredAt:
          type: string
          format: date-time
        message:
          type: string
          minLength: 1
          maxLength: 1024
        actor:
          type: string
          minLength: 1
          maxLength: 128

    IncidentDetailResponse:
      allOf:
        - $ref: '#/components/schemas/IncidentSummary'
        - type: object
          additionalProperties: false
          required: [description, impact, timeline]
          properties:
            description:
              type: string
              minLength: 1
              maxLength: 4096
            impact:
              type: string
              minLength: 1
              maxLength: 2048
            accountingBatchId:
              type: [string, 'null']
              format: uuid
            paymentId:
              type: [string, 'null']
              format: uuid
            paymentReference:
              type: [string, 'null']
              maxLength: 64
            correlationId:
              type: [string, 'null']
              format: uuid
            timeline:
              type: array
              items:
                $ref: '#/components/schemas/IncidentTimelineEntry'

    IncidentPageResponse:
      type: object
      additionalProperties: false
      required: [content, page, size, totalElements, totalPages]
      properties:
        content:
          type: array
          items:
            $ref: '#/components/schemas/IncidentSummary'
        page:
          type: integer
          minimum: 0
        size:
          type: integer
          minimum: 1
        totalElements:
          type: integer
          format: int64
          minimum: 0
        totalPages:
          type: integer
          minimum: 0

    ProblemDetail:
      type: object
      additionalProperties: false
      required: [type, title, status, code, correlationId]
      properties:
        type:
          type: string
          format: uri
        title:
          type: string
        status:
          type: integer
          minimum: 400
          maximum: 599
        code:
          type: string
        correlationId:
          type: string
          format: uuid
        detail:
          type: string
'''


def fail(message: str) -> None:
    print(f"ERROR: {message}")
    sys.exit(1)


def require_file(path: Path) -> str:
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")


def main() -> None:
    engineering = require_file(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            f"{EXPECTED_BRANCH}."
        )

    if MERGED.exists():
        fail(f"{MERGED} already exists; review before applying.")

    admin_source = require_file(ADMIN)
    incident_source = require_file(INCIDENT)
    registry = require_file(REGISTRY)
    gate = require_file(FULLSTACK_GATE)

    for token in [
        "/internal/api/v1/administration/overview:",
        "/internal/api/v1/administration/settings:",
        "/internal/api/v1/administration/integrations:",
        "capability: ADMINISTRATION_OPERATIONAL_QUERY",
    ]:
        if token not in admin_source:
            fail(f"Administration source missing: {token}")

    for token in [
        "/internal/api/v1/incidents:",
        "/internal/api/v1/incidents/{incidentId}:",
        "capability: OPERATIONAL_INCIDENT_QUERY",
    ]:
        if token not in incident_source:
            fail(f"Incident source missing: {token}")

    new_path = (
        "documentation/contracts/internal/"
        "administration-operational-api-v1.yaml"
    )
    old_admin_path = (
        "documentation/contracts/internal/"
        "administration-query-api-v1.yaml"
    )
    old_incident_path = (
        "documentation/contracts/internal/"
        "incident-query-api-v1.yaml"
    )

    if registry.count(old_admin_path) != 1:
        fail("Expected one Administration registry path.")
    if registry.count(old_incident_path) != 1:
        fail("Expected one Incident registry path.")

    updated_registry = registry.replace(
        old_admin_path, new_path, 1
    ).replace(
        old_incident_path, new_path, 1
    )

    if gate.count(old_admin_path) != 1:
        fail("Expected one Administration path in FS-1.2 gate.")
    if gate.count(old_incident_path) != 1:
        fail("Expected one Incident path in FS-1.2 gate.")

    updated_gate = gate.replace(
        old_admin_path, new_path, 1
    ).replace(
        old_incident_path, new_path, 1
    )

    endpoints = [
        "/internal/api/v1/administration/overview:",
        "/internal/api/v1/administration/settings:",
        "/internal/api/v1/administration/integrations:",
        "/internal/api/v1/incidents:",
        "/internal/api/v1/incidents/{incidentId}:",
    ]
    for endpoint in endpoints:
        if MERGED_CONTRACT.count(endpoint) != 1:
            fail(f"Merged endpoint invariant failed: {endpoint}")

    MERGED.write_text(MERGED_CONTRACT, encoding="utf-8")
    REGISTRY.write_text(updated_registry, encoding="utf-8")
    FULLSTACK_GATE.write_text(updated_gate, encoding="utf-8")
    ADMIN.unlink()
    INCIDENT.unlink()

    final_registry = REGISTRY.read_text(encoding="utf-8")

    if final_registry.count(new_path) != 2:
        fail("Expected exactly two registry capabilities on merged path.")

    if ADMIN.exists() or INCIDENT.exists():
        fail("Old physical contracts still exist.")

    print("FS-2.1.1 applied successfully.")
    print("2 registry capabilities / 1 physical contract / 5 endpoints")
    print()
    print("Validate:")
    print("  git diff -- documentation/contracts frontend/scripts/verify-full-stack-conformance.mjs")
    print("  cd frontend")
    print("  npm run verify:full-stack-conformance")
    print("  npm run verify:integration-contract-backed")


if __name__ == "__main__":
    main()
