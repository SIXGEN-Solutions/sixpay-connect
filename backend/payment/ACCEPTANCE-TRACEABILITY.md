# Traçabilité d’acceptation — Payment Lot 3.4

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L3.4-ACC-001` | 14 Policies présentes | `PaymentArchitectureTest.fourteenPoliciesTwelveProfilesAndFourServicesExist` |
| `PAY-L3.4-ACC-002` | 12 profils immuables présents | même test d’architecture |
| `PAY-L3.4-ACC-003` | 4 Domain Services présents | même test d’architecture |
| `PAY-L3.4-ACC-004` | aucun I/O, repository, client externe ou horloge | `policiesAndServicesRemainPure` |
| `PAY-L3.4-ACC-005` | aucune mutation Payment ni event registration | `noAggregateMutationOrEventPackageIsIntroduced` et scan des tokens |
| `PAY-L3.4-ACC-006` | décisions temporelles typées | `CoreAcceptancePoliciesTest` |
| `PAY-L3.4-ACC-007` | acceptation authorization/banking/funds typée | `CoreAcceptancePoliciesTest` |
| `PAY-L3.4-ACC-008` | replay, instruction unique et failure classification | `ReplayAuthorizationAndFailurePoliciesTest` |
| `PAY-L3.4-ACC-009` | result intent indépendant de Notification | `ResultAndDisclosurePoliciesTest` |
| `PAY-L3.4-ACC-010` | divulgation explicite allowlist/denylist | `ResultAndDisclosurePoliciesTest` |
| `PAY-L3.4-ACC-011` | services posting/TFJ/result retournent des décisions | `DomainServicesTest` |
| `PAY-L3.4-ACC-012` | seule l’autorisation Lot 3.4 est active | `currentAuthorizationIsLot34DomainOnly` |
