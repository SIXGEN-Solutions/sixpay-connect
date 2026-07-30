# SIXPAY CONNECT — Payment Security, Audit & Observability Baseline

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Document | `PAYMENT_SECURITY_AUDIT_BASELINE.md` |
| Gate | `IA-0P — Payment Preflight` |
| Étape | `0P.10 — Définir sécurité, audit et observabilité` |
| Branche | `feat/payment-contract-pack` |
| Commit de référence | `2836dde669487c4d8427f1805f568d737c206065` |
| Statut | `SECURITY_AUDIT_OBSERVABILITY_BASELINE_ESTABLISHED` |
| Caractère | Normatif pour le MVP Payment |
| Génération de code | Interdite |
| Étape suivante | `0P.11 — Résilience et reprise opérationnelle` |

---

## 2. Objectif

Cette baseline fixe les exigences minimales de sécurité, d’audit et
d’observabilité du MVP Payment avant la production et l’approbation des
contrats du Gate IA-0.5P.

Elle garantit que :

- seuls les systèmes et utilisateurs authentifiés accèdent au parcours ;
- les autorisations appliquent le moindre privilège ;
- les secrets et données bancaires restent protégés ;
- chaque décision et accès sensible est traçable ;
- les opérations peuvent diagnostiquer un incident sans consulter de données
  sensibles ;
- un paiement bloqué, une confirmation TFJ absente ou une notification en
  échec déclenche une alerte exploitable.

Cette étape définit des exigences. Elle n’autorise pas encore
l’implémentation du domaine Payment.

## 3. Sources normatives

| Référence | Rôle |
| --- | --- |
| `PAYMENT_SOURCE_BASELINE.md` | Hiérarchie des sources et exigences `PAY-SRC-*` |
| `PAYMENT_CONTEXT_MAP.md` | Autorités et responsabilités des modules |
| `PAYMENT_BUSINESS_FLOWS.md` | Parcours nominaux, erreurs et reprises |
| `PAYMENT_DOMAIN_MODEL.md` | Données métier, snapshots et frontières |
| `PAYMENT_STATE_MACHINE.yaml` | États à surveiller et transitions auditables |
| `PAYMENT_EVENT_CATALOG.yaml` | Événements, corrélation et données interdites |
| `PAYMENT_CONTRACT_REQUIREMENTS.yaml` | Frontières d’authentification et contrats |
| `IA_0R_BLOCKING_DECISIONS.yaml` | RBAC, rétention, authentification et TFJ |
| `SIXPAY_BACKEND_TECHNOLOGY_MATRIX.md` | Technologies de sécurité et observabilité |
| `SIXPAY_BACKEND_ENGINEERING_GENERATION_CONTRACT.md` | Règles d’implémentation obligatoires |

Les exigences issues de `IA0R-D03`, `IA0R-D04` et `IA0R-D05` sont reprises
sans modification. Les seuils d’alertes non contractualisés sont des
paramètres SIXPAY configurables et non des SLA.

---

# Partie A — Sécurité

## 4. Principes

1. **Deny by default** : tout accès non explicitement autorisé est refusé.
2. **Moindre privilège** : un rôle ne reçoit que les permissions nécessaires.
3. **Séparation des responsabilités** : administration, exploitation et audit
   restent séparés.
4. **Défense en profondeur** : authentification, autorisation, chiffrement,
   validation, masquage et audit sont cumulés.
5. **Minimisation** : seules les données indispensables sont conservées.
6. **Traçabilité sans divulgation** : le diagnostic ne nécessite jamais un
   secret ou un compte bancaire en clair.

Sources : `PAY-SRC-005`, `PAY-SRC-006`, `PAY-SRC-041` à `PAY-SRC-046`,
`IA0R-D03`, `IA0R-D05`.

## 5. Authentification TRESOR PAY

Tout appel TRESOR PAY → SIXPAY de demande de paiement utilise simultanément :

- TLS ;
- un Bearer Authorization Token ;
- une Subscription Key.

L’absence ou l’invalidité de l’un des deux credentials provoque un refus avant
toute création de Payment et tout appel Amplitude.

OAuth2 et mTLS restent des évolutions différées non bloquantes pour ce flux
MVP. Ils ne sont jamais simulés ou déclarés actifs sans approbation.

Règles de traitement :

- les credentials sont validés à la frontière Integration ;
- Payment reçoit uniquement un contexte d’appelant minimal ;
- les erreurs `401` et `403` ne révèlent aucune information sensible ;
- les tentatives échouées sont comptabilisées sans conserver la valeur reçue ;
- Token et Subscription Key sont absents de l’agrégat, des événements, de
  l’Outbox, de l’audit et des logs.

Décision : `IA0R-D05`.

## 6. Autres frontières

| Frontière | Baseline |
| --- | --- |
| SIXPAY → Amplitude | OAuth2 Client Credentials et mTLS selon le contrat bancaire approuvé |
| Amplitude → SIXPAY, confirmation TFJ | mTLS et signature du payload selon le contrat approuvé |
| SIXPAY → TRESOR PAY, webhooks | TLS, credentials dédiés, signature HMAC et protection anti-rejeu |
| Utilisateur interne → SIXPAY | OAuth2 / OpenID Connect, JWT validé cryptographiquement |
| Services SIXPAY internes | Identité de service approuvée et contexte minimal |

Les mécanismes externes restent soumis à validation au Gate IA-0.5P. Aucun
environnement ne remplace un mécanisme absent par une configuration permissive.

## 7. Gestion des secrets

Sont notamment considérés comme secrets :

- Bearer tokens et Subscription Keys ;
- secrets HMAC ;
- clés privées et mots de passe de certificats ;
- client secrets OAuth2 ;
- credentials Amplitude ;
- clés de chiffrement ;
- credentials des bases, brokers et outils d’observabilité.

Ils doivent :

- résider dans HashiCorp Vault via Spring Cloud Vault, ou un mécanisme externe
  explicitement validé ;
- être injectés à l’exécution et séparés par environnement ;
- être accessibles uniquement à l’identité technique autorisée ;
- ne posséder aucune valeur fonctionnelle par défaut dans le repository ;
- être associés à un propriétaire, une rotation et une révocation ;
- pouvoir être renouvelés sans modifier le domaine Payment.

Les signatures acceptent une période contrôlée de chevauchement des clés. Les
accès Vault et changements de secrets sont audités.

Un secret ne doit apparaître dans aucun log, span, baggage, label de métrique,
message d’erreur, événement, audit métier, dump, alerte, test ou payload DLQ.

## 8. Chiffrement

### 8.1 En transit

- TLS 1.3 constitue la baseline.
- La validation des certificats et noms d’hôte est obligatoire.
- Les certificats expirés, révoqués ou non approuvés sont refusés.
- Les suites cryptographiques obsolètes sont désactivées.

### 8.2 Au repos

Les données bancaires et informations personnelles sensibles utilisent
AES-256 ou un mécanisme équivalent validé, notamment pour :

- les références bancaires persistées ;
- les données Payment sensibles ;
- les sauvegardes et réplications ;
- les payloads temporaires dont la persistance est indispensable ;
- les archives d’audit et exports autorisés.

Les clés sont séparées des données, stockées dans le gestionnaire approuvé et
soumises à rotation. Les projections et index ne créent aucune copie en clair.

## 9. Masquage RIB/IBAN

Dans le MVP, aucun rôle interne ne possède un droit générique d’afficher un
RIB ou IBAN complet.

À l’extérieur de l’adaptateur bancaire autorisé :

- seule une référence opaque ou une valeur masquée est exposée ;
- au maximum les quatre derniers caractères sont visibles ;
- la longueur originale ne doit pas être révélée si elle augmente le risque ;
- un hash de rapprochement éventuel n’est jamais affiché.

Exemple autorisé : `•••• 1234`.

Le masquage s’applique aux APIs, écrans, exports, logs, traces, métriques,
erreurs RFC 7807, événements, Outbox, audits, alertes et données de test.

Une valeur complète nécessaire à Amplitude reste en mémoire le temps strictement
nécessaire et ne devient pas une donnée libre de Payment.

Décision : `IA0R-D03`.

## 10. Logs sécurisés

Les logs ne contiennent jamais :

- Token, Subscription Key, header `Authorization` ou cookie ;
- secret de signature ;
- RIB, IBAN ou compte complet ;
- NIU complet sans justification explicite ;
- payload externe brut ou donnée KYC ;
- stack trace ou réponse Amplitude dans une réponse externe.

Un log structuré peut contenir :

- timestamp UTC, service, environnement et niveau ;
- opération et état Payment ;
- résultat et code d’erreur stables ;
- `correlationId`, `traceId` et `spanId` ;
- référence Payment opaque ;
- institution autorisée ;
- durée, tentative et indicateur de replay.

Les valeurs externes sont normalisées pour empêcher l’injection de logs.

---

# Partie B — RBAC

## 11. Modèle d’autorisation

Les APIs internes utilisent OAuth2/OIDC, un JWT validé cryptographiquement et
des contrôles serveur combinant :

- rôle plateforme ;
- permission fine ;
- institution ou périmètre autorisé ;
- finalité de l’accès ;
- règle métier ou propriété de la ressource.

Le frontend n’est jamais le contrôle d’autorisation. Payment ne dépend pas de
Spring Security et reçoit une représentation applicative minimale de l’acteur.

## 12. Matrice RBAC Payment

`A` = autorisé, `C` = contrôle supplémentaire et audit renforcé,
`—` = interdit.

| Capacité | OPS | MANAGER | AUDITOR | ADMIN |
| --- | :---: | :---: | :---: | :---: |
| Rechercher un ObservedCustomer | A | A | — | — |
| Consulter un résumé Payment masqué | A | A | — | — |
| Consulter un motif d’échec opérationnel | A | A | — | — |
| Consulter la timeline Payment | — | A | — | — |
| Consulter l’état TFJ | — | A | — | — |
| Exporter un rapport opérationnel masqué | — | C | — | — |
| Lire la piste d’audit immuable | — | — | A | — |
| Consulter les corrélations masquées | — | — | A | — |
| Exporter une preuve d’audit | — | — | C | — |
| Gérer la configuration technique | — | — | — | A |
| Modifier Payment ou ObservedCustomer | — | — | — | — |
| Valider une Subscription | — | — | — | — |
| Afficher un RIB/IBAN complet | — | — | — | — |
| Déclencher librement posting ou extourne | — | — | — | — |

Un export contrôlé exige une permission explicite, un motif professionnel, une
période bornée, des données masquées, une disponibilité limitée et un audit de
la demande, production et récupération.

Une extourne passe uniquement par le cas d’usage et le runbook approuvés.

Les rôles plateforme `SUPPORT`, `READ_ONLY` et `PARTNER` ne reçoivent aucune
permission Payment par défaut. `ADMIN` gère la configuration sans accès
implicite aux données métier.

Décision : `IA0R-D03`.

---

# Partie C — Audit

## 13. Audit, historique et logs

| Mécanisme | Finalité |
| --- | --- |
| Audit métier | Prouver acteur, action, cible et résultat |
| Historique Payment | Prouver transitions et décisions |
| Journal d’intégration | Suivre les échanges normalisés |
| Log technique | Diagnostiquer un composant |
| Trace distribuée | Reconstituer appels et latences |

Un log applicatif ne remplace jamais un audit.

## 14. Actions auditables

### 14.1 Traitement métier

- réception, rejet, doublon et conflit d’idempotence ;
- contrôles bancaires et décisions ;
- posting, outcome confirmé/inconnu/partiel ;
- rapprochement et extourne ;
- TFJ reçue, rapprochée, mise en quarantaine ou rejetée ;
- transition Payment ;
- création ou actualisation d’ObservedCustomer ;
- intention et résultat de notification ;
- passage DLQ et replay opérationnel.

### 14.2 Accès aux données

- recherche Payment ou ObservedCustomer ;
- consultation de détail, timeline ou état TFJ ;
- lecture de piste d’audit ;
- demande, génération et téléchargement d’export ;
- accès refusé ;
- modification de configuration technique.

Les recherches, détails et exports sont toujours audités.

## 15. Schéma minimal

Chaque audit contient :

- `auditId` UUID ;
- `occurredAt` UTC ;
- `actorType`, `actorId` opaque et rôles utiles ;
- `action`, `targetType`, `targetId` opaque ;
- `result` et `reasonCode` stables ;
- `correlationId` et, si disponible, `traceId` ;
- `sourceSystem` ;
- institution uniquement si nécessaire ;
- `beforeState` / `afterState` sans payload complet ;
- métadonnées minimisées sur liste blanche.

Pour une consultation ou un export :

- type de requête ;
- filtres normalisés et masqués ;
- période ;
- nombre de résultats ;
- identifiant d’export ;
- motif professionnel ;
- décision d’accès.

## 16. Intégrité et conservation

- L’audit est append-only.
- Les mises à jour et suppressions applicatives sont interdites.
- Les identités d’écriture et de lecture sont séparées.
- Les accès directs sont restreints et tracés.
- Un contrôle d’intégrité rend toute altération détectable.
- Les horodatages utilisent une source synchronisée.
- L’audit est transactionnel avec le fait métier ou garanti par Outbox.
- L’échec d’un audit obligatoire interdit une réussite silencieuse.

| Donnée | Conservation |
| --- | --- |
| Payment et audit métier | 10 ans après l’état final |
| ObservedCustomer | Jusqu’au dernier Payment conservé, puis suppression ou anonymisation |
| Idempotence | 13 mois après la dernière tentative |
| Payload Outbox | 90 jours après livraison terminale |
| Métadonnées Outbox | 10 ans |
| Logs techniques | 13 mois, sans payload sensible |

Une politique légale ou bancaire plus stricte prévaut. Source : `IA0R-D04`.

## 17. Audit des rejeux

Tout replay :

- exige une identité d’opérateur ou runbook ;
- conserve l’`eventId` ou l’idempotency key d’origine ;
- possède un nouvel identifiant d’exécution ;
- enregistre motif, portée et résultat ;
- conserve les tentatives antérieures ;
- interdit tout rejeu financier aveugle.

---

# Partie D — Observabilité

## 18. Stack officielle

| Capacité | Baseline |
| --- | --- |
| Endpoints opérationnels | Spring Boot Actuator |
| Instrumentation | Micrometer |
| Collecte | Prometheus |
| Dashboards | Grafana |
| Traces | OpenTelemetry |
| Logs centralisés | ELK ou plateforme compatible |
| Corrélation | `correlationId`, `traceId`, `spanId` |

Les endpoints Actuator sensibles sont authentifiés, autorisés et non publics.

## 19. Corrélation

`X-Correlation-ID` est propagé de TRESOR PAY vers Integration, Payment,
Customer, Accounting, Amplitude, Outbox, Notification, TFJ, audit et read
models.

- Le même `correlationId` suit le parcours métier.
- Chaque appel possède aussi un `traceId` et des `spanId`.
- Un identifiant reçu est validé.
- Si le contrat autorise son absence, SIXPAY le génère avant Payment.
- Aucun identifiant ne contient de donnée métier.
- Le baggage ne contient ni NIU, compte, token ou montant.
- Une recherche par corrélation respecte le RBAC et est auditée.

## 20. Métriques métier

| Métrique logique | Type | Dimensions bornées |
| --- | --- | --- |
| `payment_requests_total` | Counter | institution, résultat |
| `payment_duplicates_total` | Counter | institution, catégorie |
| `payment_rejections_total` | Counter | institution, code |
| `payments_current_state` | Gauge | institution, état |
| `payment_state_age_seconds` | Histogram/Gauge | institution, état |
| `payment_postings_total` | Counter | institution, outcome |
| `payment_unknown_outcomes_total` | Counter | institution |
| `payment_reversals_total` | Counter | institution, outcome |
| `payment_pending_tfj_total` | Gauge | institution |
| `payment_tfj_confirmation_delay_seconds` | Histogram | institution, outcome |
| `payment_notifications_total` | Counter | type, outcome |
| `payment_notification_delivery_delay_seconds` | Histogram | type, outcome |

Les montants ne sont jamais associés à un Payment individuel dans la
télémétrie.

## 21. Métriques techniques

| Métrique logique | Dimensions bornées |
| --- | --- |
| `payment_http_requests_total` | route normalisée, méthode, classe HTTP |
| `payment_http_duration_seconds` | route normalisée, méthode, classe HTTP |
| `amplitude_calls_total` | opération, outcome |
| `amplitude_call_duration_seconds` | opération, outcome |
| `amplitude_timeouts_total` | opération |
| `amplitude_retries_total` | opération, motif |
| `payment_outbox_backlog` | type d’événement |
| `payment_outbox_oldest_age_seconds` | type d’événement |
| `payment_consumer_lag` | consumer logique |
| `payment_dlq_messages_total` | consumer, motif |
| `payment_webhook_attempts_total` | type, outcome |
| `payment_audit_write_failures_total` | type d’audit |
| `payment_security_denials_total` | frontière, motif |

Sont interdits comme labels : `PaymentId`, `PaymentReference`,
`TresorPayRequestId`, `correlationId`, `traceId`, `spanId`, NIU, RIB/IBAN,
utilisateur, idempotency key et erreur libre.

## 22. Traces et santé

Un span est créé pour l’entrée TRESOR PAY, la persistance, les appels
Amplitude, le posting, la recherche d’outcome, l’extourne, l’Outbox, les
notifications, la TFJ, le fallback et les accès sensibles.

Des health indicators contrôlent PostgreSQL, Amplitude, Vault, broker, Outbox,
endpoint TRESOR PAY, stockage d’audit et pipeline TFJ sans révéler la
topologie.

## 23. Dashboards

1. **Payment Operations** : volumes, états, ancienneté, rejets, outcomes
   inconnus et extournes.
2. **Bank Integration** : disponibilité, latence, timeouts, retries, circuit
   breaker et rapprochement.
3. **TFJ** : attente, cut-off, confirmations et quarantaine.
4. **Notifications** : livraisons, retries, Outbox et DLQ.
5. **Security and Audit** : refus, erreurs d’authentification, audits et
   exports.

---

# Partie E — Alertes

## 24. Politique

Une alerte contient identifiant, sévérité, service, environnement, catégorie,
fenêtre, seuil, dashboard et runbook. Elle ne contient aucun secret, NIU,
payload ou compte bancaire.

Les seuils sont configurables par environnement et restent non contractuels
jusqu’à validation Operations.

## 25. Catalogue minimal

| ID | Condition | Sévérité initiale | Action |
| --- | --- | --- | --- |
| `PAY-ALERT-001` | Payment non terminal plus ancien que son seuil | Warning puis Critical | Identifier l’étape bloquée |
| `PAY-ALERT-002` | `ACCOUNTING_OUTCOME_UNKNOWN` au-delà du seuil | Critical | Interroger l’outcome, sans rejeu |
| `PAY-ALERT-003` | `REVERSAL_REQUIRED/PENDING` au-delà du seuil | Critical | Escalade Accounting/Operations |
| `PAY-ALERT-004` | Confirmation TFJ absente au cut-off | Critical | Déclencher le fallback et maintenir l’attente |
| `PAY-ALERT-005` | TFJ non rapprochable | Critical | Quarantaine et rapprochement manuel |
| `PAY-ALERT-006` | Message Payment/Notification en DLQ | Critical | Correction puis replay audité |
| `PAY-ALERT-007` | Notification immédiate en échec terminal | Warning/Critical | Reprendre la livraison |
| `PAY-ALERT-008` | Notification TFJ finale en échec terminal | Critical | Reprendre sans modifier la finalité |
| `PAY-ALERT-009` | Backlog ou âge Outbox excessif | Critical | Restaurer le relay |
| `PAY-ALERT-010` | Amplitude indisponible ou circuit ouvert | Critical | Runbook bancaire |
| `PAY-ALERT-011` | Échec d’audit obligatoire | Critical | Empêcher la réussite silencieuse |
| `PAY-ALERT-012` | Hausse anormale des refus d’authentification | Warning/Critical | Analyse Security |
| `PAY-ALERT-013` | Secret ou certificat proche de l’expiration | Warning puis Critical | Rotation |

## 26. Paiement bloqué

La surveillance couvre au minimum :

- `AUTHORIZATION_CHECKING` ;
- `BANKING_CHECKING` ;
- `POSTING` ;
- `ACCOUNTING_OUTCOME_UNKNOWN` ;
- `REVERSAL_REQUIRED` ;
- `REVERSAL_PENDING` ;
- `PENDING_END_OF_DAY_CONFIRMATION`.

Une alerte ne déclenche jamais automatiquement un second débit, crédit CUT ou
une extourne.

## 27. Absence de TFJ

Au cut-off :

1. Payment reste `PENDING_END_OF_DAY_CONFIRMATION`.
2. SIXPAY poursuit le fallback.
3. Une alerte opérationnelle est créée.
4. Aucune notification finale favorable n’est émise.
5. Aucune extourne automatique n’est décidée sur le seul retard.
6. Les tentatives et la résolution sont auditées.

Source : `IA0R-D08`.

## 28. DLQ et notifications

- Toute entrée DLQ déclenche une alerte.
- Le message conserve `eventId`, `correlationId`, version et motif catégorisé,
  sans donnée sensible inutile.
- Le replay suit un runbook approuvé et est audité.
- Il réutilise l’`eventId` métier avec un nouvel identifiant d’exécution.
- Un échec de notification ne modifie pas l’état financier.
- L’échec final ne retire pas `TREASURY_INTEGRATED`.

---

# Partie F — Validation

## 29. Tests obligatoires

### 29.1 Sécurité et données

- Token ou Subscription Key absent, invalide, expiré ou révoqué ;
- accès sans rôle, permission ou périmètre ;
- signature ou certificat invalide ;
- secret absent et rotation ;
- absence de credentials dans logs, traces, métriques et erreurs ;
- masquage RIB/IBAN dans APIs, écrans, exports et audit ;
- payload sensible absent des événements et DLQ ;
- chiffrement au repos et restauration vérifiés.

### 29.2 RBAC et audit

- chaque cellule autorisée et interdite ;
- audit des recherches, détails, exports et accès refusés ;
- immutabilité ;
- échec contrôlé si l’audit obligatoire échoue ;
- export borné, protégé, expirant et audité ;
- ADMIN sans accès métier implicite.

### 29.3 Observabilité

- propagation corrélation et trace synchrone/asynchrone ;
- absence de labels non bornés ;
- métriques succès, rejet, timeout, retry et DLQ ;
- alertes Payment bloqué, outcome inconnu, extourne, TFJ et notification ;
- dashboard et runbook pour chaque alerte critique.

## 30. Critères de sortie

- [x] Token + Subscription Key formalisés.
- [x] Secrets, rotation et révocation définis.
- [x] Chiffrement en transit et au repos défini.
- [x] Masquage RIB/IBAN défini.
- [x] Credentials interdits dans les logs.
- [x] Matrice RBAC fermée.
- [x] Consultations, exports et rejeux audités.
- [x] Audit immuable distinct des logs.
- [x] Corrélation définie.
- [x] Métriques métier et techniques cataloguées.
- [x] Cardinalité bornée.
- [x] Alertes Payment, TFJ, DLQ et notifications définies.
- [x] Tests de sécurité et observabilité définis.

## 31. Verdict 0P.10

```text
PAYMENT SECURITY BASELINE: ESTABLISHED
TRESOR PAY MVP AUTHENTICATION: TOKEN + SUBSCRIPTION KEY
SECRETS: EXTERNALIZED AND ROTATABLE
SENSITIVE DATA: ENCRYPTED AND MASKED
PAYMENT RBAC: CLOSED
BUSINESS AUDIT: IMMUTABLE AND CORRELATED
BUSINESS AND TECHNICAL METRICS: DEFINED
CRITICAL ALERTS: DEFINED
UNBOUNDED METRIC LABELS: FORBIDDEN
CODE GENERATION: FORBIDDEN
NEXT STEP: 0P.11 — RESILIENCE AND OPERATIONAL RECOVERY
```
