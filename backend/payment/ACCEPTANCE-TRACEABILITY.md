# Traçabilité d’acceptation — Payment Lot 3.10

| ID | Critère | Preuve automatisée |
| --- | --- | --- |
| `PAY-L3.10-ACC-001` | 17 états et 4 terminaux | `PaymentDomainKernelCatalogueTest` |
| `PAY-L3.10-ACC-002` | 17 opérations nommées | `PaymentDomainKernelCatalogueTest` |
| `PAY-L3.10-ACC-003` | 38 transitions `PAY-TR-*` | `PaymentDomainKernelCatalogueTest` + `PAYMENT_STATE_MACHINE.yaml` |
| `PAY-L3.10-ACC-004` | 76 invariants `PAY-INV-*` | `PaymentDomainKernelCatalogueTest` + `PAYMENT_INVARIANT_CATALOGUE.yaml` |
| `PAY-L3.10-ACC-005` | 33 événements `PAY-EVT-*` | `PaymentEventCatalogueTest` + `PaymentDomainKernelCatalogueTest` |
| `PAY-L3.10-ACC-006` | 14 Policies et 4 Domain Services | `PaymentArchitectureTest` |
| `PAY-L3.10-ACC-007` | quatre états terminaux sans mutation | `PaymentTerminalStateProtectionTest` |
| `PAY-L3.10-ACC-008` | version et séquence ordonnées | tests de cycles préfinancier, posting, TFJ et reversal |
| `PAY-L3.10-ACC-009` | replay identique et conflits atomiques | tests Aggregate Root existants |
| `PAY-L3.10-ACC-010` | confidentialité événementielle | `PaymentEventCatalogueTest` |
| `PAY-L3.10-ACC-011` | domaine sans Spring/JPA/infrastructure | `PaymentArchitectureTest` |
| `PAY-L3.10-ACC-012` | exception dans `domain.exception` | `PaymentArchitectureTest` |
| `PAY-L3.10-ACC-013` | compilation Java 21 | build Maven / validation locale |
