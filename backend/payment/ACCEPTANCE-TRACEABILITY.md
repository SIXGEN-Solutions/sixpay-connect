# Traçabilité d’acceptation — Payment Lot 4

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L4-ACC-001` | 17 états normatifs | `PAYMENT_STATE_MACHINE.yaml` |
| `PAY-L4-ACC-002` | 38 transitions exhaustives | `PAYMENT_NORMATIVE_STATE_MACHINE.md` |
| `PAY-L4-ACC-003` | source, commande, gardes, cible, événements, audit, replay et effet documentés | table exhaustive Lot 4 |
| `PAY-L4-ACC-004` | diagramme Mermaid complet | `PAYMENT_NORMATIVE_STATE_MACHINE.md` |
| `PAY-L4-ACC-005` | quatre états terminaux sans sortie | matrice terminale et machine YAML |
| `PAY-L4-ACC-006` | matrice état/commande interdite exhaustive | `PAYMENT_FORBIDDEN_TRANSITION_MATRIX.md` |
| `PAY-L4-ACC-007` | matrice état vers événement externe | `PAYMENT_STATE_EXTERNAL_EVENT_MATRIX.md` |
| `PAY-L4-ACC-008` | `NOTIFIED` exclu de `PaymentStatus` | décision `PAY-DEC-IA1-100` |
| `PAY-L4-ACC-009` | outcome posting inconnu explicite | `POSTING_OUTCOME_UNKNOWN` |
| `PAY-L4-ACC-010` | outcome reversal inconnu explicite | `REVERSAL_OUTCOME_UNKNOWN` |
| `PAY-L4-ACC-011` | aucun rejeu financier aveugle | décisions `PAY-DEC-IA1-101` et machine YAML |
| `PAY-L4-ACC-012` | toute combinaison non déclarée est déterministe | `PAYMENT_INVALID_TRANSITION` |
| `PAY-L4-ACC-013` | aucun comportement Java modifié | périmètre documentaire du manifeste |
