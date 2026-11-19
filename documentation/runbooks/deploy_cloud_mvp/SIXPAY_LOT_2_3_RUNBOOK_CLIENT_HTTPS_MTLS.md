# SIXPAY CONNECT — Runbook de mise en œuvre HTTPS/mTLS côté client Java

**Lot :** 2.3 — Client Java SIXPAY → Regional Core Banking API  
**Date des preuves locales :** 18 novembre 2026 (EST, UTC−05:00)  
**Repository :** `SIXGEN-Solutions/sixpay-connect`  
**Branche observée :** `feat/repository-baseline-consolidation-cucumber`  
**HEAD GitHub vérifié lors de la revue :** `d743f5cf4276b85172f99878e2f38eafe03aaa9b`  
**Mode :** rapport documentaire rétrospectif ; aucune modification Git  
**Statut :** validation du handshake mTLS **en local** ; tests automatisés du dépôt non exécutés dans cette rédaction.

> **Périmètre.** Ce rapport documente uniquement les travaux nécessaires pour que le **client Java SIXPAY** établisse une connexion HTTPS/mTLS vers Regional, ainsi que les prérequis PKI locaux qui lui fournissent une identité et une CA de confiance. Les éléments de création de la CA et du certificat serveur sont rappelés pour comprendre et reproduire la chaîne de confiance : ce ne sont pas des travaux de déploiement. La configuration du VPS Contabo, du DNS privé de déploiement et des conteneurs distants est **hors périmètre**.

## 1. Objectif et résultats

Passer des appels bancaires initiés par SIXPAY sur HTTP local à un client Java utilisant **`https://localhost:9443`**, avec : (1) identification du serveur Regional par une CA approuvée ; (2) présentation du certificat client SIXPAY ; (3) preuve de possession de la clé privée ; (4) négociation TLS 1.3 ; (5) refus de la combinaison HTTP non sécurisé + mTLS.

**Résultat observé :** les journaux du client SIXPAY et du serveur Regional, à **22:56:49 EST**, montrent le même handshake TLS 1.3. Le client émet `Certificate` et `CertificateVerify` ; Regional consomme ces messages et le `Finished` client. Une réponse métier de vérification OTP `CHALLENGE_EXPIRED` a également été fournie ; elle ne prouve pas à elle seule la corrélation HTTP exacte avec cette session TLS.

### État des checkpoints

| Contrôle | État de la campagne | Preuve / réserve |
|---|---|---|
| Identité client en PKCS12 | **VALIDÉ LOCAL** | `sixpay-client.p12`, alias `sixpay-client`, `PrivateKeyEntry`. |
| CA de confiance côté SIXPAY | **VALIDÉ LOCAL** | `sixpay-regional-truststore.p12`, alias `regional-test-ca`, `trustedCertEntry`. |
| SSL Bundle chargé au démarrage SIXPAY | **VALIDÉ PAR DÉMARRAGE** | Tomcat HTTP entrant sur `8080`, application démarrée. |
| TLS 1.3 client ↔ serveur | **VALIDÉ LOCAL** | Protocole négocié `TLSv1.3`, suite `TLS_AES_256_GCM_SHA384`. |
| Certificat client présenté et reçu | **VALIDÉ LOCAL** | `CN=SIXPAY Integration Test Client`, journaux des deux processus. |
| Vérification HTTP bancaire précise | **PARTIEL** | Résultat métier disponible, mais pas de journal HTTP corrélé à 22:56:49. |
| Gates Maven / CI de SIXPAY | **NON EXÉCUTÉS ICI** | Aucune preuve de `mvn verify` pour cette campagne. |
| Déploiement VPS Contabo | **HORS PÉRIMÈTRE** | Chantier ultérieur séparé. |

## 2. Architecture et responsabilité des artefacts

```text
Postman / Swagger
       |
       | HTTP local : http://localhost:8080
       v
SIXPAY CONNECT — contrôleur / service Payment
       |
       | RestClient Java + SSLContext (Spring SSL Bundle)
       | HTTPS / TLS 1.3 : https://localhost:9443
       |     1. ClientHello  -------------------------->
       |     2. ServerHello + certificat Regional <----
       |     3. CertificateRequest <--------------------
       |     4. Certificat SIXPAY + CertificateVerify -->
       |     5. Finished / requête HTTP chiffrée <----->
       v
Regional Core Banking API — serveur HTTPS/mTLS
```

- **`regional-test-ca.crt`** : certificat public de l'autorité de test ; SIXPAY l'utilise pour vérifier la chaîne du certificat serveur. Regional approuve également cette CA pour les clients dans la campagne locale.
- **`regional-test-ca.key`** : clé privée de signature de l'autorité ; **ne doit jamais être chargée dans SIXPAY**.
- **`regional-server.crt` / `.key` / `.p12`** : identité du serveur, avec `serverAuth` et SAN `localhost`, `127.0.0.1` ; chargée **par Regional**, pas par SIXPAY.
- **`sixpay-client.crt` / `.key`** : identité du client, avec EKU `clientAuth` ; la clé privée est utilisée par SIXPAY pour signer `CertificateVerify`.
- **`sixpay-client.p12`** : keystore PKCS12 de SIXPAY contenant clé privée, certificat et chaîne CA ; **secret**.
- **`sixpay-regional-truststore.p12`** : truststore PKCS12 du client, contenant la CA publique Regional ; **ne contient pas la clé privée SIXPAY**.
- **`regional-client-truststore.p12`** : truststore du **serveur Regional** ; à ne pas confondre avec le truststore de SIXPAY.

**Séparation de responsabilités :** TLS chiffre le transport et authentifie les machines ; les contrôles métier, les scopes, l'idempotence OTP et l'autorisation de paiement restent indépendants. Le port HTTP **entrant** `8080` de SIXPAY n'empêche pas son client **sortant** d'utiliser HTTPS `9443`.

## 3. Préparer l'environnement local

Campagne exécutée sous **Windows, PowerShell, IntelliJ IDEA et Docker Desktop** ; l'OpenSSL de la campagne est celui d'un conteneur Alpine, **pas un exécutable OpenSSL installé sur Windows**. Préparer le dossier hors du dépôt :

```powershell
$PkiV2 = Join-Path $env:TEMP 'regional-mtls-integration\pki-v2'
New-Item -ItemType Directory -Force -Path $PkiV2 | Out-Null
$Keytool = 'C:\Program Files\Java\jdk-21\bin\keytool.exe'
java -version
docker version
& $Keytool -help
```

**Checkpoint P0 :** Java 21, Docker Desktop et `keytool` disponibles ; aucun fichier privé ni mot de passe ajouté au repository. Pour les blocs INI ci-dessous, enregistrer les fichiers `.cnf` en texte dans `$PkiV2` avant d'exécuter les commandes.

## 4. Construire la chaîne de confiance de test

### 4.1 Créer la CA locale (précondition du client)

Fichier **`ca.cnf`** :

```ini
[req]
default_bits = 3072
prompt = no
distinguished_name = dn
x509_extensions = v3_ca

[dn]
CN = Regional mTLS Integration Test CA

[v3_ca]
basicConstraints = critical, CA:TRUE, pathlen:0
keyUsage = critical, keyCertSign, cRLSign
subjectKeyIdentifier = hash
```

Création et vérification via Docker (OpenSSL dans Alpine) :

```powershell
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -x509 -newkey rsa:3072 -sha256 -noenc -days 30 -keyout /pki/regional-test-ca.key -out /pki/regional-test-ca.crt -config /pki/ca.cnf'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -in /pki/regional-test-ca.crt -noout -subject -issuer -dates -ext basicConstraints,keyUsage && openssl verify -x509_strict -CAfile /pki/regional-test-ca.crt /pki/regional-test-ca.crt'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**Pourquoi :** la CA sert de racine de confiance pour signer les identités serveur et client. **Checkpoint P1 observé :** `CA:TRUE,pathlen:0`, `keyCertSign`, `cRLSign`, `openssl verify ...: OK` ; empreinte CA `74026D8075C6594CB26AE5407FCA02BE76E251147AAFF28B4FB82D2F5B073F29`.

### 4.2 Certificat serveur Regional (rappel du prérequis)

Fichier **`server.cnf`** :

```ini
[req]
default_bits = 3072
prompt = no
distinguished_name = dn

[dn]
CN = localhost

[v3_server]
basicConstraints = critical, CA:FALSE
keyUsage = critical, digitalSignature, keyEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer

[alt_names]
DNS.1 = localhost
IP.1 = 127.0.0.1
```

```powershell
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -new -newkey rsa:3072 -sha256 -noenc -keyout /pki/regional-server.key -out /pki/regional-server.csr -config /pki/server.cnf'
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -req -in /pki/regional-server.csr -CA /pki/regional-test-ca.crt -CAkey /pki/regional-test-ca.key -CAcreateserial -out /pki/regional-server.crt -days 14 -sha256 -extfile /pki/server.cnf -extensions v3_server'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl verify -x509_strict -purpose sslserver -verify_hostname localhost -CAfile /pki/regional-test-ca.crt /pki/regional-server.crt && openssl verify -x509_strict -purpose sslserver -verify_ip 127.0.0.1 -CAfile /pki/regional-test-ca.crt /pki/regional-server.crt'
```

**Pourquoi :** permet au client SIXPAY de vérifier **qui** lui présente le serveur HTTPS et la concordance du nom `localhost`. **Checkpoint P2 observé :** deux vérifications `regional-server.crt: OK`, EKU `serverAuth`, SAN `localhost` / `127.0.0.1`. Le PKCS12 serveur `regional-server.p12` existe côté Regional, sans être consommé par SIXPAY.

### 4.3 Émettre le certificat client SIXPAY

Fichier **`sixpay-client.cnf`** :

```ini
[req]
default_bits = 3072
prompt = no
distinguished_name = dn

[dn]
CN = SIXPAY Integration Test Client

[v3_client]
basicConstraints = critical, CA:FALSE
keyUsage = critical, digitalSignature
extendedKeyUsage = clientAuth
subjectAltName = @alt_names
subjectKeyIdentifier = hash
authorityKeyIdentifier = keyid,issuer

[alt_names]
URI.1 = urn:sixpay:integration-test:client
```

```powershell
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -new -newkey rsa:3072 -sha256 -noenc -keyout /pki/sixpay-client.key -out /pki/sixpay-client.csr -config /pki/sixpay-client.cnf'
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -req -in /pki/sixpay-client.csr -CA /pki/regional-test-ca.crt -CAkey /pki/regional-test-ca.key -CAcreateserial -out /pki/sixpay-client.crt -days 14 -sha256 -extfile /pki/sixpay-client.cnf -extensions v3_client'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl verify -x509_strict -purpose sslclient -CAfile /pki/regional-test-ca.crt /pki/sixpay-client.crt && openssl x509 -in /pki/sixpay-client.crt -noout -fingerprint -sha256'
```

**Pourquoi :** le serveur Regional exige une identité cliente `clientAuth`. **Checkpoint P3 observé :** vérification X.509 réussie, empreinte client autorisé `DFFD2EF9F6B6892C9B19A46137C9FAAA8A95DD1BF28BB14C0ADCA2846BF5EE28`. La politique d'autorisation de Regional est distincte de la confiance dans la CA.

## 5. Générer les deux magasins PKCS12 de SIXPAY

### 5.1 Keystore — identité client et clé privée

```powershell
docker run --rm -it -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl pkcs12 -export -inkey /pki/sixpay-client.key -in /pki/sixpay-client.crt -certfile /pki/regional-test-ca.crt -name sixpay-client -out /pki/sixpay-client.p12'
& $Keytool -list -v -keystore (Join-Path $PkiV2 'sixpay-client.p12') -storetype PKCS12
```

Le mot de passe est demandé **interactivement** ; ne pas l'inscrire dans le script, Git ou un rapport. **Pourquoi :** Java dispose d'un keystore compatible SSL Bundle contenant l'identité et sa clé de signature. **Checkpoint P4 observé :** alias `sixpay-client`, `PrivateKeyEntry`, chaîne signée par la CA ; empreinte identique au `.crt` original.

### 5.2 Truststore — CA approuvée pour authentifier Regional

```powershell
& $Keytool -importcert -trustcacerts -noprompt -alias regional-test-ca `
  -file (Join-Path $PkiV2 'regional-test-ca.crt') `
  -keystore (Join-Path $PkiV2 'sixpay-regional-truststore.p12') -storetype PKCS12
& $Keytool -list -v -keystore (Join-Path $PkiV2 'sixpay-regional-truststore.p12') -storetype PKCS12
```

**Pourquoi :** le client n'accepte que les certificats serveur reliés à une CA approuvée. **Checkpoint P5 observé :** `trustedCertEntry`, alias `regional-test-ca`, empreinte CA `74026D8075C6594CB26AE5407FCA02BE76E251147AAFF28B4FB82D2F5B073F29`. Ce magasin est **différent** du `regional-client-truststore.p12` côté serveur.

## 6. Câblage Java/Spring Boot de SIXPAY

Le code observé à la révision Git indiquée comprend :

- `backend/bootstrap/.../RegionalBankingSslBundleConfiguration.java` : configuration active si `SIXPAY_BANKING_MTLS_ENABLED=true`, lecture des emplacements et mots de passe, ouverture des deux PKCS12, vérification d'une clé privée et de racines de confiance, enregistrement du bundle nommé et options **TLS 1.3**.
- `backend/integration/.../StandardRestClientFactory.java` : `RestClient` fondé sur `java.net.http.HttpClient`, `JdkClientHttpRequestFactory` et `SSLContext` transmis ; rejet d'une URL externe non HTTPS sauf autorisation HTTP explicite.
- `backend/payment/.../AmplitudePaymentConfirmationConfiguration.java` : sélectionne le SSL Bundle pour le client de confirmation lorsque `mtlsEnabled=true`.
- `application-payment-banking-capabilities.yml` et `application-production.yml` : exposent `base-url`, `mtls-enabled`, `ssl-bundle`, `allow-insecure-http` pour les intégrations bancaires concernées.

**Variables d'exécution IntelliJ utilisées / vérifiées** (valeurs secrètes masquées) :

```dotenv
SIXPAY_BANKING_MTLS_ENABLED=true
AMPLITUDE_PAYMENT_SSL_BUNDLE=amplitude-payment-client
AMPLITUDE_PAYMENT_BASE_URL=https://localhost:9443
AMPLITUDE_PAYMENT_KEYSTORE_LOCATION=file:///C:/Users/<USER>/AppData/Local/Temp/regional-mtls-integration/pki-v2/sixpay-client.p12
AMPLITUDE_PAYMENT_KEYSTORE_PASSWORD=<SECRET>
AMPLITUDE_PAYMENT_TRUSTSTORE_LOCATION=file:///C:/Users/<USER>/AppData/Local/Temp/regional-mtls-integration/pki-v2/sixpay-regional-truststore.p12
AMPLITUDE_PAYMENT_TRUSTSTORE_PASSWORD=<SECRET>
AMPLITUDE_CONFIRMATION_ALLOW_INSECURE_HTTP=false
AMPLITUDE_POSTING_ALLOW_INSECURE_HTTP=false
AMPLITUDE_FUNDS_RESERVATION_ALLOW_INSECURE_HTTP=false
AMPLITUDE_COMPENSATION_ALLOW_INSECURE_HTTP=false
```

`<USER>` est à remplacer par le compte Windows courant. Les mots de passe demeurent dans la configuration locale protégée / un mécanisme d'injection de secrets ; **ne pas les versionner**. La configuration du serveur Regional sur le port `9443` est un prérequis extérieur au client, déjà testé dans le chantier serveur.

**Checkpoint P6 :** application SIXPAY démarrée sur le port entrant `8080` (`Tomcat started on port 8080 (http)`) ; il s'agit d'un autre canal que le HTTPS **sortant** vers Regional. Une erreur de configuration précédemment rencontrée — `insecure HTTP cannot be combined with mTLS` — a cessé après mise en cohérence de l'URL HTTPS et des flags `allow-insecure-http=false`.

## 7. Test de bout en bout et preuves cryptographiques

### 7.1 Préparer la demande

Vérifier la disponibilité de SIXPAY et de Regional :

```powershell
Test-NetConnection 127.0.0.1 -Port 8080
Test-NetConnection localhost -Port 9443
```

Ces commandes ne testent que TCP. Dans IntelliJ **SIXPAY → VM options**, activer temporairement :

```text
-Djavax.net.debug=ssl,handshake
```

Puis déclencher avec Postman une opération **de test** réellement couverte par le client bancaire, par exemple la vérification OTP déjà exercée :

```http
POST http://127.0.0.1:8080/api/v1/payments/{paymentReference}/confirmation-challenge/verify
Authorization: Bearer <TOKEN_DE_TEST>
Idempotency-Key: <UUID>
Content-Type: application/json

{"otp":"<OTP_DE_TEST>"}
```

**Ne pas confondre :** `127.0.0.1:8080` est l'entrée Postman → SIXPAY ; `https://localhost:9443` est la sortie SIXPAY → Regional. Une réponse `ECONNREFUSED 127.0.0.1:8081` a été corrigée en utilisant le port entrant `8080`. Le contrôle HMAC OTP local (`SIXPAY_PAYMENT_OTP_HMAC_ENABLED=true` avec clé HMAC externalisée) doit être actif pour que `verify` ne soit pas bloqué **avant** l'appel à la banque. Ne jamais journaliser l'OTP ou le Bearer token.

### 7.2 Preuves observées dans les deux JVM

| Événement | Côté SIXPAY (client) | Côté Regional (serveur) |
|---|---|---|
| Début | `Produced ClientHello` | `Consuming ClientHello` |
| Version | `Negotiated protocol version: TLSv1.3` | `Negotiated protocol version: TLSv1.3` |
| Serveur authentifié | `Consuming server Certificate` (`CN=localhost`) | `Produced server Certificate` |
| Demande de certificat | `Consuming CertificateRequest` | `Produced CertificateRequest` |
| Identité SIXPAY | `Produced client Certificate` | `Consuming client Certificate` |
| Preuve de clé privée | `Produced client CertificateVerify` | `Consuming CertificateVerify` |
| Terminaison | `Produced client Finished` | `Consuming client Finished` |
| Session | `Consuming NewSessionTicket` | `Produced NewSessionTicket` |

Les deux traces portent la même négociation vers **22:56:49 EST, le 18/11/2026**, le même certificat `CN=SIXPAY Integration Test Client` et la suite **`TLS_AES_256_GCM_SHA384`**. **Checkpoint P7 validé :** handshake mTLS complet côté client **et** serveur ; ce n'est plus une simple inférence tirée du démarrage Spring.

### 7.3 Réponse métier et limite de la corrélation

Une réponse de test de confirmation a été observée :

```json
{
  "paymentReference": "PAY-9829F37E66584A3DBCAD5901B1",
  "challengeStatus": "EXPIRED",
  "businessCode": "CHALLENGE_EXPIRED",
  "deliveryChannel": "EMAIL",
  "sentAt": "2026-11-19T03:44:49.368532Z",
  "expiresAt": "2026-11-19T03:49:48.412088Z"
}
```

Cette réponse exprime **l'expiration du challenge**, pas un échec TLS. Les extraits de handshake ne contiennent toutefois ni méthode/chemin HTTP, ni `X-Correlation-ID`, ni code de réponse HTTP : **la correspondance exacte entre cette réponse et le handshake de 22:56:49 n'est pas démontrée**. À conserver comme réserve documentaire, sans réexécuter inutilement le workflow OTP.

## 8. Dépannage rencontré et mesures de sécurité

| Symptôme | Diagnostic | Action locale / enseignement |
|---|---|---|
| `ECONNREFUSED 127.0.0.1:8081` | Mauvais port Postman | Utiliser `http://127.0.0.1:8080` pour l'entrée SIXPAY. |
| `insecure HTTP cannot be combined with mTLS` | Configuration contradictoire | URL bancaire en `https://localhost:9443`, tous les `allow-insecure-http` pertinents à `false`. |
| `Payment OTP verify idempotency HMAC key is not configured` | Bean HMAC conditionnel non activé | Conserver la clé existante et activer `SIXPAY_PAYMENT_OTP_HMAC_ENABLED=true` ; ne pas supprimer le contrôle. |
| Traces `SSLCipher.java` au démarrage | Pas une preuve d'appel externe | Chercher `ClientHello`, certificat client et `Finished` au moment de l'appel. |
| `CHALLENGE_EXPIRED` | Résultat métier OTP | Traiter séparément du succès ou échec TLS. |

**Sécurité :** ne jamais utiliser `-k`/`--insecure`, ne jamais mettre de clé privée CA dans SIXPAY, ne jamais committer les fichiers `.key` ou les `.p12` à clé privée, et ne pas diffuser les mots de passe, tokens ou OTP. Limiter `javax.net.debug` à la campagne de diagnostic ; ses traces sont volumineuses et peuvent exposer des métadonnées de certificats/session. Les certificats temporaires de cette campagne expirent le **2 décembre 2026** (serveur/client) et la CA le **18 décembre 2026**.

## 9. Checklist reproductible et décision de clôture du périmètre local

- [x] Dossier PKI isolé hors Git ; OpenSSL utilisé dans Docker.
- [x] CA de test créée et vérifiée (`CA:TRUE`, `openssl verify: OK`).
- [x] Certificat serveur signé avec SAN `localhost` / `127.0.0.1` (prérequis).
- [x] Certificat client signé avec EKU `clientAuth` et empreinte connue.
- [x] `sixpay-client.p12` créé et vérifié (`PrivateKeyEntry`).
- [x] `sixpay-regional-truststore.p12` créé et vérifié (`trustedCertEntry`).
- [x] SSL Bundle Java et `RestClient` configurés pour HTTPS/TLS 1.3.
- [x] URL bancaire HTTPS et refus HTTP/mTLS contradictoire.
- [x] Handshake TLS 1.3 observé des deux côtés, avec preuve cryptographique du client.
- [ ] Corrélation HTTP/`X-Correlation-ID` de l'opération précise (preuve complémentaire non fournie).
- [ ] Gates Maven/tests de régression exécutés et consignés (non exécutés dans ce rapport).
- [ ] État de travail Git local (`git status --short`) observé (worktree non accessible).

**Conclusion de ce rapport :** l'**implémentation locale du client Java HTTPS/mTLS** a démontré son fonctionnement cryptographique réel. Les réserves d'observabilité HTTP et de tests du repository doivent être consignées avant une déclaration `REPOSITORY_VALIDATED`. Aucun élément du présent document n'est une validation du VPS Contabo.

**Sources et traçabilité :** code GitHub SIXPAY au SHA ci-dessus (`RegionalBankingSslBundleConfiguration`, `StandardRestClientFactory`, `AmplitudePaymentConfirmationConfiguration`, YAML bancaires), rapport de campagne **Regional Core Banking API — Lot 1.1 HTTPS/mTLS** (commandes PKI et contrôles X.509), traces JSSE côté SIXPAY et Regional transmises le 18 novembre 2026 et résultat de vérification OTP fourni dans la conversation. Les commandes de création sont reprises de la campagne locale documentée ; les gates non observés ne sont pas présentés comme exécutés.
