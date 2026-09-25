# INIT-2B — AppID dual representation

## Approved decision

`X-TresorPay-App-Id` remains present and required as the transport-level
Partner identifier.

JSON `AppID` is also required and represents the same canonical
`partnerIdentifier`.

Neither value authenticates the caller on its own.

The consistency invariant is:

```text
authenticatedPartner.partnerIdentifier
    == X-TresorPay-App-Id
    == request.AppID
```

A mismatch is rejected before Payment business processing.

The authenticated Partner is obtained from the trusted M2M machine identity
and its Security-owned link to the registered Partner.
