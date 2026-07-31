# SIXPAY CONNECT — Décision de vérification de l’autorisation TRESOR PAY

## 1. Identification

| Propriété | Valeur |
| --- | --- |
| Gate | `IA-0.5P_PAYMENT_CONTRACT_PACK` |
| Étape | `1.2 — Vérification de l’autorisation TRESOR PAY` |
| Branche | `feat/payment-contract-pack` |
| Commit analysé | `a0bf61a3dde0c66971ffe11006ae8fd56bc60b80` |
| Statut | `DECIDED_PENDING_EXTERNAL_APPROVAL` |
| Décision | `LOCAL_SIGNED_TOKEN_VALIDATION` |
| API distante TRESOR PAY | **Non retenue pour le MVP** |
| Génération de code | **Interdite** |

## 2. Question à arbitrer

Avant de traiter un ordre de paiement, SIXPAY doit obtenir une preuve que :

- l’abonnement TRESOR PAY existe ;
- l’abonnement est actif ;
- il appartient au client déclaré ;
- il autorise la banque et le compte débiteur déclarés ;
- le token est authentique, valide et destiné à SIXPAY.

Deux solutions ont été étudiées :

1. valider localement un token signé émis par TRESOR PAY ;
2. appeler synchroniquement une API TRESOR PAY à chaque paiement.

Cette vérification ne doit ni créer une base locale d’abonnements, ni
transférer à SIXPAY l’autorité sur leur cycle de vie.

## 3. Contraintes normatives

- TRESOR PAY reste le système maître des abonnements.
- SIXPAY ne crée, ne valide, ne suspend et ne réactive aucun abonnement.
- `subscriptionReference` est une référence de traçabilité, pas une clé vers
  un Aggregate Root local.
- le paiement doit pouvoir être reçu et persisté sans dépendance réseau
  synchrone supplémentaire vers TRESOR PAY ;
- le Bearer Token et la Subscription Key sont obligatoires pour le MVP ;
- aucun token, secret, PIN ou clé ne doit être persisté ou journalisé ;
- la preuve doit être liée à la requête pour empêcher sa substitution ou son
  rejeu sur un autre client, compte, établissement ou paiement.

Sources : `PAY-SRC-005`, `PAY-SRC-007`, `PAY-SRC-011`, `PAY-SRC-012`,
`PAY-SRC-046`, `IA0R-D05`.

## 4. Comparaison des solutions

| Critère | Token signé validé localement | Appel synchrone TRESOR PAY |
| --- | --- | --- |
| Autorité TRESOR PAY | Préservée : TRESOR PAY signe l’attestation | Préservée : TRESOR PAY répond en temps réel |
| Gestion locale d’abonnement | Aucune | Aucune |
| Disponibilité du paiement | Indépendante de l’API métier TRESOR PAY | Couplée à sa disponibilité et sa latence |
| Latence | Vérification cryptographique locale | Aller-retour réseau à chaque paiement |
| Résilience | Clés publiques mises en cache et rotation contrôlée | Retry, timeout et circuit breaker nécessaires |
| Fraîcheur de révocation | Limitée par le TTL du token | Potentiellement immédiate |
| Risque de rejeu | Maîtrisé par `jti`, TTL et liaison à la requête | Maîtrisé par contrat et idempotence distante |
| Nouveau contrat métier | Non | Oui |
| Exposition de données | Claims minimaux | Requête/réponse supplémentaires |
| Mode dégradé | Possible avec clé publique déjà approuvée | Refus ou attente si TRESOR PAY indisponible |

## 5. Décision

Le MVP retient :

```text
LOCAL_SIGNED_TOKEN_VALIDATION
```

Le Bearer Token du contrat
`tresorpay-payment-request-api-v1.yaml` constitue une attestation
d’autorisation courte, signée asymétriquement par TRESOR PAY.

La Subscription Key authentifie en complément l’intégration appelante. Les
deux mécanismes sont obligatoires et leur identité doit être cohérente.

Le fichier suivant n’est donc pas produit :

`documentation/contracts/tresorpay/tresorpay-subscription-verification-api-v1.yaml`

Son absence est intentionnelle et ne constitue pas un livrable manquant.

## 6. Sémantique de l’attestation

TRESOR PAY ne doit émettre le token qu’après avoir constaté que :

- l’abonnement existe ;
- son statut est `ACTIVE` ;
- le client, l’institution et le compte correspondent à l’abonnement ;
- le paiement présenté peut utiliser cette autorisation.

SIXPAY ne reconstruit pas cette décision. Il vérifie cryptographiquement que
l’attestation est authentique, fraîche, non rejouée et concordante avec la
requête.

## 7. Profil JWT/JWS obligatoire

### 7.1 Format et signature

- format : JWT compact signé JWS ;
- algorithmes asymétriques autorisés : `RS256`, `PS256` ou `ES256` ;
- algorithme `none`, secrets partagés et algorithmes non approuvés : interdits ;
- `kid` obligatoire dans le header ;
- clés publiques publiées par un endpoint JWKS HTTPS TRESOR PAY approuvé ;
- URL JWKS configurée côté SIXPAY, jamais fournie par la requête ;
- rotation de clé avec période de chevauchement ;
- validation stricte de `typ`, `alg` et `kid`.

### 7.2 Claims obligatoires

| Claim | Rôle |
| --- | --- |
| `iss` | Émetteur TRESOR PAY autorisé |
| `aud` | Audience exacte SIXPAY Payment |
| `sub` | Référence d’abonnement TRESOR PAY |
| `jti` | Identifiant unique de l’attestation |
| `iat` | Instant d’émission |
| `nbf` | Début de validité |
| `exp` | Fin de validité |
| `client_id` | Identité de l’application TRESOR PAY |
| `subscription_status` | Doit valoir `ACTIVE` |
| `customer_niu` | NIU lié à l’abonnement |
| `financial_institution_code` | Banque autorisée |
| `debtor_account_hash` | Empreinte du compte autorisé |
| `payment_reference` | Référence TRESOR PAY du paiement |
| `scope` | Doit contenir `payment:initiate` |

Le token ne contient jamais :

- le RIB/IBAN en clair ;
- un PIN ;
- une API Key ou Subscription Key ;
- un document KYC ;
- des données bancaires sans utilité pour la décision.

### 7.3 Durée de validité

- TTL maximal recommandé : cinq minutes ;
- tolérance d’horloge configurable et bornée ;
- token expiré ou utilisé avant `nbf` : rejet ;
- un TTL supérieur à la valeur approuvée : rejet.

La valeur définitive du TTL et la tolérance d’horloge sont soumises à
approbation Security/TRESOR PAY sans modifier le modèle métier.

## 8. Concordance avec la requête Payment

SIXPAY compare obligatoirement :

| Claim | Donnée de la requête |
| --- | --- |
| `sub` | `subscriptionReference` |
| `client_id` | `X-TresorPay-App-Id` et identité de la Subscription Key |
| `customer_niu` | `customer.niu` |
| `financial_institution_code` | `financialInstitutionCode` |
| `debtor_account_hash` | empreinte canonique de `debtorAccount` |
| `payment_reference` | `tresorPayPaymentReference` |
| `scope` | opération Payment demandée |

Toute divergence entraîne un rejet avant les contrôles Amplitude.

## 9. Ordre de validation

1. vérifier TLS et la Subscription Key ;
2. extraire le Bearer Token sans le journaliser ;
3. valider la structure JWT et les limites de taille ;
4. sélectionner une clé approuvée par `kid` ;
5. vérifier la signature et l’algorithme ;
6. vérifier `iss`, `aud`, `iat`, `nbf`, `exp` et le TTL ;
7. vérifier `subscription_status=ACTIVE` et le scope ;
8. vérifier la concordance de tous les claims métier ;
9. contrôler `jti` contre le registre de rejeu ;
10. persister uniquement les résultats minimaux de validation et l’empreinte
    du token, jamais le token brut.

## 10. Rejeu et idempotence

- `jti` est unique pendant au moins la durée de validité du token augmentée de
  la tolérance d’horloge ;
- un rejeu strict du même paiement avec la même clé d’idempotence peut
  restituer l’accusé original ;
- le même `jti` associé à un autre payload, une autre référence ou une autre
  clé d’idempotence est rejeté et audité ;
- le registre de rejeu ne stocke ni token brut ni données bancaires ;
- l’idempotence Payment reste fondée sur `Idempotency-Key` et l’empreinte
  canonique du payload ; `jti` est une protection complémentaire.

## 11. Gestion des clés et indisponibilités

- les JWKS sont récupérés hors du chemin critique lorsque possible ;
- les clés approuvées sont mises en cache selon un TTL configurable ;
- un `kid` inconnu déclenche au maximum un rafraîchissement JWKS contrôlé ;
- aucune clé n’est téléchargée depuis une URL contenue dans le token ;
- si la clé nécessaire n’est pas disponible ou ne peut être approuvée, le
  paiement est rejeté techniquement sans appel Amplitude ;
- la dernière clé valide peut rester utilisable uniquement pendant la fenêtre
  de chevauchement approuvée ;
- toute anomalie de rotation produit métrique, audit et alerte.

## 12. Erreurs contractuelles

| Situation | HTTP | Code RFC 7807 |
| --- | ---: | --- |
| Token absent ou mal formé | 401 | `AUTHENTICATION_FAILED` |
| Signature ou émetteur invalide | 401 | `AUTHORIZATION_TOKEN_INVALID` |
| Token expiré ou hors fenêtre | 401 | `AUTHORIZATION_TOKEN_EXPIRED` |
| Abonnement non actif | 403 | `AUTHORIZATION_DENIED` |
| Scope insuffisant | 403 | `AUTHORIZATION_DENIED` |
| Claims différents de la requête | 403 | `AUTHORIZATION_CLAIMS_MISMATCH` |
| `jti` rejoué dans un autre contexte | 409 | `AUTHORIZATION_REPLAY_DETECTED` |
| Clé publique indisponible | 503 | `AUTHORIZATION_KEY_UNAVAILABLE` |

Les erreurs ne renvoient ni token, ni claim sensible, ni compte complet.

## 13. Raisons du rejet de l’appel distant

L’appel synchrone n’est pas retenu car il :

- ajoute TRESOR PAY au chemin critique de chaque paiement ;
- crée une panne en cascade possible ;
- impose timeout, retry et circuit breaker supplémentaires ;
- augmente la latence avant les contrôles Amplitude ;
- duplique des informations pouvant être attestées cryptographiquement ;
- exige un contrat supplémentaire sans bénéfice décisif pour le MVP.

## 14. Conditions de réexamen

L’API distante devra être réévaluée si :

- TRESOR PAY ne peut pas émettre le profil JWT défini ;
- une révocation d’abonnement doit être visible quasi instantanément ;
- le TTL maximal accepté ne satisfait pas les exigences de risque ;
- la banque exige une confirmation en ligne pour chaque paiement ;
- la rotation ou la publication JWKS ne peut pas être sécurisée ;
- des données autoritatives indispensables ne peuvent pas être portées dans
  une attestation minimisée.

Une telle évolution exigera une nouvelle décision, le contrat
`tresorpay-subscription-verification-api-v1.yaml` et la révision du Contract
Pack.

## 15. Tests d’acceptation

- signature valide et claims concordants ;
- token absent, mal formé ou tronqué ;
- algorithme `none` ou non autorisé ;
- signature invalide ;
- `kid` inconnu et rafraîchissement contrôlé ;
- `iss` ou `aud` invalide ;
- token expiré, prématuré ou TTL excessif ;
- abonnement autre que `ACTIVE` ;
- client, banque, compte ou paiement non concordant ;
- scope absent ;
- rejeu identique idempotent ;
- `jti` réutilisé avec un autre contexte ;
- JWKS temporairement indisponible avec clé en cache ;
- clé absente après rotation ;
- absence du token brut dans logs, traces, événements et audit.

## 16. Verdict

```text
AUTHORIZATION VERIFICATION: LOCAL SIGNED TOKEN
TRESOR PAY SUBSCRIPTION AUTHORITY: PRESERVED
LOCAL SUBSCRIPTION MANAGEMENT: FORBIDDEN
SYNCHRONOUS TRESOR PAY VERIFICATION API: NOT REQUIRED FOR MVP
REMOTE API CONTRACT: NOT PRODUCED
CODE GENERATION: FORBIDDEN
```
