# SIXPAY CONNECT — Payment Business Flows

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.4 — Décrire les parcours de paiement` |
| Branche | `feat/payment-contract-pack` |
| Commit analysé | `e7746b0de32f6a660b2b06250f68f9c1305e14dd` |
| Domaine pilote | `payment` |
| Statut | `BUSINESS_FLOWS_ESTABLISHED` |
| Génération de code | **Interdite** |
| Étape suivante | `0P.5 — Définir le modèle métier Payment` |

## 2. Objectif

Ce document décrit le parcours nominal et tous les parcours alternatifs du
MVP Payment. Pour chaque scénario, il fixe :

- le déclencheur et les préconditions ;
- le propriétaire de chaque action ;
- les faits persistés et les effets bancaires possibles ;
- le résultat immédiat communiqué à TRESOR PAY ;
- le traitement de la confirmation TFJ ;
- les exigences d’idempotence, de reprise et d’audit ;
- les critères de terminaison.

Il applique sans les remplacer :

- `PAYMENT_SOURCE_BASELINE.md` ;
- la section `Scope / Out of Scope` de
  `GATE_IA_0P_PAYMENT_PREFLIGHT.md` ;
- `PAYMENT_CONTEXT_MAP.md` ;
- `IA_0R_BLOCKING_DECISIONS.yaml`.

Les états mentionnés sont des **états candidats de parcours**. Leur catalogue,
leurs transitions exactes et leurs états terminaux seront fermés à l’étape
0P.6.

## 3. Conventions

### 3.1 Acteurs

| Acteur | Responsabilité dans les parcours |
| --- | --- |
| TRESOR PAY | Crée l’ordre demandé, fournit ses références et reçoit les résultats |
| Integration | Authentifie, valide le transport, traduit les contrats et appelle les systèmes externes |
| Payment | Enregistre la demande, applique l’idempotence métier, orchestre et possède l’état du paiement traité |
| Customer | Produit la vérification canonique et maintient `ObservedCustomer` |
| Accounting | Porte l’instruction de posting, son outcome, le rapprochement TFJ et l’extourne |
| Notification | Assure la livraison fiable et idempotente des résultats |
| Reporting | Construit les vues de consultation, la timeline et les vues d’investigation |
| Amplitude | Fait autorité sur le client, le compte, les fonds, les écritures et le résultat TFJ |

### 3.2 Types de résultat vers TRESOR PAY

| Résultat | Signification |
| --- | --- |
| `REJECTED` | La demande ne peut pas produire d’écriture ; le motif métier est stable |
| `FAILED` | Un échec technique est confirmé sans effet financier |
| `PROCESSING` | Le résultat financier n’est pas encore sûr ou une résolution est en cours |
| `POSTED_PENDING_TFJ` | Débit client et crédit comptable du CUT sont confirmés, mais la finalité TFJ ne l’est pas |
| `TREASURY_INTEGRATED` | Une confirmation TFJ favorable a été rapprochée et durablement persistée |
| `REVERSAL_REQUIRED` | Un effet partiel impose une extourne explicite selon instruction ou runbook approuvé |
| `REVERSED` | L’extourne bancaire a été confirmée |

`POSTED_PENDING_TFJ` ne signifie jamais que les fonds sont définitivement
intégrés au Trésor.

### 3.3 Règles communes

1. Toute demande authentifiée est enregistrée avant le premier appel
   Amplitude, y compris si elle est ensuite rejetée.
2. `ObservedCustomer` est créé ou actualisé dès la première demande
   enregistrée, qu’elle réussisse ou échoue.
3. Aucun token, secret ou numéro de compte complet n’est persisté ou
   journalisé.
4. Une référence TRESOR PAY, une clé d’idempotence et une empreinte canonique
   permettent de distinguer rejeu, conflit et nouvelle demande.
5. Tout changement d’état et toute intention de notification sont enregistrés
   atomiquement avec la Transactional Outbox.
6. Un retry de lecture peut être automatique selon la politique configurée.
   Un retry d’écriture n’est permis qu’avec idempotence bancaire confirmée et
   gestion explicite du résultat inconnu.
7. Un timeout de posting ne prouve ni succès ni échec. Il interdit tout rejeu
   aveugle et déclenche une recherche du résultat.
8. Une erreur de notification ne modifie jamais la vérité financière du
   Payment.
9. Une confirmation TFJ ne modifie Payment qu’après rapprochement univoque et
   persistance durable.
10. Toute action manuelle de rapprochement, replay ou extourne est soumise au
    RBAC et auditée.

Sources : `PAY-SRC-007` à `PAY-SRC-010`, `PAY-SRC-013`,
`PAY-SRC-020` à `PAY-SRC-026`, `PAY-SRC-030` à `PAY-SRC-046`.

## 4. Vue d’ensemble

```text
TRESOR PAY
    │ ordre + idempotence + corrélation
    ▼
Integration ── authentification et validation transport
    ▼
Payment ───── persistance immédiate + décision d’orchestration
    ├────────► Customer ─► Integration ─► Amplitude
    │          vérification client, compte, statut et fonds
    │
    └────────► Accounting ─► Integration ─► Amplitude
               débit client + crédit CUT
    │
    ▼
Outbox ─► Notification ─► Integration ─► TRESOR PAY
    │       résultat immédiat
    ▼
PENDING_END_OF_DAY_CONFIRMATION
    ▲
Amplitude ─► Integration ─► Accounting
    │          confirmation TFJ ou rapprochement planifié
    ▼
Payment ─► Outbox ─► Notification ─► Integration ─► TRESOR PAY
                       résultat définitif
```

Les projections `ObservedCustomer`, Payment Search et Payment Timeline sont
alimentées de manière idempotente à partir des faits persistés et événements.

---

# 5. Parcours nominal — `PAY-FLOW-00`

## 5.1 Déclencheur et préconditions

TRESOR PAY transmet un ordre de paiement REST à SIXPAY.

Préconditions :

- l’appel système est authentifiable par Token et Subscription Key ;
- la demande contient les références, le montant, la devise, l’institution,
  le NIU, le compte protégé, `Idempotency-Key` et `X-Correlation-ID` attendus ;
- l’ordre n’est pas déjà connu sous une empreinte différente ;
- la configuration bancaire et le compte CUT sont actifs ;
- aucun résultat bancaire antérieur n’est associé à la même intention.

## 5.2 Séquence nominale

| Étape | Responsable | Action et résultat durable |
| ---: | --- | --- |
| 1 | TRESOR PAY | Crée l’ordre demandé et appelle l’API SIXPAY avec ses références |
| 2 | Integration | Authentifie l’appel, valide headers et schéma, propage la corrélation et produit `ReceivePaymentRequest` |
| 3 | Payment | Résout l’idempotence, crée `PaymentId`, persiste le snapshot canonique, l’empreinte, les références et l’état candidat `RECEIVED` |
| 4 | Customer | Crée ou actualise une première vue minimisée d’`ObservedCustomer` à partir de la demande enregistrée |
| 5 | Payment | Passe au contrôle bancaire et émet `VerifyBankingCustomer` |
| 6 | Customer / Integration / Amplitude | Vérifient l’existence du client, la concordance du NIU, l’existence et l’appartenance du compte, les faits KYC nécessaires, le statut, les blocages et oppositions |
| 7 | Customer | Persiste le snapshot daté de vérification, actualise `ObservedCustomer` et retourne un résultat canonique favorable |
| 8 | Payment / Accounting / Integration / Amplitude | Obtiennent une vérification fraîche des fonds disponibles pour le montant et la devise |
| 9 | Payment | Décide de demander le posting ; persiste cette décision et une instruction stable |
| 10 | Accounting / Integration | Envoie à Amplitude une instruction idempotente de débit client et crédit CUT |
| 11 | Amplitude | Exécute l’effet bancaire atomique et retourne une référence stable, la date métier et un outcome favorable |
| 12 | Accounting | Persiste les références et l’outcome bancaire observé |
| 13 | Payment | Persiste l’état candidat `PENDING_END_OF_DAY_CONFIRMATION` et l’intention de notification `POSTED_PENDING_TFJ` dans la même transaction |
| 14 | Notification / Integration | Livre à TRESOR PAY le résultat immédiat, avec retry/backoff/DLQ sans modifier l’état financier |
| 15 | Reporting | Actualise les vues Payment, `ObservedCustomer`, notification et timeline |
| 16 | Amplitude | Après TFJ, émet une confirmation contenant les clés de rapprochement ; une interrogation SIXPAY planifiée sert de fallback |
| 17 | Integration / Accounting | Normalisent, dédupliquent et rapprochent la confirmation sur institution, date métier, référence Payment et référence bancaire |
| 18 | Accounting | Persiste le résultat TFJ rapproché |
| 19 | Payment | Persiste l’état candidat `TREASURY_INTEGRATED` et l’intention de notification définitive dans la même transaction |
| 20 | Notification / Integration | Livre à TRESOR PAY `TREASURY_INTEGRATED` de manière idempotente |
| 21 | Reporting | Rend le paiement, le client observé et la timeline complète consultables selon le RBAC |

## 5.3 Postconditions

- un seul Payment SIXPAY existe pour l’intention ;
- un seul effet bancaire est associé à sa clé d’idempotence ;
- le débit client et le crédit comptable CUT sont référencés ;
- une confirmation TFJ favorable et rapprochée est persistée ;
- les deux intentions de notification sont durables ;
- les éventuels échecs de livraison restent suivis indépendamment ;
- la timeline permet de reconstruire les décisions et échanges.

Sources : `PAY-SRC-001` à `PAY-SRC-010`, `PAY-SRC-013` à
`PAY-SRC-018`, `PAY-SRC-020` à `PAY-SRC-046`.

---

# 6. Parcours alternatifs avant écriture

## 6.1 Demande invalide — `PAY-FLOW-01`

### Détection

Deux frontières sont distinguées :

- **échec d’authentification** : Integration refuse l’appel et produit
  uniquement la trace de sécurité minimisée autorisée ; aucun Payment métier
  n’est créé ;
- **demande authentifiée invalide** : le transport est accepté, mais un champ,
  une référence ou une règle sémantique est invalide.

### Traitement d’une demande authentifiée invalide

1. Integration traduit les erreurs de transport en codes stables RFC 7807.
2. Payment enregistre la demande minimisée, son empreinte, la corrélation et
   le rejet avant tout appel Amplitude.
3. Customer crée ou actualise `ObservedCustomer` avec les seules données
   exploitables et leur niveau de confiance.
4. Payment persiste le motif candidat `INVALID_REQUEST`.
5. TRESOR PAY reçoit `REJECTED` dans la réponse ou par le mécanisme de résultat
   défini au Contract Pack ; aucun posting n’est demandé.
6. Reporting expose la demande et sa timeline aux rôles autorisés.

**Fin :** rejet final avant écriture.  
**Retry :** nouvelle demande seulement après correction ; la même clé avec un
payload différent reste un conflit.  
**Sources :** `PAY-SRC-005`, `PAY-SRC-007` à `PAY-SRC-010`,
`PAY-SRC-041`, `PAY-SRC-055`.

## 6.2 Demande dupliquée — `PAY-FLOW-02`

### Doublon identique

1. Payment retrouve la même clé d’idempotence et la même empreinte canonique.
2. Aucun nouveau Payment, contrôle ou posting n’est créé.
3. SIXPAY restitue le résultat déjà durablement connu ou l’état `PROCESSING`
   courant avec les mêmes références SIXPAY.
4. Un rejeu de notification est traité par l’identifiant de livraison, jamais
   par réexécution du paiement.
5. La tentative dupliquée est ajoutée à l’audit technique.

### Même clé, payload différent

1. Payment détecte le conflit d’empreinte.
2. La demande est refusée avec un code stable d’idempotence conflictuelle.
3. Le Payment d’origine n’est pas modifié et aucun appel Amplitude n’est fait.

**Fin :** résultat du Payment original ou conflit final.  
**Sources :** `PAY-SRC-007`, `PAY-SRC-008`, `PAY-SRC-024`,
`PAY-SRC-026`, `PAY-SRC-038`.

## 6.3 Client bancaire introuvable — `PAY-FLOW-03`

1. Amplitude retourne un fait frais `CUSTOMER_NOT_FOUND`.
2. Customer persiste le snapshot de vérification et actualise
   `ObservedCustomer` comme observation non rapprochée.
3. Payment persiste le rejet motivé.
4. Notification prend en charge un résultat immédiat `REJECTED`.
5. Aucun contrôle de fonds ni posting n’est demandé.

**Fin :** rejet final sans écriture.  
**Reprise :** une nouvelle intention peut être soumise après correction dans
le système maître ; le Payment rejeté n’est pas rouvert implicitement.  
**Sources :** `PAY-SRC-013` à `PAY-SRC-016`, `PAY-SRC-023`,
`PAY-SRC-028`, `PAY-SRC-029`.

## 6.4 NIU non concordant — `PAY-FLOW-04`

1. Amplitude retourne le NIU bancaire ou un résultat explicite de
   non-concordance.
2. Customer compare les faits normalisés sans modifier le NIU Amplitude.
3. `ObservedCustomer` conserve l’observation et le résultat de concordance,
   sous forme minimisée.
4. Payment rejette avec le motif stable `NIU_MISMATCH`.
5. Notification transmet `REJECTED`; aucun posting n’est demandé.

Une concordance de nom ou de téléphone ne permet pas de contourner le NIU.

**Fin :** rejet final sans écriture.  
**Sources :** `PAY-SRC-014` à `PAY-SRC-016`, `PAY-SRC-019`,
`PAY-SRC-023`.

## 6.5 Compte inexistant — `PAY-FLOW-05`

1. Amplitude confirme que le compte demandé n’existe pas dans l’institution.
2. Customer persiste ce résultat frais et actualise `ObservedCustomer`.
3. Payment rejette avec `ACCOUNT_NOT_FOUND`.
4. Notification transmet `REJECTED`; aucun contrôle de fonds ou posting
   n’est exécuté.

**Fin :** rejet final sans écriture.  
**Sources :** `PAY-SRC-014` à `PAY-SRC-017`, `PAY-SRC-023`.

## 6.6 RIB n’appartenant pas au client — `PAY-FLOW-06`

1. Amplitude retrouve le client et le compte mais ne confirme pas leur
   appartenance.
2. Customer retourne `ACCOUNT_OWNERSHIP_MISMATCH` et conserve la preuve
   minimale masquée.
3. Payment rejette la demande.
4. Notification transmet `REJECTED`; aucun posting n’est exécuté.

SIXPAY ne rattache jamais localement le compte au client pour corriger ce cas.

**Fin :** rejet final sans écriture.  
**Sources :** `PAY-SRC-014`, `PAY-SRC-016`, `PAY-SRC-019`,
`PAY-SRC-023`.

## 6.7 Compte bloqué ou en opposition — `PAY-FLOW-07`

1. Amplitude retourne un statut, blocage ou opposition empêchant l’opération.
2. Customer normalise le motif sans exposer plus de détails que nécessaire.
3. Payment rejette avec un code stable correspondant.
4. Notification transmet `REJECTED`; aucun posting n’est exécuté.

Le résultat doit être frais ; une vérification favorable antérieure ne peut
pas neutraliser le blocage courant.

**Fin :** rejet final sans écriture.  
**Sources :** `PAY-SRC-017`, `PAY-SRC-023`, `PAY-SRC-043`.

## 6.8 Solde insuffisant — `PAY-FLOW-08`

1. Amplitude vérifie le solde disponible, le montant, la devise et les
   restrictions applicables.
2. Accounting/Customer retournent un résultat canonique
   `INSUFFICIENT_AVAILABLE_FUNDS`.
3. Payment persiste le rejet et son snapshot de décision.
4. Notification transmet `REJECTED`; aucun posting n’est demandé.

Le solde exact n’est pas exposé à TRESOR PAY et n’est pas conservé comme
vérité locale.

**Fin :** rejet final sans écriture.  
**Sources :** `PAY-SRC-018`, `PAY-SRC-023`, `PAY-SRC-043`.

---

# 7. Parcours alternatifs techniques et comptables

## 7.1 Indisponibilité d’Amplitude — `PAY-FLOW-09`

### Pendant une lecture

1. Integration applique timeout, retry transitoire, backoff avec jitter et
   circuit breaker selon la configuration approuvée.
2. Si la lecture finit par réussir, le parcours reprend avec un résultat frais.
3. Si l’indisponibilité persiste, Payment conserve la demande sans écriture et
   enregistre un état technique candidat `PROCESSING` ou un échec confirmé
   selon la capacité de reprise convenue.
4. Notification transmet `PROCESSING` ou `FAILED`, jamais `REJECTED` pour une
   indisponibilité pure.

### Pendant ou après l’envoi d’une écriture

Le parcours bascule obligatoirement vers `PAY-FLOW-10` si SIXPAY ne peut pas
prouver que l’écriture n’a pas été reçue.

**Fin :** reprise contrôlée, échec technique confirmé sans effet, ou outcome
inconnu ; jamais succès supposé.  
**Sources :** `PAY-SRC-024`, `PAY-SRC-039`, `PAY-SRC-040`,
`IA0R-D06`.

## 7.2 Résultat comptable inconnu — `PAY-FLOW-10`

### Déclencheur

Timeout, rupture de connexion, réponse ambiguë ou erreur après soumission du
posting, sans preuve que l’effet bancaire est absent.

### Traitement

1. Accounting persiste l’instruction, sa clé d’idempotence, la référence
   disponible et l’outcome `UNKNOWN`.
2. Payment persiste l’état candidat `ACCOUNTING_OUTCOME_UNKNOWN`.
3. Aucun nouvel ordre de débit/crédit n’est envoyé.
4. Notification transmet `PROCESSING` à TRESOR PAY.
5. Accounting interroge Amplitude par clé d’idempotence ou référence de
   posting.
6. Un dossier de rapprochement et une alerte opérationnelle sont ouverts.
7. Si la recherche confirme :
   - l’absence d’effet, Payment peut terminer en `FAILED` sans effet ;
   - le débit et le crédit CUT, le parcours rejoint l’attente TFJ ;
   - un effet partiel, le parcours rejoint `PAY-FLOW-11`.
8. Chaque interrogation et décision manuelle est auditée.

**Fin :** uniquement après établissement durable d’un outcome connu ou
résolution opérationnelle approuvée.  
**Sources :** `PAY-SRC-024` à `PAY-SRC-026`, `IA0R-D07`.

## 7.3 Débit réussi, crédit CUT non confirmé — `PAY-FLOW-11`

### Crédit CUT inconnu

1. Accounting conserve l’outcome partiel.
2. Payment reste `ACCOUNTING_OUTCOME_UNKNOWN`.
3. TRESOR PAY reçoit `PROCESSING`.
4. Accounting recherche le crédit par les références disponibles et ouvre un
   rapprochement ; aucun second crédit n’est envoyé aveuglément.

### Crédit CUT explicitement échoué

1. Accounting persiste l’échec partiel confirmé.
2. Payment passe à l’état candidat `REVERSAL_REQUIRED`.
3. Aucune notification de succès n’est autorisée.
4. TRESOR PAY reçoit `REVERSAL_REQUIRED` ou `PROCESSING` selon le futur contrat,
   sans masquer le caractère non final.
5. Accounting prépare une demande d’extourne distincte.
6. L’extourne n’est exécutée qu’après instruction bancaire ou runbook
   approuvé, avec une clé d’idempotence propre.
7. Le parcours rejoint `PAY-FLOW-15`.

**Fin :** crédit CUT confirmé et attente TFJ, ou extourne confirmée, ou dossier
de rapprochement toujours ouvert.  
**Sources :** `PAY-SRC-022`, `PAY-SRC-024`, `PAY-SRC-025`,
`IA0R-D07`.

## 7.4 Notification TRESOR PAY en échec — `PAY-FLOW-12`

1. L’intention de notification existe déjà dans l’Outbox avec le résultat
   métier immuable.
2. Notification enregistre chaque tentative et Integration effectue l’appel.
3. Les erreurs transitoires déclenchent retry, backoff avec jitter et circuit
   breaker.
4. Une erreur permanente ou l’épuisement des tentatives conduit en DLQ et
   déclenche une alerte.
5. Un replay opérationnel autorisé réutilise le même identifiant d’événement
   et ne rejoue jamais le Payment.
6. Reporting expose séparément l’état financier et l’état de livraison.

Un échec de notification immédiate n’empêche pas l’attente TFJ. Un échec de
notification définitive ne retire pas `TREASURY_INTEGRATED`.

**Fin :** livraison confirmée ou échec terminal/DLQ sous suivi opérationnel.  
**Sources :** `PAY-SRC-037` à `PAY-SRC-042`.

---

# 8. Parcours alternatifs TFJ et extourne

## 8.1 Confirmation TFJ absente — `PAY-FLOW-13`

1. Au cut-off configuré, Accounting constate qu’aucune confirmation
   rapprochable n’est persistée.
2. Payment reste dans l’état candidat
   `PENDING_END_OF_DAY_CONFIRMATION`.
3. Aucune notification définitive favorable n’est autorisée.
4. Accounting déclenche l’interrogation planifiée de rapprochement auprès
   d’Amplitude, avec retries de lecture contrôlés.
5. Une alerte opérationnelle est ouverte et visible dans Reporting.
6. Si la confirmation arrive plus tard, elle est traitée de manière
   idempotente et le parcours nominal reprend à son étape TFJ.

L’absence de confirmation n’autorise ni un nouveau posting ni une extourne
automatique.

**Fin :** confirmation ultérieure rapprochée ou résolution opérationnelle
formellement auditée ; tant que ce n’est pas le cas, le Payment reste en
attente.  
**Sources :** `PAY-SRC-030` à `PAY-SRC-032`, `IA0R-D08`.

## 8.2 Confirmation TFJ non rapprochable — `PAY-FLOW-14`

1. Integration authentifie et normalise la confirmation Amplitude.
2. Accounting calcule sa clé d’idempotence et recherche une correspondance
   univoque sur institution, date métier, référence Payment et référence
   bancaire.
3. En l’absence de correspondance ou en cas d’ambiguïté, la confirmation est
   placée en quarantaine.
4. Une alerte et un dossier de rapprochement manuel sont ouverts.
5. Aucun Payment n’est modifié et aucune notification définitive n’est émise.
6. Après résolution autorisée, la liaison est auditée ; la confirmation est
   retraitée de manière idempotente.

Une confirmation identique déjà rapprochée est ignorée sans nouvelle
transition ni nouvelle intention logique de notification.

**Fin :** confirmation rapprochée après résolution, ou élément toujours en
quarantaine.  
**Sources :** `PAY-SRC-032`, `PAY-SRC-033`, `IA0R-D08`.

## 8.3 Extourne bancaire — `PAY-FLOW-15`

### Déclencheurs

- débit client confirmé et crédit CUT explicitement échoué ;
- anomalie bancaire confirmée imposant une annulation ;
- instruction bancaire ou runbook approuvé après rapprochement.

### Traitement

1. Accounting ouvre un dossier d’extourne lié au posting original.
2. Payment persiste `REVERSAL_REQUIRED` puis, après autorisation, un état
   candidat `REVERSAL_PENDING`.
3. L’opérateur habilité ou le processus approuvé valide l’instruction ; toute
   action est auditée.
4. Accounting crée une instruction d’extourne avec une clé d’idempotence
   distincte et Integration la transmet à Amplitude.
5. En cas d’outcome inconnu, aucun rejeu aveugle n’a lieu ; Accounting
   recherche le résultat comme dans `PAY-FLOW-10`.
6. Amplitude confirme l’extourne et fournit sa référence bancaire.
7. Accounting persiste la preuve ; Payment passe à l’état candidat `REVERSED`.
8. Notification transmet à TRESOR PAY le résultat d’extourne prévu au futur
   contrat.
9. Reporting conserve la chaîne complète : posting original, anomalie,
   autorisation, extourne et notifications.

L’extourne ne supprime ni ne réécrit l’historique du Payment original.

**Fin :** extourne confirmée, ou `REVERSAL_PENDING` /
`ACCOUNTING_OUTCOME_UNKNOWN` sous rapprochement.  
**Sources :** `PAY-SRC-024`, `PAY-SRC-025`, `IA0R-D07`.

---

# 9. Matrice récapitulative

| ID | Scénario | Effet bancaire possible | Résultat immédiat | Suivi TFJ | Terminaison attendue |
| --- | --- | --- | --- | --- | --- |
| `PAY-FLOW-00` | Nominal | Débit + crédit CUT confirmés | `POSTED_PENDING_TFJ` | Oui | `TREASURY_INTEGRATED` |
| `PAY-FLOW-01` | Demande invalide | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-02` | Demande dupliquée | Aucun nouvel effet | Résultat existant ou conflit | Selon Payment original | État original ou conflit |
| `PAY-FLOW-03` | Client introuvable | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-04` | NIU non concordant | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-05` | Compte inexistant | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-06` | RIB non détenu | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-07` | Compte bloqué/opposé | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-08` | Solde insuffisant | Aucun | `REJECTED` | Non | Rejet |
| `PAY-FLOW-09` | Amplitude indisponible | Aucun ou inconnu selon la phase | `PROCESSING` / `FAILED` | Conditionnel | Reprise ou résultat établi |
| `PAY-FLOW-10` | Outcome comptable inconnu | Inconnu | `PROCESSING` | Après outcome favorable | Résultat rapproché |
| `PAY-FLOW-11` | Débit OK, CUT non confirmé | Partiel/inconnu | `PROCESSING` / `REVERSAL_REQUIRED` | Seulement si crédit confirmé | Rapprochement ou extourne |
| `PAY-FLOW-12` | Notification en échec | Inchangé | Livraison en retry/DLQ | Inchangé | Livraison ou suivi DLQ |
| `PAY-FLOW-13` | TFJ absente | Posting confirmé | Aucun succès définitif | Oui, fallback | Confirmation ou suivi |
| `PAY-FLOW-14` | TFJ non rapprochable | Non déterminé pour un Payment | Aucun succès définitif | Quarantaine | Rapprochement manuel |
| `PAY-FLOW-15` | Extourne | Annulation explicite | `REVERSAL_REQUIRED` puis `REVERSED` | Selon résultat bancaire | Extourne confirmée |

## 10. Exigences d’audit par parcours

Chaque timeline doit permettre d’établir :

- la réception et la validation de la demande ;
- la clé d’idempotence, l’empreinte et la corrélation ;
- l’identité technique de l’appelant sans credential ;
- les snapshots minimisés des contrôles Amplitude ;
- la décision Payment et son motif ;
- l’instruction comptable, sa clé et les références bancaires ;
- tous les outcomes connus, inconnus ou partiels ;
- les recherches de résultat et rapprochements ;
- les intentions et tentatives de notification ;
- la confirmation TFJ, sa clé, son rapprochement ou sa quarantaine ;
- les alertes, DLQ, replays et actions manuelles ;
- toute instruction et confirmation d’extourne.

Les vues exposent les données selon le RBAC, avec masquage des références
sensibles et audit de la consultation.

## 11. Impacts sur les étapes suivantes

### 0P.5 — Modèle métier

Le modèle devra représenter au minimum Payment, intention externe, décision,
snapshots de contrôle, instruction et outcome comptables, références TFJ et
liens vers les livraisons, sans incorporer les DTO externes.

### 0P.6 — Machine à états

La machine devra formaliser les états candidats cités ici, distinguer les
états financiers des états de livraison et interdire :

- une transition vers le succès depuis un rejet ;
- une finalité Trésor sans TFJ rapprochée ;
- un rejeu financier depuis un outcome inconnu ;
- la disparition d’un historique après extourne.

### 0P.8 — Événements

Le catalogue devra couvrir réception, rejet, contrôles, posting, outcome
inconnu/partiel, attente TFJ, rapprochement, quarantaine, finalité,
notification et extourne.

### 0P.9 — Exigences contractuelles

Le Contract Pack devra fournir les champs, codes stables et opérations
nécessaires à chacun des parcours, en particulier :

- idempotence conflictuelle ;
- recherche d’un posting inconnu ;
- résultat partiel ;
- notification `PROCESSING` et extourne ;
- confirmation TFJ et quarantaine ;
- rejeu idempotent des notifications.

## 12. Critères de sortie de l’étape 0P.4

- [x] Le parcours nominal couvre réception, persistance, contrôles, posting,
  notification immédiate, TFJ et notification définitive.
- [x] Les quinze parcours alternatifs demandés sont documentés.
- [x] Chaque action respecte le propriétaire défini dans la Context Map.
- [x] Toute demande authentifiée est persistée avant appel Amplitude.
- [x] `ObservedCustomer` est alimenté sur succès comme sur échec.
- [x] Les rejets avant écriture ne produisent aucun effet bancaire.
- [x] Le doublon identique ne produit aucun nouveau traitement financier.
- [x] Le conflit d’idempotence ne modifie pas le Payment original.
- [x] Un résultat comptable inconnu n’est jamais rejoué aveuglément.
- [x] Un effet partiel déclenche rapprochement et extourne explicite.
- [x] L’échec de notification ne modifie pas le résultat financier.
- [x] L’absence de TFJ maintient le Payment en attente.
- [x] Une TFJ non rapprochable est mise en quarantaine sans modifier Payment.
- [x] La finalité Trésor exige une TFJ favorable rapprochée et persistée.
- [x] Les états cités sont identifiés comme candidats jusqu’à 0P.6.
- [x] Les exigences d’audit et de reprise sont explicites.

## 13. Verdict 0P.4

```text
PAYMENT BUSINESS FLOWS: ESTABLISHED
NOMINAL FLOW: COMPLETE
ALTERNATIVE FLOWS: 15/15
BLIND FINANCIAL RETRY: FORBIDDEN
TFJ FINALITY: EXPLICIT
FLOW OWNERSHIP AMBIGUITIES: 0
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.5 — PAYMENT BUSINESS MODEL
```
