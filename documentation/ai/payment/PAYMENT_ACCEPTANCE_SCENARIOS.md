# SIXPAY CONNECT — Payment Acceptance Scenarios

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Document | `PAYMENT_ACCEPTANCE_SCENARIOS.md` |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.12 — Construire la stratégie de test` |
| Branche | `feat/payment-contract-pack` |
| Commit de référence | `741038f67992c9dacaf7c4a7ceb11d865c15cc1f` |
| Statut | `PAYMENT_ACCEPTANCE_SCENARIOS_ESTABLISHED` |
| Caractère | Normatif pour les futurs développements Payment |
| Génération de code | Interdite |
| Étape suivante | `0P.13 — Consolider le Payment Preflight Pack` |

---

## 2. Objectif

Ce document transforme les décisions du Payment Preflight en scénarios
d’acceptation vérifiables.

Il doit permettre aux équipes de :

- dériver les tests avant ou pendant l’implémentation ;
- vérifier les invariants sans dépendre de détails techniques privés ;
- couvrir les parcours nominaux et alternatifs ;
- empêcher les doubles effets financiers ;
- valider les contrats TRESOR PAY et Amplitude ;
- tester les reprises, rejeux, TFJ et extournes ;
- reconstruire les projections de lecture ;
- produire des preuves de conformité au Gate.

Les scénarios définissent le comportement attendu. Ils ne prescrivent pas une
implémentation interne non arrêtée et n’autorisent pas encore la génération du
code Payment.

---

## 3. Sources normatives

| Source | Utilisation |
| --- | --- |
| `PAYMENT_SOURCE_BASELINE.md` | Traçabilité des exigences `PAY-SRC-*` |
| `PAYMENT_CONTEXT_MAP.md` | Responsabilités des systèmes et modules |
| `PAYMENT_BUSINESS_FLOWS.md` | Parcours nominal et quinze parcours alternatifs |
| `PAYMENT_DOMAIN_MODEL.md` | Aggregate Root, Value Objects et invariants |
| `PAYMENT_STATE_MACHINE.yaml` | 16 états distincts et 34 transitions normatives |
| `PAYMENT_EVENT_CATALOG.yaml` | Événements, producteurs, consumers et replay |
| `PAYMENT_CONTRACT_REQUIREMENTS.yaml` | Sept familles et neuf artefacts contractuels |
| `PAYMENT_SECURITY_AUDIT_BASELINE.md` | Authentification, RBAC, audit et observabilité |
| `PAYMENT_RESILIENCE_BASELINE.md` | Idempotence, Outbox, DLQ, rapprochement et extourne |
| `IA_0R_BLOCKING_DECISIONS.yaml` | Décisions bloquantes fermées |
| `SIXPAY_BACKEND_ENGINEERING_GENERATION_CONTRACT.md` | Pyramide, conventions et règles de test |

### 3.1 Traçabilité par famille

| Famille | Sources principales |
| --- | --- |
| Domaine et états | `PAY-SRC-020` à `PAY-SRC-026`, modèle et machine |
| Idempotence et concurrence | `PAY-SRC-007`, `PAY-SRC-010`, `PAY-SRC-026` |
| Replay et Outbox | `PAY-SRC-037` à `PAY-SRC-040` |
| Contrats et Amplitude | `PAY-SRC-050` à `PAY-SRC-055` |
| Notifications et TFJ | `PAY-SRC-028` à `PAY-SRC-034` |
| Sécurité | `PAY-SRC-005`, `PAY-SRC-006`, `PAY-SRC-043` à `PAY-SRC-046` |
| ObservedCustomer | `PAY-SRC-013`, `PAY-SRC-014`, `PAY-SRC-019` |
| Écriture partielle et extourne | `PAY-SRC-024`, `PAY-SRC-025`, `IA0R-D07` |

---

# Partie A — Stratégie générale

## 4. Principes de test

1. Un test vérifie un comportement observable, pas une méthode privée.
2. Les invariants du domaine sont testés sans contexte Spring.
3. `@SpringBootTest` n’est pas utilisé par défaut.
4. Les tests de persistance utilisent PostgreSQL Testcontainers, jamais H2.
5. Les horloges, UUID, hasard, timeouts et schedulers sont contrôlables.
6. Les tests sont déterministes et indépendants de leur ordre.
7. Les mocks ne remplacent pas les tests de mapping, contrat ou contrainte.
8. Tout test financier négatif prouve l’absence de second effet.
9. Toute livraison au moins une fois est testée avec doublon.
10. Les données de test sont synthétiques et ne contiennent aucun secret réel.
11. Les scénarios d’erreur vérifient l’audit et l’observabilité attendus.
12. Une exigence critique non couverte empêche la validation du Gate concerné.

## 5. Niveaux de test

| Niveau | Cible | Dépendances |
| --- | --- | --- |
| Domaine | Invariants, Value Objects, transitions | Java pur |
| Application | Orchestration et ports | Doubles contrôlés |
| Persistance | JPA, contraintes, verrouillage, Outbox | PostgreSQL Testcontainer |
| API | Validation, HTTP, RFC 7807, sécurité | Slice ou intégration ciblée |
| Contrat | OpenAPI, schémas, exemples, compatibilité | Validation contractuelle |
| Intégration | HTTP, messaging, Vault, sécurité | Composant réel ou environnement contrôlé |
| Intermodules | Payment, Customer, Accounting, Notification | Assemblage modulaire |
| End-to-end | Parcours TRESOR PAY → Amplitude → TFJ | Environnement de recette |
| Résilience | Pannes, timeout, crash, reprise, DLQ | Environnement contrôlé |
| Sécurité | AuthN, AuthZ, masquage, audit | Environnement contrôlé |

## 6. Conventions d’exécution

- tests unitaires : `*Test.java` ou `*Tests.java` ;
- tests d’intégration : `*IT.java` ou `*ITCase.java` ;
- tests unitaires : Maven Surefire ;
- tests d’intégration : Maven Failsafe ;
- profil complet : `full-tests` ;
- couverture : profil `coverage` ;
- PostgreSQL 15.x via Testcontainers ;
- Java 21 et versions gérées par le parent/BOM.

Le build par défaut peut ignorer les tests d’intégration via `skipITs`. La
validation Payment complète doit donc invoquer explicitement le profil
`full-tests`.

## 7. Étapes de pipeline

| Étape | Contenu obligatoire |
| --- | --- |
| Pull Request | Domaine, application, API slice, architecture, lint et schémas |
| Build complet | Persistance PostgreSQL, messaging, sécurité, contrats |
| Nightly | Concurrence, crash/reprise, DLQ, reconstruction, résilience |
| Pré-release | E2E, Amplitude sandbox, TRESOR PAY, TFJ, sécurité |
| Post-déploiement | Smoke tests sans effet financier réel et health checks |

Un test financier de production n’utilise jamais un compte réel sans procédure
bancaire explicitement approuvée.

## 8. Données et doubles de test

Les jeux de données couvrent :

- plusieurs institutions ;
- clients existants et absents ;
- NIU concordant, absent, corrigé et divergent ;
- comptes actifs, bloqués, opposés et étrangers au client ;
- soldes suffisants et insuffisants ;
- montants et devises valides/invalides ;
- références et clés identiques ou conflictuelles ;
- posting complet, partiel, inconnu et extourné ;
- TFJ favorable, adverse, absente, dupliquée et non rapprochable.

Tous les comptes sont synthétiques et masqués dans les rapports.

Les doubles externes permettent de contrôler :

- statut HTTP ;
- latence et timeout ;
- fermeture avant/après réception de la requête ;
- réponse malformée ;
- répétition et ordre ;
- `Retry-After` ;
- résultats successifs ;
- signature et certificat.

## 9. Format d’un scénario

Chaque scénario implémenté conserve :

- identifiant stable ;
- exigence ou transition couverte ;
- niveau de test ;
- données et préconditions ;
- stimulus ;
- résultat observable ;
- effets interdits ;
- preuves : état, événements, audit, appels, métriques.

Les tableaux ci-dessous constituent le catalogue minimal. Des variantes
paramétrées peuvent couvrir plusieurs lignes sans perdre l’identifiant de
traçabilité.

---

# Partie B — Catalogue des scénarios

## 10. Tests du domaine — `PAY-ACC-DOM`

| ID | Scénario | Précondition / action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-DOM-001` | Créer un Payment valide | Identifiants, Money, PayerSnapshot et compte valides | État initial `RECEIVED`, références et corrélation immuables |
| `PAY-ACC-DOM-002` | Rejeter un montant invalide | Montant nul, négatif ou devise invalide | Value Object refusé, aucun Payment ni événement |
| `PAY-ACC-DOM-003` | Protéger l’identité | Tenter de modifier `PaymentId` ou `PaymentReference` | Modification impossible |
| `PAY-ACC-DOM-004` | Approuver après vérification | Snapshot bancaire frais et favorable | `APPROVED` et événement unique |
| `PAY-ACC-DOM-005` | Refuser avec motif | Vérification défavorable | `REJECTED`, motif stable, aucune écriture |
| `PAY-ACC-DOM-006` | Refuser une vérification obsolète | Snapshot hors fenêtre | Posting interdit |
| `PAY-ACC-DOM-007` | Posting complet | Débit et CUT confirmés | Snapshots persistés, progression vers TFJ |
| `PAY-ACC-DOM-008` | Outcome inconnu | Résultat bancaire ambigu | `ACCOUNTING_OUTCOME_UNKNOWN`, aucun succès supposé |
| `PAY-ACC-DOM-009` | Exiger une extourne | Débit confirmé, CUT échoué | `REVERSAL_REQUIRED`, succès interdit |
| `PAY-ACC-DOM-010` | Finalité TFJ | TFJ favorable rapprochée | `TREASURY_INTEGRATED` uniquement après persistance |
| `PAY-ACC-DOM-011` | Préserver l’historique après extourne | Reversal confirmée | `REVERSED`, posting original conservé |
| `PAY-ACC-DOM-012` | Optimistic concurrency | Version attendue incorrecte | Commande obsolète rejetée sans mutation |

## 11. Tests de machine à états — `PAY-ACC-STM`

### 11.1 Couverture généralisée

Un test paramétré doit parcourir les 34 transitions normatives de
`PAYMENT_STATE_MACHINE.yaml` et vérifier pour chacune :

- état source ;
- trigger ;
- préconditions ;
- état cible ;
- effets ;
- événements ;
- interdictions ;
- replay.

Un second test paramétré doit vérifier que toute transition non autorisée par
la machine est rejetée.

### 11.2 Scénarios critiques

| ID | Scénario | Action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-STM-001` | Chemin nominal | Parcourir réception → TFJ | États dans l’ordre, terminaison `TREASURY_INTEGRATED` |
| `PAY-ACC-STM-002` | Rejet pré-posting | Contrôle défavorable | `REJECTED`, aucun état financier |
| `PAY-ACC-STM-003` | Échec technique sans effet | Échec bancaire prouvé avant écriture | `FAILED` selon transition autorisée |
| `PAY-ACC-STM-004` | Timeout de posting | Réponse absente après soumission possible | `ACCOUNTING_OUTCOME_UNKNOWN` |
| `PAY-ACC-STM-005` | Effet partiel | Débit OK, CUT échoué | `REVERSAL_REQUIRED` |
| `PAY-ACC-STM-006` | Extourne en cours | Instruction autorisée | `REVERSAL_PENDING` |
| `PAY-ACC-STM-007` | Extourne confirmée | Référence bancaire valide | `REVERSED`, terminal |
| `PAY-ACC-STM-008` | TFJ absente | Dépasser cut-off | Reste `PENDING_END_OF_DAY_CONFIRMATION` |
| `PAY-ACC-STM-009` | Terminal immuable | Nouvelle commande changeante sur terminal | Rejet, aucun nouvel événement |
| `PAY-ACC-STM-010` | Replay du même fait | Même eventId/payload | No-op et état inchangé |

## 12. Tests d’idempotence — `PAY-ACC-IDM`

| ID | Scénario | Action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-IDM-001` | Première demande | Nouvelle clé + empreinte | Un registre et un Payment |
| `PAY-ACC-IDM-002` | Doublon en cours | Même clé et empreinte | Même Payment, statut courant |
| `PAY-ACC-IDM-003` | Doublon terminé | Même clé et empreinte | Même résultat, aucun nouvel effet |
| `PAY-ACC-IDM-004` | Conflit payload | Même clé, autre empreinte | `409`, Payment original intact |
| `PAY-ACC-IDM-005` | Référence réutilisée | Même requestId, autre clé | Conflit audité |
| `PAY-ACC-IDM-006` | Posting rejoué | Même clé bancaire et instruction | Même référence/outcome |
| `PAY-ACC-IDM-007` | Posting conflictuel | Même clé, autre instruction | Conflit, aucun second posting |
| `PAY-ACC-IDM-008` | TFJ dupliquée | Même clé TFJ et payload | Un seul rapprochement/événement |
| `PAY-ACC-IDM-009` | Notification dupliquée | Même `eventId` | Une seule prise en compte |
| `PAY-ACC-IDM-010` | Rétention | Dernière tentative + 13 mois | Purge selon politique sans perte de l’audit Payment |

## 13. Tests de concurrence — `PAY-ACC-CON`

| ID | Scénario | Concurrence | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-CON-001` | Deux requêtes identiques | Création simultanée | Un Payment, un registre |
| `PAY-ACC-CON-002` | Deux payloads conflictuels | Même clé simultanée | Un gagnant, un `409` |
| `PAY-ACC-CON-003` | Deux commandes d’état | Même version Payment | Une transition, l’autre stale |
| `PAY-ACC-CON-004` | Deux workers Outbox | Même ligne | Un claim via `SKIP LOCKED` |
| `PAY-ACC-CON-005` | Deux consumers | Même eventId | Un effet projection/notification |
| `PAY-ACC-CON-006` | Callback et lookup posting | Outcomes simultanés | Une résolution cohérente et auditée |
| `PAY-ACC-CON-007` | TFJ push et fallback | Même confirmation | Un rapprochement |
| `PAY-ACC-CON-008` | Deux extournes | Même dossier | Une instruction idempotente |

Les scénarios utilisent de vrais threads et PostgreSQL pour les contraintes
transactionnelles ; une simple simulation séquentielle est insuffisante.

## 14. Tests de rejeu — `PAY-ACC-RPL`

| ID | Scénario | Action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-RPL-001` | Republication Outbox | Rejouer même eventId | Aucun nouveau fait métier |
| `PAY-ACC-RPL-002` | Crash après publish | Publier puis interrompre avant marquage | Redelivery dédupliquée |
| `PAY-ACC-RPL-003` | Replay DLQ notification | Cause corrigée | Livraison unique, audit complet |
| `PAY-ACC-RPL-004` | Replay projection | Reconstruire depuis événements | Même état fonctionnel |
| `PAY-ACC-RPL-005` | Payload conflictuel | Même eventId, autre payload | Rejet, quarantaine et alerte |
| `PAY-ACC-RPL-006` | Version non supportée | Rejouer schéma inconnu | Quarantaine version, aucun effet |
| `PAY-ACC-RPL-007` | Replay batch borné | Lot autorisé | Limite, rapport et reprise contrôlée |
| `PAY-ACC-RPL-008` | Rejeu financier interdit | Outcome inconnu | Refus du replay, rapprochement requis |

## 15. Tests de contrats — `PAY-ACC-CTR`

Les neuf artefacts identifiés en 0P.9 doivent passer validation OpenAPI 3.1,
tests de schéma, exemples et tests consommateur/fournisseur.

| ID | Contrat / règle | Résultat attendu |
| --- | --- | --- |
| `PAY-ACC-CTR-001` | TRESOR PAY payment request | Headers, schéma, `202`, RFC 7807 et idempotence conformes |
| `PAY-ACC-CTR-002` | Amplitude verification | Client, compte, restrictions et fonds représentables |
| `PAY-ACC-CTR-003` | Amplitude posting | Atomicité, lookup et reversal couverts |
| `PAY-ACC-CTR-004` | Notification immédiate | Tous les résultats immédiats et `204` couverts |
| `PAY-ACC-CTR-005` | TFJ webhook | Matching keys, doublon et conflit couverts |
| `PAY-ACC-CTR-006` | TFJ fallback API | Résultat présent, absent et erreur couverts |
| `PAY-ACC-CTR-007` | Notification définitive | Émise uniquement après TFJ rapprochée |
| `PAY-ACC-CTR-008` | Payment query API | Lecture, pagination, masquage et RBAC |
| `PAY-ACC-CTR-009` | ObservedCustomer query API | Projection non autoritative et masquée |
| `PAY-ACC-CTR-010` | Compatibilité additive v1 | Ajout compatible accepté, rupture refusée |
| `PAY-ACC-CTR-011` | Données sensibles | Aucun exemple avec secret ou compte complet |
| `PAY-ACC-CTR-012` | Registre | Classification OpenAPI et registry cohérentes |

## 16. Tests d’intégration Amplitude — `PAY-ACC-AMP`

| ID | Scénario | Réponse simulée/sandbox | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-AMP-001` | Client et compte valides | Vérification favorable | Payment approuvable |
| `PAY-ACC-AMP-002` | Client absent | Not found métier | Rejet stable, aucun posting |
| `PAY-ACC-AMP-003` | NIU divergent | Mismatch | Rejet, ObservedCustomer actualisé selon règle |
| `PAY-ACC-AMP-004` | Compte étranger | Ownership false | Rejet, aucune écriture |
| `PAY-ACC-AMP-005` | Blocage/opposition | Restriction active | Rejet motivé |
| `PAY-ACC-AMP-006` | Fonds insuffisants | Available funds insuffisants | Rejet, aucun posting |
| `PAY-ACC-AMP-007` | Lecture transitoire | `503`, puis succès | Retry/backoff/jitter puis succès |
| `PAY-ACC-AMP-008` | Erreur permanente | `422` | Aucun retry |
| `PAY-ACC-AMP-009` | Circuit ouvert | Seuil atteint | Fast-fail, aucun faux accord |
| `PAY-ACC-AMP-010` | Posting atomique | Débit + CUT confirmés | Références persistées |
| `PAY-ACC-AMP-011` | Timeout après envoi | Connexion interrompue | Outcome inconnu, lookup |
| `PAY-ACC-AMP-012` | Lookup retrouve posting | Même clé/référence | Résolution sans second posting |
| `PAY-ACC-AMP-013` | Réponse malformée | Payload invalide | Erreur normalisée, audit, aucune supposition |
| `PAY-ACC-AMP-014` | Authentification bancaire refusée | `401/403` | Aucun retry automatique, alerte sécurisée |

## 17. Tests de notification — `PAY-ACC-NOT`

| ID | Scénario | Action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-NOT-001` | Résultat rejeté | Consommer événement | Webhook rejet une fois |
| `PAY-ACC-NOT-002` | Posting en attente TFJ | Résultat immédiat | `POSTED_PENDING_TFJ`, pas de finalité |
| `PAY-ACC-NOT-003` | Outcome inconnu | Résultat immédiat | `PROCESSING` |
| `PAY-ACC-NOT-004` | Succès HTTP | TRESOR PAY retourne `204` | Livraison terminale persistée |
| `PAY-ACC-NOT-005` | Retryable | `503`, puis `204` | Même eventId, deliveryId distinct |
| `PAY-ACC-NOT-006` | Erreur permanente | `400/422` | Pas de boucle, DLQ/alerte |
| `PAY-ACC-NOT-007` | Épuisement | Toutes tentatives échouent | DLQ sans mutation financière |
| `PAY-ACC-NOT-008` | Signature invalide | Receiver refuse | Échec sécurisé et audité |
| `PAY-ACC-NOT-009` | Notification finale prématurée | Pas de TFJ rapprochée | Emission interdite |
| `PAY-ACC-NOT-010` | Replay autorisé | Cause corrigée | Livraison unique et auditée |

## 18. Tests TFJ — `PAY-ACC-TFJ`

| ID | Scénario | Confirmation | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-TFJ-001` | TFJ nominale | Toutes clés concordent | `TREASURY_INTEGRATED` |
| `PAY-ACC-TFJ-002` | Doublon identique | Même clé/payload | No-op |
| `PAY-ACC-TFJ-003` | Doublon conflictuel | Même clé/autre statut | Quarantaine et alerte |
| `PAY-ACC-TFJ-004` | Payment absent | Aucune correspondance | Quarantaine, aucun Payment modifié |
| `PAY-ACC-TFJ-005` | Correspondance multiple | Plusieurs candidats | Quarantaine |
| `PAY-ACC-TFJ-006` | Référence incohérente | Institution/date/posting divergent | Quarantaine |
| `PAY-ACC-TFJ-007` | Absence au cut-off | Aucun callback | Attente, fallback et alerte |
| `PAY-ACC-TFJ-008` | Fallback retrouve résultat | Query favorable | Rapprochement idempotent |
| `PAY-ACC-TFJ-009` | Fallback indisponible | Erreur transitoire | Retry borné, attente maintenue |
| `PAY-ACC-TFJ-010` | Résolution manuelle | Preuve corrigée | Retraitement même confirmation et audit |
| `PAY-ACC-TFJ-011` | TFJ adverse | Résultat défavorable | Pas de fausse finalité, traitement prévu |
| `PAY-ACC-TFJ-012` | Final notification | TFJ persistée | Une notification définitive |

## 19. Tests de sécurité — `PAY-ACC-SEC`

| ID | Scénario | Action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-SEC-001` | Token absent/invalide | Appel entrant | `401`, aucun Payment |
| `PAY-ACC-SEC-002` | Subscription Key absente/invalide | Appel entrant | Refus avant domaine |
| `PAY-ACC-SEC-003` | Un credential sur deux valide | Appel entrant | Refus |
| `PAY-ACC-SEC-004` | OPS | Lecture résumé/motif | Autorisée et auditée |
| `PAY-ACC-SEC-005` | OPS timeline/TFJ | Accès interdit | `403` sans fuite |
| `PAY-ACC-SEC-006` | MANAGER export | Permission + motif | Export masqué et audité |
| `PAY-ACC-SEC-007` | AUDITOR | Audit/corrélation | Lecture autorisée, données masquées |
| `PAY-ACC-SEC-008` | ADMIN | Lecture Payment | Refus, configuration seulement |
| `PAY-ACC-SEC-009` | SUPPORT/READ_ONLY/PARTNER | Accès Payment | Refus par défaut |
| `PAY-ACC-SEC-010` | RIB/IBAN | API, log, trace, export | Jamais complet, maximum quatre derniers |
| `PAY-ACC-SEC-011` | Credentials | Capturer logs/metrics/traces/DLQ | Aucune valeur sensible |
| `PAY-ACC-SEC-012` | Ressource interdite | ID existant/non existant | Pas de révélation d’existence |
| `PAY-ACC-SEC-013` | Secret absent | Démarrage/appel | Échec fermé |
| `PAY-ACC-SEC-014` | Rotation secret | Ancienne/nouvelle clé | Chevauchement contrôlé puis révocation |
| `PAY-ACC-SEC-015` | Audit obligatoire indisponible | Action sensible | Pas de réussite silencieuse |

## 20. Reconstruction d’ObservedCustomer — `PAY-ACC-OBS`

| ID | Scénario | Flux d’événements | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-OBS-001` | Première demande réussie | PaymentRequestReceived | Projection créée |
| `PAY-ACC-OBS-002` | Première demande échouée | Requête puis rejet/échec | Projection créée malgré échec |
| `PAY-ACC-OBS-003` | Enrichissement bancaire | VerificationCompleted | Référence bancaire et faits masqués actualisés |
| `PAY-ACC-OBS-004` | Même événement | eventId dupliqué | No-op |
| `PAY-ACC-OBS-005` | Événements rejoués | Flux complet dans le même ordre logique | Projection identique |
| `PAY-ACC-OBS-006` | Reprise après crash | Checkpoint avant événement | Reprise sans perte ni doublon |
| `PAY-ACC-OBS-007` | NIU corrigé | Nouvelle observation vérifiée | Attribut mis à jour, identité stable |
| `PAY-ACC-OBS-008` | Plusieurs institutions | Même NIU, banques différentes | Projections distinctes, aucun merge automatique |
| `PAY-ACC-OBS-009` | Ordre retardé | Événement ancien après récent | Version/fraîcheur empêche la régression |
| `PAY-ACC-OBS-010` | Reconstruction totale | Vider projection puis replay | Même contenu fonctionnel et mêmes liens Payment |

La comparaison de reconstruction ignore uniquement les métadonnées techniques
explicitement recalculables, jamais une donnée fonctionnelle.

## 21. Tests d’écriture partielle — `PAY-ACC-PAR`

| ID | Scénario | Outcome | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-PAR-001` | Débit + CUT confirmés | Complet | Poursuite TFJ |
| `PAY-ACC-PAR-002` | Débit OK, CUT inconnu | Partiel ambigu | `ACCOUNTING_OUTCOME_UNKNOWN` |
| `PAY-ACC-PAR-003` | Débit OK, CUT échoué | Partiel adverse | `REVERSAL_REQUIRED` |
| `PAY-ACC-PAR-004` | Aucun effet prouvé | Lookup négatif | Reprise seulement selon contrat |
| `PAY-ACC-PAR-005` | Timeout puis lookup positif | Posting retrouvé | Aucun second posting |
| `PAY-ACC-PAR-006` | Lookup toujours inconnu | Aucune preuve | Dossier ouvert, alerte, `PROCESSING` |
| `PAY-ACC-PAR-007` | Retry automatique interdit | Outcome inconnu | Zéro second appel d’écriture |
| `PAY-ACC-PAR-008` | Crash avant résultat local | Banque a traité | Rapprochement retrouve l’effet |
| `PAY-ACC-PAR-009` | Callback/query divergents | Deux résultats | Quarantaine/rapprochement, pas de finalité |

Chaque scénario vérifie le nombre exact d’instructions financières envoyées.

## 22. Tests d’extourne — `PAY-ACC-REV`

| ID | Scénario | Précondition/action | Résultat attendu |
| --- | --- | --- | --- |
| `PAY-ACC-REV-001` | Extourne nominale | Débit confirmé, CUT échoué, validations | `REVERSAL_PENDING` puis `REVERSED` |
| `PAY-ACC-REV-002` | Absence de preuve | Demande d’extourne | Refus |
| `PAY-ACC-REV-003` | TFJ seulement absente | Retard TFJ | Aucune extourne automatique |
| `PAY-ACC-REV-004` | Double contrôle absent | Une seule validation | Refus et audit |
| `PAY-ACC-REV-005` | Clé identique au posting | Instruction reversal | Refus |
| `PAY-ACC-REV-006` | Même reversal rejouée | Même clé et instruction | Même référence/outcome |
| `PAY-ACC-REV-007` | Reversal conflictuelle | Même clé, autre montant | Conflit, aucun effet |
| `PAY-ACC-REV-008` | Timeout reversal | Réponse inconnue | Lookup, aucun renvoi aveugle |
| `PAY-ACC-REV-009` | Reversal déjà confirmée | Nouvelle commande | No-op/rejet selon machine |
| `PAY-ACC-REV-010` | Historique | Extourne confirmée | Posting et preuves originales conservés |
| `PAY-ACC-REV-011` | Notification | Reversal confirmée | Résultat contractuel unique vers TRESOR PAY |
| `PAY-ACC-REV-012` | Runbook complet | Exécuter toutes étapes | Audit, références, métriques et clôture |

---

# Partie C — Couverture transverse

## 23. Matrice parcours / familles

| Parcours | DOM | STM | IDM | CON | RPL | CTR | AMP | NOT | TFJ | SEC | OBS | PAR | REV |
| --- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| Nominal | X | X | X |  |  | X | X | X | X | X | X |  |  |
| Demande invalide | X | X |  |  |  | X |  |  |  | X | X |  |  |
| Doublon/conflit |  |  | X | X | X | X |  | X | X |  | X |  |  |
| Rejet bancaire | X | X |  |  |  |  | X | X |  |  | X |  |  |
| Amplitude indisponible |  | X |  |  |  |  | X |  |  |  |  | X |  |
| Outcome inconnu | X | X | X | X | X | X | X | X |  |  | X | X |  |
| Effet partiel | X | X | X | X |  | X | X | X |  |  | X | X | X |
| Notification échouée |  |  | X |  | X | X |  | X |  | X |  |  |  |
| TFJ absente/non reconnue | X | X | X | X | X | X | X | X | X | X | X |  |  |
| Extourne | X | X | X | X | X | X | X | X |  | X | X | X | X |

## 24. Preuves attendues

Chaque exécution de recette produit selon le scénario :

- résultat de test ;
- état Payment avant/après ;
- événements et versions ;
- appels externes observés ;
- lignes d’idempotence et Outbox ;
- audit corrélé ;
- métriques/alertes ;
- rapport de contrat ;
- preuve d’absence de donnée sensible ;
- preuve du nombre d’écritures financières.

Les rapports ne contiennent jamais de credentials ou comptes complets.

## 25. Critères de passage

### 25.1 Pull Request

- tous les tests unitaires et API ciblés passent ;
- aucune architecture rule ne régresse ;
- les contrats modifiés restent valides et compatibles ;
- aucune donnée sensible n’apparaît ;
- aucun test désactivé sans justification et échéance.

### 25.2 Gate IA-0.5P

- les neuf contrats passent leurs scénarios `CTR` ;
- les deux parties externes ont validé les cas applicables ;
- les erreurs et règles d’idempotence sont testables ;
- les scénarios de posting, lookup, TFJ et reversal sont contractuellement
  représentables.

### 25.3 Autorisation de développement

- chaque exigence MVP possède au moins un scénario ;
- les 34 transitions et les transitions interdites sont couvertes ;
- les risques de double effet possèdent des tests négatifs ;
- les tests d’intégration requis peuvent s’exécuter dans le pipeline complet ;
- les dépendances externes disposent de doubles et d’un environnement de
  validation ;
- les équipes Architecture, Security, Integration, Operations et Product
  acceptent la stratégie.

## 26. Critères de sortie 0P.12

- [x] Tests du domaine définis.
- [x] Tests des 34 transitions et transitions interdites définis.
- [x] Tests d’idempotence définis.
- [x] Tests de concurrence réelle définis.
- [x] Tests de rejeu et DLQ définis.
- [x] Tests des neuf artefacts contractuels définis.
- [x] Tests Amplitude définis.
- [x] Tests de notification définis.
- [x] Tests TFJ définis.
- [x] Tests de sécurité définis.
- [x] Reconstruction d’ObservedCustomer définie.
- [x] Tests d’écriture partielle définis.
- [x] Tests d’extourne définis.
- [x] Pyramide, pipeline, données et preuves définis.
- [x] Matrice de couverture transverse définie.

## 27. Verdict 0P.12

```text
PAYMENT ACCEPTANCE SCENARIOS: ESTABLISHED
TEST FAMILIES: 13/13
STATE MACHINE TRANSITIONS: 34/34 REQUIRED
FORBIDDEN TRANSITIONS: EXHAUSTIVE COVERAGE REQUIRED
FINANCIAL DOUBLE-EFFECT NEGATIVE TESTS: REQUIRED
CONTRACT ARTIFACTS: 9/9 COVERED
OBSERVED CUSTOMER REBUILD: REQUIRED
PARTIAL WRITE AND REVERSAL: COVERED
FULL INTEGRATION PROFILE: REQUIRED
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.13 — CONSOLIDATE PAYMENT PREFLIGHT PACK
```
