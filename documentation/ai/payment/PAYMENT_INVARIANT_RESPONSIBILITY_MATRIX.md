# Payment — Matrice de responsabilité des invariants

| Responsable | Nombre d’invariants concernés | Références |
| --- | ---: | --- |
| `aggregate` | 65 | `PAY-INV-001` `PAY-INV-002` `PAY-INV-004` `PAY-INV-005` `PAY-INV-006` `PAY-INV-007` `PAY-INV-008` `PAY-INV-009` `PAY-INV-010` `PAY-INV-011` `PAY-INV-012` `PAY-INV-013` `PAY-INV-015` `PAY-INV-016` `PAY-INV-018` `PAY-INV-020` `PAY-INV-021` `PAY-INV-022` `PAY-INV-023` `PAY-INV-024` `PAY-INV-025` `PAY-INV-026` `PAY-INV-027` `PAY-INV-028` `PAY-INV-029` `PAY-INV-030` `PAY-INV-031` `PAY-INV-032` `PAY-INV-033` `PAY-INV-034` `PAY-INV-035` `PAY-INV-036` `PAY-INV-037` `PAY-INV-038` `PAY-INV-039` `PAY-INV-041` `PAY-INV-042` `PAY-INV-043` `PAY-INV-044` `PAY-INV-045` `PAY-INV-046` `PAY-INV-047` `PAY-INV-048` `PAY-INV-049` `PAY-INV-051` `PAY-INV-053` `PAY-INV-054` `PAY-INV-055` `PAY-INV-056` `PAY-INV-057` `PAY-INV-058` `PAY-INV-059` `PAY-INV-060` `PAY-INV-061` `PAY-INV-062` `PAY-INV-063` `PAY-INV-064` `PAY-INV-065` `PAY-INV-066` `PAY-INV-069` `PAY-INV-070` `PAY-INV-072` `PAY-INV-073` `PAY-INV-074` `PAY-INV-076` |
| `application` | 26 | `PAY-INV-003` `PAY-INV-016` `PAY-INV-018` `PAY-INV-019` `PAY-INV-023` `PAY-INV-024` `PAY-INV-025` `PAY-INV-031` `PAY-INV-039` `PAY-INV-040` `PAY-INV-042` `PAY-INV-050` `PAY-INV-051` `PAY-INV-052` `PAY-INV-053` `PAY-INV-054` `PAY-INV-057` `PAY-INV-059` `PAY-INV-060` `PAY-INV-061` `PAY-INV-067` `PAY-INV-068` `PAY-INV-071` `PAY-INV-073` `PAY-INV-074` `PAY-INV-075` |
| `persistence` | 17 | `PAY-INV-003` `PAY-INV-004` `PAY-INV-010` `PAY-INV-011` `PAY-INV-012` `PAY-INV-019` `PAY-INV-039` `PAY-INV-040` `PAY-INV-050` `PAY-INV-052` `PAY-INV-067` `PAY-INV-068` `PAY-INV-070` `PAY-INV-071` `PAY-INV-072` `PAY-INV-073` `PAY-INV-074` |
| `idempotencyStore` | 5 | `PAY-INV-003` `PAY-INV-039` `PAY-INV-040` `PAY-INV-069` `PAY-INV-070` |
| `bankIntegration` | 3 | `PAY-INV-034` `PAY-INV-040` `PAY-INV-075` |
| `accounting` | 3 | `PAY-INV-054` `PAY-INV-055` `PAY-INV-058` |

## Frontière normative

Aucune couche ne doit prétendre garantir seule une règle distribuée.

- L’agrégat refuse les incohérences internes.
- L’application orchestre les comparaisons et les décisions de replay.
- La persistance arbitre les courses par contraintes atomiques.
- L’idempotency store restitue le résultat original.
- Les adaptateurs bancaires effectuent les lookups autoritatifs.
- Accounting fournit la preuve TFJ durable et uniquement appariée.
