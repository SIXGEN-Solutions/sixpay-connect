# SIXPAY CONNECT — Payment Resilience & Operational Recovery Baseline

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Document | `PAYMENT_RESILIENCE_BASELINE.md` |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.11 — Définir la stratégie de résilience` |
| Branche | `feat/payment-contract-pack` |
| Commit de référence | `1e5f45f4f9adb18a7085291e39112140d6ec0a5a` |
| Statut | `PAYMENT_RESILIENCE_BASELINE_ESTABLISHED` |
| Caractère | Normatif pour le MVP Payment |
| Génération de code | Interdite |
| Étape suivante | `0P.12 — Critères d’acceptation et stratégie de vérification` |

---

## 2. Objectif

Cette baseline définit les mécanismes permettant au parcours Payment de
résister aux doublons, timeouts, indisponibilités, arrêts de processus,
livraisons répétées et résultats bancaires ambigus sans créer de second effet
financier.

Elle formalise :

- l’idempotence entrante ;
- l’Outbox transactionnelle ;
- les retries avec backoff et jitter ;
- les circuit breakers ;
- les DLQ ;
- le rapprochement bancaire ;
- la quarantaine TFJ ;
- le traitement des résultats inconnus ;
- la reprise après incident ;
- le replay opérationnel contrôlé ;
- le runbook d’extourne.

Cette étape fixe des règles et procédures. Elle ne constitue ni une
autorisation de génération de code, ni une autorisation d’exécuter une
extourne bancaire.

---

## 3. Sources normatives

| Référence | Rôle |
| --- | --- |
| `PAYMENT_SOURCE_BASELINE.md` | Exigences Payment normalisées |
| `PAYMENT_CONTEXT_MAP.md` | Responsabilités Payment, Accounting, Integration et Notification |
| `PAYMENT_BUSINESS_FLOWS.md` | Parcours nominaux et alternatifs |
| `PAYMENT_DOMAIN_MODEL.md` | Frontière de l’agrégat et données de reprise |
| `PAYMENT_STATE_MACHINE.yaml` | Transitions et capacités de rejeu |
| `PAYMENT_EVENT_CATALOG.yaml` | Publication Outbox, déduplication et replay |
| `PAYMENT_CONTRACT_REQUIREMENTS.yaml` | Idempotence et résilience contractuelles |
| `PAYMENT_SECURITY_AUDIT_BASELINE.md` | Audit, alertes et accès aux runbooks |
| `IA_0R_BLOCKING_DECISIONS.yaml` | Defaults, outcome inconnu, extourne et TFJ |
| `backend/integration/README.md` | Implémentation Outbox existante de référence |

En cas de contradiction, les décisions `IA0R-D06`, `IA0R-D07` et `IA0R-D08`
prévalent.

### 3.1 Matrice de traçabilité

| Domaine de résilience | Exigences sources |
| --- | --- |
| Idempotence et persistance avant appel | `PAY-SRC-007`, `PAY-SRC-008`, `PAY-SRC-010` |
| Posting idempotent et outcome inconnu | `PAY-SRC-024`, `PAY-SRC-025`, `PAY-SRC-026`, `IA0R-D07` |
| TFJ, fallback et quarantaine | `PAY-SRC-030` à `PAY-SRC-033`, `IA0R-D08` |
| Outbox, consumers et projections | `PAY-SRC-037`, `PAY-SRC-038` |
| Retry, backoff, circuit breaker et DLQ | `PAY-SRC-039`, `PAY-SRC-040`, `IA0R-D06` |
| Audit, rétention et secrets | `PAY-SRC-041`, `PAY-SRC-045`, `PAY-SRC-046`, `IA0R-D03/D04/D05` |
| Contrats nécessaires à la reprise bancaire | `PAY-SRC-052`, `PAY-SRC-054` |

---

# Partie A — Principes de résilience

## 4. Principes normatifs

1. **At-least-once, effets métier idempotents** : le transport peut répéter une
   livraison ; le domaine ne répète pas l’effet.
2. **Aucune supposition après timeout** : une absence de réponse ne prouve ni
   le succès ni l’échec.
3. **Aucun rejeu financier aveugle** : un posting ou une extourne ambiguë est
   recherchée et rapprochée avant toute nouvelle instruction.
4. **Persister avant d’appeler** : demande, instruction, clé d’idempotence et
   état de reprise sont durables avant l’effet externe correspondant.
5. **Séparer décision et livraison** : un échec de notification ne modifie pas
   l’état financier.
6. **Reprise déterministe** : une opération interrompue reprend à partir des
   faits persistés, jamais à partir de la mémoire du processus.
7. **Retry borné** : toute répétition possède une limite, une classification et
   une sortie contrôlée.
8. **Échec fermé** : une dépendance indisponible ne devient jamais une
   approbation implicite.
9. **Traçabilité** : retry, rapprochement, quarantaine, replay et extourne sont
   corrélés et audités.
10. **Pas d’ordre global implicite** : chaque agrégat utilise sa version et sa
    clé de partition ; les consommateurs tolèrent les répétitions.

## 5. Taxonomie des reprises

| Type | Objet répété | Clé de protection | Effet métier permis |
| --- | --- | --- | --- |
| Rejeu entrant | Requête TRESOR PAY | `Idempotency-Key` + empreinte | Aucun nouvel effet pour un doublon identique |
| Retry de lecture | Consultation Amplitude/TFJ | Corrélation et paramètres identiques | Nouvelle observation uniquement |
| Retry d’écriture | Posting ou extourne | Clé bancaire contractuellement idempotente | Même instruction, jamais une nouvelle |
| Republication Outbox | Événement immuable | `eventId` | Aucun nouveau fait métier |
| Redelivery consumer | Message déjà consommé | `eventId` + registre consumer | Aucun second effet |
| Retry notification | Même notification | `eventId` métier | Une seule prise en compte destinataire |
| Replay opérationnel | Élément en échec/DLQ | Identité originale + nouvelle exécution | Seulement l’étape autorisée par runbook |
| Rapprochement | Recherche d’un résultat inconnu | Idempotency key ou référence bancaire | Établir un fait existant, pas le recréer |

Une implémentation ne doit pas utiliser un mécanisme générique de retry sans
identifier la ligne correspondante de cette taxonomie.

---

# Partie B — Idempotence entrante

## 6. Portée et clés

Toute demande TRESOR PAY → SIXPAY comporte :

- `Idempotency-Key` obligatoire ;
- `X-Correlation-ID` ;
- `TresorPayRequestId` ;
- institution financière ;
- payload métier canonique.

La clé d’unicité du registre SIXPAY est :

```text
TRESOR_PAY_CLIENT_ID
    + FINANCIAL_INSTITUTION_CODE
    + IDEMPOTENCY_KEY
```

`TresorPayRequestId` possède également une contrainte d’unicité dans le
périmètre contractuel approuvé.

## 7. Empreinte canonique

SIXPAY calcule une empreinte cryptographique du payload métier canonique :

- ordre et format des champs normalisés ;
- absence des headers de transport ;
- absence des credentials ;
- valeurs métier significatives uniquement ;
- algorithme et version de canonicalisation persistés.

Une évolution de canonicalisation ne doit pas rendre les enregistrements
existants impossibles à comparer.

## 8. Résolution d’une demande

| Situation | Résultat |
| --- | --- |
| Nouvelle clé | Création atomique du registre et du Payment |
| Même clé, même empreinte, traitement terminé | Restitution de l’identité et du résultat courant |
| Même clé, même empreinte, traitement en cours | Restitution du même Payment et statut `PROCESSING` |
| Même clé, empreinte différente | `409 IDEMPOTENCY_CONFLICT`, sans modifier le Payment original |
| Même `TresorPayRequestId`, clé différente | Conflit contractuel et audit |
| Deux créations concurrentes | Une seule gagne la contrainte ; l’autre relit l’enregistrement |

Le registre et la création initiale de Payment partagent une transaction.

## 9. Contenu et conservation du registre

Le registre conserve :

- scope de la clé ;
- hash de la clé lorsque la valeur brute n’est pas nécessaire ;
- version et empreinte canonique ;
- `PaymentId` ;
- état de traitement ;
- réponse ou résultat minimal restituable ;
- dates de première et dernière tentative ;
- code de conflit éventuel.

Conservation : 13 mois après la dernière tentative, conformément à
`IA0R-D04`.

La purge ne doit jamais supprimer les preuves Payment et audit encore soumises
à leur rétention de dix ans.

---

# Partie C — Outbox transactionnelle

## 10. Garantie

Tout événement produit par une mutation Payment est enregistré dans l’Outbox
dans la même transaction que :

- la version de l’agrégat ;
- la transition d’état ;
- les snapshots nécessaires ;
- l’audit métier obligatoire.

La transaction produit soit le fait et son intention de publication, soit
aucun des deux.

La garantie de transport est **au moins une fois**. L’exactly-once de bout en
bout n’est pas supposé.

## 11. Enveloppe et identité

Une ligne Outbox conserve au minimum :

- `outboxId` ;
- `eventId` immuable ;
- type et version de schéma ;
- `aggregateId`, `aggregateVersion` et clé de partition ;
- `correlationId` ;
- payload minimisé ;
- date de création ;
- statut, compteur et prochaine tentative ;
- erreur catégorisée et date de dernière tentative.

Une republication conserve `eventId`, payload et version. Elle ne recrée jamais
le fait métier.

## 12. Cycle de vie

```text
PENDING
  → PROCESSING
      → PUBLISHED
      → FAILED → PENDING/PROCESSING
      → DEAD
```

Règles :

- les lignes sont revendiquées par lots ;
- PostgreSQL `FOR UPDATE SKIP LOCKED` sépare les workers ;
- un claim abandonné redevient éligible après `processing-timeout` ;
- un succès n’est marqué `PUBLISHED` qu’après accusé du transport ;
- un échec retryable calcule `nextAttemptAt` ;
- l’épuisement ou une erreur permanente conduit à `DEAD` et à la DLQ
  opérationnelle ;
- la publication suit l’ordre `aggregateVersion` au sein d’un Payment ;
- aucun ordre global entre Payments n’est garanti.

## 13. Compatibilité avec l’Outbox existante

Le relay existant de `integration` fournit déjà :

- sélection exclusive Internal Bus/Kafka ;
- revendication concurrente ;
- reprise des claims interrompus ;
- nombre maximal de tentatives ;
- état `DEAD`.

Pour Payment, le retry linéaire historique ne constitue pas la cible
normative. Le relay doit supporter un backoff exponentiel avec jitter
configurable, ou démontrer une politique équivalente approuvée avant
activation du flux Payment.

Les valeurs existantes sont des defaults techniques, pas des SLA.

---

# Partie D — Retry, backoff et circuit breaker

## 14. Defaults techniques

Conformément à `IA0R-D06` :

| Paramètre | Default non contractuel |
| --- | --- |
| Connection timeout | 2 secondes |
| Response timeout | 10 secondes |
| Overall operation timeout | 15 secondes |
| Tentatives maximales HTTP | 3 |
| Backoff | Exponentiel avec full jitter |
| Backoff initial | 500 ms |
| Backoff maximal | 2 secondes |

Les valeurs finales proviennent de la configuration approuvée avec la banque.

## 15. Classification des erreurs

### 15.1 Retryables

- échec de connexion avant envoi prouvé ;
- timeout d’une lecture rejouable ;
- `429`, en respectant `Retry-After` ;
- `502`, `503`, `504` ;
- indisponibilité transitoire catégorisée ;
- échec de transport d’événement ou notification non permanent.

### 15.2 Non retryables automatiques

- `400` ;
- `401` ;
- `403` ;
- `404` métier ;
- `409` conflit métier/idempotence ;
- `422` ;
- signature, certificat ou schéma invalide ;
- invariant métier violé.

### 15.3 Écriture à outcome inconnu

Un timeout, une coupure ou un `5xx` après soumission possible d’un posting ou
d’une extourne n’est pas un simple échec retryable. Il déclenche la procédure
de résultat inconnu.

## 16. Algorithme de backoff

Pour une tentative `n` :

```text
cap(n) = min(maxBackoff, initialBackoff × 2^(n-1))
delay  = random(0, cap(n))
```

Le compteur inclut l’appel initial. `Retry-After`, lorsqu’il est valide, est
respecté dans les limites opérationnelles approuvées.

Un budget global empêche l’empilement de retries aux couches HTTP, use case,
consumer et scheduler.

## 17. Politique par opération

| Opération | Retry automatique | Condition |
| --- | --- | --- |
| Vérification client/compte/fonds | Oui | Erreur transitoire, résultat frais exigé |
| Posting débit + crédit CUT | Seulement si autorisé | Idempotence bancaire confirmée et outcome géré |
| Recherche de posting | Oui | Lecture idempotente |
| Extourne | Seulement avec la même clé | Instruction autorisée et idempotence bancaire confirmée |
| Recherche d’extourne | Oui | Lecture idempotente |
| Requête fallback TFJ | Oui | Lecture/recherche idempotente |
| Webhook TRESOR PAY | Oui | Même `eventId`, erreur transitoire |
| Publication Outbox | Oui | Même événement immuable |
| Erreur de contrat/sécurité | Non | Correction préalable |

## 18. Circuit breaker

Un circuit breaker protège chaque dépendance externe et type d’opération
indépendamment :

- Amplitude lectures ;
- Amplitude posting ;
- Amplitude rapprochement/TFJ ;
- TRESOR PAY notification immédiate ;
- TRESOR PAY notification finale.

États :

```text
CLOSED → OPEN → HALF_OPEN → CLOSED/OPEN
```

Règles :

- seuls les échecs techniques pertinents alimentent le taux d’échec ;
- un rejet métier n’ouvre pas le circuit ;
- `OPEN` échoue rapidement sans supposer un résultat métier ;
- `HALF_OPEN` autorise un nombre borné de probes ;
- les écritures financières ambiguës restent en rapprochement même si le
  circuit s’ouvre ;
- l’ouverture produit métrique, log corrélé et alerte ;
- le fallback ne réutilise jamais une donnée bancaire obsolète pour autoriser
  un paiement.

Seuils, fenêtre, durée d’ouverture et probes sont configurables et soumis à
validation Operations.

---

# Partie E — DLQ et replay

## 19. Entrée en DLQ

Un message ou une livraison rejoint la DLQ lorsque :

- le nombre maximal de tentatives est épuisé ;
- l’erreur est permanente mais nécessite une analyse ;
- le schéma ou la version est non supporté ;
- le payload est invalide ;
- le même `eventId` porte un payload différent ;
- le consumer ne peut pas établir un traitement sûr.

Une entrée DLQ ne change jamais à elle seule l’état financier de Payment.

## 20. Enveloppe DLQ

La DLQ conserve :

- identité de l’événement original ;
- type et version ;
- aggregate et correlation ;
- consumer et étape en échec ;
- catégorie d’erreur stable ;
- nombre et dates des tentatives ;
- hash ou référence protégée du payload original ;
- emplacement du payload chiffré si sa conservation est indispensable.

Credentials, comptes complets, payload bancaire brut et stack traces sont
interdits.

## 21. Triage DLQ

| Catégorie | Action |
| --- | --- |
| Transitoire épuisée | Vérifier dépendance puis replay contrôlé |
| Contrat/version | Corriger ou déployer le consumer compatible |
| Donnée invalide | Quarantaine fonctionnelle, pas de replay inchangé |
| Sécurité/signature | Investigation Security, rotation si nécessaire |
| Conflit d’identité | Quarantaine et rapprochement manuel |
| Effet financier ambigu | Procédure d’outcome inconnu, jamais replay direct |

Toute entrée DLQ déclenche l’alerte définie dans
`PAYMENT_SECURITY_AUDIT_BASELINE.md`.

## 22. Replay opérationnel contrôlé

### 22.1 Préconditions

Un replay exige :

- cause identifiée et corrigée ou dépendance restaurée ;
- type de replay autorisé ;
- opérateur ou runbook authentifié ;
- permission explicite ;
- périmètre borné ;
- preuve que le replay ne répète pas un effet financier ambigu ;
- ticket/incident et motif ;
- audit avant et après exécution.

### 22.2 Identités

- l’`eventId`, le payload et la version d’origine sont conservés ;
- un nouveau `replayExecutionId` identifie l’exécution ;
- le `correlationId` métier reste lié ;
- le numéro de tentative augmente ;
- aucun nouvel événement métier n’est inventé pour masquer le replay.

### 22.3 Autorisations

| Replay | Autorité minimale |
| --- | --- |
| Notification TRESOR PAY | Operations selon runbook |
| Publication Outbox | Operations/Integration selon runbook |
| Projection de lecture | Operations avec contrôle d’idempotence |
| Confirmation TFJ quarantinée | Accounting + validation manuelle |
| Posting ou extourne inconnue | **Replay direct interdit** ; rapprochement obligatoire |

### 22.4 Batch

Un replay par lot :

- utilise un filtre explicite et prévisualisable ;
- impose un volume maximal ;
- permet arrêt et reprise ;
- produit un rapport par élément ;
- limite le débit pour ne pas recréer l’incident ;
- est interdit sur des écritures financières ambiguës.

---

# Partie F — Rapprochement bancaire

## 23. Déclencheurs

Un dossier de rapprochement est ouvert pour :

- timeout ou coupure après soumission d’un posting ;
- réponse bancaire `UNKNOWN` ;
- débit confirmé avec crédit CUT inconnu ;
- débit confirmé avec crédit CUT échoué ;
- résultat incohérent entre callback et recherche ;
- extourne au résultat inconnu ;
- confirmation TFJ absente au cut-off ;
- confirmation TFJ non rapprochable ;
- divergence détectée lors d’un contrôle opérationnel.

## 24. Dossier de rapprochement

Il conserve :

- `reconciliationCaseId` ;
- `PaymentId` et `PaymentReference` ;
- institution ;
- type d’opération ;
- clé d’idempotence bancaire protégée ;
- référence de posting ou d’extourne ;
- montant/devise protégés selon les droits ;
- faits connus et preuves ;
- outcome courant ;
- dates, échéance et priorité ;
- responsable et statut ;
- recherches effectuées ;
- décision et clôture ;
- audit et corrélation.

États candidats :

```text
OPEN
INVESTIGATING
AWAITING_BANK
ACTION_REQUIRED
RESOLVED
CLOSED
```

La clôture exige un résultat bancaire établi ou une décision manuelle
documentée et autorisée.

## 25. Ordre des recherches

1. Rechercher par clé d’idempotence bancaire.
2. Rechercher par référence de posting/extourne.
3. Croiser institution, PaymentReference, montant, devise et fenêtre.
4. Interroger la confirmation TFJ si le posting est confirmé.
5. Obtenir une preuve ou instruction bancaire si l’ambiguïté persiste.

Une similarité partielle ne suffit jamais à rattacher automatiquement une
écriture à Payment.

## 26. Résolution

| Résultat établi | Action Payment |
| --- | --- |
| Aucun débit et aucun crédit | Reprise autorisée uniquement selon contrat et nouvelle décision contrôlée |
| Débit + crédit CUT confirmés | Poursuite vers attente TFJ |
| Débit confirmé, CUT inconnu | Maintien outcome inconnu et poursuite recherche |
| Débit confirmé, CUT échoué | `REVERSAL_REQUIRED` |
| Posting adverse ou incohérent | Escalade et runbook approuvé |
| Extourne confirmée | `REVERSED` |
| Extourne absente | Nouvelle instruction seulement si autorisée et avec la même clé de reversal |

Le rapprochement établit un fait existant ; il ne fabrique pas une écriture.

---

# Partie G — TFJ et quarantaine

## 27. Rapprochement TFJ

La clé normative combine :

- `FinancialInstitutionCode` ;
- `BusinessDate` ;
- `PaymentReference` ;
- `BankPostingReference`.

La clé d’idempotence TFJ combine :

```text
FINANCIAL_INSTITUTION_CODE
    + BUSINESS_DATE
    + BANK_POSTING_REFERENCE
    + TFJ_STATUS
```

Seule une confirmation favorable, univoque et durablement persistée permet
`TREASURY_INTEGRATED`.

## 28. Quarantaine

Une confirmation est mise en quarantaine si :

- aucun Payment ne correspond ;
- plusieurs Payments correspondent ;
- les références se contredisent ;
- l’institution ou la date métier est invalide ;
- le posting n’est pas connu ;
- le payload est valide techniquement mais non rapprochable ;
- le même identifiant présente un payload différent.

La quarantaine conserve :

- identité et hash du message ;
- clés de rapprochement protégées ;
- raison stable ;
- date de réception ;
- corrélation ;
- état et propriétaire de résolution ;
- preuve de toute tentative de rapprochement.

Elle ne met pas Payment à jour et ne déclenche aucune notification finale.

## 29. Résolution de quarantaine

1. Triage automatique déterministe.
2. Recherche fallback Amplitude.
3. Analyse Accounting autorisée.
4. Correction d’une référence uniquement avec preuve.
5. Retraitement idempotent de la même confirmation.
6. Audit de la décision.

La suppression silencieuse et le rattachement approximatif sont interdits.

---

# Partie H — Résultats inconnus

## 30. Détection

Un outcome devient inconnu lorsque SIXPAY ne peut pas prouver l’effet complet
d’une instruction financière, notamment après :

- timeout après envoi possible ;
- fermeture de connexion avant réponse complète ;
- réponse mal formée après soumission ;
- indisponibilité avant persistance locale du résultat ;
- réponse Amplitude explicitement inconnue ;
- divergence entre réponse et recherche.

## 31. Traitement normatif

```text
Persist instruction + idempotency key
    → submit
    → outcome uncertain
    → ACCOUNTING_OUTCOME_UNKNOWN
    → notify PROCESSING
    → open reconciliation case
    → query by idempotency/reference
    → establish outcome or escalate
```

Règles :

- aucun succès ou échec financier n’est supposé ;
- aucun second posting n’est envoyé aveuglément ;
- la notification immédiate indique `PROCESSING` ;
- une alerte est créée ;
- le circuit breaker n’efface pas le dossier ;
- les recherches sont idempotentes ;
- toute résolution produit un fait versionné et audité.

## 32. Outcome partiel

### 32.1 Débit confirmé, CUT inconnu

- état : `ACCOUNTING_OUTCOME_UNKNOWN` ;
- recherche par clé/référence ;
- dossier de rapprochement ;
- aucun second crédit CUT aveugle ;
- aucun succès final.

### 32.2 Débit confirmé, CUT échoué

- état : `REVERSAL_REQUIRED` ;
- succès interdit ;
- ouverture d’un dossier d’extourne ;
- exécution uniquement sur instruction bancaire ou runbook approuvé.

Décision : `IA0R-D07`.

---

# Partie I — Reprise après incident

## 33. Principes

Après redémarrage ou bascule :

- la base est la source de reprise ;
- les claims expirés sont libérés ;
- les schedulers reprennent les éléments échus ;
- les consumers dédupliquent par `eventId` ;
- les versions d’agrégat préviennent les commandes obsolètes ;
- aucun cache local ne décide de l’état financier ;
- les alertes existantes restent ouvertes jusqu’à preuve de résolution.

## 34. Séquence de reprise

1. Stabiliser l’infrastructure et empêcher une nouvelle dégradation.
2. Vérifier PostgreSQL, Vault, broker, Amplitude et endpoints TRESOR PAY.
3. Vérifier l’intégrité Payment, audit, idempotence et Outbox.
4. Identifier les transactions interrompues par état et ancienneté.
5. Reprendre les claims techniques expirés.
6. Traiter d’abord les outcomes financiers inconnus.
7. Relancer les lectures et rapprochements sûrs.
8. Réactiver la publication Outbox avec débit borné.
9. Rejouer les notifications autorisées.
10. Traiter DLQ et quarantaines.
11. Vérifier TFJ, extournes et projections.
12. Produire le rapport d’incident et clôturer les alertes.

## 35. Ordre de priorité

1. Risque de double débit ou perte financière.
2. `ACCOUNTING_OUTCOME_UNKNOWN`.
3. `REVERSAL_REQUIRED` / `REVERSAL_PENDING`.
4. Paiements en attente TFJ au cut-off.
5. Outbox métier.
6. Notifications finales.
7. Notifications immédiates.
8. Projections et reporting.

## 36. Modes dégradés

| Dépendance indisponible | Comportement |
| --- | --- |
| Amplitude avant posting | Suspendre/rejeter selon contrat, aucun posting |
| Amplitude après soumission ambiguë | Outcome inconnu et rapprochement |
| Broker/Internal Bus | Commit métier + Outbox, publication différée |
| TRESOR PAY webhook | État financier inchangé, retry puis DLQ |
| Pipeline TFJ | Maintenir attente, fallback et alerte |
| Read model | Traitement métier continue si indépendant ; projection rejouable |
| Audit obligatoire | Pas de réussite silencieuse de l’action sensible |
| Vault/credentials | Échec fermé et alerte |

## 37. RTO/RPO

Les valeurs numériques de RTO/RPO restent à approuver avec Operations et la
banque. Cette absence ne modifie pas les garanties :

- aucune perte acceptée pour Payment, instruction financière, audit et Outbox
  après commit confirmé ;
- reconstruction des projections depuis les faits conservés ;
- sauvegardes et restaurations testées ;
- reprise sans double effet.

---

# Partie J — Runbook d’extourne

## 38. Conditions d’ouverture

Le runbook s’applique lorsque :

- le débit client est confirmé ;
- le crédit CUT a échoué ou l’anomalie est établie ;
- ou une instruction bancaire explicite impose l’extourne.

Une simple absence de TFJ ou un timeout non rapproché ne suffit pas à autoriser
l’extourne.

## 39. Préconditions obligatoires

- Payment est `REVERSAL_REQUIRED` ou dans l’état de reprise autorisé ;
- posting original identifié ;
- résultat partiel confirmé par preuve bancaire ;
- dossier de rapprochement ouvert ;
- montant, devise, compte et institution vérifiés ;
- absence d’extourne déjà confirmée ;
- clé d’idempotence de reversal distincte et persistée ;
- autorisation bancaire ou runbook approuvé ;
- séparation des tâches et double contrôle opérationnel ;
- fenêtre et canal bancaire disponibles ;
- audit initial écrit.

Le double contrôle est une décision SIXPAY de sécurité pour une opération
financière à haut risque et reste soumis à validation formelle du Gate.

## 40. Étapes d’exécution

1. Geler tout retry de posting/crédit lié.
2. Rassembler les preuves et références originales.
3. Vérifier l’absence de reversal existante par clé et référence.
4. Générer/persister `ReversalId` et la clé idempotente distincte.
5. Obtenir les validations requises.
6. Passer Payment à `REVERSAL_PENDING` selon la machine à états.
7. Envoyer une seule instruction logique à Amplitude.
8. Persister immédiatement la réponse ou l’incertitude.
9. En cas d’outcome inconnu, rechercher ; ne pas renvoyer aveuglément.
10. Sur confirmation, persister référence et preuve bancaire.
11. Passer Payment à `REVERSED`.
12. Émettre les événements et notification autorisés par contrat.
13. Clôturer les dossiers et produire le rapport.

## 41. Contrôles post-extourne

- montant extourné égal à l’effet à corriger ;
- aucune seconde extourne ;
- référence liée au posting original ;
- solde et écritures confirmés par Amplitude ;
- état Payment cohérent ;
- audit et Outbox publiés ;
- TRESOR PAY informé selon contrat ;
- dashboards et alertes actualisés ;
- conservation de l’historique original.

L’extourne ne supprime, ne remplace et ne réécrit aucun fait antérieur.

## 42. Arrêt et escalade

Le runbook s’arrête et escalade si :

- références incohérentes ;
- montant ou devise divergent ;
- reversal déjà existante mais outcome inconnu ;
- preuve bancaire insuffisante ;
- double contrôle absent ;
- contrat Amplitude indisponible ou non approuvé ;
- résultat inattendu.

Dans ces cas, Payment reste sous rapprochement et aucune nouvelle écriture
n’est envoyée.

---

# Partie K — Observabilité et validation

## 43. Métriques et alertes

La résilience expose au minimum :

- doublons et conflits d’idempotence ;
- retries par opération et motif ;
- circuit breaker par dépendance ;
- backlog/âge Outbox ;
- messages `DEAD` et DLQ ;
- lag consumer ;
- dossiers de rapprochement ouverts et ancienneté ;
- outcomes inconnus ;
- TFJ en attente et quarantaine ;
- replay par type et résultat ;
- extournes requises, en attente, confirmées et inconnues.

Les alertes de `PAYMENT_SECURITY_AUDIT_BASELINE.md` s’appliquent.

## 44. Tests obligatoires

### 44.1 Idempotence et concurrence

- nouvelle demande ;
- doublon identique en cours et terminé ;
- même clé avec payload différent ;
- même référence avec clé différente ;
- concurrence réelle sur la contrainte ;
- expiration/rétention.

### 44.2 Outbox et messaging

- atomicité mutation + audit + Outbox ;
- rollback complet ;
- claim concurrent `SKIP LOCKED` ;
- crash après claim et avant publication ;
- crash après publication et avant marquage ;
- republication même `eventId` ;
- ordre par aggregate version ;
- passage `DEAD`, DLQ et replay.

### 44.3 Retry et circuit breaker

- classification retryable/non retryable ;
- backoff exponentiel avec jitter borné ;
- respect `Retry-After` ;
- budget global ;
- ouverture, fast-fail et half-open ;
- rejet métier exclu du calcul ;
- aucune donnée obsolète utilisée pour approuver.

### 44.4 Banque et TFJ

- timeout avant/après soumission ;
- lookup par clé/référence ;
- débit OK/CUT inconnu ;
- débit OK/CUT échoué ;
- TFJ absente ;
- TFJ identique rejouée ;
- TFJ conflictuelle ou non rapprochable ;
- quarantaine et résolution ;
- extourne confirmée ou inconnue.

### 44.5 Incident et runbooks

- restart à chaque frontière critique ;
- reprise Outbox et consumer ;
- replay unitaire et batch borné ;
- autorisation/audit du replay ;
- restauration de sauvegarde ;
- runbook d’extourne nominal, refusé et ambigu ;
- preuve qu’aucun scénario ne crée un double effet.

## 45. Critères de sortie 0P.11

- [x] Idempotence entrante et conflits formalisés.
- [x] Outbox transactionnelle et garantie at-least-once formalisées.
- [x] Retry, backoff exponentiel et full jitter définis.
- [x] Retry financier séparé du retry technique.
- [x] Circuit breakers et modes dégradés définis.
- [x] DLQ, triage et données minimales définis.
- [x] Rapprochement bancaire et dossier de suivi définis.
- [x] Quarantaine TFJ et résolution définies.
- [x] Résultats inconnus et partiels fermés.
- [x] Reprise après incident ordonnée.
- [x] Replay opérationnel contrôlé et audité.
- [x] Runbook d’extourne complet.
- [x] Métriques, alertes et tests de résilience définis.
- [x] Aucun rejeu financier aveugle autorisé.

## 46. Verdict 0P.11

```text
PAYMENT RESILIENCE BASELINE: ESTABLISHED
INBOUND IDEMPOTENCY: CLOSED
TRANSACTIONAL OUTBOX: REQUIRED
DELIVERY GUARANTEE: AT_LEAST_ONCE
RETRY: BOUNDED WITH EXPONENTIAL FULL JITTER
CIRCUIT BREAKERS: DEFINED
DLQ AND CONTROLLED REPLAY: DEFINED
BANK RECONCILIATION: DEFINED
UNMATCHED TFJ: QUARANTINED
UNKNOWN FINANCIAL OUTCOME: RECONCILE, NEVER BLINDLY REPLAY
INCIDENT RECOVERY: DEFINED
REVERSAL RUNBOOK: DEFINED
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.12 — ACCEPTANCE AND VERIFICATION BASELINE
```
