# SIXPAY CONNECT — Payment Confirmation OTP en développement local

Révision de préparation : `37e81201fa476699c22b6363eaca45cb19c2f929`.
Contrat actif : `documentation/contracts/amplitude/amplitude-payment-confirmation-api-v1.yaml`.

## Responsabilités

La Régionale génère, conserve, délivre et vérifie l'OTP et gère les tentatives.
SIXPAY conserve l'état du Payment et l'idempotence des commandes. Pour `verify`,
un HMAC-SHA256 à clé secrète de l'OTP transitoire et de la référence Payment
permet de distinguer les rejeux sans stocker de code ni de SHA-256 réversible
par dictionnaire. Cela **ne valide pas** le code OTP.

`POST /api/v1/payment-confirmation-challenges` utilise exclusivement
`paymentReference`, `customerReference`, `debtorAccountReference`, `amount`
avec `Idempotency-Key`, `X-Correlation-ID`, `X-Financial-Institution-Code`.
Aucun OTP ni empreinte OTP dans ce payload.

## Correction

- `CREATE`, `REPLACE`, `REVOKE` sont opérationnels sans clé HMAC OTP.
- `VERIFY` échoue avant tout appel bancaire et avant toute écriture d'idempotence
  lorsque le jeu de clés HMAC n'est pas disponible.
- Les idempotences existantes, la récupération après issue incertaine et les
  contrats HTTP sont conservés.
- `payment-banking-local` active le connecteur en HTTP local sans import du
  bundle OAuth2/mTLS ; cela ne change pas les environnements sécurisés.

## Configuration IntelliJ / .env local (fichier non versionné)

Profils :
`standalone,integration,customer-banking-enabled,m2m-local,payment-banking-local`

Exemple de variables déjà utilisées :

```dotenv
AMPLITUDE_PAYMENT_BASE_URL=http://localhost:8080
AMPLITUDE_CONFIRMATION_ENABLED=true
AMPLITUDE_CONFIRMATION_OAUTH2_ENABLED=false
AMPLITUDE_CONFIRMATION_MTLS_ENABLED=false
AMPLITUDE_CONFIRMATION_ALLOW_INSECURE_HTTP=true
AMPLITUDE_CONFIRMATION_CONNECT_TIMEOUT=2s
AMPLITUDE_CONFIRMATION_READ_TIMEOUT=10s
AMPLITUDE_FUNDS_RESERVATION_ENABLED=false
AMPLITUDE_POSTING_ENABLED=false
AMPLITUDE_COMPENSATION_ENABLED=false
AMPLITUDE_POSTING_STATUS_ENABLED=false
```

Pour **tester la vérification OTP** (sans cette clé, la demande initiale de
challenge reste possible), créer localement une clé aléatoire de 32 octets et
la transmettre via la variable d'environnement
`SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64` (Base64 standard, pas hexadécimal).
Exemple PowerShell pour générer la valeur **localement** :

```powershell
$key = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($key)
[Convert]::ToBase64String($key)
```

Copier la valeur dans le `.env` **non versionné** et charger ce fichier dans
la configuration d'exécution IntelliJ. Ne pas afficher ni commiter de clé réelle.

La variable facultative `SIXPAY_PAYMENT_OTP_HMAC_PREVIOUS_KEYS_BASE64` contient
les clés précédentes encodées Base64 séparées par des virgules ; conserver les
clés nécessaires à la reconnaissance des rejeux après rotation. Ne pas changer
une clé entre deux redémarrages si des requêtes d'idempotence peuvent persister.

Ce mécanisme de chargement est **strictement local**. Pour les autres profils,
la gestion des secrets (Vault ou équivalent approuvé) demeure une décision
de sécurité à mettre en œuvre distinctement ; ne pas réutiliser cette
configuration locale en production.

## Gates à exécuter par l'utilisateur (non exécutés par le script)

Depuis `backend` :

```powershell
mvn -pl payment -am "-Dtest=PaymentConfirmationOptionalHmacTest,PaymentConfirmationIdempotencyAdapterTest,PaymentOtpIdempotencyFingerprintSetTest,PaymentOtpIdempotencyFingerprintTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl bootstrap -am "-Dtest=PaymentOtpLocalIdempotencyConfigurationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl payment -am test
mvn -pl bootstrap -am test
mvn verify
```

Depuis la racine :

```powershell
git diff --check
git status --short
```

Inspecter également le rapport Spring `--debug`, puis exercer une création,
une vérification OTP et un rejeu `Idempotency-Key` sur Core Banking local.
Ce document ne constitue pas une preuve de réussite des tests.
