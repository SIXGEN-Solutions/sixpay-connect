# T0.7 — Scénarios intégrés à implémenter et à démontrer

Révision de référence : `4a882934708b3e8fc7e0ecb6d7ada121a5f02243`.

**État : spécification d'acceptation / données de test, PAS test E2E exécuté ni terminé.**

## Exigences du véritable test

- Démarrer le vrai `SixpayApplication` en profil de test et PostgreSQL 15 via Testcontainers/Flyway.
- Appeler les vrais contrôleurs HTTP `/v1/payments/initiate` et
  `/v1/payments/{paymentReference}/confirmation-challenge/verify` avec sécurité test explicite.
- Utiliser les vrais services d'orchestration, repositories, JPA, transaction, audit et Outbox.
- Simuler exclusivement les frontières HTTP Core Banking à l'aide d'un serveur stub HTTP
  contractuel ; ne pas substituer les services Payment par des mocks.
- Contrôler les requêtes émises vers Core Banking, la clé d'idempotence et le nombre de POST.
- Lire les états durables avec `JdbcTemplate` après transactions terminées ; ne pas
  se contenter des codes HTTP ou d'interactions Mockito.

## Matrice de scénarios obligatoires

| ID | Stimulation | Core Banking | Assertions durables |
|---|---|---|---|
| T07-01 | Initiation HTTP valide | Customer PASS, challenge ACTIVE | RECEIVED puis PENDING_CONFIRMATION, une identité et un challenge |
| T07-02 | OTP correct via HTTP | Vérification VERIFIED, posting COMPLETED | POSTED_PENDING_TFJ, référence bancaire, snapshot finalisé, 2 écritures, audit/outbox |
| T07-03 | OTP incorrect | Vérification refusée | Aucun posting, pas de snapshot finalisé prématuré |
| T07-04 | Refus bancaire T0 | Posting REJECTED + échecs de contrôle | REJECTED, échec enregistré, aucun second POST |
| T07-05 | Timeout après envoi T0 | POST timeout, GET par référence COMPLETED | Récupération et date comptable initiale ; un seul POST |
| T07-06 | POSTING_PENDING après interruption | GET retourne NOT_FOUND/UNKNOWN | Aucune émission de POST supplémentaire ; statut non conclu |
| T07-07 | Réexécution avec même clé | Résultat initial conservé | Une seule opération banque, une seule identité Payment |
| T07-08 | Vérification PostgreSQL | Aucun appel bancaire | Atomicité Payment + audit + Outbox, rollback sans événement orphelin |

## Conditions indispensables avant classement READY_FOR_LOCAL_VALIDATION

1. Ajouter une classe JUnit `*IT` qui exécute réellement le parcours de T07-01 à T07-08.
2. Fournir un stub HTTP compatible avec les réponses bancaires contractuelles complètes.
3. Contrôler les profils d'activation bancaire, sécurité et partenaires en test.
4. Rendre toutes les assertions PostgreSQL indépendantes de l'ordre d'exécution.
5. Exécuter localement `mvn -pl tests -am -Pfull-tests,assembled-tests verify`.

## Tests du dépôt à réutiliser, sans les présenter comme E2E complet

- `backend/tests/src/test/java/com/sixpay/tests/assembled/FullStackContractBackedConformanceIT.java` : HTTP de lecture + PostgreSQL + assemblage.
- `backend/payment/src/test/java/com/sixpay/payment/infrastructure/outbox/PaymentEndToEndIntegrationIT.java` : écritures Outbox persistées mais événements insérés directement dans le test.
- `backend/payment/src/test/java/com/sixpay/payment/e2e/TresorPayPaymentEndToEndReliabilityTest.java` : assertions sur textes sources, pas exécution HTTP réelle.
- `backend/payment/src/test/java/com/sixpay/payment/PaymentFinancialSnapshotConcurrencyIT.java` : concurrence snapshot PostgreSQL.

Les jeux de données de cette livraison sont fictifs et réservés aux tests ; ne jamais les envoyer à une banque réelle.
