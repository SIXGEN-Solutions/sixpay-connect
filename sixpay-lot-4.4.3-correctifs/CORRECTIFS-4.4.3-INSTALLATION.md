# Correctifs du Lot 4.4.3

## Cause corrigée

The Payment module was made to depend directly on Customer. Consequently,
Payment integration tests discovered `CustomerModuleConfiguration` and tried
to instantiate the Amplitude OAuth2 client even though those tests do not
configure an `OAuth2AuthorizedClientManager`.

## Correct architecture

```text
payment  -X-> customer
customer -X-> payment

bootstrap
  -> payment
  -> customer
  -> CustomerVerificationModuleAdapter
```

## Installation

1. Extract this ZIP at the repository root.
2. Run the cleanup script so the old Payment adapter is deleted.

PowerShell:

```powershell
.pply-correctifs-4.4.3.ps1
```

Bash:

```bash
chmod +x apply-correctifs-4.4.3.sh
./apply-correctifs-4.4.3.sh
```

3. Verify no Customer dependency remains in Payment:

```bash
git grep "com.sixpay.customer" -- backend/payment
```

The command must return no production import.

4. The Amplitude banking adapter is now opt-in:

```yaml
sixpay:
  customer:
    verification:
      banking:
        enabled: true
```

Without this property, its OAuth2, SSL and RestClient beans are not created.

## Validation

```bash
cd backend

mvn --batch-mode --no-transfer-progress     -pl payment -am clean test

mvn --batch-mode --no-transfer-progress     -pl customer -am clean test

mvn --batch-mode --no-transfer-progress     -pl bootstrap -am clean test

mvn --batch-mode --no-transfer-progress     clean verify
```

## Important

`PaymentCustomerIntegrationConfiguration` only creates the adapter when a
`VerifyCustomerUseCase` bean exists. The concrete Spring wiring of
`CustomerVerificationService`, repository and event publisher remains part of
the next Customer infrastructure lot.
