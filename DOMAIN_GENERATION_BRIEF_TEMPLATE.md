# SIXPAY CONNECT — Domain Generation Brief

> Copier ce modèle pour chaque campagne. Remplacer tous les marqueurs `<...>`. Un brief
> contenant un marqueur non résolu ne peut pas être approuvé.

## 1. Métadonnées

| Champ | Valeur |
| --- | --- |
| Identifiant | `<domain>-<capability>-v<version>` |
| Domaine | `<domain>` |
| Capacité | `<capability>` |
| Version du brief | `<semver>` |
| Statut | `DRAFT / IN_REVIEW / APPROVED / SUPERSEDED` |
| Product Owner | `<name-or-team>` |
| Architecte | `<name-or-team>` |
| Tech Lead | `<name-or-team>` |
| Date d’approbation | `<YYYY-MM-DD>` |
| Stratégie IA | `AI_GENERATION_STRATEGY.md@1.0.0` |
| Master Prompt | `V0 / V1 / V1.1` |

## 2. Objectif et valeur métier

- Problème : `<problem>`
- Résultat attendu : `<outcome>`
- Utilisateurs concernés : `<actors>`
- Indicateurs de succès : `<measurable-indicators>`

## 3. Périmètre

### Inclus

- `<included-use-case>`

### Hors périmètre

- `<explicit-non-goal>`

### Hypothèses approuvées

- `<approved-assumption-and-approver>`

## 4. Sources d’autorité applicables

| Priorité | Source | Version/révision | Sections applicables |
| --- | --- | --- | --- |
| 1 | `<volume-or-adr>` | `<version>` | `<sections>` |
| 2 | `<requirements>` | `<version>` | `<criteria>` |
| 3 | `<contract-path>` | `<hash-or-version>` | `<operations>` |

## 5. Langage ubiquitaire

| Terme | Définition | Termes interdits/ambigus |
| --- | --- | --- |
| `<term>` | `<definition>` | `<aliases-to-avoid>` |

## 6. Frontière du domaine

- Responsabilités : `<responsibilities>`
- Données possédées : `<owned-data>`
- Domaines amont : `<upstream-domains-and-contracts>`
- Domaines aval : `<downstream-domains-and-contracts>`
- Interactions interdites : `<forbidden-couplings>`

## 7. Modèle métier

### Agrégats et Value Objects

| Élément | Type | Identité | Responsabilité |
| --- | --- | --- | --- |
| `<name>` | `Aggregate Root / Entity / Value Object` | `<identity>` | `<responsibility>` |

### Invariants

| ID | Invariant | Moment de contrôle | Erreur attendue |
| --- | --- | --- | --- |
| `INV-001` | `<rule>` | `<command-or-transition>` | `<problem-code>` |

### États et transitions

| État initial | Commande | Conditions | État final | Événement |
| --- | --- | --- | --- | --- |
| `<from>` | `<command>` | `<guards>` | `<to>` | `<event>` |

## 8. Cas d’usage et critères d’acceptation

| ID | Acteur | Cas d’usage | Préconditions | Résultat |
| --- | --- | --- | --- | --- |
| `UC-001` | `<actor>` | `<use-case>` | `<preconditions>` | `<result>` |

### Critères

- `AC-001` — Given `<context>`, when `<action>`, then `<outcome>`.
- `AC-002` — Given `<forbidden-context>`, when `<action>`, then `<problem>`.

## 9. Sécurité

| Rôle | Action | Portée objet | Autorisé | Preuve attendue |
| --- | --- | --- | --- | --- |
| `<role>` | `<action>` | `<ownership-or-scope>` | `yes/no` | `<test>` |

- Données sensibles : `<classification>`
- Masquage/rétention : `<rules>`
- Exigences d’audit : `<actor-action-object-result-correlation>`

## 10. Contrat API

| Méthode | Route | Operation ID | Rôle | Succès | Erreurs RFC 7807 |
| --- | --- | --- | --- | --- | --- |
| `<METHOD>` | `/api/v1/<resource>` | `<operationId>` | `<role>` | `<status/schema>` | `<status/problem-code>` |

- Fichier OpenAPI : `<path>`
- Compatibilité/versionnement : `<rules>`
- Idempotency key : `<required-or-not-applicable>`
- Pagination/tri/filtres : `<contract-or-not-applicable>`

## 11. Événements et intégrations

| Direction | Événement/contrat | Version | Producteur | Consommateur | Garantie |
| --- | --- | --- | --- | --- | --- |
| `IN/OUT` | `<name>` | `<version>` | `<producer>` | `<consumer>` | `<delivery-ordering-idempotence>` |

- Outbox/retry/DLQ : `<rules>`
- Corrélation et traçabilité : `<rules>`
- Systèmes externes simulés en test : `<systems>`

## 12. Persistance et migrations

- Tables possédées : `<tables>`
- Contraintes et index : `<constraints>`
- Migration Flyway prévue : `<path-or-none>`
- Compatibilité montante/descendante : `<strategy>`
- Volumétrie et conservation : `<requirements>`
- Reprise/rollback : `<procedure>`

## 13. Frontend

- Parcours/pages : `<routes-and-pages-or-not-applicable>`
- États UI : `<loading-empty-success-error-forbidden>`
- Composants Design System : `<sp-components>`
- Modèles et mapping API : `<models>`
- Gestion d’état : `<signals-rxjs>`
- Responsive : `<breakpoints-or-reference>`
- Accessibilité : `<keyboard-focus-labels-live-regions>`
- Thème/libellés configurables : `<requirements>`

## 14. Exigences non fonctionnelles

| Catégorie | Exigence mesurable | Preuve |
| --- | --- | --- |
| Performance | `<target>` | `<test-or-metric>` |
| Résilience | `<target>` | `<test>` |
| Observabilité | `<logs-metrics-traces>` | `<dashboard-or-test>` |
| Accessibilité | `WCAG A/AA` | `<axe-and-manual-check>` |

## 15. Plan de tests et traçabilité

| Exigence | Test | Niveau | Commande/CI | Preuve |
| --- | --- | --- | --- | --- |
| `AC-001` | `<test-name>` | `<unit/integration/contract/e2e>` | `<command>` | `<artifact>` |

## 16. Périmètre de fichiers

### Autorisés

- `<path-or-glob>`

### Interdits

- `<path-or-glob>`

## 17. Gates et commandes

| Gate | Critère de succès | Commande/preuve |
| --- | --- | --- |
| `IA-0` | Brief prêt | `<approval-reference>` |
| `IA-1..IA-7` | `<criterion>` | `<command-or-artifact>` |

## 18. Décisions ouvertes

| ID | Question | Options/impacts | Propriétaire | Échéance | Bloquante |
| --- | --- | --- | --- | --- | --- |
| `DEC-001` | `<question>` | `<options>` | `<owner>` | `<date>` | `yes/no` |

## 19. Definition of Ready

- [ ] Tous les marqueurs sont remplacés.
- [ ] Critères d’acceptation et invariants approuvés.
- [ ] Contrats API/événements et matrice des rôles approuvés.
- [ ] Migration, sécurité, NFR et tests définis.
- [ ] Chemins autorisés/interdits définis.
- [ ] Décisions bloquantes résolues.
- [ ] Product Owner, Architecture et Tech Lead ont approuvé.

## 20. Approbations

| Autorité | Nom/équipe | Décision | Date | Référence |
| --- | --- | --- | --- | --- |
| Product | `<name>` | `APPROVED/REJECTED` | `<date>` | `<link-or-id>` |
| Architecture | `<name>` | `APPROVED/REJECTED` | `<date>` | `<link-or-id>` |
| Engineering | `<name>` | `APPROVED/REJECTED` | `<date>` | `<link-or-id>` |
| Security, si requis | `<name>` | `<decision>` | `<date>` | `<link-or-id>` |
