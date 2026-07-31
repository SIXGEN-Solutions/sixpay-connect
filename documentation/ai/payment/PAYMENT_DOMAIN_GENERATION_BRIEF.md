# SIXPAY CONNECT — Payment Domain Generation Brief

> Ce brief consolide le Gate IA-0P et prépare le Contract Pack Payment. Il
> n’autorise aucune génération de code, de schéma, de migration ou de contrat.

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Domaine pilote | `payment` |
| Gate courant | `IA-0P_PAYMENT_PREFLIGHT` |
| Branche | `feat/payment-contract-pack` |
| Commit de référence | `00469905c048200277b4012238486e28f62a50b8` |
| Statut | `IA-0P_PASSED` |
| Génération de code | **Interdite** |
| Gate suivant | `IA-0.5P_PAYMENT_CONTRACT_PACK` |

## 2. Usage et autorité

Ce document est le brief humain consolidé du domaine Payment. Il doit être lu
avec `AI_CONTEXT_MANIFEST.yaml`, qui constitue l’index machine-readable, et
avec les livrables normatifs qu’il référence.

La hiérarchie de `PAYMENT_SOURCE_BASELINE.md` s’applique. Une exigence Payment
doit citer une source ou être identifiée comme décision SIXPAY. En cas de
contradiction, les décisions IA-0R priment.

## 3. Périmètre du MVP

Le MVP couvre :

- la réception et la persistance immédiate d’un ordre TRESOR PAY ;
- la création ou l’actualisation de la projection `ObservedCustomer` ;
- la vérification Amplitude du client, du compte, de son appartenance, de son
  statut, de ses blocages, oppositions et fonds disponibles ;
- le débit du client et le crédit comptable du CUT ;
- la notification du résultat immédiat ;
- l’attente, le rapprochement et la confirmation TFJ ;
- la notification du résultat définitif ;
- la consultation et l’audit dans SIXPAY.

Sont exclus :

- la gestion locale et la validation SIXPAY des abonnements ;
- le KYC numérique par document et selfie ;
- la compensation interbancaire hors confirmation TFJ ;
- la gestion des marchands TRESOR PAY.

## 4. Autorité et responsabilités

| Fait ou opération | Système maître |
| --- | --- |
| Abonnement et ordre de paiement | TRESOR PAY |
| Client, compte et écritures bancaires | Amplitude |
| Paiement traité et audit d’intégration | SIXPAY |
| Projection `ObservedCustomer` | SIXPAY |
| Confirmation TFJ | Amplitude |

Les modules SIXPAY se répartissent ainsi :

- `payment` possède l’agrégat, ses décisions et son cycle de vie ;
- `customer` interprète les vérifications et maintient `ObservedCustomer` ;
- `integration` possède les adaptateurs et contrats de transport externes ;
- `accounting` interprète posting, résultat inconnu, TFJ et extourne ;
- `notification` garantit la livraison des résultats à TRESOR PAY ;
- `reporting` expose les projections de consultation et d’export.

## 5. Parcours canonique

```text
TRESOR PAY → réception SIXPAY → persistance → contrôles Amplitude
           → débit client → crédit CUT → notification immédiate
           → attente TFJ → confirmation TFJ → notification définitive
```

Tout résultat financier inconnu ou partiel est rapproché avant décision. Une
écriture financière ne doit jamais être rejouée aveuglément. Les parcours
alternatifs et leurs issues normatives sont définis dans
`PAYMENT_BUSINESS_FLOWS.md`.

## 6. Modèle métier

`Payment` est l’unique Aggregate Root d’écriture du périmètre. Il contient son
identité, ses références externes et de corrélation, le montant, les snapshots
minimaux nécessaires, les décisions bancaires acceptées, les résultats de
posting, les intentions de notification, la confirmation TFJ et l’extourne.

Les identifiants typés, `Money`, `PayerSnapshot`, `BankAccountReference`,
`FinancialInstitutionCode`, `BankingVerification`, `PostingResult`,
`TreasuryAccount`, `NotificationDelivery`, `EndOfDayConfirmation` et
`Reversal` suivent les classifications exactes de
`PAYMENT_DOMAIN_MODEL.md`.

`ObservedCustomer` reste une projection CQRS reconstruisible. Il ne devient ni
Aggregate Root Payment, ni référentiel KYC, ni système maître bancaire.

## 7. États, invariants et événements

La machine normative comporte 16 états et 34 transitions. Ses états terminaux
sont `REJECTED`, `TREASURY_INTEGRATED`, `FAILED` et `REVERSED`.

Les invariants essentiels sont :

- persister avant tout appel externe ;
- garantir l’idempotence métier et technique ;
- interdire tout posting avant contrôles favorables ;
- séparer résultat immédiat et finalité TFJ ;
- ne pas confondre intention Outbox et accusé de livraison ;
- ne pas modifier un état financier à cause d’un échec de notification ;
- rapprocher tout outcome inconnu avant reprise ou extourne ;
- conserver les échecs dans la consultation et l’audit.

Les invariants 0P.7 sont intégrés à `PAYMENT_DOMAIN_MODEL.md` et
`PAYMENT_STATE_MACHINE.yaml`. Le catalogue normatif contient 16 événements :
les 15 candidats et `PaymentFailed`, requis par l’état terminal `FAILED`.

## 8. Contrats à produire au Gate suivant

Les sept familles correspondent à huit artefacts OpenAPI 3.1 :

1. demande de paiement TRESOR PAY → SIXPAY ;
2. vérification et contrôle des fonds SIXPAY → Amplitude ;
3. débit client et crédit CUT SIXPAY → Amplitude ;
4. notification immédiate SIXPAY → TRESOR PAY ;
5. confirmation TFJ Amplitude → SIXPAY, avec fallback de rapprochement ;
6. notification définitive TFJ SIXPAY → TRESOR PAY ;
7. APIs SIXPAY de consultation Payment, ObservedCustomer et audit.

Le Gate IA-0.5P devra fixer les payloads, erreurs RFC 7807, authentification,
idempotence, corrélation, timeouts, retries, compatibilité et responsabilités.

## 9. Sécurité, audit et observabilité

- TRESOR PAY utilise Token + Subscription Key pour le MVP.
- Les secrets sont externalisés, rotatifs et révocables.
- TLS 1.3 et AES-256 ou équivalent validé sont requis.
- RIB/IBAN sont minimisés et masqués hors frontière bancaire autorisée.
- Aucun credential, secret ou compte en clair ne figure dans les logs.
- Les droits suivent la matrice `OPS`, `MANAGER`, `AUDITOR`, `ADMIN`.
- Consultations, exports, rejeux et actions sensibles sont audités.
- Logs, traces, événements et appels portent la corrélation de bout en bout.
- Des alertes couvrent les paiements bloqués, TFJ absente, DLQ, notifications,
  outcomes inconnus et extournes.

## 10. Résilience

Le traitement impose idempotence entrante, Outbox transactionnelle, livraison
au moins une fois, retry borné avec backoff exponentiel et full jitter, circuit
breaker, DLQ, rapprochement bancaire, quarantaine TFJ, reprise ordonnée,
replay contrôlé et runbook d’extourne avec double contrôle.

Un retry technique, une republication et une reprise financière sont trois
opérations distinctes. Aucun automatisme ne doit transformer une incertitude
bancaire en double effet financier.

## 11. Vérification attendue

La stratégie comprend 13 familles et 142 scénarios identifiés. Elle exige :

- la couverture des 34 transitions et de toutes les transitions interdites ;
- des tests d’idempotence, concurrence et rejeu ;
- la couverture des huit artefacts contractuels ;
- des tests Amplitude, notifications et TFJ ;
- des tests RBAC, masquage, audit et absence de fuite ;
- la reconstruction d’`ObservedCustomer` ;
- les résultats inconnus, écritures partielles et extournes ;
- l’exécution du profil Maven `full-tests`.

## 12. Contraintes de génération

Avant approbation du Gate `IA-0.5P_PAYMENT_CONTRACT_PACK`, il est interdit de :

- générer ou modifier du code applicatif ;
- créer des tables, migrations ou mappings de persistance ;
- produire les OpenAPI cibles ou des clients/serveurs dérivés ;
- inventer un champ, état, événement, responsabilité ou règle métier ;
- activer les contrats TRESOR PAY d’abonnement classés hors MVP.

Les validations externes encore attendues portent notamment sur les contrats,
capacités, authentification et SLA Amplitude/TRESOR PAY, ainsi que sur les
signatures Architecture, Payment, Integration, Security et Operations.

## 13. Verdict

Le contrôle final 0P.14 confirme la cohérence du Gate, de ce brief et du
manifeste. Les 55 exigences sont tracées, les 15 parcours alternatifs sont
couverts, la machine à 16 états et 34 transitions est cohérente, les neuf
contrats cibles sont recensés et aucune ambiguïté structurante ne reste
ouverte.

Le contexte Payment est prêt pour la production et l’approbation du Contract
Pack. Il n’est pas prêt pour la génération de code.

```text
IA-0P PASSED
READY FOR IA-0.5P — PAYMENT CONTRACT PACK
CODE GENERATION FORBIDDEN
```
