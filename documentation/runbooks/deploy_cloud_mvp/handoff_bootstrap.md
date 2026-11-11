# SIXPAY CONNECT — Runbook de préparation du premier démarrage sur Contabo

**Date de consolidation :** 10 novembre 2026  
**Projet :** SIXPAY CONNECT — déploiement MVP cloud  
**Repository :** `SIXGEN-Solutions/sixpay-connect`  
**Branche de référence :** `feat/repository-baseline-consolidation-cucumber`  
**HEAD GitHub observé le 10 novembre 2026 :** `4f932d493c85dcb4e4baef20c6209fa096302ec8`  
**Mode du lot :** préparation, installation et validations techniques ciblées sur le VPS. **Aucun démarrage du backend SIXPAY n'a été effectué.**  
**Contexte documentaire :** sources de gouvernance examinées suivant le manifeste ; `MASTER_ENGINEERING_PROMPT.md` obtenu depuis GitHub. Statut `READY_FOR_PROJECT_ANALYSIS` pour la documentation ; cela n'équivaut pas à `REPOSITORY_VALIDATED` ni à la certification opérationnelle de production.

> **Sécurité du document :** aucun mot de passe, clé HMAC, jeton, secret LDAP, valeur sensible ou contenu de secret n'est reproduit. Les chemins de secrets sont indiqués uniquement pour les opérations autorisées. Les commandes ou validations citées comme « confirmées » proviennent des sorties remontées durant la session. Les points non vérifiés restent explicitement ouverts.

## 1. Décision opérationnelle et périmètre

Le déploiement vise une **première mise en service technique contrôlée** du backend SIXPAY sur un VPS Contabo AlmaLinux, puis une ouverture du MVP après validation des prérequis d'exploitation. Le démarrage de l'application peut déclencher **Flyway automatiquement** et modifier la base PostgreSQL : il est soumis à un **GO humain distinct**, non donné à ce stade.

Décisions établies :

- Intégration bancaire **désactivée temporairement** pour le premier démarrage : réservation de fonds, posting T0, confirmation bancaire, vérification client bancaire et callbacks selon les paramètres effectivement supportés par le code. Les fonctionnalités sont destinées à être réactivées après déploiement du Core Banking et validation des contrats/configurations.
- **LDAP doit rester activé**. Utiliser temporairement un Active Directory Samba de préproduction, avec liaison LDAPS réelle testée depuis le VPS ; passer plus tard à l'AD de La Régionale par changement de configuration validé.
- Le Core Banking disponible à ce stade ne fournit pas de serveur OAuth2 : ne pas imposer son activation pour les services bancaires désactivés. **Cela ne préjuge pas du fonctionnement des autres composants de sécurité Spring, à vérifier dans G4.**
- La sauvegarde PostgreSQL G7 peut être différée **uniquement pour un premier démarrage technique sur base jetable sans données à préserver**. Elle reste exigée avant ouverture aux transactions réelles.
- Suivre une **seule opération SSH bornée à la fois**, analyser la sortie, éviter de répéter les contrôles réussis et ne jamais déclarer une commande réussie si son statut est incorrect.

## 2. Infra Contabo — inventaire vérifié

| Composant | Configuration observée |
|---|---|
| VPS | Contabo, système AlmaLinux 10.2, SELinux `enforcing` |
| Ressources | 8 vCPU, ~23 GiB RAM, disque racine XFS ~299 GiB (~290 GiB disponibles à l'inspection), pas de swap |
| Compte SSH opérationnel | `sixpayadmin` (les opérations Docker nécessitant des privilèges passent par `sudo`) |
| Docker Engine | 29.9.3 |
| Docker Compose | 5.7.0 |
| Racine déploiement | `/opt/sixpay/` |
| Compose principal | `/opt/sixpay/compose/docker-compose.yml` |
| Overlay backend | `/opt/sixpay/compose/docker-compose.backend.yml` |
| Overlay LDAP | `/opt/sixpay/compose/docker-compose.ldap.yml` |
| Réseau interne | `sixpay-connect-cloud_sixpay_backend`, Docker `internal=true` |
| Backend SIXPAY | image téléchargée/inspectée ; conteneur applicatif **jamais démarré** |

**Image backend sélectionnée dans Compose et vérifiée :**

```text
ghcr.io/sixgen-solutions/sixpay-backend@sha256:e15f2ec3f4c740a7f7b774a05e9ebe84f066fb7d2132ef511f99323ee035d1d9
```

**Image PostgreSQL sélectionnée :**

```text
docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37
```

Les images ont été confirmées par `docker compose -f docker-compose.yml -f docker-compose.backend.yml config --images`. Cette vérification ne constitue pas une preuve que les images correspondantes tournent toutes : seule PostgreSQL était démarrée parmi ces deux services.

## 3. Finalisation de l'image et du profil de production

### 3.1. Image corrigée et packaging

Un problème significatif d'artefact backend a conduit à une **nouvelle image contenant le véritable JAR applicatif**. L'ancien digest connu était :

```text
sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136
```

Il a été remplacé dans l'overlay Compose par le digest `e15f2ec...035d1d9`, avec les marqueurs antérieurement confirmés :

```text
BACKEND_IMAGE_PATCH=OK
BACKEND_COMPOSE_CONFIG=OK
```

Le contenu de la nouvelle image a été contrôlé : `/opt/sixpay/app.jar`, JAR Spring Boot d'environ 91 MiB, Java Temurin 21.0.13 dans l'image inspectée. Le JAR est **à l'intérieur de l'image Docker** ; il n'est pas nécessaire d'installer ni d'exécuter un JAR directement sur le VPS.

### 3.2. Exécution défensive du backend (configuration préparée, non testée en démarrage)

Configuration constatée lors de la préparation de l'overlay :

- UID d'exécution applicatif `10001` ;
- `read_only: true` pour le système de fichiers racine ;
- aucun port backend publié directement sur l'hôte ;
- réseau Docker interne pour les dépendances ;
- pas de redémarrage automatique activé durant la phase de premier lancement contrôlé ;
- configuration DB et secrets montés via fichiers Docker ;
- montage persistant d'exports d'audit ;
- truststore LDAP JDK monté en lecture seule.

**Attention :** la conformité du Compose rendu a été vérifiée pour les éléments cités, mais aucun test de démarrage complet n'a eu lieu. Ne pas qualifier le profil de production de « prêt » tant que G4 reste ouvert.

### 3.3. Paramétrage Spring/Flyway observé sur GitHub

Dans `backend/bootstrap/src/main/resources/application-production.yml` à la révision de référence :

```yaml
spring:
  flyway:
    enabled: true
    schemas: sixpay
    default-schema: sixpay
    create-schemas: true
    locations: classpath:db/migration
    validate-on-migrate: true
    clean-disabled: true
    out-of-order: true
  datasource:
    url: ${SIXPAY_DB_URL}
    username: ${SIXPAY_DB_USERNAME}
    password: ${SIXPAY_DB_PASSWORD}
    hikari:
      schema: sixpay
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: sixpay
```

**Point important G4 :** le fichier de production déclare également des paramètres de sécurité client OAuth2/SSL liés à l'intégration bancaire. Même si les fonctionnalités bancaires sont désactivées, il faut vérifier les effets réels de la résolution des propriétés et de l'instanciation des beans, sans supposer que les placeholders sont automatiquement ignorés.

## 4. PostgreSQL — G2/G3/G8

### 4.1. Installation, accès, secrets et réseau

Conteneur : `sixpay-connect-cloud-postgres-1` ; PostgreSQL 16.15 ; base `sixpaydb` ; compte de service `sixpay_user` ; compte administrateur `postgres`. La base PostgreSQL utilise un **bind mount de données** vers `/var/lib/postgresql/data` dans le conteneur et un secret monté à `/run/secrets/postgres_admin_password`.

Validations :

- Conteneur PostgreSQL sain ; réseau Docker interne actif.
- Lecture du secret JDBC par l'utilisateur de service du conteneur backend confirmée (G2 : `configtree`, traitement des fins de ligne LF).
- Connexion SQL depuis un **sonde Java temporaire**, authentification JDBC et `SELECT 1` confirmées (G3). Aucun démarrage du backend applicatif.
- Test d'authentification locale administrateur : `postgres|sixpaydb`.
- Privilèges `sixpay_user` sur la base : `CONNECT=true`, `CREATE=true`, `TEMP=true`.
- Compte `sixpay_user` : `LOGIN=true`, `SUPERUSER=false`, `CREATEDB=false`.
- Propriétaire de la base `sixpaydb` : `postgres`.
- Schéma `public` présent, avec `USAGE=true` et `CREATE=false` pour `sixpay_user` ; schéma `sixpay` absent.
- Aucune table utilisateur et aucune table `flyway_schema_history` détectée à l'inspection.

**Conclusion :** le schéma `sixpay` n'a pas à préexister : `spring.flyway.create-schemas=true` et le privilège `CREATE` sur `sixpaydb` permettent sa création, sous réserve de l'exécution effective. L'absence des tables est cohérente avec l'absence de premier démarrage.

### 4.2. G7 — Outillage de sauvegarde

- `pg_dump`, `pg_restore`, `pg_dumpall`, `pg_isready` disponibles **dans** le conteneur PostgreSQL 16.15 ; outils hôte `pg_dump` et `pg_restore` absents.
- Répertoire créé : `/var/backups/sixpay/postgres`, **propriétaire `root:root`, mode `0700`** ; création confirmée par `G7_BACKUP_DIRECTORY=CREATED`.
- Authentification et accès lecture au schéma par `pg_dump` validés (`G7_PG_DUMP_ACCESS=OK`).
- **Aucune sauvegarde persistante créée, aucune restauration testée**, pas de politique de rétention ni de copie hors VPS établie.

G7 n'est **pas terminé** : seule la préparation est validée. Ne pas assimiler le répertoire créé à une sauvegarde restaurable.

## 5. LDAP/LDAPS de préproduction — installation et validation

### 5.1. Objectif et périmètre

Samba Active Directory (AD) de préproduction provisionné sur le même environnement Docker pour tester l'authentification réelle en LDAPS. Cette installation n'est **pas l'Active Directory officiel de La Régionale**. L'intégration finale doit remplacer l'endpoint, le certificat de confiance, l'identité de service et les paramètres de domaine après validation avec l'équipe bancaire.

### 5.2. Identité et exécution du service

| Paramètre | Valeur constatée |
|---|---|
| Image Samba immuable | `instantlinux/samba-dc@sha256:7b569a7f21f86d9f6570de561dd07cb7e3d85ab57da7866b7006a9a932a8feb6` |
| Conteneur | `sixpay-cloud-ldap` |
| Hôte AD | `dc.sixpay.test` |
| Domaine DNS / realm | `sixpay.test` |
| DN racine | `DC=sixpay,DC=test` |
| Compte de service temporaire | `CN=Administrator,CN=Users,DC=sixpay,DC=test` |
| Port LDAP interne | 389/TCP |
| Port LDAPS interne | 636/TCP |
| Ports exposés publiquement | Aucun |
| Sécurité conteneur | `privileged: false`, `no-new-privileges: true`, `cap_add: [SYS_ADMIN]` |
| Hôte | SELinux `enforcing` |
| Réseau | Réseau Docker interne partagé avec PostgreSQL/backend |

La capacité Linux `SYS_ADMIN` a été nécessaire pour le provisionnement SYSVOL/ACL de l'image Samba utilisée. Elle est **large et fait l'objet d'un risque résiduel** ; son remplacement par une exécution moins privilégiée doit être étudié séparément, sans casser l'AD fonctionnel.

### 5.3. Volumes Samba et incident significatif

Volumes nommés :

```text
sixpay-ldap-samba-etc  -> /etc/samba
sixpay-ldap-samba-lib  -> /var/lib/samba
```

**Incident majeur résolu :** le premier provisionnement AD avait échoué avec `NT_STATUS_ACCESS_DENIED` lors des opérations ACL/SYSVOL, faute de capacité `SYS_ADMIN`. Après décision humaine, les **deux volumes de provisionnement partiel** ont été détruits/recréés, la capacité requise ajoutée, puis le provisionnement réexécuté avec succès. Cela correspondait à une première instance de test ; cette opération ne doit **jamais être rejouée** sur une instance contenant des identités/données à préserver.

### 5.4. DNS et vérifications réseau

Vérifications confirmées :

- SOA du domaine depuis le DNS Samba ;
- enregistrement A `dc.sixpay.test` résolu vers l'IP du conteneur sur le réseau interne (observée `172.18.0.3` lors du test, **non contractuelle**) ;
- enregistrements SRV LDAP 389 et Kerberos 88 présents ;
- résolution du FQDN depuis le réseau Docker ;
- port 389 accessible depuis le réseau interne ;
- connexion TLS sur `dc.sixpay.test:636` valide.

**Réserve connue :** Samba a émis un `SERVFAIL` lié au résolveur Docker `127.0.0.11` pendant `dnsupdate` initial ; cela n'a pas empêché les contrôles DNS/LDAPS et JNDI suivants de réussir. Surveiller cet avertissement si des mises à jour dynamiques DNS deviennent nécessaires.

### 5.5. Certificat CA et truststore Java

Autorité de certification de l'AD Samba : `/var/lib/samba/private/tls/ca.pem` dans le conteneur/volume Samba. Connexion LDAPS contrôlée avec validation de chaîne et du nom du pair : `Verification: OK` et `Verified peername: dc.sixpay.test`.

Truststore utilisé par le backend :

```text
Hôte : /opt/sixpay/secrets/ldap/sixpay-ldap-truststore-new.p12
Conteneur backend : /opt/sixpay/trust/ldap-truststore.p12
Alias : sixpay-samba-ad-ca
Format : PKCS12
Propriétaire hôte : root:10001
Mode hôte : 0640
Montage : lecture seule
```

Créé à partir du truststore Java 21 réel : 118 certificats d'origine et 119 après importation de la CA Samba. Le fichier initial `/opt/sixpay/secrets/ldap/sixpay-ldap-truststore.p12` (118 entrées) a été préservé. L'accès UID 10001 au nouveau truststore a été testé avec succès.

Paramètres JVM de confiance déclarés dans Compose :

```text
-Djavax.net.ssl.trustStore=/opt/sixpay/trust/ldap-truststore.p12
-Djavax.net.ssl.trustStoreType=PKCS12
-Djavax.net.ssl.trustStorePassword=[secret de truststore configuré ; ne pas recopier dans le runbook]
```

**Incident significatif résolu :** SELinux a empêché l'écriture du truststore au travers d'un bind mount hôte depuis le conteneur d'outillage. Le certificat a finalement été importé dans un conteneur temporaire doté d'un système de fichiers interne inscriptible, puis copié sur l'hôte avec les droits attendus. Ne pas désactiver SELinux pour contourner ce contrôle.

### 5.6. Secrets LDAP et droits d'accès

Chemins observés :

```text
/opt/sixpay/secrets/ldap/samba-admin-password.txt
  -> secret d'administration Samba ; root:root ; 0600

/opt/sixpay/secrets/backend/sixpay_ldap_service_account_password
  -> copie de service dédiée pour Spring ; root:10001 ; 0640

/opt/sixpay/secrets/ldap/testuser-password.txt
  -> secret du compte de test ; root:root ; 0600
```

Le compte technique UID 10001 **ne pouvait pas lire** le secret d'administration d'origine `0600`; un fichier de service dédié, dans l'arborescence backend, a donc été créé avec `root:10001` et `0640`. Son accès effectif a été validé : `LDAP_SECRET_ACCESS=OK`.

**Point de sécurité :** la copie du mot de passe administrateur en tant que secret de service répond au besoin de test, mais n'est pas une bonne cible de production : provisionner ultérieurement un compte LDAP à privilèges minimaux, restreindre ses droits de lecture et effectuer la rotation du secret. Un mot de passe de préproduction a été divulgué dans l'historique de discussion ; **le considérer comme compromis et le renouveler** avant réutilisation.

### 5.7. Configuration LDAP du backend (préparée dans Compose)

```yaml
SIXPAY_LDAP_ENABLED: "true"
SIXPAY_LDAP_URLS: "ldaps://dc.sixpay.test:636"
SIXPAY_LDAP_BASE_DN: "DC=sixpay,DC=test"
SIXPAY_LDAP_USER_SEARCH_BASE: "CN=Users"
SIXPAY_LDAP_SERVICE_ACCOUNT_DN: "CN=Administrator,CN=Users,DC=sixpay,DC=test"
SIXPAY_LDAP_TRUST_DOMAIN: "sixpay-preproduction-ldap"
SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED: "false"
```

Le mot de passe de liaison est fourni par secret Docker et non inséré en clair dans le runbook. La configuration de filtre/login LDAP est celle du profil applicatif à confirmer lors de G4. Aucune création d'administrateur SIXPAY via bootstrap LDAP n'a été autorisée.

### 5.8. Validation fonctionnelle technique LDAPS

Avec une sonde **Java 21 JNDI autonome** sur le réseau interne, truststore et secret montés :

```text
LDAP_BIND=OK
LDAP_ROOTDSE=OK
JAVA_LDAPS_AUTHENTICATION=OK
LDAP_TESTUSER_BIND=OK
JAVA_LDAPS_TESTUSER_AUTHENTICATION=OK
```

Compte de test créé dans AD Samba : `sixpay.testuser`, sous `CN=Users`, actif (`userAccountControl=512`). L'existence et le bind du compte ont été vérifiés. La validation ne prouve **pas encore** le login complet dans l'application SIXPAY, ni le mapping local des rôles/permissions.

## 6. G6 — Volume d'exports d'audit

Volume Docker nommé : `sixpay-audit-exports` ; `driver=local` ; racine volume `UID=10001`, `GID=10001`, mode `0700`.

Overlay backend :

```text
sixpay-audit-exports:/var/lib/sixpay/audit-exports:rw
```

Le volume est déclaré `external: true`. Le montage est de type `volume`, en lecture/écriture, avec la cible exacte ci-dessus. Tests réels réussis sous UID 10001 dans des conteneurs temporaires utilisant le runtime backend, racine du conteneur en lecture seule et capacités réduites :

```text
AUDIT_PERSISTENCE_WRITE=OK
AUDIT_PERSISTENCE_READ=OK
AUDIT_PERSISTENCE_CLEANUP=OK
AUDIT_PERSISTENCE_VALIDATION=OK
```

La persistance a été vérifiée **entre deux conteneurs éphémères distincts**. Le stockage est techniquement prêt. Restent ouverts : activation/configuration effective des exports dans Spring, durée de conservation, sauvegarde, chiffrement et supervision du volume.

## 7. G8 — Flyway : préparation et inventaire intégral validés

### 7.1. État initial de la base

La base `sixpaydb` comporte le schéma `public`, mais **pas** de schéma `sixpay`, **pas** de table `flyway_schema_history` et **pas** de table applicative au moment de l'inspection. C'est attendu car SIXPAY n'a jamais été démarré sur Contabo.

### 7.2. Inventaire réel de l'image corrigée

Onze JAR applicatifs/modules ont été identifiés dans `BOOT-INF/lib` :

```text
accounting-1.0.0-SNAPSHOT.jar
administration-1.0.0-SNAPSHOT.jar
common-1.0.0-SNAPSHOT.jar
customer-1.0.0-SNAPSHOT.jar
integration-1.0.0-SNAPSHOT.jar
notification-1.0.0-SNAPSHOT.jar
partner-1.0.0-SNAPSHOT.jar
payment-1.0.0-SNAPSHOT.jar
reporting-1.0.0-SNAPSHOT.jar
security-1.0.0-SNAPSHOT.jar
shared-kernel-1.0.0-SNAPSHOT.jar
```

Douze migrations SQL identifiées sous `db/migration/` :

| Module | Migrations embarquées |
|---|---|
| Partner | `V100__partner_baseline.sql` |
| Customer | `V200__customer_baseline.sql` |
| Payment | `V300__payment_baseline.sql`; `V301__payment_state_schema_version_structural_guard.sql`; `V302__payment_financial_snapshots_banking_fields.sql`; `V303__payment_t0_recovery_attempts.sql`; `V304__payment_t0_manual_reconciliation_audit.sql` |
| Accounting | `V400__accounting_baseline.sql` |
| Reporting | `V500__reporting_baseline.sql` |
| Notification | `V600__notification_baseline.sql` |
| Security | `V700__security_baseline.sql` |
| Administration | `V800__administration_baseline.sql` |

Aucune migration SQL sous ce chemin dans `common`, `integration`, `shared-kernel`, ni directement sous `BOOT-INF/classes/db/migration` dans l'archive principale. Aucun doublon de **numéro de version** parmi les douze fichiers listés.

**Limite explicite :** les migrations ont été validées auparavant en local selon l'utilisateur ; la session Contabo a vérifié leur présence et leur versionnement, mais **n'a pas réexécuté ni audité leur SQL et n'a pas exécuté Flyway**. Le marqueur d'inventaire était `G8_COMPLETE_MIGRATION_INVENTORY=OK`, pas un marqueur de succès de migration.

### 7.3. Prérequis G8 confirmés

- Configuration `spring.flyway.schemas= sixpay`, `default-schema=sixpay`, `create-schemas=true`.
- Compte `sixpay_user` en capacité de créer un schéma sur `sixpaydb` (`CREATE=true`).
- Configuration Hibernate `ddl-auto=validate` : Hibernate valide, mais n'est pas chargé de créer les tables.
- Chemin `classpath:db/migration` conforme aux chemins SQL observés dans les JAR internes.

**Statut : `G8 PRÊT POUR EXÉCUTION CONTRÔLÉE`**, et **non** « migrations exécutées ». Premier démarrage / Flyway : **NO-GO maintenu jusqu'au GO explicite**.

## 8. Incidents techniques significatifs et décisions

| Incident / risque | Impact réel | Traitement / état |
|---|---|---|
| Image backend initiale contenant un artefact incorrect | Risque de déployer une application incomplète | Nouvel artefact/image publiés ; digest corrigé dans Compose ; JAR réel et modules embarqués contrôlés |
| Échec initial du provisionnement Samba AD (`NT_STATUS_ACCESS_DENIED` sur SYSVOL/ACL) | AD inutilisable | Ajout explicite `SYS_ADMIN`, recréation des volumes d'instance partielle après autorisation, provisionnement repris et validé ; privilège à réduire ultérieurement |
| Secret LDAP administrateur illisible par UID 10001 | Échec potentiel du bind LDAP du backend | Secret backend dédié `root:10001 0640` ; lecture testée ; rotation et compte de service dédié à prévoir |
| SELinux bloquant la modification du truststore depuis un conteneur temporaire | Import CA LDAPS impossible par bind mount | Import effectué dans le FS interne d'un conteneur d'outillage, puis copie contrôlée ; SELinux préservé |
| Warning `dnsupdate` Samba / résolveur Docker | Risque de mise à jour DNS future | Contrôles DNS/SRV/LDAPS/JNDI réussis ; suivi recommandé |
| Secrets et paramètres obligatoires encore manquants côté Spring production | Risque d'échec de création de beans / démarrage | G4 ouvert : à diagnostiquer sans changer de politique de sécurité sans décision |
| Absence de sauvegarde PostgreSQL restaurable | Pas de protection des données après incident | G7 reportable pour démarrage technique sur base jetable uniquement ; bloquant avant ouverture réelle |

**Non retenus comme incidents majeurs :** erreurs ponctuelles de syntaxe SQL, usage de `/dev/null` avec `pg_dump -Fc`, absence d'outil `jar` dans l'image JRE, montage d'inspection SELinux, sorties trompeuses `Successfully copied 0B`, pipelines shell ayant imprimé un `OK` non probant. Ces erreurs ont concerné les **scripts de diagnostic**, pas l'architecture ou les composants métier SIXPAY.

## 9. Statut consolidé des gates

| Gate / capacité | Statut | Preuve ou réserve |
|---|---|---|
| Fondations VPS / Docker / SELinux | Validées pour les inspections et services préparés | Docker et Compose opérationnels ; SELinux enforcing ; aucune preuve de durcissement complet / audit externe |
| G2 — Configtree / permissions DB | Validé | Secret accessible depuis UID 10001 ; traitement LF vérifié |
| G3 — Authentification JDBC PostgreSQL | Validé | Sonde Java et `SELECT 1` ; base saine |
| G4 — Configuration de démarrage Spring production | **OUVERT — prochaine priorité** | Paramètres obligatoires, flags, secrets, OAuth2/SSL, HMAC et protections des observations clients à finaliser ; login LDAP dans SIXPAY non testé |
| LDAP/LDAPS technique | Validé indépendamment du backend | TLS CA, DNS, bind admin et bind utilisateur Java JNDI |
| G6 — Volume persistant audit | Validé techniquement | Lecture, écriture et persistance inter-conteneurs ; rétention/backup applicatifs ouverts |
| G7 — Préparation backup PostgreSQL | Partiellement validée / reportée | Dossier sécurisé et `pg_dump` accessibles ; aucune archive ni restauration |
| G8 — Préparation Flyway | **Validée techniquement** | 12 migrations en image, droits DB, base non initialisée ; exécution non effectuée |
| Premier démarrage backend | **NO-GO** | Démarrage jamais effectué ; autorisation distincte requise |
| Ouverture MVP aux paiements réels | **NO-GO** | G4, restauration backup, supervision, sécurité et intégrations bancaires à finaliser |

## 10. Plan de continuation — première mise en route technique

### Étape A — Terminer G4 en lecture seule

1. Vérifier le **Compose rendu** et les variables/profils effectivement utilisés par le backend ; ne jamais afficher des valeurs de secret.
2. Contrôler que chaque secret référencé existe, est lisible par UID 10001 et correspond au mécanisme Spring attendu, en évitant toute exposition de valeur.
3. Identifier les propriétés obligatoires du profil de production et la stratégie d'activation des beans OAuth2/SSL lorsque l'intégration bancaire est désactivée ; ne pas contourner la sécurité pour « faire démarrer ».
4. Vérifier le bloc LDAP LDAPS (déjà validé techniquement) et la configuration de truststore, sans réinstaller Samba ni recréer les secrets.
5. Compléter les clés HMAC/protections et paramètres de reporting/customer observation nécessaires, selon les contrats, la configuration actuelle et les règles de sécurité.
6. Produire une liste courte : **bloquant pour startup**, **requis avant transactions réelles**, **reportable**.

### Étape B — Préparer la fenêtre de premier démarrage

1. Confirmer expressément que la base `sixpaydb` peut être traitée comme base initiale jetable, si G7 reste reporté.
2. Établir les commandes de démarrage et d'arrêt, les logs et la stratégie de reprise en cas de migration partielle, sans les exécuter avant approbation.
3. Obtenir le **GO humain explicite** pour démarrer SIXPAY et déclencher Flyway. **Ne pas assimiler le GO d'analyse/planification à un GO de déploiement.**

### Étape C — Après GO explicite seulement

1. Premier démarrage Compose borné, avec les intégrations bancaires prévues désactivées et LDAP activé.
2. Lire les logs Spring, sécurité, LDAP et Flyway ; ne pas confondre `healthy` Docker et disponibilité applicative.
3. Vérifier `flyway_schema_history`, le schéma `sixpay`, les 12 migrations effectivement appliquées, les validations Hibernate et `/actuator/health` selon exposition autorisée.
4. Diagnostiquer les échecs sans réinitialiser ni effacer la base ou les volumes.
5. Pour l'ouverture réelle : sauvegarde et restauration G7, copie externe, rotation des secrets de test, politiques d'accès, audit, observabilité, TLS/frontière réseau, Core Banking et critères d'acceptation métier.

## 11. Commandes de diagnostic réutilisables (lecture seule)

**Image effective Compose :**

```bash
cd /opt/sixpay/compose
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml config --images
```

**Conteneurs en cours :**

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
```

**État schéma Flyway (lecture seule, ne montre pas de données métier) :**

```bash
sudo docker exec --user postgres sixpay-connect-cloud-postgres-1 \
  psql -X -U postgres -d sixpaydb -v ON_ERROR_STOP=1 -Atc \
  "SELECT schemaname || '.' || tablename FROM pg_catalog.pg_tables WHERE tablename='flyway_schema_history';"
```

**Règles d'exploitation :** jamais de `docker compose up`, `flyway migrate`, `psql` DDL/DML, `docker volume rm`, rotation de secret ou modification des permissions sans décision explicite et périmètre validé. Ne pas copier des sorties `docker compose config` intégrales si elles exposent des secrets.

## 12. Sources, traçabilité et limitations

- Gouvernance active : `AGENTS.md`, `ENGINEERING_CONTEXT.md`, `MASTER_PROMPT_INPUT_MANIFEST.yaml`, sources `alwaysLoad`, `MASTER_ENGINEERING_PROMPT.md` (récupéré sur GitHub) ; ne pas utiliser de document historique comme source active.
- Implémentation GitHub consultée à la révision mentionnée : `backend/bootstrap/src/main/resources/application.yml`, `backend/bootstrap/src/main/resources/application-production.yml`, `infrastructure/docker/preproduction/docker-compose.yml` (comparaison de modèle, **pas configuration du VPS**).
- État réel du VPS : **sorties de commandes SSH produites dans la discussion** ; aucun accès SSH direct ni inspection du contenu actuel des fichiers `/opt/sixpay/compose` depuis cette session de rédaction.
- HEAD GitHub de la branche reconfirmé à `4f932d493c85dcb4e4baef20c6209fa096302ec8` lors de la rédaction ; **révision Git locale sur Contabo non observable**, puisqu'aucun worktree Git du VPS n'a été inspecté.
- Les flags, variables et permissions spécifiques doivent être **revérifiés sur la configuration Compose effective** au prochain G4 ; les permissions sensibles ne doivent pas être modifiées sans autorisation.
- **Aucun fichier du repository GitHub modifié ; aucun commit/push/PR ; aucun backend démarré ; aucune migration exécutée durant la préparation du présent runbook.**
