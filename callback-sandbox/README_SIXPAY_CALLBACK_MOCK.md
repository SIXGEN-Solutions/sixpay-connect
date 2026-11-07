# SIXPAY CONNECT — Mock partenaire callback (Windows/PowerShell)

**Base de lecture :** `SIXGEN-Solutions/sixpay-connect`, `feat/repository-baseline-consolidation-cucumber` @ `cd0e01d75a3aae3ed8e6cf5dbbf125def52955ac` (7 novembre 2026).

Le script `sixpay_callback_mock.py` est **externe au repository**, n'effectue aucun paiement et utilise exclusivement Python standard, `http.server` + SQLite. Ne l'utiliser qu'en environnement local de test avec données fictives. La réception `204` est envoyée **après commit SQLite**. La validation du bearer JWT n'est PAS implémentée ; `--require-auth` vérifie des valeurs statiques de test uniquement. En mode par défaut on teste le profil HMAC / header ; cela n'est pas une certification de conformité de l'authentification contractuelle complète.

## 1. Préparer le récepteur

```powershell
mkdir C:\sixpay-callback-lab -ErrorAction SilentlyContinue
cd C:\sixpay-callback-lab
# Copier les fichiers téléchargés sixpay_callback_mock.py et test_sixpay_callback_mock.py dans ce dossier
py --version
$env:SIXPAY_MOCK_KEY_ID = 'local-test-v1'
$env:SIXPAY_MOCK_HMAC_SECRET = 'TEST-ONLY-replace-with-long-random-local-value'
$env:SIXPAY_MOCK_ADMIN_TOKEN = [guid]::NewGuid().ToString('N')
py .\sixpay_callback_mock.py --host 127.0.0.1 --port 8765 --db .\callback.sqlite3
```

Garder ces valeurs dans la session PowerShell, **ne pas les committer**. Dans une autre fenêtre, recopier les mêmes variables nécessaires aux diagnostics. Pour créer un secret aléatoire en local :

```powershell
$env:SIXPAY_MOCK_HMAC_SECRET = [Convert]::ToBase64String([System.Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
```

Démarrer le script après avoir défini le secret. Le terminal imprime une ligne par réception sans afficher les secrets ni le corps JSON. SQLite crée `events` (une ligne par `eventId`) et `attempts` (une ligne par tentative validée ou rejetée). Les octets de requête sont conservés dans `events.raw_body` pour diagnostic local : chiffrer/protéger ou supprimer la base après les tests.

## 2. Tester le simulateur sans SIXPAY

Depuis le dossier contenant les deux scripts :

```powershell
py .\test_sixpay_callback_mock.py
```

Le test lance lui-même un récepteur isolé sur `127.0.0.1:18765` et une base SQLite temporaire. Il teste signature, `204`, rejouement, conflit `409`, rejet HMAC `401`, `503` puis succès et protection du diagnostic. Il ne requiert pas PostgreSQL.

Diagnostic du récepteur démarré sur `8765` :

```powershell
$headers = @{ 'X-Mock-Admin-Token' = $env:SIXPAY_MOCK_ADMIN_TOKEN }
Invoke-RestMethod -Uri http://127.0.0.1:8765/__mock/diagnostics -Headers $headers
```

Simuler la prochaine livraison :

```powershell
$headers = @{ 'X-Mock-Admin-Token' = $env:SIXPAY_MOCK_ADMIN_TOKEN }
Invoke-RestMethod -Method Put -Uri http://127.0.0.1:8765/__mock/mode -Headers $headers -ContentType application/json -Body '{"mode":"503"}'
# Autres modes : normal, timeout-before-commit, timeout-after-commit, disconnect
Invoke-RestMethod -Method Put -Uri http://127.0.0.1:8765/__mock/mode -Headers $headers -ContentType application/json -Body '{"mode":"normal"}'
```

`timeout-after-commit` attend par défaut 35 secondes **après** l'acceptation SQLite, puis tente de répondre `504`. Pour provoquer réellement une coupure/timeout côté SIXPAY, son timeout HTTP doit être inférieur à cette durée ; le délai côté SIXPAY n'a pas été vérifié. `disconnect` simule une fermeture brutale avant persistance. Pour une interruption réseau totale, arrêter temporairement le processus du tunnel ; on ne désactive aucune règle de sécurité du repository.

## 3. HTTPS avec Cloudflare Quick Tunnel

Le `CallbackEndpoint` SIXPAY exige HTTPS. Installer `cloudflared` depuis la documentation officielle Cloudflare, puis lancer **dans une autre fenêtre** :

```powershell
cloudflared tunnel --url http://127.0.0.1:8765
```

Cloudflare fournit une URL aléatoire `https://...trycloudflare.com`. Construire l'URL **complète** :

```text
https://<hôte-temporaire>.trycloudflare.com/sixpay/callback
```

`<hôte-temporaire>` est un emplacement d'exemple, **pas une URL réelle à utiliser telle quelle**. Le tunnel n'est pas une authentification du récepteur : toute personne connaissant l'URL peut tenter de l'appeler. Ne jamais exposer ce récepteur avec un secret de production, ni utiliser de vraies données clients. Le diagnostic exige une valeur admin distincte, non envoyée par SIXPAY. Arrêter `cloudflared` après le test.

### SSRF et allow-list

Ne pas neutraliser la protection SSRF de SIXPAY. Une URL `trycloudflare.com` est temporaire, et son hostname public doit être explicitement autorisé par la **politique de sécurité locale existante**, après décision et vérification. Si l'allow-list locale ne supporte pas un hostname temporaire, utiliser un tunnel nommé avec hostname stable approuvé, ou exécuter uniquement les tests autonomes du mock jusqu'à configuration autorisée. **Ne pas ajouter de wildcard `*.trycloudflare.com` en production.**

## 4. Configuration SIXPAY (environnement local, sans modifier Git)

`PaymentCallbackProperties` porte exactement :

```properties
sixpay.payment.callback.enabled=true
sixpay.payment.callback.signing-key-id=local-test-v1
sixpay.payment.callback.signing-secret=${SIXPAY_CALLBACK_SIGNING_SECRET}
sixpay.payment.callback.batch-size=50
sixpay.payment.callback.max-attempts=8
sixpay.payment.callback.claim-timeout=PT5M
sixpay.payment.callback.initial-retry-delay=PT10S
sixpay.payment.callback.maximum-retry-delay=PT1H
sixpay.payment.callback.worker-id=local-worker-1
sixpay.payment.callback.poll-delay=PT2S
```

**Attention :** ce bloc est une proposition d'override local alignée sur les propriétés et leurs valeurs par défaut observées, à injecter dans l'environnement/configuration locale existante de `backend/bootstrap` ; ne pas le coller dans un fichier versionné. Le secret HMAC de SIXPAY doit correspondre **octet pour octet** à `SIXPAY_MOCK_HMAC_SECRET`. En PowerShell, la variable Spring Boot utilisable est `SIXPAY_PAYMENT_CALLBACK_SIGNING_SECRET`. L'usage d'un placeholder `${SIXPAY_CALLBACK_SIGNING_SECRET}` exige de définir cette variable explicitement, ou d'utiliser directement le nom natif Spring.

Il faut également que la configuration existante de paiement bancaire de test, le provisionnement Partner M2M, le CustomerSubscription fictif, le compte bancaire mock et les contrôles soient fonctionnels. Leur configuration ne peut pas être déduite du script.

## 5. Initiation et OTP : Swagger / Postman

1. Utiliser `POST /v1/payments/initiate` (`PaymentCommandController`). Authentifier le client M2M selon la configuration locale (scope `payment.initiate`) et fournir `X-TresorPay-App-Id`, `Idempotency-Key` et éventuellement `X-Correlation-ID`.
2. Exemple minimal **à remplacer par des fixtures fictives déjà provisionnées** :

```json
{
  "referenceClient": "11111111-1111-4111-8111-111111111111",
  "AppID": "PARTNER_LAB_001",
  "endToEndId": "LAB-T0-20261107-001",
  "montantTotal": 100,
  "devise": "XAF",
  "typeCreance": "AVI",
  "callbackURL": "https://<hôte-temporaire>.trycloudflare.com/sixpay/callback"
}
```

`referenceClient` DOIT correspondre à un **CustomerSubscription local réel de test**, pas à un UUID arbitraire. `AppID` DOIT correspondre au partenaire authentifié et `X-TresorPay-App-Id` reste requis comme identifiant transport. Le hostname doit être remplacé par l'URL réelle approuvée dans l'allow-list.

3. Vérifier `Status: AWAITING_OTP` puis conserver le `paymentReference` retourné. L'OTP est généré et livré par la banque mock ; n'utiliser que sa valeur de test obtenue dans le canal de test autorisé.
4. Utiliser `POST /v1/payments/{paymentReference}/confirmation-challenge/verify` (`PaymentConfirmationController`) avec un **nouvel** `Idempotency-Key` et le corps conforme au schéma `VerifyPaymentConfirmationRequest` affiché par Swagger. Ne pas inventer de nom de propriété OTP : utiliser celui du runtime Swagger.
5. Attendre le traitement T0 concluant `COMPLETED` et l'état Payment `POSTED_PENDING_TFJ`. La réponse OTP seule n'est pas la preuve du posting.
6. Vérifier la console mock, le diagnostic et PostgreSQL. En cas de succès, un `CUT_CREDITED` avec `data.immediateResult=POSTED_PENDING_TFJ`, `data.tfjFinality=false`, `data.bankPostingReference` est attendu.

**Important :** `GET /api/v1/integrations/tresorpay/payments/{paymentReference}` lit le Payment local et **ne déclenche pas** de récupération Core Banking.

## 6. SQL de contrôle PostgreSQL — lecture seule

Remplacer dans psql `:payment_ref` par une référence SIXPAY existante (`\set payment_ref 'PAY-...'`). Les requêtes sont fondées sur `V300`, `V302` et `PaymentOutboxEntity` du SHA analysé.

```sql
-- psql : \set payment_ref 'PAY-XXXXXXXXXXXXXXXXXXXXXXXXXX'
SELECT payment_id, public_payment_reference, partner_identifier,
       status, business_version, updated_at
FROM payments WHERE public_payment_reference = :'payment_ref';

SELECT a.event_id, a.event_type, a.payment_status, a.business_version,
       a.event_sequence, a.occurred_at
FROM payment_audit a JOIN payments p ON p.payment_id=a.payment_id
WHERE p.public_payment_reference = :'payment_ref'
ORDER BY a.business_version,a.event_sequence;

SELECT o.event_id, o.event_type, o.status AS generic_status,
       o.callback_status, o.callback_attempt_count,
       o.callback_next_attempt_at, o.callback_last_attempt_at,
       o.callback_published_at, o.callback_failure_reason,
       o.correlation_id, o.occurred_at
FROM payment_outbox_events o
JOIN payments p ON p.payment_id=o.aggregate_id
WHERE p.public_payment_reference = :'payment_ref'
ORDER BY o.occurred_at,o.event_id;

SELECT s.public_payment_reference, s.bank_event_number,
       s.accounting_date, s.operation_date
FROM payment_financial_event_snapshots s
WHERE s.public_payment_reference = :'payment_ref';

SELECT e.entry_sequence, e.direction, e.bkmvti_dco
FROM payment_financial_entry_snapshots e
JOIN payment_financial_event_snapshots s ON s.snapshot_id = e.event_snapshot_id
WHERE s.public_payment_reference = :'payment_ref'
ORDER BY e.entry_sequence;
```

`callback_status=PUBLISHED` sur un événement non éligible ne prouve PAS un appel HTTP distant. Le rapprochement fort repose sur l'identité de l'événement **éligible**, la preuve d'une tentative `204`, et la ligne SQLite `events` portant le même `eventId`.

## 7. Scénarios à exécuter (sans nouveau POST bancaire)

| ID | Préparation | Preuve attendue |
|---|---|---|
| T01 | `COMPLETED` à la banque test | `CUT_CREDITED` validé `204`, une ligne SQLite `events` |
| T02 | Redélivrer un même callback logique | Même `eventId`, `deliveryId` différent, `204` sans nouvelle ligne `events` |
| T03 | Banque retourne `REJECTED` | Payment `REJECTED`, aucun `CUT_CREDITED` |
| T04 | Banque retourne `UNKNOWN` | Payment `POSTING_OUTCOME_UNKNOWN`, aucun `CUT_CREDITED` |
| T05 | Mock mode `503`, puis `normal` | `FAILED` puis `PUBLISHED`, numéro de tentative croissant |
| T06 | Mode `timeout-after-commit`, puis `normal` | Même `eventId`, tentative nouvelle ; SQLite déduplique |
| T07 | TFJ avant retry T0 | Payload `CUT_CREDITED` historique conservé |
| T08 | TFJ effectivement intégré | `TREASURY_INTEGRATED` distinct |
| T09 | HMAC faux | `401`, aucune ligne `events` |
| T10 | Deux workers configurés distincts | Lock `SKIP LOCKED`, un claim à la fois ; aucune double exécution financière |
| T11 | 8 échecs durables | `callback_status=DEAD`, sans changer Payment |

Ne pas mettre à jour manuellement les lignes d'outbox en base : les transitions de relay et fencing sont métier. Ne pas ré-initier un Payment simplement pour rejouer son callback. T02/T09 peuvent être testés de manière autonome par `test_sixpay_callback_mock.py` ; la redélivrance émise par SIXPAY suppose un scénario contrôlé du relay, pas une modification directe de PostgreSQL.

## 8. Écarts à suivre

- Le contrat callback commun exige `Authorization: Bearer` et `X-Subscription-Key`, absents des headers effectivement envoyés par `PaymentStatusCallbackHttpAdapter` au SHA étudié. L'authentification contractuelle complète n'est donc pas couverte par le test HMAC seul.
- Le contrat de commande `payment-command-api-v1.yaml` comporte aussi une description de callback embarqué (JWS / `200`), différente du webhook commun (HMAC / `204`). Conflit documentaire à arbitrer **sans changer le code ni les contrats automatiquement**.
- Le contrôleur `TresorPayPaymentRecoveryController` utilise `@PreAuthorize("isAuthenticated()")`, sans contrôle d'appartenance Partner visible dans le contrôleur ni dans le service consulté. Il faut auditer la SecurityFilterChain et les accès objet avant de considérer ce GET sûr pour plusieurs partenaires.
- La réponse Recovery existante ne présente pas le motif bancaire, référence de posting ni `dco`. Aucune évolution contractuelle n'est approuvée ici.
- L'intégration de récupération bancaire doit être testée en véritable environnement de test Core Banking avant de certifier les états `NOT_FOUND`, `UNKNOWN`, `REJECTED`, `COMPLETED`.

## 9. Validation effectuée pour ce kit

- `python -m py_compile sixpay_callback_mock.py` : réussi.
- `python test_sixpay_callback_mock.py` : 4 tests réussis ; exécution locale standard-lib.
- SIXPAY / PostgreSQL / tunnel / Core Banking / end-to-end : **NON EXÉCUTÉ**.
- GitHub : lecture seule ; aucun fichier du dépôt modifié, aucune PR, commit, push, branche ou gate lourd.
