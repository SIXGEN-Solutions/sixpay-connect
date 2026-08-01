# Traçabilité d’acceptation — Payment Lot 9

| ID | Critère | Preuve |
| --- | --- | --- |
| `PAY-L9-ACC-001` | 20 sections présentes et ordonnées | `PaymentBriefValidationTest` |
| `PAY-L9-ACC-002` | aucun marqueur non résolu | `briefContainsNoUnresolvedMarker` |
| `PAY-L9-ACC-003` | modèle complet | `PAYMENT_IA1_GATE_VALIDATION.yaml` |
| `PAY-L9-ACC-004` | persistance et tests décrits | sections 12 et 15 |
| `PAY-L9-ACC-005` | décisions ouvertes explicitement classées | section 18 |
| `PAY-L9-ACC-006` | Definition of Ready complète | section 19 |
| `PAY-L9-ACC-007` | approbations non fabriquées | section 20 et validation YAML |
| `PAY-L9-ACC-008` | statut `IN_REVIEW` cohérent | manifeste et gate validation |
| `PAY-L9-ACC-009` | génération globale interdite | manifeste et section 17 |
| `PAY-L9-ACC-010` | aucun comportement domaine modifié | périmètre Lot 9 |
