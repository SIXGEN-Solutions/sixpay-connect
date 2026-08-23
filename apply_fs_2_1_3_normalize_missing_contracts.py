from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
REGISTRY = ROOT / "documentation/contracts/CONTRACT_REGISTRY.yaml"
GATE = ROOT / "frontend/scripts/verify-full-stack-conformance.mjs"
CUSTOMER = ROOT / "documentation/contracts/internal/customer-management-query-api-v1.yaml"
SECURITY = ROOT / "documentation/contracts/internal/security-user-administration-api-v1.yaml"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

CUSTOMER_CONTRACT = r'''openapi: 3.1.0
info:
  title: SIXPAY CONNECT - Internal Customer Management API
  version: 1.0.0
  description: |
    Canonical contract normalized from the existing Customer Management implementation.
    It documents the complete implemented /internal/api/v1/customers boundary.
    Subscription endpoints under /internal/api/v1/subscriptions are outside this contract.
    No endpoint or field is introduced by this contract.
  x-sixpay-contract:
    registryId: customer-management-query-api-v1
    gate: FS-2_REPOSITORY_BASELINE_CONSOLIDATION
    phaseStep: FS-2.1.3
    lifecycleStatus: ACTIVE_MVP
    approvalStatus: PENDING_APPROVAL
    generationPolicy: REFERENCE_ONLY
    codeGenerationAllowed: false
    domain: customer
    businessOwner: customer
    securityOwner: security
    capability: CUSTOMER_MANAGEMENT
    direction: INTERNAL_CLIENT_TO_SIXPAY
    dataClassification: CONFIDENTIAL
    sourceSystem: SIXPAY
    systemOfRecord: SIXPAY
servers:
  - url: https://sixpay-api.example
security:
  - bearerAuth: []
tags:
  - name: Customer Management
paths:
  /internal/api/v1/customers:
    get:
      tags: [Customer Management]
      operationId: searchCustomers
      summary: Search enrolled SIXPAY customers
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.read]
      parameters:
        - {name: niu, in: query, schema: {type: string, maxLength: 100}}
        - {name: legalName, in: query, schema: {type: string, maxLength: 200}}
        - name: status
          in: query
          schema: {$ref: '#/components/schemas/CustomerStatus'}
        - {name: financialInstitutionCode, in: query, schema: {type: string, maxLength: 32}}
        - {name: page, in: query, schema: {type: integer, minimum: 0, default: 0}}
        - {name: size, in: query, schema: {type: integer, minimum: 1, maximum: 100, default: 20}}
      responses:
        '200':
          description: Customer page
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerPageResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
    post:
      tags: [Customer Management]
      operationId: enrollCustomer
      summary: Enroll a verified banking customer into SIXPAY
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.create]
      parameters:
        - {name: financialInstitutionCode, in: query, required: true, schema: {type: string}}
        - {name: niu, in: query, schema: {type: string}}
        - {name: customerNumber, in: query, schema: {type: string}}
        - {name: accountReference, in: query, required: true, schema: {type: string}}
        - {$ref: '#/components/parameters/OptionalCorrelationId'}
      responses:
        '201':
          description: Customer enrolled
          headers:
            Location: {schema: {type: string, format: uri}}
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
  /internal/api/v1/customers/banking-preview:
    post:
      tags: [Customer Management]
      operationId: previewBankingCustomer
      summary: Preview a banking customer before enrollment
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.create]
      parameters:
        - {$ref: '#/components/parameters/OptionalCorrelationId'}
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/BankingCustomerPreviewRequest'}
      responses:
        '200':
          description: Banking customer preview
          content:
            application/json:
              schema: {$ref: '#/components/schemas/BankingCustomerPreviewResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
  /internal/api/v1/customers/{customerId}:
    get:
      tags: [Customer Management]
      operationId: getCustomer
      summary: Get a SIXPAY customer
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.read]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      responses:
        '200':
          description: Customer
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
    put:
      tags: [Customer Management]
      operationId: updateCustomer
      summary: Update editable SIXPAY customer profile fields
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/UpdateCustomerRequest'}
      responses:
        '200':
          description: Updated customer
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
    delete:
      tags: [Customer Management]
      operationId: closeCustomer
      summary: Logically close a SIXPAY customer
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/ReasonRequest'}
      responses:
        '204': {description: Customer logically closed}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/customers/{customerId}/suspension:
    post:
      tags: [Customer Management]
      operationId: suspendCustomer
      summary: Suspend a SIXPAY customer
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.suspend]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/ReasonRequest'}
      responses:
        '200':
          description: Suspended customer
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/customers/{customerId}/reactivation:
    post:
      tags: [Customer Management]
      operationId: reactivateCustomer
      summary: Reactivate a suspended SIXPAY customer
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      responses:
        '200':
          description: Reactivated customer
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/customers/{customerId}/accounts:
    get:
      tags: [Customer Management]
      operationId: listCustomerBankAccounts
      summary: List customer bank accounts
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.read]
      parameters: [{$ref: '#/components/parameters/CustomerId'}]
      responses:
        '200':
          description: Customer bank accounts
          content:
            application/json:
              schema:
                type: array
                items: {$ref: '#/components/schemas/CustomerBankAccountResponse'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
    post:
      tags: [Customer Management]
      operationId: addCustomerBankAccount
      summary: Lookup, freshly verify and link a bank account
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters:
        - {$ref: '#/components/parameters/CustomerId'}
        - {$ref: '#/components/parameters/OptionalCorrelationId'}
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/AddBankAccountRequest'}
      responses:
        '200':
          description: Customer with linked bank account
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/customers/{customerId}/accounts/{accountId}/default:
    put:
      tags: [Customer Management]
      operationId: setDefaultCustomerBankAccount
      summary: Set the default customer bank account
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters:
        - {$ref: '#/components/parameters/CustomerId'}
        - {$ref: '#/components/parameters/AccountId'}
      responses:
        '200':
          description: Customer with updated default bank account
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/customers/{customerId}/accounts/{accountId}:
    delete:
      tags: [Customer Management]
      operationId: unlinkCustomerBankAccount
      summary: Unlink a bank account from a customer
      x-sixpay-authorization:
        requiredAuthorities: [SCOPE_customer.update]
      parameters:
        - {$ref: '#/components/parameters/CustomerId'}
        - {$ref: '#/components/parameters/AccountId'}
      responses:
        '200':
          description: Customer with bank account unlinked
          content:
            application/json:
              schema: {$ref: '#/components/schemas/CustomerResponse'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
components:
  securitySchemes:
    bearerAuth: {type: http, scheme: bearer, bearerFormat: JWT}
  parameters:
    CustomerId:
      name: customerId
      in: path
      required: true
      schema: {type: string, format: uuid}
    AccountId:
      name: accountId
      in: path
      required: true
      schema: {type: string, format: uuid}
    OptionalCorrelationId:
      name: X-Correlation-ID
      in: header
      required: false
      schema: {type: string, maxLength: 150}
  responses:
    BadRequest: {description: Validation or request error}
    Unauthorized: {description: Authentication required}
    Forbidden: {description: Required Customer authority is missing}
    NotFound: {description: Customer or Customer bank account not found}
  schemas:
    CustomerStatus:
      type: string
      enum: [ACTIVE, SUSPENDED, CLOSED]
    CustomerBankAccountResponse:
      type: object
      required: [id, bankingAccountReference, accountBindingFingerprint, maskedAccountIdentifier, currency, accountType, defaultAccount, verifiedAt]
      properties:
        id: {type: string, format: uuid}
        bankingAccountReference: {type: string}
        accountBindingFingerprint: {type: string}
        maskedAccountIdentifier: {type: string}
        currency: {type: string}
        accountType: {type: string}
        defaultAccount: {type: boolean}
        verifiedAt: {type: string, format: date-time}
    CustomerResponse:
      type: object
      required: [id, financialInstitutionCode, bankingCustomerReference, legalName, status, createdAt, updatedAt, bankAccounts]
      properties:
        id: {type: string, format: uuid}
        financialInstitutionCode: {type: string}
        bankingCustomerReference: {type: string}
        customerNumber: {type: [string, 'null']}
        niu: {type: [string, 'null']}
        legalName: {type: string}
        email: {type: [string, 'null']}
        phoneNumber: {type: [string, 'null']}
        status: {$ref: '#/components/schemas/CustomerStatus'}
        statusReason: {type: [string, 'null']}
        createdAt: {type: string, format: date-time}
        updatedAt: {type: string, format: date-time}
        bankAccounts:
          type: array
          items: {$ref: '#/components/schemas/CustomerBankAccountResponse'}
    CustomerPageResponse:
      type: object
      required: [content, totalElements, totalPages, page, size, first, last]
      properties:
        content:
          type: array
          items: {$ref: '#/components/schemas/CustomerResponse'}
        totalElements: {type: integer, format: int64}
        totalPages: {type: integer}
        page: {type: integer}
        size: {type: integer}
        first: {type: boolean}
        last: {type: boolean}
    BankingCustomerPreviewRequest:
      type: object
      required: [financialInstitutionCode, accountReference]
      properties:
        financialInstitutionCode: {type: string, maxLength: 50}
        niu: {type: [string, 'null'], maxLength: 100}
        customerNumber: {type: [string, 'null'], maxLength: 100}
        accountReference: {type: string, maxLength: 100}
    BankingCustomerPreviewResponse:
      type: object
      required: [financialInstitutionCode, bankingCustomerReference, legalName, accountReference, maskedAccountIdentifier, currency, accountType, retrievedAt]
      properties:
        financialInstitutionCode: {type: string}
        bankingCustomerReference: {type: string}
        customerNumber: {type: [string, 'null']}
        niu: {type: [string, 'null']}
        legalName: {type: string}
        email: {type: [string, 'null']}
        phoneNumber: {type: [string, 'null']}
        accountReference: {type: string}
        maskedAccountIdentifier: {type: string}
        currency: {type: string}
        accountType: {type: string}
        retrievedAt: {type: string, format: date-time}
    UpdateCustomerRequest:
      type: object
      required: [legalName]
      properties:
        legalName: {type: string, maxLength: 200}
        email: {type: [string, 'null'], format: email, maxLength: 254}
        phoneNumber: {type: [string, 'null'], maxLength: 32}
    ReasonRequest:
      type: object
      required: [reason]
      properties:
        reason: {type: string, maxLength: 500}
    AddBankAccountRequest:
      type: object
      required: [accountReference]
      properties:
        accountReference: {type: string, maxLength: 100}
'''

SECURITY_CONTRACT = r'''openapi: 3.1.0
info:
  title: SIXPAY CONNECT - Internal Security User Administration API
  version: 1.0.0
  description: |
    Canonical contract normalized from the existing Security User Administration implementation.
    It documents the ADMIN-only boundary for user accounts, authorization assignments,
    local authentication, linked OIDC identities and local password reset.
    Authentication runtime endpoints remain outside this contract.
  x-sixpay-contract:
    registryId: security-user-administration-api-v1
    gate: FS-2_REPOSITORY_BASELINE_CONSOLIDATION
    phaseStep: FS-2.1.3
    lifecycleStatus: ACTIVE_MVP
    approvalStatus: PENDING_APPROVAL
    generationPolicy: REFERENCE_ONLY
    codeGenerationAllowed: false
    domain: security
    businessOwner: security
    deliveryBoundary: administration
    securityOwner: security
    capability: SECURITY_USER_ADMINISTRATION
    direction: INTERNAL_CLIENT_TO_SIXPAY
    dataClassification: RESTRICTED
    sourceSystem: SIXPAY
    systemOfRecord: SIXPAY
servers:
  - url: https://sixpay-api.example
security:
  - bearerAuth: []
tags:
  - name: Security User Administration
paths:
  /internal/api/v1/administration/users:
    get:
      tags: [Security User Administration]
      operationId: listSecurityUsers
      summary: List SIXPAY security users
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      responses:
        '200':
          description: Security users
          content:
            application/json:
              schema:
                type: array
                items: {$ref: '#/components/schemas/SecurityUserSummary'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
    post:
      tags: [Security User Administration]
      operationId: createSecurityUser
      summary: Create a SIXPAY security user
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/CreateSecurityUserRequest'}
      responses:
        '201':
          description: Security user created
          headers:
            Location: {schema: {type: string, format: uri}}
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
  /internal/api/v1/administration/users/{userId}:
    get:
      tags: [Security User Administration]
      operationId: getSecurityUser
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      responses:
        '200':
          description: Security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
    put:
      tags: [Security User Administration]
      operationId: updateSecurityUser
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/UpdateSecurityUserRequest'}
      responses:
        '200':
          description: Updated security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
    delete:
      tags: [Security User Administration]
      operationId: deleteSecurityUser
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      responses:
        '204': {description: Security user deleted}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/enable:
    post:
      tags: [Security User Administration]
      operationId: enableSecurityUser
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      responses:
        '200':
          description: Enabled security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/disable:
    post:
      tags: [Security User Administration]
      operationId: disableSecurityUser
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      responses:
        '200':
          description: Disabled security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/authentication-methods/local:
    put:
      tags: [Security User Administration]
      operationId: setLocalAuthenticationMethod
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/SetAuthenticationMethodRequest'}
      responses:
        '200':
          description: Updated security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/local-password-reset:
    post:
      tags: [Security User Administration]
      operationId: resetLocalPassword
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/ResetLocalPasswordRequest'}
      responses:
        '200':
          description: Updated security user
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/identities/oidc:
    post:
      tags: [Security User Administration]
      operationId: linkOidcIdentity
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters: [{$ref: '#/components/parameters/UserId'}]
      requestBody:
        required: true
        content:
          application/json:
            schema: {$ref: '#/components/schemas/LinkOidcIdentityRequest'}
      responses:
        '200':
          description: Security user with linked OIDC identity
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '400': {$ref: '#/components/responses/BadRequest'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
  /internal/api/v1/administration/users/{userId}/identities/{identityId}:
    delete:
      tags: [Security User Administration]
      operationId: unlinkOidcIdentity
      x-sixpay-authorization: {requiredRoles: [ADMIN]}
      parameters:
        - {$ref: '#/components/parameters/UserId'}
        - {$ref: '#/components/parameters/IdentityId'}
      responses:
        '200':
          description: Security user with identity unlinked
          content:
            application/json:
              schema: {$ref: '#/components/schemas/SecurityUserDetail'}
        '401': {$ref: '#/components/responses/Unauthorized'}
        '403': {$ref: '#/components/responses/Forbidden'}
        '404': {$ref: '#/components/responses/NotFound'}
components:
  securitySchemes:
    bearerAuth: {type: http, scheme: bearer, bearerFormat: JWT}
  parameters:
    UserId:
      name: userId
      in: path
      required: true
      schema: {type: string, format: uuid}
    IdentityId:
      name: identityId
      in: path
      required: true
      schema: {type: string, format: uuid}
  responses:
    BadRequest: {description: Validation or request error}
    Unauthorized: {description: Authentication required}
    Forbidden: {description: ROLE_ADMIN is required}
    NotFound: {description: Security user or identity not found}
  schemas:
    SecurityUserAccountStatus:
      type: string
      enum: [ACTIVE, DISABLED]
    AuthenticationIdentityType:
      type: string
      enum: [LOCAL, OIDC]
    SecurityAuditEventType:
      type: string
      enum: [LOGIN_SUCCESS, LOGIN_FAILURE, LOGOUT, PASSWORD_CHANGED, PASSWORD_RESET, ACCOUNT_LOCKED, OIDC_LOGIN_SUCCESS, OIDC_LOGIN_FAILURE, IDENTITY_LINKED, IDENTITY_UNLINKED, AUTH_METHOD_ENABLED, AUTH_METHOD_DISABLED, USER_CREATED, USER_UPDATED, USER_ENABLED, USER_DISABLED, USER_DELETED]
    CreateSecurityUserRequest:
      type: object
      required: [username, roles, permissions, localAuthenticationEnabled]
      properties:
        username: {type: string, maxLength: 150}
        email: {type: [string, 'null'], format: email, maxLength: 320}
        roles:
          type: array
          uniqueItems: true
          items: {type: string, maxLength: 100}
        permissions:
          type: array
          uniqueItems: true
          items: {type: string, maxLength: 150}
        localAuthenticationEnabled: {type: boolean}
        initialPassword: {type: [string, 'null'], minLength: 12, maxLength: 200}
    UpdateSecurityUserRequest:
      type: object
      required: [username, roles, permissions]
      properties:
        username: {type: string, maxLength: 150}
        email: {type: [string, 'null'], format: email, maxLength: 320}
        roles:
          type: array
          uniqueItems: true
          items: {type: string, maxLength: 100}
        permissions:
          type: array
          uniqueItems: true
          items: {type: string, maxLength: 150}
    SetAuthenticationMethodRequest:
      type: object
      required: [enabled]
      properties:
        enabled: {type: boolean}
    ResetLocalPasswordRequest:
      type: object
      required: [newPassword]
      properties:
        newPassword: {type: string, minLength: 12, maxLength: 200}
    LinkOidcIdentityRequest:
      type: object
      required: [provider, providerSubject]
      properties:
        provider: {type: string, maxLength: 500}
        providerSubject: {type: string, maxLength: 255}
    SecurityUserSummary:
      type: object
      required: [id, username, status, localEnabled, oidcLinked]
      properties:
        id: {type: string, format: uuid}
        username: {type: string}
        email: {type: [string, 'null']}
        status: {$ref: '#/components/schemas/SecurityUserAccountStatus'}
        localEnabled: {type: boolean}
        oidcLinked: {type: boolean}
        lastAuthenticationAt: {type: [string, 'null'], format: date-time}
    SecurityIdentityView:
      type: object
      required: [id, identityType, status, createdAt, updatedAt]
      properties:
        id: {type: string, format: uuid}
        identityType: {$ref: '#/components/schemas/AuthenticationIdentityType'}
        provider: {type: [string, 'null']}
        providerSubject: {type: [string, 'null']}
        status: {type: string}
        createdAt: {type: string, format: date-time}
        updatedAt: {type: string, format: date-time}
    SecurityAuditView:
      type: object
      required: [eventType, occurredAt]
      properties:
        eventType: {$ref: '#/components/schemas/SecurityAuditEventType'}
        actorSubject: {type: [string, 'null']}
        provider: {type: [string, 'null']}
        detail: {type: [string, 'null']}
        occurredAt: {type: string, format: date-time}
    SecurityUserDetail:
      type: object
      required: [id, username, status, localEnabled, oidcLinked, roles, permissions, identities, recentAuthenticationEvents]
      properties:
        id: {type: string, format: uuid}
        username: {type: string}
        email: {type: [string, 'null']}
        status: {$ref: '#/components/schemas/SecurityUserAccountStatus'}
        localEnabled: {type: boolean}
        oidcLinked: {type: boolean}
        roles:
          type: array
          uniqueItems: true
          items: {type: string}
        permissions:
          type: array
          uniqueItems: true
          items: {type: string}
        identities:
          type: array
          items: {$ref: '#/components/schemas/SecurityIdentityView'}
        recentAuthenticationEvents:
          type: array
          items: {$ref: '#/components/schemas/SecurityAuditView'}
'''

CUSTOMER_REGISTRY_ENTRY = r'''
  - id: "customer-management-query-api-v1"
    path: "documentation/contracts/internal/customer-management-query-api-v1.yaml"
    domain: "customer"
    businessOwner: "customer"
    securityOwner: "security"
    capability: "CUSTOMER_MANAGEMENT"
    direction: "INTERNAL_CLIENT_TO_SIXPAY"
    sourceSystem: "SIXPAY"
    systemOfRecord: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "PENDING_APPROVAL"
    generationPolicy: "REFERENCE_ONLY"
    codeGenerationAllowed: false
    gate: "FS-2_REPOSITORY_BASELINE_CONSOLIDATION"
    phaseStep: "FS-2.1.3"
    security:
      authentication: "SIXPAY_AUTHENTICATED_PRINCIPAL"
      scopes: ["customer.read", "customer.create", "customer.update", "customer.suspend"]
      dataClassification: "CONFIDENTIAL"
    mvpUsage:
      included: true
      readOnly: false
      pagination: "OFFSET_PAGE"
      purpose:
        - "Search and read authoritative SIXPAY Customer master data"
        - "Preview banking Customer data before enrollment"
        - "Enroll and update SIXPAY Customers"
        - "Suspend, reactivate and logically close Customers"
        - "List, link, unlink and select the default verified bank account"
      constraints:
        - "ObservedCustomer projection remains outside this capability"
        - "Subscription endpoints remain outside this physical contract"
        - "Customer close is logical, not physical deletion"
'''

SECURITY_REGISTRY_ENTRY = r'''
  - id: "security-user-administration-api-v1"
    path: "documentation/contracts/internal/security-user-administration-api-v1.yaml"
    domain: "security"
    businessOwner: "security"
    deliveryBoundary: "administration"
    securityOwner: "security"
    capability: "SECURITY_USER_ADMINISTRATION"
    direction: "INTERNAL_CLIENT_TO_SIXPAY"
    sourceSystem: "SIXPAY"
    systemOfRecord: "SIXPAY"
    lifecycleStatus: "ACTIVE_MVP"
    approvalStatus: "PENDING_APPROVAL"
    generationPolicy: "REFERENCE_ONLY"
    codeGenerationAllowed: false
    gate: "FS-2_REPOSITORY_BASELINE_CONSOLIDATION"
    phaseStep: "FS-2.1.3"
    security:
      authentication: "SIXPAY_AUTHENTICATED_PRINCIPAL"
      authorization: "ROLE_BASED"
      roles: ["ADMIN"]
      dataClassification: "RESTRICTED"
    mvpUsage:
      included: true
      readOnly: false
      purpose:
        - "Create, list, read, update and delete SIXPAY security users"
        - "Enable and disable user accounts"
        - "Manage local authentication availability and local password resets"
        - "Link and unlink OIDC identities"
        - "Manage roles and permissions through the existing update boundary"
      constraints:
        - "ROLE_ADMIN is required for every endpoint"
        - "Authentication runtime endpoints are outside this contract"
        - "Passwords are request-only and never returned"
        - "Security remains owner of users, identities, roles, permissions and authentication"
'''

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def read_required(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def main():
    engineering = read_required(ENGINEERING)
    if EXPECTED_BRANCH not in engineering:
        fail(f"Expected branch not declared: {EXPECTED_BRANCH}")
    if CUSTOMER.exists() or SECURITY.exists():
        fail("Target contract already exists; review branch state.")

    registry = read_required(REGISTRY)
    gate = read_required(GATE)

    if 'id: "customer-management-query-api-v1"' in registry:
        fail("Customer registry entry already exists.")
    if 'id: "security-user-administration-api-v1"' in registry:
        fail("Security registry entry already exists.")

    marker = "\nmissingMvpContracts:\n"
    if marker not in registry:
        fail("missingMvpContracts marker not found.")

    updated_registry = registry.replace(
        marker,
        "\n" + CUSTOMER_REGISTRY_ENTRY.strip("\n")
        + "\n\n" + SECURITY_REGISTRY_ENTRY.strip("\n")
        + marker,
        1,
    )

    customer_old = """    backendOwnership: ['CustomerController'],
    contract: null,"""
    customer_new = """    backendOwnership: ['CustomerController'],
    contract: {
      path:
        'documentation/contracts/internal/customer-management-query-api-v1.yaml',
      endpointToken: '/internal/api/v1/customers',
    },"""

    security_old = """    backendOwnership: ['SecurityUserAdministration'],
    contract: null,"""
    security_new = """    backendOwnership: ['SecurityUserAdministration'],
    contract: {
      path:
        'documentation/contracts/internal/security-user-administration-api-v1.yaml',
      endpointToken: '/internal/api/v1/administration/users',
    },"""

    if customer_old not in gate:
        fail("Customer contract:null block not found in gate.")
    if security_old not in gate:
        fail("Security contract:null block not found in gate.")

    updated_gate = gate.replace(customer_old, customer_new, 1)
    updated_gate = updated_gate.replace(security_old, security_new, 1)

    old_policy = """ * - We MUST NOT invent a Partner, Customer Management or Security User
 *   Administration contract merely to satisfy this gate.
 * - Payment, Observed Customer and Payment Audit have explicit internal
 *   contracts registered by the repository and are checked against those
 *   exact files."""
    new_policy = """ * - Partner still has no published internal contract in this registry and
 *   therefore remains contract:null in this static gate.
 * - Customer Management and Security User Administration now have normalized
 *   contracts derived from their existing implemented boundaries.
 * - Every published internal contract declared here is checked against its
 *   exact physical file."""
    if old_policy in updated_gate:
        updated_gate = updated_gate.replace(old_policy, new_policy, 1)

    CUSTOMER.write_text(CUSTOMER_CONTRACT, encoding="utf-8")
    SECURITY.write_text(SECURITY_CONTRACT, encoding="utf-8")
    REGISTRY.write_text(updated_registry, encoding="utf-8")
    GATE.write_text(updated_gate, encoding="utf-8")

    print("FS-2.1.3 applied.")
    print("Created:")
    print(" - documentation/contracts/internal/customer-management-query-api-v1.yaml")
    print(" - documentation/contracts/internal/security-user-administration-api-v1.yaml")
    print("Updated:")
    print(" - documentation/contracts/CONTRACT_REGISTRY.yaml")
    print(" - frontend/scripts/verify-full-stack-conformance.mjs")
    print()
    print("Validate:")
    print(" git diff -- documentation/contracts frontend/scripts/verify-full-stack-conformance.mjs")
    print(" cd frontend")
    print(" npm run verify:full-stack-conformance")
    print(" npm run verify:integration-contract-backed")

if __name__ == "__main__":
    main()
