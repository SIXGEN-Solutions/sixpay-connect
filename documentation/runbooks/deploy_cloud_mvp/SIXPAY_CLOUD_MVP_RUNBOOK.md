# SIXPAY CONNECT — Runbook de préparation cloud MVP

**Révision inspectée** : `feat/repository-baseline-consolidation-cucumber` @ `d43ef85045cc7896bbc30e6d2d530233a155562c`.
**Mode** : préparation hors dépôt, aucun déploiement ni test exécuté. **Statut** : validations et décisions d'infrastructure en attente.

## 1. Couverture vérifiée et limites

Composants demandés : frontend Angular/Nginx, backend Spring Boot `bootstrap`, PostgreSQL 15, Keycloak (M2M), LDAP (Samba AD pour simulation), API Core Banking séparée, simulateur SMS. Les quatre premiers ont des éléments dans SIXPAY ; l'API Core Banking et la simulation SMS doivent être assemblées depuis leurs sources propres. La stack `infrastructure/docker/preproduction` déploie déjà frontend/backend/PostgreSQL/Samba, mais **pas** Keycloak ni Core Banking. `infrastructure/docker/m2m-keycloak` est une stack **locale**, non prête à être déployée telle quelle.

La configuration source `.env` locale annoncée dans la demande n'a pas été fournie parmi les pièces accessibles. Il est donc impossible de démontrer une égalité complète entre tous les paramètres locaux et ceux du profil cloud. Ne transmettre aucun `.env` contenant des secrets en clair pour contourner cette limite.

## 2. Décisions obligatoires avant provisioning

1. Confirmer fournisseur cloud, région, politique de réseau privé, DNS et exposition externe.
2. Confirmer modèle de déploiement (VM + Compose pour simulation isolée, ou Kubernetes/Helm approuvés), disponibilité et volumes persistants.
3. Confirmer gateway TLS/mTLS, émission/révocation des certificats clients partenaires, relation certificat / client Keycloak / Partner.
4. Confirmer gestionnaire de secrets externe approuvé (Vault est prévu par la matrice ; service cloud équivalent uniquement avec approbation).
5. Confirmer origine et image/version du Core Banking, configuration des fournisseurs d'OTP/SMS et capacités autorisées du mock.
6. Confirmer stratégie de bootstrap du premier administrateur SIXPAY : le hook existant porte `@Profile("preproduction")` et n'est pas automatiquement utilisable en `production`.

**Ne pas exposer `/v1/payments/initiate` à Internet avant validation de la chaîne mTLS et M2M.**

## 3. Architecture cible à valider

```text
Navigateur --HTTPS--> passerelle TLS --HTTPS--> Angular/Nginx --/api--> SIXPAY backend
                                                               |-- JDBC/TLS --> PostgreSQL 15
                                                               |-- LDAPS/636 --> AD simulé Samba
Partenaire --mTLS + OAuth2/JWT --> passerelle d'accès --> SIXPAY backend
                           |                          |-- JWKS HTTPS --> Keycloak M2M
                           |                          |-- HTTPS/mTLS --> Core Banking API
                           |                                            |-- SMS sandbox
                           +-- Auth client_credentials --> Keycloak
```

Backend, PostgreSQL, LDAP, Keycloak, Core Banking et simulateur SMS doivent être cloisonnés dans des réseaux privés. N'exposer publiquement ni PostgreSQL, ni LDAP, ni interfaces d'administration, ni les ports internes de Core Banking/SMS.

## 4. Préparation des images et données

- Examiner les `Dockerfile` existants avant construction ; backend Java 21, frontend Node 22/Nginx.
- Bâtir les images à partir du SHA approuvé et enregistrer leur digest ; scanner et signer selon les contrôles retenus par la plateforme.
- PostgreSQL 15 : provisionner volume persistant, sauvegardes et restauration ; base `sixpaydb` et utilisateur dédié ; laisser Flyway créer le schéma `sixpay`. Ne pas utiliser `ddl-auto: update`, ni `flyway clean`.
- Keycloak : utiliser une configuration persistante avec TLS et base de données selon le standard approuvé ; **ne pas** déployer `start-dev` ni le realm permissif local tels quels.
- LDAP : pour la simulation seulement, conserver un Samba AD isolé, des utilisateurs fictifs et une PKI de test ; pour la Régionale, raccorder les DC réels en LDAPS avec `objectGUID` stable.
- Core Banking : déployer depuis son propre repository et confirmer ses propres dépendances DB, SMTP et SMS ; SIXPAY ne génère et ne vérifie pas les OTP à sa place.
- SMS : sélectionner le simulateur effectivement supporté par le Core Banking ; aucun simulateur SMS intégré à cette stack SIXPAY n'a été établi dans les sources inspectées.

## 5. Configuration applicative unique

Après application **locale** du script, lancer SIXPAY avec `SPRING_PROFILES_ACTIVE=production` uniquement, en conservant le socle `application.yml`. Le profil production importe les fragments existants LDAP, Customer Banking, Payment Banking Capabilities, notification opérationnelle et projection Customer. Il n'importe pas les valeurs de test `standalone`, `integration`, `m2m-local`, `payment-banking-local`.

L'authentification humaine est LDAP ; le frontend est synchronisé (`local=false`, `oidc=false`, `ldap=true`). L'authentification machine M2M demeure **désactivée par défaut**, à activer exclusivement après validation JWT et mTLS. Le fait de disposer d'un token Keycloak ne prouve pas qu'un partenaire peut effectuer un paiement.

Paramètres à injecter par le runtime, **pas depuis un fichier de secrets versionné** :

- PostgreSQL : `SIXPAY_DB_URL`, `SIXPAY_DB_USERNAME`, `SIXPAY_DB_PASSWORD`.
- LDAP : `SIXPAY_LDAP_URLS`, `SIXPAY_LDAP_BASE_DN`, `SIXPAY_LDAP_USER_SEARCH_BASE`, `SIXPAY_LDAP_SERVICE_ACCOUNT_DN`, `SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD`, `SIXPAY_LDAP_TRUST_DOMAIN`; vérifier attributs AD et CA LDAPS.
- M2M : `SIXPAY_M2M_ENABLED`, `SIXPAY_M2M_ISSUER_URI`, `SIXPAY_M2M_JWK_SET_URI`, `SIXPAY_M2M_AUDIENCE` ; issuer exact, JWKS HTTPS, audience approuvée.
- Core Banking : `CORE_BANKING_BASE_URL`, `AMPLITUDE_PAYMENT_BASE_URL`, `AMPLITUDE_CONFIRMATION_CONNECT_TIMEOUT`, `AMPLITUDE_CONFIRMATION_READ_TIMEOUT`, et toutes les variables d'activation et transport déjà présentes dans `application-payment-banking-capabilities.yml`.
- Si OAuth2/mTLS bancaire actif : `AMPLITUDE_PAYMENT_OAUTH2_TOKEN_URI`, `AMPLITUDE_PAYMENT_OAUTH2_CLIENT_ID`, `AMPLITUDE_PAYMENT_OAUTH2_CLIENT_SECRET`, `AMPLITUDE_PAYMENT_KEYSTORE_LOCATION`, `AMPLITUDE_PAYMENT_KEYSTORE_PASSWORD`, `AMPLITUDE_PAYMENT_TRUSTSTORE_LOCATION`, `AMPLITUDE_PAYMENT_TRUSTSTORE_PASSWORD`, avec fichiers montés hors Git et CA approuvées. Le fichier `application-payment-banking.yml` existe mais n'est pas importé par défaut : valider les besoins de certificat/token et la stratégie de chargement avant activation réelle.
- Audit et protection données : `SIXPAY_REPORTING_AUDIT_CURSOR_HMAC_KEY`, `SIXPAY_CUSTOMER_OBSERVATION_PROTECTION_KEY_BASE64` et `SIXPAY_CUSTOMER_OBSERVATION_CURSOR_KEY_BASE64` ; pas de valeurs de test, conserver les clés entre redémarrages.
- Callback : `SIXPAY_PAYMENT_CALLBACK_SIGNING_KEY_ID`, `SIXPAY_PAYMENT_CALLBACK_SIGNING_SECRET` si cette capacité est autorisée.

Ne pas utiliser `ALLOW_INSECURE_HTTP=true`, ni désactiver artificiellement OAuth2/mTLS, ni copier les mots de passe `integration`/`standalone`. Fournir toutes les propriétés nécessaires à l'activation de chaque adaptateur ; un échec de démarrage doit être corrigé à la source et non contourné.

## 6. Gestion des secrets

**Option recommandée à faire approuver : Vault** (alignée avec la matrice officielle), ou gestionnaire cloud institutionnel équivalent. Le déploiement reçoit une identité de workload courte durée ; il lit les secrets par mécanisme externe approuvé, avec politique d'accès minimale. Les secrets sont injectés au runtime sous la forme attendue par Spring, sans `.env` embarqué et sans visibilité dans les arguments process ou les logs. Définir rotation, audit des lectures, séparation par environnement et secours. Éviter les variables d'environnement si la politique exige une meilleure isolation ; préférer des fichiers secrets montés temporairement avec permissions strictes ou une intégration Vault approuvée. **Aucune dépendance Vault n'est ajoutée par ce correctif**.

Le `.env.example` est un modèle de noms de paramètres, pas un emplacement acceptable pour des secrets de cloud.

## 7. Ordre d'installation proposé (après décision d'infrastructure)

1. Provisionner réseau, DNS, certificats, règles egress/ingress et gestionnaire de secrets.
2. Provisionner PostgreSQL et sauvegardes, puis Keycloak persistant et LDAP simulé.
3. Provisionner Core Banking API et ses dépendances propres de simulation SMS/OTP.
4. Injecter les secrets et les références de certificats ; tester DNS et TLS depuis les réseaux applicatifs.
5. Construire les images et déployer SIXPAY backend en profil `production` ; vérifier Flyway et readiness ; **ne pas encore exposer les routes paiement**.
6. Déployer frontend Angular/Nginx derrière HTTPS et vérifier le routage `/api/` / `/internal/api/`.
7. Provisionner utilisateur LDAP de test, lien canonique Security et permissions SIXPAY via procédure approuvée ; résoudre la limitation du bootstrap `preproduction`.
8. Configurer Keycloak M2M, l'identité client/Partner et le dispositif de mTLS ; exécuter les scénarios de refus avant ouverture du trafic.
9. Activer uniquement les capacités bancaires validées avec vrais transports sécurisés ou un simulateur **isolé** dont les données ne sont pas réelles.
10. Effectuer un paiement contrôlé avec OTP envoyé par le simulateur : initiation -> `AWAITING_OTP` -> vérification -> posting T0 -> consultation `COMPLETED` ou gestion autoritative `UNKNOWN` ; vérifier qu'aucun second débit n'est créé lors d'un replay.
11. Confirmer logs, métriques, audit, sauvegarde/restauration, rotation des secrets, rollback applicatif et fermeture des ports d'administration.

## 8. Validation locale à effectuer par l'opérateur — non exécutée ici

Depuis la racine du repository :

```powershell
py scripts/verify_spring_configuration_hygiene.py
py scripts/verify_master_prompt_input_manifest.py
py scripts/verify_repository_hygiene.py
cd backend
mvn -pl bootstrap -am "-Dtest=ProductionCloudProfileConfigurationTest" -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl security -am test
mvn -pl payment -am test
mvn verify
cd ..\frontend
npm run verify:sixpay
cd ..
py scripts/verify_baseline.py
```

Tests runtime additionnels après décisions : `GET /actuator/health`, login LDAP positif/négatif, compte non lié/refusé, token M2M invalide/expiré/audience incorrecte, mTLS client absent/refusé, DB inaccessible, Core Banking indisponible, timeout posting `UNKNOWN`, lookup autoritatif et contrôle anti-double-débit. Confirmer l'accès frontal en HTTPS et l'absence de fallback mock. **Aucun de ces contrôles n'est attesté par la seule présence d'un bean ou d'un test.**

## 9. Sauvegarde et restauration du correctif

Le script conserve une copie horodatée des fichiers existants **hors du repository**, dans le dossier parent. Restaurer manuellement les fichiers de cette sauvegarde après arrêt de l'application, puis valider. Le script n'exécute aucun commit, aucune commande Git, ni aucun déploiement.

## 10. Gates de mise en service

G0 : décision cloud, réseau, TLS/mTLS, Vault et accès Core Banking ; G1 : configuration cohérente et tests locaux ; G2 : intégration Keycloak/LDAP + Core Banking/SMS ; G3 : paiement T0 bout en bout et rejets de sécurité ; G4 : reprise et monitoring ; G5 : ouverture contrôlée. **Ne pas lancer Accounting T1 avant preuve de fermeture Payment T0.**

## 11. Complément — paramètres du `.env` local reçu le 8 novembre 2026

Cette section remplace la mention antérieure selon laquelle le `.env` local n'était pas disponible. Les valeurs sensibles n'ont pas été recopiées. La configuration ci-dessous est une **matrice de migration**, pas une preuve de déploiement.

| Propriété locale | Décision de migration vers le profil `production` |
|---|---|
| `DEBUG=false` | Maintenir l'absence de débogage en cloud |
| `LOGGING_LEVEL_ORG_HIBERNATE_SQL=INFO`, `LOGGING_LEVEL_ORG_HIBERNATE_ORM_JDBC_BIND=INFO` | Ne pas activer des logs de paramètres SQL contenant des données sensibles |
| `AMPLITUDE_PAYMENT_BASE_URL` | Remplacer `localhost` par l'adresse interne approuvée du Core Banking, transport TLS selon le contexte |
| `AMPLITUDE_CONFIRMATION_ENABLED=true` | Maintenir ; timeouts locaux 2s/10s conservés comme points de départ à valider |
| `AMPLITUDE_FUNDS_RESERVATION_ENABLED=true` | Maintenir |
| `SIXPAY_PAYMENT_TREASURY_ENABLED=true` | Maintenir uniquement après confirmation des trois comptes institutionnels de simulation ; aucune référence de compte n'est écrite dans le profil |
| `AMPLITUDE_POSTING_ENABLED=true` | Maintenir |
| `AMPLITUDE_COMPENSATION_ENABLED=false` | Maintenir l'état local, ne pas forcer l'activation par les defaults du fragment bancaire |
| `AMPLITUDE_POSTING_STATUS_ENABLED=false` | Maintenir l'état local, sous réserve de validation du besoin de reprise bancaire |
| `SIXPAY_PAYMENT_CALLBACK_ENABLED=true` | Maintenir, mais utiliser une clé de signature propre au cloud |
| `SIXPAY_PAYMENT_OTP_HMAC_KEY_BASE64` | Secret à faire tourner et injecter depuis le gestionnaire sécurisé ; ne jamais copier dans le YAML ou le runbook |
| `SIXPAY_PAYMENT_CALLBACK_SIGNING_KEY_ID`, `SIXPAY_PAYMENT_CALLBACK_SIGNING_SECRET` | Identifiant et secret à provisionner pour l'environnement ; remplacer le secret partagé dans la conversation |
| `AMPLITUDE_*_OAUTH2_ENABLED=false` | Exception locale, **non reprise** comme politique de sécurité cloud |
| `AMPLITUDE_*_MTLS_ENABLED=false` | Exception locale, **non reprise** comme politique de sécurité cloud |
| `AMPLITUDE_*_ALLOW_INSECURE_HTTP=true` | Exception locale, **non reprise** ; aucune autorisation HTTP non chiffré dans un cloud exposé |

Le `.env` ne contient pas de configuration PostgreSQL, LDAP, Keycloak M2M, stockage d'exports d'audit, HMAC de curseur Reporting ni clés de protection ObservedCustomer. Elles doivent provenir d'autres paramètres déjà approuvés de l'environnement ou du gestionnaire de secrets.

### Correctif local préparé

Le script `apply_sixpay_cloud_mvp.py` modifie seulement le profil Spring de production et l'environnement frontend **après comparaison exacte du contenu attendu des fichiers**. Il conserve les profils locaux intacts. Une sauvegarde hors repository est créée avant les écritures. Il ne lit pas le `.env` et ne charge pas ses secrets. Le script n'est pas un installateur cloud : il n'installe ni Vault, ni Keycloak, ni LDAP, ni Core Banking.

Depuis la racine du repository, en plaçant le script hors du dépôt :

```powershell
py ..\apply_sixpay_cloud_mvp.py --repo . --dry-run
py ..\apply_sixpay_cloud_mvp.py --repo . --apply
```

**Point bloquant de sécurité** : l'activation M2M côté API reste volontairement `false` par défaut jusqu'à l'approbation et la vérification du mTLS, du JWT et de l'association client/partenaire. Les valeurs bancaire OAuth2/mTLS doivent être provisionnées et validées avant le paiement réel. Le premier administrateur LDAP n'est pas automatiquement créé par ce script dans `production`, puisque son bootstrap existant est restreint à `preproduction`.

### Commandes de vérification supplémentaires (non exécutées)

```powershell
# Depuis la racine
py scripts/verify_spring_configuration_hygiene.py
cd backend
mvn -pl bootstrap -am test
mvn -pl security -am test
mvn -pl payment -am test
mvn verify
cd ..\frontend
npm run verify:sixpay
```

Critère d'acceptation cloud : un paiement T0 exécuté de bout en bout avec LDAP humain, identité partenaire M2M vérifiée, mTLS, Core Banking sécurisé, OTP délivré via le simulateur, posting et callback observables. L'application du script, à elle seule, ne constitue pas une preuve de ce résultat.
