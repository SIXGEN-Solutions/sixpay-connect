Feature: Partner MVP acceptance

  Scenario: P-CUC-01 create a valid partner
    Given an administrator
    When the administrator creates a valid partner
    Then the response status is 201
    And a partner id is returned

  Scenario: P-CUC-02 new partner starts pending validation
    Given an administrator
    When the administrator creates a valid partner
    Then the response status is 201
    And the partner status is "PENDING_VALIDATION"

  Scenario: P-CUC-05 reject invalid partner creation
    Given an administrator
    When the administrator submits an invalid partner
    Then the response status is 400

  Scenario: P-CUC-11 manager approves a pending partner
    Given a pending partner exists
    When the manager approves the partner
    Then the response status is 200
    And the partner status is "ACTIVE"

  Scenario: P-CUC-13 manager rejects a pending partner
    Given a pending partner exists
    When the manager rejects the partner
    Then the response status is 200
    And the partner status is "REJECTED"

  Scenario: P-CUC-19 invalid lifecycle transition is rejected
    Given an active partner exists
    When the manager tries to approve the already active partner
    Then the response status is 422

  Scenario: P-CUC-21 administrator suspends an active partner
    Given an active partner exists
    When the administrator suspends the partner
    Then the response status is 200
    And the partner status is "SUSPENDED"

  Scenario: P-CUC-28 administrator reactivates a suspended partner
    Given a suspended partner exists
    When the administrator reactivates the partner
    Then the response status is 200
    And the partner status is "ACTIVE"

  Scenario: P-CUC-33 administrator configures a validation threshold
    Given an active partner exists
    When the administrator configures a payment validation threshold
    Then the response status is 200
    And the configured threshold is returned

  Scenario: P-CUC-45 partner reads its own status
    Given an active partner exists
    When the partner reads its own status
    Then the response status is 200
    And the partner status is "ACTIVE"

  Scenario: P-CUC-46 partner cannot read another partner status
    Given an active partner exists
    When another partner tries to read that status
    Then the response status is 403

  Scenario: P-CUC-48 creation produces an audit record
    Given an administrator
    When the administrator creates a partner for audit verification
    Then the response status is 201
    And one audit record has been added

  Scenario: P-CUC-58 partner creation is idempotent
    Given an administrator
    When the administrator replays the same creation with the same idempotency key
    Then the response status is 201
    And only one partner has been created
    And only one creation audit record has been added
    And only one creation outbox event has been added

  Scenario: P-CUC-64 creation produces an outbox event
    Given an administrator
    When the administrator creates a partner for outbox verification
    Then the response status is 201
    And one outbox event has been added

  Scenario: P-CUC-72 failed mutation does not persist partial state
    Given an active partner exists
    When an invalid second approval is attempted
    Then the failed mutation leaves no partial persistence
