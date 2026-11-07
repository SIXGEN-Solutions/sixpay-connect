# INIT-2B — AppID and transport application identifier

## Approved decision

`AppID` is the required business Partner identifier carried by the
`initiateDebit` JSON payload. It maps to the registered Partner
`partnerIdentifier`.

`X-TresorPay-App-Id` remains a required transport/integration identifier.

The two values may be equal, but SIXPAY does **not** require:

```text
X-TresorPay-App-Id == AppID
```

Neither value authenticates the machine caller by itself.

The mandatory business identity invariant is:

```text
authenticated machine caller
    -> Security machine identity link
    -> registered Partner
    -> Partner.partnerIdentifier
    == request.AppID
```

`X-TresorPay-App-Id` is validated according to its own transport/security
contract and is not used as a substitute for Partner business identity.

A mismatch between the Partner resolved from the authenticated machine caller
and the Partner declared by `AppID` is rejected before Payment business
processing.
