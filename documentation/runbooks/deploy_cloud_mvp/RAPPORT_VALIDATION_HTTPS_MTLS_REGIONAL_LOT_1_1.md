# Regional Core Banking API — Rapport complet de validation HTTPS + mTLS

**Lot :** 1.1 — validation serveur HTTPS/mTLS et contrôles de sécurité  
**Date des essais :** 18 novembre 2026 (heure locale EST, UTC−05:00)  
**Statut :** **9 scénarios conformes aux attentes dans l'environnement local**, sous les réserves détaillées ci-dessous  
**Mode :** rapport documentaire, reconstitution des opérations et des preuves communiquées par l'opérateur ; aucune modification du dépôt par ce rapport  
**Révision :** une référence de travail antérieure `feat/payment-orchestration-worflow` / `28e602d77cdebd503a609c7df836f8724a23db67` était associée au chantier ; **le HEAD réellement exécuté n'a pas été vérifié pendant cette campagne**. Ne pas confondre cette référence et une révision Git observée.  
**Contexte d'exécution observé :** Windows, IntelliJ IDEA, Java 21.0.5, Spring Boot 3.5.6, Tomcat 10.1.46, Docker Desktop, Alpine 3.23, OpenSSL 3.5.9 (dans les traces), curl/OpenSSL 8.23.0 (dans les traces), PostgreSQL 16 (Docker) et Oracle mock (Docker).  
**Périmètre de sécurité :** le service Regional, sa chaîne de certificats, le refus des clients non autorisés et les protections de démarrage. L'intégration du client HTTP **Java de SIXPAY**, le réseau privé entre services et tout environnement Linux cible **ne sont pas encore validés**.

> **Portée des sources.** Les constats PASS ci-dessous sont issus des commandes et sorties transmises dans cette conversation. Les procédures Linux constituent des **recettes reproductibles recommandées, non exécutées** durant cette campagne. Les commandes de configuration sont des exemples de test, à adapter au profil, à la version et à l'environnement réels. Le rapport n'affirme pas une validation intégrale du dépôt ni du déploiement de production.

---

## 1. Pourquoi HTTPS et mTLS ?

**HTTPS** protège la communication client→serveur au moyen de TLS : authentification du serveur par son certificat, confidentialité des données échangées et intégrité du canal. Sans vérification correcte du certificat serveur et de son nom, le chiffrement seul ne suffit pas à garantir l'identité du destinataire.

**mTLS (mutual TLS)** ajoute l'authentification cryptographique du **client** : Regional demande un certificat au client, vérifie sa chaîne et exige la preuve que le client possède la clé privée correspondante. Dans l'architecture testée, le serveur applique **ensuite** une politique d'autorisation basée sur l'empreinte SHA-256 du certificat client, en plus de la confiance envers sa CA.

Le mTLS apporte une **authentification de service à service** (SIXPAY↔Regional) mais **ne remplace pas automatiquement** les contrôles d'autorisation métier, d'audit, d'identification des partenaires, d'authentification applicative ou de réseau privé. Ne pas assimiler un HTTP 200 sur `/actuator/health` à la validation de toutes les API bancaires.

### Architecture logique

```text
                          HTTPS / TLS 1.3 avec authentification mutuelle
  SIXPAY (client)                                                      Regional (serveur)
  ┌───────────────────────────┐                         ┌─────────────────────────────────────┐
  │ Clé privée du client      │  ClientHello             │ Certificat serveur + clé privée     │
  │ Certificat client signé   │ ────────────────────────>│ regional-server.p12                 │
  │ par CA Regional           │  Certificat serveur      │                                     │
  │                           │ <────────────────────────│ Demande de certificat client        │
  │ CA de confiance du serveur│  Certificat client +    │ Truststore des CA clientes         │
  │ pour vérifier Regional    │  preuve de possession    │ regional-client-truststore.p12      │
  │                           │ ────────────────────────>│                                     │
  │                           │                          │ Filtre : empreinte SHA-256 autorisée│
  └───────────────────────────┘                         └─────────────────────────────────────┘
        Client autorisé  → HTTP 200 (test health) ; client certifié mais non autorisé → HTTP 403
        Client sans certificat ou CA cliente inconnue → rejet au niveau TLS
```

---

## 2. Glossaire PKI et rôle des artefacts

| Notion / fichier | Définition | Qui l'utilise ? | Secret ? |
|---|---|---|---|
| **CA** (*Certificate Authority*) | Autorité de certification qui signe des certificats et constitue une ancre de confiance. | Serveur et client, suivant la direction de vérification. | Sa **clé privée** est critique. |
| `regional-test-ca.crt` | Certificat public de la CA de test ; permet la validation des chaînes signées par elle. | Client pour valider Regional ; Regional pour valider les clients. | Non. |
| `regional-test-ca.key` | Clé privée servant à signer les certificats de test. | Machine de génération PKI seulement. | **Oui, très sensible**. |
| `.crt` | Certificat X.509, généralement encodé PEM ou DER ; contient la clé **publique**, l'identité, le signataire, les usages et les dates. L'extension seule ne garantit pas l'encodage. | Client ou serveur pour prouver son identité. | Non. |
| `.key` | Fichier contenant généralement la **clé privée** associée au certificat ; le format peut être PEM. | Détenteur de l'identité correspondante. | **Oui**. |
| `.csr` | Demande de signature de certificat (*Certificate Signing Request*) ; ne contient pas la clé privée. | Soumise à la CA avant émission. | Généralement non. |
| `.p12` / `.pfx` | Conteneur **PKCS#12** regroupant clé privée, certificat et éventuellement chaîne CA ; peut être protégé par mot de passe. | Java/Spring, Windows, OpenSSL, selon clients. | **Oui lorsqu'il contient une clé privée**. |
| **Keystore serveur** | Stocke l'identité cryptographique de Regional (certificat serveur + clé privée + chaîne). | Regional, pour se présenter en HTTPS. | **Oui**. |
| **Truststore serveur** | Stocke les certificats **CA approuvés pour vérifier les clients**. Il ne contient normalement pas les clés privées clientes. | Regional, pour accepter/refuser les chaînes clientes au handshake. | Certificats publics ; protection d'intégrité recommandée. |
| Certificat client SIXPAY | Identité X.509 de SIXPAY, signée par la CA de test et dotée d'`extendedKeyUsage=clientAuth`. | SIXPAY se présente à Regional. | Certificat public non secret ; clé privée secrète. |
| Truststore côté client | CA(s) que SIXPAY approuve pour vérifier le certificat HTTPS de Regional. | SIXPAY, pour authentifier Regional. | Certificats publics ; gestion contrôlée. |
| SAN | *Subject Alternative Name* : noms DNS/IP exacts pour lesquels un certificat serveur est valide. | Client HTTPS vérifie le nom contacté. | Non. |
| EKU `serverAuth` / `clientAuth` | Usages autorisés du certificat, à vérifier selon son rôle. | Validation X.509. | Non. |
| Empreinte SHA-256 | Résumé du certificat DER, utilisé ici comme identifiant d'autorisation (*fingerprint allowlist*). | Filtre Regional après mTLS. | Non ; change si le certificat change. |

**Lien CA → certificats :** la CA signe avec sa clé privée ; toute partie qui approuve la CA peut vérifier la signature avec sa clé publique. **Lien `.crt` ↔ `.key` :** la clé privée doit correspondre à la clé publique du certificat ; la signature TLS démontre que le pair possède cette clé. **Lien truststore ↔ CA :** le truststore de Regional ne contient que les CA dont Regional accepte les certificats clients. Cela est **distinct de l'allowlist** des empreintes de clients autorisés.

**Attention :** un client peut posséder un certificat signé par une CA de confiance et rester interdit par la politique métier/technique (HTTP 403). À l'inverse, un certificat non signé par une CA de confiance échoue avant tout traitement HTTP. Un truststore « côté client » et un truststore « côté serveur » sont deux rôles distincts, même si notre campagne a utilisé **une même CA de test** dans les deux directions.

---

## 3. Inventaire, identité et échéances des certificats de test

### 3.1 CA et serveur

| Artefact | Identité / caractéristiques confirmées | Expiration |
|---|---|---|
| CA principale | `CN=Regional mTLS Integration Test CA` ; RSA 3072 ; `CA:TRUE,pathlen:0` ; `keyCertSign,cRLSign` ; contrôle strict `openssl verify -x509_strict ...: OK` | **18 déc. 2026, 19:53:56 GMT** |
| Certificat serveur | `CN=localhost` ; SAN `DNS:localhost,IP:127.0.0.1` ; `serverAuth` ; signé par CA principale ; validation `-verify_hostname localhost` et `-verify_ip 127.0.0.1` : OK | **2 déc. 2026, 20:06:27 GMT** |
| Client SIXPAY | `CN=SIXPAY Integration Test Client` ; `clientAuth` ; signé par CA principale ; vérification stricte OK | **2 déc. 2026, 20:17:10 GMT** |
| Client non autorisé | `CN=Unauthorized Integration Test Client` ; `clientAuth` ; signé par la **même** CA principale ; vérification stricte OK | Émis pour 14 jours (date exacte non affichée) |
| CA inconnue | `CN=Unknown Integration Test CA` ; `CA:TRUE,pathlen:0`, usages CA ; validation stricte OK | Émise pour 14 jours (date exacte non affichée) |
| Client CA inconnue | `CN=Untrusted Integration Test Client` ; `clientAuth` ; signé par **Unknown Integration Test CA** ; vérification stricte OK | Émis pour 7 jours (date exacte non affichée) |

Empreinte du client **SIXPAY autorisé** (sans `:`) :

```text
DFFD2EF9F6B6892C9B19A46137C9FAAA8A95DD1BF28BB14C0ADCA2846BF5EE28
```

Empreinte du client **signé par la même CA mais non autorisé** :

```text
B4C49322C07A58BAEBFB6B4893DF4BA95A99A64D10F364AD11CCE5904249EE97
```

Empreinte SHA-256 de la CA principale dans le keystore **et** le truststore serveur :

```text
74026D8075C6594CB26AE5407FCA02BE76E251147AAFF28B4FB82D2F5B073F29
```

**Ne pas utiliser ces certificats temporaires pour la production.** Après expiration, les scénarios positifs échoueront ; les résultats historiques ci-dessous restent des preuves du test du 18 novembre 2026, pas une assurance de fonctionnement futur.

### 3.2 Chemins locaux de la campagne Windows

Les fichiers ont été créés hors dépôt dans :

```powershell
$PkiV2 = Join-Path $env:TEMP 'regional-mtls-integration\pki-v2'
$UnknownPki = Join-Path $PkiV2 'unknown-ca'
```

```text
pki-v2/
  ca.cnf
  regional-test-ca.crt
  regional-test-ca.key            [PRIVÉ]
  regional-server.key             [PRIVÉ]
  regional-server.csr
  regional-server.crt
  regional-server.p12             [PRIVÉ / mot de passe]
  regional-client-truststore.p12
  server.cnf
  sixpay-client.cnf
  sixpay-client.key                [PRIVÉ]
  sixpay-client.csr
  sixpay-client.crt
  sixpay-client.p12                [PRIVÉ / mot de passe]
  unauthorized-client.cnf
  unauthorized-client.key         [PRIVÉ]
  unauthorized-client.csr
  unauthorized-client.crt
  unknown-ca/
    unknown-ca.cnf
    unknown-ca.key                 [PRIVÉ]
    unknown-ca.crt
    untrusted-client.cnf
    untrusted-client.key           [PRIVÉ]
    untrusted-client.csr
    untrusted-client.crt
```

Le **premier essai de CA** sous `regional-mtls-integration/` (hors `pki-v2`) n'était pas retenu : les arguments OpenSSL passés par PowerShell/Docker avaient perdu une partie de leur structure. La CA correcte est celle de `pki-v2` créée à partir d'un fichier `.cnf` explicite.

---

## 4. Préparation de l'environnement et gates de préflight

### 4.1 Windows / IntelliJ / Docker Desktop — exécuté

**Observations :** JDK 21.0.5, Maven wrapper 3.9.11, Docker Desktop, PostgreSQL `regional-core-banking-postgres` *healthy* sur `localhost:15434`, Oracle `regional-customer-oracle` *healthy* sur `localhost:1521`. Le serveur Regional n'était initialement pas démarré ; le port `9443` était libre.

```powershell
java -version
.\mvnw.cmd -version
docker version
docker compose ps
Get-NetTCPConnection -LocalPort 9443 -ErrorAction SilentlyContinue
```

**Gate :** Java 21 accessible ; Docker disponible ; bases de test saines ; `9443` non occupé avant lancement ; pas de redémarrage ni de suppression des volumes des bases.

Pour appeler l'outil de Java si absent du `PATH` :

```powershell
$Keytool = 'C:\Program Files\Java\jdk-21\bin\keytool.exe'
& $Keytool -help
```

**Gate observé :** aide de `keytool` affichée. Ce correctif ne nécessite aucune modification du `PATH` système.

### 4.2 Linux — recette à rejouer, non exécutée

```bash
java -version
openssl version
curl --version
docker compose ps              # seulement si les bases tournent via Compose
ss -ltnp | grep ':9443' || true
```

**Gate recommandé :** mêmes objectifs de disponibilité. Sur serveur de production, utiliser l'état de service `systemctl`, les métriques et les ACL du pare-feu, et **ne jamais modifier l'hôte distant uniquement pour satisfaire une commande de test**.

---

## 5. Construction de la PKI : procédures complètes

Les commandes de cette section sont destinées à un environnement **de test isolé**. Ne pas régénérer une clé ou un certificat si des services en dépendent ; garder une trace de l'émetteur, du numéro de série, des dates, des usages et des empreintes. Les exemples de configuration sont les paramètres de la campagne, non des profils de production.

### 5.1 CA principale — méthode robuste et portable

Fichier `ca.cnf` :

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

**Windows PowerShell / Docker :** enregistrer la configuration en ASCII dans `$PkiV2\ca.cnf` ; employer des commandes shell Linux **sur une seule ligne** dans `sh -c` pour éviter l'expansion des `*` ou les problèmes de continuations `CRLF`.

```powershell
$PkiV2 = Join-Path $env:TEMP 'regional-mtls-integration\pki-v2'
New-Item -ItemType Directory -Force -Path $PkiV2 | Out-Null
# Enregistrer le bloc INI ci-dessus dans (Join-Path $PkiV2 'ca.cnf')
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -x509 -newkey rsa:3072 -sha256 -noenc -days 30 -keyout /pki/regional-test-ca.key -out /pki/regional-test-ca.crt -config /pki/ca.cnf'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -in /pki/regional-test-ca.crt -noout -subject -issuer -dates -ext basicConstraints,keyUsage && openssl verify -x509_strict -CAfile /pki/regional-test-ca.crt /pki/regional-test-ca.crt'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**Linux natif :** avec `ca.cnf` enregistré dans le dossier courant de PKI et un `umask` restrictif :

```bash
mkdir -p "$HOME/regional-mtls-test"
cd "$HOME/regional-mtls-test"
umask 077
# Enregistrer ca.cnf tel que ci-dessus.
openssl req -x509 -newkey rsa:3072 -sha256 -noenc -days 30 \
  -keyout regional-test-ca.key -out regional-test-ca.crt -config ca.cnf
openssl x509 -in regional-test-ca.crt -noout -subject -issuer -dates -ext basicConstraints,keyUsage
openssl verify -x509_strict -CAfile regional-test-ca.crt regional-test-ca.crt
```

**Gate observé :** CA `CA:TRUE,pathlen:0`, `Certificate Sign, CRL Sign`, `...: OK`, exit `0`. Ne pas partager la clé privée CA.

### 5.2 Certificat serveur Regional

`server.cnf` :

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

**Linux natif** (`$PWD` contient les fichiers de la section précédente ; les commandes Docker Windows utilisent les mêmes chemins internes `/pki/...`) :

```bash
openssl req -new -newkey rsa:3072 -sha256 -noenc \
  -keyout regional-server.key -out regional-server.csr -config server.cnf
openssl x509 -req -in regional-server.csr \
  -CA regional-test-ca.crt -CAkey regional-test-ca.key -CAcreateserial \
  -out regional-server.crt -days 14 -sha256 -extfile server.cnf -extensions v3_server
openssl x509 -in regional-server.crt -noout -subject -issuer -dates -ext subjectAltName
openssl verify -x509_strict -purpose sslserver -verify_hostname localhost \
  -CAfile regional-test-ca.crt regional-server.crt
openssl verify -x509_strict -purpose sslserver -verify_ip 127.0.0.1 \
  -CAfile regional-test-ca.crt regional-server.crt
```

**Windows Docker — commandes utilisées :**

```powershell
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -new -newkey rsa:3072 -sha256 -noenc -keyout /pki/regional-server.key -out /pki/regional-server.csr -config /pki/server.cnf'
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -req -in /pki/regional-server.csr -CA /pki/regional-test-ca.crt -CAkey /pki/regional-test-ca.key -CAcreateserial -out /pki/regional-server.crt -days 14 -sha256 -extfile /pki/server.cnf -extensions v3_server'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl verify -x509_strict -purpose sslserver -verify_hostname localhost -CAfile /pki/regional-test-ca.crt /pki/regional-server.crt && openssl verify -x509_strict -purpose sslserver -verify_ip 127.0.0.1 -CAfile /pki/regional-test-ca.crt /pki/regional-server.crt'
```

**Gate observé :** `CN=localhost`, SAN DNS/IP attendus et deux sorties `regional-server.crt: OK`. Sur Linux distant, **remplacer les SAN par les vrais noms DNS/IP utilisés**, et maintenir la vérification du nom d'hôte ; ne pas exporter tel quel un certificat `localhost` pour une URL distante.

### 5.3 Certificat client SIXPAY et certificat client non autorisé

Configuration `sixpay-client.cnf` :

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

Pour `unauthorized-client.cnf`, conserver la structure des sections et remplacer `CN` par `Unauthorized Integration Test Client` et `URI.1` par `urn:regional:integration-test:unauthorized-client`.

**Linux natif — réutilisable pour chaque identité :**

```bash
# SIXPAY autorisé
openssl req -new -newkey rsa:3072 -sha256 -noenc \
  -keyout sixpay-client.key -out sixpay-client.csr -config sixpay-client.cnf
openssl x509 -req -in sixpay-client.csr \
  -CA regional-test-ca.crt -CAkey regional-test-ca.key -CAcreateserial \
  -out sixpay-client.crt -days 14 -sha256 \
  -extfile sixpay-client.cnf -extensions v3_client
openssl verify -x509_strict -purpose sslclient -CAfile regional-test-ca.crt sixpay-client.crt
openssl x509 -in sixpay-client.crt -noout -fingerprint -sha256

# Certificat valide mais non autorisé, avec configuration séparée
openssl req -new -newkey rsa:3072 -sha256 -noenc \
  -keyout unauthorized-client.key -out unauthorized-client.csr -config unauthorized-client.cnf
openssl x509 -req -in unauthorized-client.csr \
  -CA regional-test-ca.crt -CAkey regional-test-ca.key -CAcreateserial \
  -out unauthorized-client.crt -days 14 -sha256 \
  -extfile unauthorized-client.cnf -extensions v3_client
openssl verify -x509_strict -purpose sslclient -CAfile regional-test-ca.crt unauthorized-client.crt
openssl x509 -in unauthorized-client.crt -noout -fingerprint -sha256
```

**Windows Docker — méthode équivalente :** pour chaque commande OpenSSL ci-dessus, utiliser les chemins `/pki/` et l'enveloppe :

```powershell
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl req -new -newkey rsa:3072 -sha256 -noenc -keyout /pki/sixpay-client.key -out /pki/sixpay-client.csr -config /pki/sixpay-client.cnf'
docker run --rm -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl x509 -req -in /pki/sixpay-client.csr -CA /pki/regional-test-ca.crt -CAkey /pki/regional-test-ca.key -CAcreateserial -out /pki/sixpay-client.crt -days 14 -sha256 -extfile /pki/sixpay-client.cnf -extensions v3_client'
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && openssl verify -x509_strict -purpose sslclient -CAfile /pki/regional-test-ca.crt /pki/sixpay-client.crt && openssl x509 -in /pki/sixpay-client.crt -noout -fingerprint -sha256'
```

Répéter avec `unauthorized-client.*` pour le second certificat en préservant les fichiers déjà produits.

**Gates observés :** les deux certificats ont `clientAuth`, sont validés par `openssl verify -x509_strict -purpose sslclient`, mais leurs empreintes diffèrent. **Seule** celle de SIXPAY est présente dans `SECURITY_MTLS_ALLOWED_CLI_CERT`.

### 5.4 CA inconnue et troisième client

Créer une **autre** `unknown-ca.cnf` en reprenant la configuration CA, avec `CN = Unknown Integration Test CA`. Produire `unknown-ca.key`/`.crt` dans un dossier isolé ; vérifier :

```bash
openssl verify -x509_strict -CAfile unknown-ca.crt unknown-ca.crt
```

Créer `untrusted-client.cnf` analogue à `sixpay-client.cnf`, avec `CN = Untrusted Integration Test Client` et `URI.1 = urn:regional:integration-test:untrusted-client`. Générer une CSR et signer **avec `unknown-ca.crt` et `unknown-ca.key`** (durée 7 jours) :

```bash
openssl req -new -newkey rsa:3072 -sha256 -noenc \
  -keyout untrusted-client.key -out untrusted-client.csr -config untrusted-client.cnf
openssl x509 -req -in untrusted-client.csr \
  -CA unknown-ca.crt -CAkey unknown-ca.key -CAcreateserial \
  -out untrusted-client.crt -days 7 -sha256 -extfile untrusted-client.cnf -extensions v3_client
openssl verify -x509_strict -purpose sslclient -CAfile unknown-ca.crt untrusted-client.crt
```

**Gates observés :** `Unknown Integration Test CA` vérifiée strictement ; `Untrusted Integration Test Client` vérifié **avec sa propre CA**, mais la CA inconnue n'a **jamais été importée dans le truststore Regional**.

---

## 6. Magasins PKCS#12 — composition et validation

### 6.1 Keystore serveur

**Windows via Docker** (saisie interactive du mot de passe d'export) :

```powershell
docker run --rm -it -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl pkcs12 -export -inkey /pki/regional-server.key -in /pki/regional-server.crt -certfile /pki/regional-test-ca.crt -name regional-server -out /pki/regional-server.p12'
```

**Linux natif :**

```bash
umask 077
openssl pkcs12 -export -inkey regional-server.key -in regional-server.crt \
  -certfile regional-test-ca.crt -name regional-server -out regional-server.p12
```

**Gate exécuté avec `keytool` :** keystore PKCS12, alias `regional-server`, `PrivateKeyEntry`, chaîne de longueur **2** (`CN=localhost` puis CA principale).

### 6.2 Truststore serveur

**Windows avec JDK local :**

```powershell
$Keytool = 'C:\Program Files\Java\jdk-21\bin\keytool.exe'
& $Keytool -importcert -trustcacerts -noprompt -alias regional-test-ca `
  -file (Join-Path $PkiV2 'regional-test-ca.crt') `
  -keystore (Join-Path $PkiV2 'regional-client-truststore.p12') -storetype PKCS12
& $Keytool -list -v -keystore (Join-Path $PkiV2 'regional-client-truststore.p12') -storetype PKCS12
```

**Linux avec JDK :**

```bash
keytool -importcert -trustcacerts -noprompt -alias regional-test-ca \
  -file regional-test-ca.crt -keystore regional-client-truststore.p12 -storetype PKCS12
keytool -list -v -keystore regional-client-truststore.p12 -storetype PKCS12
```

**Gate exécuté :** truststore PKCS12, alias `regional-test-ca`, entrée `trustedCertEntry`, empreinte CA identique à celle de la chaîne du keystore.

### 6.3 PKCS12 du client SIXPAY — commodité Windows, non nouvelle identité

**Windows via Docker :**

```powershell
docker run --rm -it -v "${PkiV2}:/pki" alpine:3.23 sh -c 'apk add --no-cache openssl >/dev/null && umask 077 && openssl pkcs12 -export -inkey /pki/sixpay-client.key -in /pki/sixpay-client.crt -certfile /pki/regional-test-ca.crt -name sixpay-client -out /pki/sixpay-client.p12'
& $Keytool -list -keystore (Join-Path $PkiV2 'sixpay-client.p12') -storetype PKCS12
```

**Linux natif si un client Java ou autre outil le requiert :**

```bash
openssl pkcs12 -export -inkey sixpay-client.key -in sixpay-client.crt \
  -certfile regional-test-ca.crt -name sixpay-client -out sixpay-client.p12
keytool -list -keystore sixpay-client.p12 -storetype PKCS12
```

**Gate exécuté :** `sixpay-client`, `PrivateKeyEntry`, empreinte **strictement identique** à `sixpay-client.crt` et à la liste autorisée. Les fichiers `.crt` et `.key` restent utilisables sans `.p12` par un outil Linux/OpenSSL. Le format PKCS12 est **portable**, pas réservé à Windows.

---

## 7. Configuration et lancement du serveur Regional

### 7.1 Variables utilisées lors de la validation locale

Le profil actif réellement observé est `docker`. Les variables mTLS essentielles étaient :

```dotenv
ENABLE_MTLS=true
REGIONAL_MTLS_REQUIRED=true
SERVER_PORT=9443
SECURITY_MTLS_ALLOWED_CLI_CERT=DFFD2EF9F6B6892C9B19A46137C9FAAA8A95DD1BF28BB14C0ADCA2846BF5EE28
REGIONAL_MTLS_KEYSTORE_LOCATION=file:///CHEMIN/ABSOLU/regional-server.p12
REGIONAL_MTLS_TRUSTSTORE_LOCATION=file:///CHEMIN/ABSOLU/regional-client-truststore.p12
REGIONAL_MTLS_KEYSTORE_PASSWORD=<secret-local>
REGIONAL_MTLS_TRUSTSTORE_PASSWORD=<secret-local>
```

**Windows IntelliJ :** utiliser `file:///C:/.../pki-v2/regional-server.p12` et `file:///C:/.../pki-v2/regional-client-truststore.p12`. Les expressions PowerShell `([System.Uri](Join-Path ...)).AbsoluteUri` servent à **calculer** les valeurs dans PowerShell, mais **ne doivent pas être collées comme valeurs littérales** d'une variable IntelliJ.

D'autres variables applicatives ont été nécessaires, notamment PostgreSQL vers `localhost:15434`, Oracle mock vers `localhost:1521` et les paramètres OTP existants. **La recette du rapport ne doit pas inventer des valeurs de secrets, des permissions ou des endpoints métier.** Réutiliser la configuration de test locale déjà vérifiée, sans publier ses identifiants.

**Lignes de configuration applicative examinées au cours du chantier** (ne constituent pas un nouvel audit de la révision courante) :

```yaml
regional:
  security:
    mtls:
      enabled: ${ENABLE_MTLS:false}
      required: ${REGIONAL_MTLS_REQUIRED:false}
      allowed-client-cert-sha256: ${SECURITY_MTLS_ALLOWED_CLI_CERT:}
server:
  ssl:
    enabled: ${ENABLE_MTLS:false}
    key-store: ${REGIONAL_MTLS_KEYSTORE_LOCATION:}
    key-store-password: ${REGIONAL_MTLS_KEYSTORE_PASSWORD:}
    key-store-type: PKCS12
    trust-store: ${REGIONAL_MTLS_TRUSTSTORE_LOCATION:}
    trust-store-password: ${REGIONAL_MTLS_TRUSTSTORE_PASSWORD:}
    trust-store-type: PKCS12
    client-auth: need
    enabled-protocols: TLSv1.3
```

**À confirmer sur toute révision future :** propriété effective après fusion des profils/configurations, TLS réellement autorisé et état du filtre applicatif. Laisser `ENABLE_MTLS=false` et `REGIONAL_MTLS_REQUIRED=false` n'est **pas** une configuration autorisée par ce rapport pour un environnement bancaire sécurisé.

### 7.2 Exécution et journaux

**Windows :** IntelliJ → Run → Edit Configurations → Spring Boot, profil `docker`, Java 21, variables ci-dessus ; clic **Run**. Pour observer Tomcat :

```dotenv
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT_WEB_EMBEDDED_TOMCAT=DEBUG
LOGGING_LEVEL_ORG_APACHE_COYOTE=DEBUG
```

Option JVM **temporaire** si le handshake exige un diagnostic détaillé :

```text
-Djavax.net.debug=ssl,handshake
```

**Linux générique :** l'application peut être lancée avec la même configuration Spring via `java -jar ... --spring.profiles.active=<profil-validé>` et les variables fournies par un environnement d'exécution protégé (service systemd, gestionnaire de secrets, orchestrateur). La commande exacte de packaging, le profil réellement disponible, l'utilisateur de service et les chemins SSL doivent être **vérifiés dans le déploiement cible avant usage**. Extrait de principe, non exécuté :

```bash
# Exemple de principe, jamais avec un vrai secret écrit dans l'historique du shell
# ENV : ENABLE_MTLS, REGIONAL_MTLS_REQUIRED, SERVER_PORT,
# REGIONAL_MTLS_KEYSTORE_LOCATION=file:/opt/regional/tls/regional-server.p12,
# REGIONAL_MTLS_TRUSTSTORE_LOCATION=file:/opt/regional/tls/regional-client-truststore.p12,
# les mots de passe obtenus par un mécanisme de secrets.
java -jar /opt/regional/app/regional-core-banking-api.jar --spring.profiles.active=<profil-verifie>
```

Ne pas exposer directement un port de test au réseau public. Utiliser un DNS/SAN approprié, la séparation réseau requise, un compte de service, des permissions minimales et des certificats à durée et rotation opérationnelles.

**Gates observés sur Windows :**

```text
The following 1 profile is active: "docker"
Tomcat initialized with port 9443 (https)
Tomcat started on port 9443 (https) with context path '/'
Started RegionalCoreBankingApiApplication
Successfully validated 3 migrations
```

La ligne Tomcat annonçant `C:\Users\...\.keystore` / alias `tomcat` / `trust store [null]` s'est révélée **insuffisante pour conclure** au magasin réellement servi : la preuve réseau a établi que Regional présentait le certificat attendu `CN=localhost`, signé par la CA principale. Ne jamais valider un magasin uniquement d'après cette ligne de journal.

**Sécurité des logs :** les traces TLS DEBUG peuvent être volumineuses et exposer des métadonnées ; désactiver après diagnostic. La campagne comportait une ligne console affichant l'allowlist, qui n'est pas un secret, mais les logs de production doivent rester maîtrisés.

---

## 8. Exécution des tests HTTPS/mTLS — scripts Windows/Docker

**Important :** sur Windows, les scripts PowerShell ont lancé `curl` **dans un conteneur Alpine Linux** pour éviter les ambiguïtés de Schannel. Le serveur Regional restait démarré dans IntelliJ sur Windows. `host.docker.internal` désignait l'hôte Windows depuis Docker Desktop ; `--connect-to` préservait `localhost` comme nom TLS vérifié dans le SAN. **Cette résolution est propre au contexte Docker Desktop, elle n'est pas une règle universelle de Linux.**

Pour éviter les problèmes rencontrés avec les suites de lignes Linux dans `sh -c`, **les exemples suivants conservent la commande Linux sur une seule ligne**. Les tests négatifs TLS retournent normalement un code non nul. Un HTTP 403 ou 400 peut s'accompagner d'un code curl **0**, car le transport a pu se terminer correctement.

```powershell
$PkiV2 = Join-Path $env:TEMP 'regional-mtls-integration\pki-v2'
```

### 8.1 T01 — client SIXPAY autorisé

```powershell
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache curl ca-certificates >/dev/null && curl -v --tlsv1.3 --tls-max 1.3 --cacert /pki/regional-test-ca.crt --cert /pki/sixpay-client.crt --key /pki/sixpay-client.key --connect-to localhost:9443:host.docker.internal:9443 --noproxy localhost https://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** `Request CERT (13)` ; certificat client et `CERT verify` émis ; `SSL connection using TLSv1.3 / TLS_AES_256_GCM_SHA384` ; `OpenSSL verify result: 0` ; SAN `localhost` validé ; `HTTP/1.1 200` ; `{"status":"UP","groups":["liveness","readiness"]}` ; `DOCKER_EXIT_CODE=0`.

### 8.2 T02 — certificat valide mais identité non autorisée

```powershell
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache curl ca-certificates >/dev/null && curl -v --tlsv1.3 --tls-max 1.3 --cacert /pki/regional-test-ca.crt --cert /pki/unauthorized-client.crt --key /pki/unauthorized-client.key --connect-to localhost:9443:host.docker.internal:9443 --noproxy localhost https://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** TLS 1.3 et chaîne serveur valides, requête émise, **HTTP 403 Forbidden**, corps JSON `status:403`, code Docker `0`. La décision est **cohérente** avec l'allowlist d'empreintes ; une trace du `RegionalMtlsClientCertificateFilter` serait la preuve directe supplémentaire de la branche de code concernée.

### 8.3 T03 — aucune identité cliente

```powershell
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache curl ca-certificates >/dev/null && curl -v --tlsv1.3 --tls-max 1.3 --cacert /pki/regional-test-ca.crt --connect-to localhost:9443:host.docker.internal:9443 --noproxy localhost https://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** `Request CERT (13)` ; le client répond par un message certificat **vide** ; `TLS alert, bad certificate (554)` ; `curl (56)` ; Docker `56` ; **aucune réponse HTTP**. En TLS 1.3, curl peut afficher une connexion initialement établie et une requête envoyée avant de lire l'alerte TLS : la présence de la ligne GET **n'implique pas** qu'une requête a été traitée par Spring.

### 8.4 T04 — certificat signé par CA inconnue

```powershell
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache curl ca-certificates >/dev/null && curl -v --tlsv1.3 --tls-max 1.3 --cacert /pki/regional-test-ca.crt --cert /pki/unknown-ca/untrusted-client.crt --key /pki/unknown-ca/untrusted-client.key --connect-to localhost:9443:host.docker.internal:9443 --noproxy localhost https://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** TLS 1.3, certificat serveur valide, certificat client présenté, **alerte `certificate unknown (558)`**, `curl (56)`, Docker `56`, **aucune réponse HTTP**.

### 8.5 T05 — TLS 1.2 explicitement interdit

```powershell
docker run --rm -v "${PkiV2}:/pki:ro" alpine:3.23 sh -c 'apk add --no-cache curl ca-certificates >/dev/null && curl -v --tlsv1.2 --tls-max 1.2 --cacert /pki/regional-test-ca.crt --cert /pki/sixpay-client.crt --key /pki/sixpay-client.key --connect-to localhost:9443:host.docker.internal:9443 --noproxy localhost https://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** `Client hello` TLS 1.2 ; `TLS alert, protocol version (582)` ; `curl (35)` ; Docker `35` ; pas de réponse HTTP.

### 8.6 T06 — HTTP en clair sur le port HTTPS

```powershell
docker run --rm alpine:3.23 sh -c 'apk add --no-cache curl >/dev/null && curl -v --noproxy localhost --connect-to localhost:9443:host.docker.internal:9443 http://localhost:9443/actuator/health'
Write-Host "DOCKER_EXIT_CODE=$LASTEXITCODE"
```

**PASS observé :** `HTTP/1.1 400`, texte **`This combination of host and port requires TLS.`**, Docker `0` ; aucune réponse applicative normale. **Périmètre limité au port `9443`** : ce test ne prouve pas qu'aucun autre service/port HTTP n'est ouvert.

---

## 9. Exécution sur Linux générique : version sans Docker Desktop

Les mêmes scénarios peuvent être rejoués directement avec **curl utilisant OpenSSL** depuis un hôte Linux pouvant joindre Regional. Ne pas supposer que le client sera installé **sur le même hôte** que le serveur ; sélectionner l'adresse et le certificat serveur adéquats.

Dans les exemples locaux ci-dessous, `localhost:9443` est pertinent **uniquement si Regional écoute sur le même hôte Linux et présente le certificat localhost de cette campagne**. Pour un service distant, utiliser `https://nom-dns-reel:port/...` avec certificat SAN correspondant et réseau/pare-feu adéquats.

```bash
cd "$HOME/regional-mtls-test"
export BASE_URL='https://localhost:9443/actuator/health'

# T01 — autorisé : HTTP 200 + code 0
curl -v --noproxy localhost --tlsv1.3 --tls-max 1.3 \
  --cacert regional-test-ca.crt --cert sixpay-client.crt --key sixpay-client.key "$BASE_URL"

# T02 — même CA mais empreinte non approuvée : HTTP 403
curl -v --noproxy localhost --tlsv1.3 --tls-max 1.3 \
  --cacert regional-test-ca.crt --cert unauthorized-client.crt --key unauthorized-client.key "$BASE_URL"

# T03 — pas de certificat : alerte TLS, pas de réponse HTTP
curl -v --noproxy localhost --tlsv1.3 --tls-max 1.3 \
  --cacert regional-test-ca.crt "$BASE_URL"

# T04 — client signé par une CA inconnue : rejet TLS
curl -v --noproxy localhost --tlsv1.3 --tls-max 1.3 \
  --cacert regional-test-ca.crt --cert unknown-ca/untrusted-client.crt \
  --key unknown-ca/untrusted-client.key "$BASE_URL"

# T05 — uniquement TLS 1.2 : rejet protocole
curl -v --noproxy localhost --tlsv1.2 --tls-max 1.2 \
  --cacert regional-test-ca.crt --cert sixpay-client.crt --key sixpay-client.key "$BASE_URL"

# T06 — HTTP clair sur port HTTPS : HTTP 400, sans réponse de santé normale
curl -v --noproxy localhost 'http://localhost:9443/actuator/health'
```

Après chaque commande Linux, exécuter `printf 'CURL_EXIT_CODE=%s\n' "$?"` **immédiatement**, sans intercaler d'autre commande. Les codes détaillés sont indicatifs pour une version de curl donnée ; **les conditions réseau/TLS/HTTP priment sur le code exact**. Pour automatiser les gates, séparer la sortie HTTP (`--write-out '%{http_code}'`) du code de sortie de curl, et ne pas considérer `curl=0` comme un succès d'autorisation.

**Cas Linux conteneurisé :** la résolution `host.docker.internal` n'est pas automatiquement disponible sur tout moteur Docker Linux. Utiliser un réseau Compose partagé, un DNS interne approuvé, une adresse de service ou une option spécifique documentée au déploiement. Éviter `--network host` sans justification de sécurité et ne jamais utiliser `-k` pour faire « passer » les tests. La topologie de réseau est un contrôle séparé du protocole TLS.

**Cas Windows Schannel (résultat de campagne) :**

- Sans CA importée, `SEC_E_UNTRUSTED_ROOT` (`curl 60`).
- Avec `--cacert`, échec de vérification de révocation Schannel pour CA de test sans CRL/OCSP (`curl 60`).
- Avec `--ssl-no-revoke` **uniquement pendant la campagne de test locale**, le nom `localhost` était vérifié ; le test sans certificat a échoué `SEC_E_CERT_UNKNOWN` (`curl 56`).
- Le PKCS12 `sixpay-client.p12` était valide selon `keytool`, mais l'import par Schannel a d'abord échoué faute de mot de passe (`curl 58`), puis avec `SEC_E_INTERNAL_ERROR` (`curl 35`) après fourniture du mot de passe. **La cause exacte de l'échec Schannel n'a pas été établie**. Il ne faut pas le transformer en PASS ; le test positif a été obtenu via Docker/OpenSSL.
- Une commande avec `--pass <mot-de-passe>` transmet temporairement le secret via les arguments du processus : **déconseillé** hors test local ; ne pas copier de secrets dans les logs, l'historique, les captures ni Git.

**Point de sécurité :** `--ssl-no-revoke` ne doit pas être une stratégie générale de production. Pour une PKI opérationnelle, établir une politique de révocation/rotation vérifiable compatible avec les clients et les risques. Une CA de test auto-signée temporaire n'est pas une architecture PKI de production.

---

## 10. Scénarios fail-closed dans IntelliJ — réellement testés

Avant chaque scénario, **arrêter la configuration HTTPS précédente** ; dupliquer la configuration fonctionnelle d'IntelliJ, ne changer que la variable testée et garder les autres valeurs valides.

### T07 — mTLS obligatoire mais désactivé

```dotenv
ENABLE_MTLS=false
REGIONAL_MTLS_REQUIRED=true
```

**Preuve observée :**

```text
BeanCreationException: Error creating bean with name 'regionalMtlsStartupGuard'
Caused by: java.lang.IllegalStateException:
Regional secure deployment requires ENABLE_MTLS=true
RegionalMtlsStartupGuard.verify(RegionalMtlsStartupGuard.java:23)
Process finished with exit code 1
```

**Gate :** démarrage refusé, code Java non nul ; garde `RegionalMtlsStartupGuard` invoquée. Ce test **ne prouve pas l'échec de tous les schémas de configuration possibles**, seulement de celui décrit.

### T08 — keystore serveur manquant

```dotenv
ENABLE_MTLS=true
REGIONAL_MTLS_REQUIRED=true
REGIONAL_MTLS_KEYSTORE_LOCATION=file:///C:/.../pki-v2/regional-seerver.p12
```

Le chemin testé comportait une faute délibérée ou accidentelle (`seerver` au lieu de `server`) et n'existait pas.

**Preuve observée :**

```text
Unable to create key store: Could not load store from '...regional-seerver.p12'
Caused by: java.io.FileNotFoundException: ...regional-seerver.p12
Process finished with exit code 1
```

**Gate :** échec de création du magasin SSL et arrêt du processus.

### T09 — truststore client manquant

```dotenv
ENABLE_MTLS=true
REGIONAL_MTLS_REQUIRED=true
REGIONAL_MTLS_KEYSTORE_LOCATION=file:///C:/.../pki-v2/regional-server.p12
REGIONAL_MTLS_TRUSTSTORE_LOCATION=file:///C:/.../pki-v2/regional-client-truuststore.p12
```

Le chemin réellement testé comportait `truuststore` (au lieu de `truststore`). **Une première tentative T09 avait encore un keystore invalide ; elle ne validait pas ce scénario**. Après correction du keystore, l'échec a concerné le truststore :

```text
Unable to create trust store: Could not load store from '...regional-client-truuststore.p12'
Caused by: java.io.FileNotFoundException: ...regional-client-truuststore.p12
Process finished with exit code 1
```

**Gate :** démarrage refusé, code non nul. Contrôle final Windows :

```powershell
Get-NetTCPConnection -LocalPort 9443 -State Listen -ErrorAction SilentlyContinue |
  Select-Object LocalAddress, LocalPort, OwningProcess
```

**Preuve observée :** **aucune ligne**, donc aucun processus local ne restait en écoute sur le port 9443 au moment du contrôle.

**Linux pour T07–T09 (recette non exécutée) :** créer des profils/service temporaires isolés, lancer le processus Java sous l'utilisateur applicatif et enregistrer `systemctl status` / `journalctl` ou le code de sortie Java, puis contrôler `ss -ltnp | grep ':9443'`. Ne pas remplacer une unité systemd de production ou des secrets en place pour provoquer un test négatif ; utiliser un environnement non-productif explicitement autorisé.

---

## 11. Matrice finale de validation, preuves et portée

| ID | Scénario | Critère PASS | Observation | Conclusion |
|---|---|---|---|---|
| T01 / L1.1-12 | SIXPAY autorisé | TLS1.3 + certificat client + HTTP 200 | TLS1.3 `TLS_AES_256_GCM_SHA384`, HTTP 200 `UP`, exit 0 | **PASS** |
| T02 / L1.1-13 | Certificat même CA mais empreinte non autorisée | TLS valide puis HTTP 403 | HTTP 403, JSON Forbidden, exit 0 | **PASS** (cause filtre cohérente, pas tracée directement) |
| T03 / L1.1-14 | Sans certificat client | Alerte TLS, pas de réponse HTTP | `bad certificate`, curl 56 | **PASS** |
| T04 / L1.1-16C | CA cliente inconnue | Alerte TLS, pas de réponse HTTP | `certificate unknown`, curl 56 | **PASS** |
| T05 / L1.1-15 | TLS 1.2 | Refus négociation | `protocol version`, curl 35 | **PASS** |
| T06 / L1.1-17 | HTTP clair sur port TLS | Pas de réponse applicative 200 | HTTP 400, message exigeant TLS | **PASS** |
| T07 / L1.1-18A | Obligatoire=true, activé=false | Échec démarrage | `RegionalMtlsStartupGuard`, exit 1 | **PASS** |
| T08 / L1.1-18B | Keystore manquant | Échec démarrage | `FileNotFoundException`, exit 1 | **PASS** |
| T09 / L1.1-18C | Truststore manquant | Échec démarrage + port fermé | `FileNotFoundException`, exit 1, port 9443 libre | **PASS** |

**Total : 9/9 scénarios conformes aux résultats attendus dans l'environnement local exécuté.** Les validations de construction PKI, de compatibilité des magasins et de démarrage HTTPS sont des **gates complémentaires** et ne font pas artificiellement augmenter ce total.

### Gates complémentaires de création de la PKI

- CA v2 conforme X.509, vérification `-x509_strict` `OK` et exit 0.
- Certificat serveur validé pour DNS `localhost` et IP `127.0.0.1`.
- Client SIXPAY `clientAuth`, SHA-256 inchangé après conditionnement `.p12`.
- Client non autorisé `clientAuth` avec empreinte différente.
- CA inconnue et client inconnu strictement valides envers leur **propre** chaîne.
- `regional-server.p12` : `PrivateKeyEntry`, chaîne 2 ; truststore : `trustedCertEntry`.
- Application Tomcat démarrée sur `9443 (https)` ; la DB PostgreSQL et les migrations Flyway ont été validées au démarrage.

### Non démontré / à compléter

- **Git :** SHA HEAD exact, état de l'arbre de travail, ancestry et CI de la révision exécutée **non vérifiés dans cette campagne**.
- **Client SIXPAY Java :** construction et injection du certificat, gestion des stores/secrets, appels réels à Regional, renouvellement, timeouts et erreurs **non testés**.
- **Réseau :** absence de port HTTP ouvert ailleurs, firewall, proxy, reverse proxy, adressage privé, service discovery et DNS **non validés**.
- **Schannel Windows :** le test positif PKCS12 natif a échoué `SEC_E_INTERNAL_ERROR` ; investigation facultative mais non close.
- **Sécurité avancée :** certificat client révoqué/expiré, CA compromise, mauvais mot de passe, chaîne incomplète, renouvellement de certificat et empreinte mise à jour, protection des stores et authentification applicative indépendante **non testés**.
- **Linux :** recettes fournies mais **aucun résultat réel observé** sur hôte Linux autonome, Kubernetes, VM, VPS ou Contabo.
- **Autorisation HTTP 403 :** preuve HTTP cohérente avec le filtre d'empreintes, mais log ou breakpoint ciblé encore à recueillir si une traçabilité exacte de la branche de décision est requise.

---

## 12. Mesures de sécurité et recommandations multi-environnements

1. **Séparer test et production.** La CA auto-signée et les certificats de 7–30 jours sont uniquement pour des essais. La PKI de production doit avoir propriétaires, procédure d'émission, inventaire, révocation, renouvellement, rotation des empreintes et contrôle d'accès explicites.
2. **Protéger les clés privées.** Ne pas committer `.key`, `.p12` avec clé privée, secrets ou exports de mot de passe. Sous Linux, dossier dédié à l'utilisateur de service, permissions restrictives (par exemple `umask 077`, fichiers `0600`, répertoires `0700` selon exploitation) ; sous Windows ACL limitées. Ne pas laisser des fichiers privés dans des répertoires temporaires partagés plus longtemps que nécessaire.
3. **Éviter les secrets dans les arguments de processus, URLs et `.env` versionnés.** Préférer un gestionnaire de secrets / injection sécurisée (IntelliJ local protégé, systemd credentials, orchestrateur, coffre de secrets selon infrastructure).
4. **Assurer l'identité du serveur sur chaque environnement.** Le SAN d'un certificat de test `localhost` ne convient pas à l'URL DNS d'un service distant. Conserver une vérification stricte de la chaîne et du nom ; ne pas utiliser `--insecure` / `-k` comme solution.
5. **Séparer authentification et autorisation.** Truststore pour la chaîne CA et `client-auth=need` ; allowlist/fingerprint pour l'identité client ; ensuite contrôler les droits d'API et le contexte métier.
6. **Politique TLS.** TLS 1.3 a été validé et TLS 1.2 refusé dans cette configuration ; réévaluer cette politique à la lumière des capacités des clients réels et de la politique sécurité applicable, sans abaisser silencieusement les exigences.
7. **Fail-closed.** Conserver et automatiser les tests de mTLS obligatoire désactivé, de keystore/truststore absents et, après décision, ajouter mot de passe incorrect, certificat expiré, stores malformés et profils divergents.
8. **Observabilité.** Collecter événements de handshake, erreurs de confiance et refus de fingerprint sans exposer de clés privées. Supprimer les logs SSL très verbeux après diagnostic, surveiller les expirations de certificats.
9. **Topologie Linux générale.** Que l'hôte soit Ubuntu, Debian, RHEL/AlmaLinux, VM, bare metal, VPS ou Kubernetes, traiter indépendamment les certificats/PKCS12, la configuration Java, les chemins/volumes, les règles réseau, les DNS/SAN, le firewall et les politiques de déploiement.
10. **Gates de livraison.** Avant mise en service réelle, exécuter les tests depuis le véritable client HTTP SIXPAY, sur la topologie cible, avec la révision Git et les images réellement déployées, puis signer les résultats. Les neuf PASS locaux **ne remplacent pas** ce contrôle.

---

## 13. Séquence de reprise reproductible

1. Identifier et consigner la révision de Regional exécutée (branche, `git rev-parse HEAD`, état Git) ainsi que Java/OS/Tomcat et l'environnement client.
2. Préparer une PKI locale temporaire **ou** une PKI de test approuvée ; vérifier CA, EKU, SAN, dates et signatures.
3. Produire le keystore serveur PKCS12 et le truststore des CA clientes ; vérifier les alias, types d'entrées et chaîne.
4. Préparer trois certificats clients : approuvé, signé par la même CA mais non autorisé, signé par une CA absente du truststore.
5. Configurer Regional avec mTLS obligatoire, TLS 1.3, mot de passe et chemins réels, autorisation fingerprint ; démarrer avec la configuration approuvée.
6. Vérifier certificat serveur et SAN avec une CA explicitement approuvée, **sans `-k`** ; conserver l'évidence de la version TLS négociée.
7. Exécuter T01 à T06 avec le moteur TLS réel du client choisi ; noter la réponse HTTP, l'alerte TLS, le code de sortie, les logs pertinents et la date.
8. Dans un environnement isolé, exécuter T07 à T09 (fail-closed), puis vérifier que le processus et le port ne restent pas ouverts.
9. Revenir à la configuration nominale, désactiver les logs TLS de diagnostic, contrôler droits/secrets, expiration et présence d'autres ports.
10. Archiver les **preuves non sensibles**, consigner les écarts et faire valider les décisions avant de passer à l'intégration cliente ou au déploiement Linux cible.

### Mini-modèle de procès-verbal par gate

```text
ID : Txx
Environnement / hôte :
Révision Git et image ou JAR :
Date / fuseau :
Configuration non sensible :
Commande exacte (sans secret) :
Attendu :
Observé :
Exit code processus / curl :
HTTP status ou alerte TLS :
Preuve (log expurgé) :
Conclusion PASS / FAIL / BLOCKED_ENVIRONMENT :
Limites / décision restante :
```

---

## 14. État final et décisions suivantes

**Conclusion :** au 18 novembre 2026, la campagne locale établit qu'un serveur Regional démarré sur le port `9443` a répondu HTTP 200 au certificat SIXPAY autorisé ; renvoyé HTTP 403 à un client signé par la même CA mais non autorisé ; rejeté sans certificat et avec CA inconnue au niveau TLS ; refusé TLS 1.2 et HTTP en clair ; et refusé le démarrage dans les trois scénarios fail-closed demandés. Le port 9443 était fermé après le dernier échec de démarrage.

**Modification réalisée dans le dépôt :** aucune par l'assistant dans cette campagne documentaire. Des scripts et certificats **locaux de test** ont été créés et exécutés par l'opérateur ; leurs fichiers et contenus ne doivent pas être intégrés dans Git. **Gates de compilation/CI de la révision actuelle :** non exécutées dans cette campagne. **État Git final :** non observé.

**Validation dynamique :** **9/9 scénarios locaux conformes**, selon les sorties transmises. Ce résultat ne signifie pas `REPOSITORY_VALIDATED` au sens d'une vérification de révision et de pipeline, et ne valide aucun Linux distant.

**Décisions / travaux avant le Lot 2 :** confirmer la révision et l'architecture du client Java SIXPAY, les contrats et politiques de sécurité pertinents, la stratégie des certificats de l'environnement cible (y compris CA, rotation, secret management et SAN), la topologie réseau privée et les critères de validation du client réel. **Aucune modification de ces éléments n'est autorisée par ce rapport.**

### Note de gouvernance SIXPAY CONNECT

Ce rapport documente en premier lieu des **essais exécutés sur Regional Core Banking API**, sans proposer de modification SIXPAY. Le contexte de gouvernance SIXPAY fourni ici ne comprend pas toutes les sources `alwaysLoad` de son manifeste : `documentation/contracts/CONTRACT_REGISTRY.yaml` (alias `CONTRACT_REGISTRY.yaml`) et `MASTER_ENGINEERING_PROMPT.md` (alias `SIXPAY_MASTER_ENGINEERING_PROMPT.md`) n'ont pas été trouvés parmi les fichiers locaux disponibles. **Statut de contexte SIXPAY : `INCOMPLETE_CONTEXT`** pour une analyse autoritative ou modification du dépôt SIXPAY. Il faudra les charger avant tout lot d'intégration côté SIXPAY.

---

*Fin du rapport — Référence documentaire du Lot 1.1. Les procédures Linux sont des prescriptions de test génériques, à vérifier et à exécuter sur chaque environnement cible ; seules les traces locales datées constituent les preuves de PASS de cette campagne.*
