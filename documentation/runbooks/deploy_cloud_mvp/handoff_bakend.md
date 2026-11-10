# SIXPAY CONNECT — Runbook consolidé E1.1–E1.3.2 : Contabo, PostgreSQL, secrets, GHCR et préparation du backend

**Version :** 1.0 — 10 novembre 2026.  
**Nature :** état opérationnel consolidé et transmission, **pas un script d'installation à rejouer**.  
**Objectif :** documenter les opérations déjà réalisées et préparer le premier démarrage contrôlé de SIXPAY CONNECT.  
**Mode :** documentation / lecture seule de GitHub et analyse des preuves d'exécution fournies par l'opérateur ; aucune modification du VPS ou du repository par le rédacteur.  
**Repository :** `SIXGEN-Solutions/sixpay-connect`.  
**Branche source de l'image :** `feat/repository-baseline-consolidation-cucumber`.  
**SHA applicatif de l'image :** `6d29af3bc382964b15dd5e7c3cdf9103dde629ae` (HEAD GitHub vérifié pendant la rédaction).  
**Révision Git du VPS :** non observable et sans objet : les sources ne sont pas clonées sur le VPS.  
**Statut documentaire :** `READY_FOR_PROJECT_ANALYSIS` (sources `alwaysLoad` disponibles avec alias locaux et `MASTER_ENGINEERING_PROMPT.md` et `CONTRACT_REGISTRY.yaml` lus dans GitHub).  
**Validation de l'infrastructure rapportée :** contrôles Docker/Compose/SQL réalisés par l'opérateur ; **aucune validation dynamique backend / Flyway / LDAP**. Ne pas interpréter ce document comme `REPOSITORY_VALIDATED` pour le déploiement.

> **RÈGLE DE REPRISE : NE PAS RÉINSTALLER ET NE PAS REJOUER LES ÉTAPES ACQUISES.** Ne pas regénérer les mots de passe, réinitialiser PostgreSQL, supprimer/recréer les conteneurs, réétiqueter récursivement PGDATA ou démarrer Spring Boot avant un GO distinct. Aucun secret ou token n'est reproduit ici.

## 1. Synthèse exécutoire et état acquis

| Composant | État documenté au dernier contrôle | Preuve / limite |
|---|---|---|
| VPS Contabo / AlmaLinux 10.2 | Préparé | Accès SSH `sixpayadmin` + `sudo` ; SELinux `Enforcing` |
| Docker | **29.9.3** fonctionnel | `sudo docker info --format '{{.ServerVersion}}'` → `29.9.3` |
| Docker Compose | v5.7.0 (précédemment contrôlé) | Compose multi-fichiers reconnu |
| PostgreSQL 16.15 | **Running + healthy** | `docker compose ps` : `Up 14 hours (healthy)` au dernier contrôle communiqué |
| Base `sixpaydb` et rôle `sixpay_user` | **Provisionnés** | Tests d'authentification TCP SCRAM réussis précédemment |
| Secret administrateur PostgreSQL | **Installé** | Fichier protégé sous `/opt/sixpay/secrets` |
| Secret original du rôle applicatif | **Installé** | Stocké côté hôte ; pas de secret en clair dans Compose |
| Copie du secret pour UID Java 10001 | **Installée et protégée** | `UID=10001 GID=10001 MODE=400` au dernier contrôle |
| GitHub Actions Backend CI | **SUCCESS** | Profil Maven `-Passembled-tests`, tests Surefire rapportés |
| GHCR | **Image privée publiée** | Publication manuelle GitHub Actions SUCCESS |
| Image GHCR sur VPS | **Téléchargée + inspectée** | Digest attendu et `linux/amd64` vérifiés |
| Authentification GHCR sur VPS | **Temporaire et nettoyée** | `docker login` OK, logout + suppression du répertoire temporaire |
| Compose PostgreSQL | **Existant et opérationnel** | Préservé sans modification depuis les étapes précédentes |
| Compose complémentaire backend | **Créé + analyse statique réussie** | `config --quiet` code 0 ; services `postgres`, `backend` |
| Backend Spring Boot / Flyway | **NON démarré** | Aucun GO de démarrage ; aucune migration exécutée |
| Injection du mot de passe dans Spring Boot | **NON prouvée** | Le montage Docker et `SPRING_CONFIG_IMPORT` sont configurés, pas testés dans l'application |
| Configuration LDAP, OAuth2, mTLS, secrets métier | **À finaliser** | Prérequis du profil `production` non provisionnés intégralement |

**Périmètre exclu :** `regional-core-banking-api` est une **stack indépendante**. Ne jamais ajouter son service, son stockage ou ses secrets au Compose SIXPAY. Pour le MVP Contabo, la simulation utilise le profil applicatif `production` et des dépendances externes de test convenues, pas la production bancaire réelle.

## 2. Infrastructure déjà préparée — ne pas reprendre l'installation

- Hébergement Contabo VPS (AlmaLinux 10.2 x86_64) ; Docker Engine 29.9.3, Compose v5.7.0, SELinux Enforcing, container-selinux et mode Docker SELinux activés. Préparation SSH/pare-feu réalisée précédemment selon runbook E1.1 ; **ne pas prétendre que chaque règle du pare-feu a été revalidée pendant cette conversation**.
- Opérateur Linux `sixpayadmin` UID/GID `1001:1001`, groupe `wheel`. Socket Docker `/var/run/docker.sock` = `root:docker 660`. `sixpayadmin` n'appartient pas au groupe `docker`; usage retenu : `sudo docker`. `sudo -n` refuse sans mot de passe, `sudo docker info` fonctionne après authentification sudo.
- **Ne pas ajouter l'opérateur au groupe Docker** pour contourner les droits ; ce groupe équivaut à des privilèges élevés sur le VPS.
- Aucun Git, Maven ni code source SIXPAY à installer sur le serveur : CI construit, GHCR distribue, Docker exécute.
- Projet Docker Compose : `sixpay-connect-cloud`. Réseau existant : `sixpay-connect-cloud_sixpay_backend` (`bridge`, `internal: true`, précédemment observé sur `172.18.0.0/16`). L'adresse IP dynamique du conteneur PostgreSQL **n'est pas** une valeur de configuration ; utiliser DNS Docker `postgres`.
- Absence de publication des ports PostgreSQL et backend vers l'hôte ; aucune exposition publique au dernier contrôle.

### Arborescence hôte — état attendu constaté

```text
/opt/sixpay/
├── compose/
│   ├── docker-compose.yml                 # PostgreSQL, conservé
│   ├── docker-compose.yml.pre-selinux-fix # sauvegarde à conserver
│   └── docker-compose.backend.yml         # Ajouté, statiquement validé ; backend non démarré
└── secrets/                                # root:root, 700
    ├── postgres_admin_password            # root:root, 600
    ├── sixpay_app_password                # root:root, 600
    └── backend/                            # root:root, 700
        └── sixpay_db_password             # UID/GID 10001:10001, mode 400
/var/lib/sixpay/postgres/data/             # PGDATA persistant : ne pas effacer/relabel récursivement
```

## 3. PostgreSQL — installation, droits et état final

### 3.1 Image et service

Image officielle épinglée par digest :

```text
docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37
```

Service : `sixpay-connect-cloud-postgres-1`. DB : `sixpaydb`. Rôle administrateur SQL : `postgres`. Rôle applicatif : `sixpay_user`. PGDATA bind mount persistant : `/var/lib/sixpay/postgres/data:/var/lib/postgresql/data:Z`.

**État SQL acquis avant toute migration :** `sixpaydb` existe. `sixpay_user` a `LOGIN`, n'est pas `SUPERUSER`, `CREATEDB`, `CREATEROLE` ou `REPLICATION`; mot de passe stocké en SCRAM-SHA-256, authentification **TCP** réussie sur le réseau Docker (test `SIXPAY_SCRAM_AUTH_OK`, exit 0). `CONNECT` et `TEMP` sont disponibles sur la base ; privilège temporaire `CREATE` sur `sixpaydb` permettant au migrateur initial de créer le schéma. Réexaminer `CREATE` après Flyway sans le retirer avant les migrations nécessaires.

**Lors du dernier preflight Flyway antérieur :** schéma `sixpay` et `sixpay.flyway_schema_history` absents. **Aucun contrôle SQL plus récent n'a été exécuté ici**, mais aucun démarrage backend n'a eu lieu. Ne créer ni schéma, ni tables, ni historique Flyway à la main.

### 3.2 Incident SELinux MCS résolu définitivement dans ce chantier

Un incident antérieur provenait de catégories MCS différentes sur le processus PostgreSQL et PGDATA. La correction effective a été le montage court Docker `:Z` ci-dessus ; PostgreSQL a ensuite démarré et atteint `healthy`. Labels observés alors : processus `container_t:s0:c356,c769` et PGDATA `container_file_t:s0:c356,c769`. Le chemin Compose de sauvegarde `.pre-selinux-fix` est conservé.

**Interdictions :** pas de retour à la syntaxe de montage défectueuse ; pas de `restorecon -R`/`chcon -R` sur PGDATA ; pas de `docker compose down -v`, pas de suppression de `/var/lib/sixpay/postgres/data`, pas de réinitialisation SQL.

### 3.3 Configuration Compose PostgreSQL exacte observée

```yaml
name: sixpay-connect-cloud
services:
  postgres:
    image: docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37
    restart: unless-stopped
    environment:
      POSTGRES_DB: sixpaydb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD_FILE: /run/secrets/postgres_admin_password
    secrets:
      - postgres_admin_password
    volumes:
      - /var/lib/sixpay/postgres/data:/var/lib/postgresql/data:Z
    networks:
      - sixpay_backend
    healthcheck:
      test:
        - CMD-SHELL
        - 'pg_isready -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
    security_opt:
      - no-new-privileges:true
secrets:
  postgres_admin_password:
    file: /opt/sixpay/secrets/postgres_admin_password
networks:
  sixpay_backend:
    driver: bridge
    internal: true
```

**Note :** les contrôles PostgreSQL `pg_hba.conf` précédents montraient `trust` local loopback et `scram-sha-256` sur les connexions réseau ; TLS PostgreSQL était `off`. Le durcissement est distinct, à planifier selon l'architecture d'exploitation ; il ne justifie pas de rejouer le provisionnement.

## 4. Secrets : identité root Linux, administrateur SQL, utilisateur applicatif, backend

**Vocabulaire :** `root` Linux est un compte OS d'administration, pas un rôle SQL. Le compte SQL administrateur est `postgres` ; **le backend ne doit jamais se connecter comme `postgres`**. `sixpay_user` est le rôle JDBC ; UID `10001` est l'identité Linux **dans le conteneur Java**, différente du rôle SQL.

| Identité / usage | Secret hôte | Propriétaire/permissions | Destination / rôle |
|---|---|---|---|
| Admin SQL `postgres` | `/opt/sixpay/secrets/postgres_admin_password` | `0:0`, `600` | PostgreSQL via `/run/secrets/postgres_admin_password`; initialisation et administration DB |
| Rôle SQL `sixpay_user` — source | `/opt/sixpay/secrets/sixpay_app_password` | `0:0`, `600` | Source de vérité du mot de passe applicatif sur VPS ; **pas exposée au conteneur** |
| Rôle SQL `sixpay_user` — copie destinée au backend | `/opt/sixpay/secrets/backend/sixpay_db_password` | `10001:10001`, `400` | Spring Boot via secret Docker nommé `SIXPAY_DB_PASSWORD` |

Secrets conservés sous `/opt/sixpay/secrets` (700 root:root), sous-dossier backend (700 root:root). Les trois fichiers avaient été observés d'une taille de **65 octets** (64 caractères base64 suivis de LF). La copie backend et l'original ont été comparés sans révéler le contenu ; égalité vérifiée. Les contextes SELinux persistants du **fichier** sont `container_file_t:s0` ; règles `semanage fcontext` déjà mises en place, vérifiées avec `matchpathcon -V`. Ne pas les recréer ni réétiqueter tout l'arbre de secrets.

**Démonstration déjà réalisée :** un conteneur de test temporaire, exécuté comme `10001:10001`, `--network none`, `--read-only`, `--cap-drop ALL`, `--security-opt no-new-privileges`, a lu le secret backend monté individuellement en lecture seule **sans `:Z`** sous SELinux Enforcing : `BACKEND_SECRET_READ_OK`, code 0. Le vrai conteneur backend n'a pas encore été démarré ; son `ProcessLabel` reste à examiner au moment autorisé.

**Ne pas** afficher le secret avec `cat`, `echo`, `sha256sum` dans des journaux partagés ; ne pas mettre de mot de passe dans une variable Compose, dans les arguments Java, dans GitHub, dans des captures ou dans ce document. Aucune rotation ou régénération n'est nécessaire au point de reprise.

## 5. Chaîne d'intégration GitHub → GHCR → Contabo : **terminée jusqu'au pull**

### 5.1 GitHub Actions et contrôle qualité

- Branche applicative sélectionnée : `feat/repository-baseline-consolidation-cucumber`, commit d'image `11f106a289d6d018a575506284a03c78bb701cd5`.
- Réparation d'un défaut Maven dans `backend/tests` : CI avec `mvn --batch-mode --no-transfer-progress -Passembled-tests clean verify`. Tous les modules ont réussi localement ; 516 rapports Surefire XML observés. `Backend CI` GitHub ensuite **SUCCESS**. Les tests d'intégration assemblés nécessitant le gate dédié ne sont pas automatiquement démontrés par la seule commande Maven : ne pas confondre les gates.
- Workflow `.github/workflows/backend-image-publish.yml` intégré à la branche par défaut `main` via un **transfert Git ciblé d'un seul fichier**, sans fusionner la longue branche de développement. Déclenchement **manuel** `workflow_dispatch`; l'opérateur sélectionne la branche, GitHub fige `github.sha`, tests Maven avant build/push. Pas de saisie de SHA ni de publication à chaque push. Pas de déploiement automatique.
- Workflow GitHub Actions `Backend image publish (GHCR)` : exécution `42842816703` **SUCCESS** le 10 novembre 2026 ; Maven gate, Docker Buildx, login et publication, relevé digest tous SUCCESS. Voir : https://github.com/SIXGEN-Solutions/sixpay-connect/actions/runs/42842816703.

### 5.2 Référence exacte validée

```text
Nom image : ghcr.io/sixgen-solutions/sixpay-backend
Tag : sha-11f106a289d6d018a575506284a03c78bb701cd5
Digest : sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136
Référence immuable : ghcr.io/sixgen-solutions/sixpay-backend@sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136
Plateforme : linux/amd64
```

Le digest a été retrouvé dans les logs GHA puis vérifié sur le VPS. Docker `image inspect` a renvoyé `linux/amd64` et l'ID image attendu. **Ne pas builder sur le VPS ni substituer le tag mutable à ce digest dans le Compose.**

### 5.3 Authentification GHCR privée sur Contabo — réalisée et nettoyée

- Package GHCR privé ; token GitHub classique, permission `read:packages`, utilisé **uniquement pour le téléchargement**. Aucun token n'est reproduit.
- `sixpayadmin` ne peut pas utiliser directement `/var/run/docker.sock` (socket `root:docker 660`, groupe `docker` absent) ; `sudo docker` fonctionne après mot de passe sudo. **Ne pas ajouter `sixpayadmin` au groupe docker.**
- Première authentification en utilisateur non privilégié réussie mais `docker pull` refusé par le socket ; la véritable récupération a été faite via `sudo docker` et un répertoire de configuration Docker temporaire root-only : `/run/sixpay-ghcr.XXXXXXXX`.
- Login `sudo docker --config "$AUTH_DIR" login ghcr.io -u <identifiant-github> --password-stdin` réussi ; image téléchargée par digest ; logout temporaire ; suppression du répertoire ; `unset AUTH_DIR` ; `docker logout ghcr.io` sous `sixpayadmin`. Aucune identification GHCR permanente requise pour utiliser l'image **déjà téléchargée**.
- Les messages Docker relatifs au stockage non chiffré des identifiants ont été traités en évitant un stockage permanent. Pour de futurs pulls, reproduire seulement une authentification temporaire ou mettre en place un credential helper approuvé, pas d'exposition du token.

## 6. Fichiers Compose réellement présents et validés, backend **non lancé**

### 6.1 Compose complémentaire backend (état communiqué)

Chemin : `/opt/sixpay/compose/docker-compose.backend.yml`.

```yaml
services:
  backend:
    image: ghcr.io/sixgen-solutions/sixpay-backend@sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136
    user: "10001:10001"
    restart: "no"
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: production
      SPRING_CONFIG_IMPORT: configtree:/run/secrets/
      SIXPAY_DB_URL: jdbc:postgresql://postgres:5432/sixpaydb
      SIXPAY_DB_USERNAME: sixpay_user
      SERVER_PORT: "8080"
      SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED: "false"
    secrets:
      - SIXPAY_DB_PASSWORD
    networks:
      - sixpay_backend
    security_opt:
      - no-new-privileges:true
    cap_drop:
      - ALL
    read_only: true
    tmpfs:
      - /tmp:rw,nosuid,nodev,size=128m
secrets:
  SIXPAY_DB_PASSWORD:
    file: /opt/sixpay/secrets/backend/sixpay_db_password
```

**La présence réelle a été confirmée** après une première tentative `no such file or directory` puis création sur le VPS. Ne pas demander de le recréer.

### 6.2 Validation statique réalisée sur le VPS

Ces deux commandes ont **réussi** (sorties rapportées par l'opérateur) :

```bash
sudo docker compose -p sixpay-connect-cloud \
  -f /opt/sixpay/compose/docker-compose.yml \
  -f /opt/sixpay/compose/docker-compose.backend.yml config --quiet

sudo docker compose -p sixpay-connect-cloud \
  -f /opt/sixpay/compose/docker-compose.yml \
  -f /opt/sixpay/compose/docker-compose.backend.yml config --services
# postgres
# backend
```

Un troisième contrôle en lecture seule `config --format json` puis projection Python **a aussi réussi** :

```text
Image: ghcr.io/sixgen-solutions/sixpay-backend@sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136
Profil: production
Secrets: ['SIXPAY_DB_PASSWORD']
Réseaux: ['sixpay_backend']
Ports publiés: []
```

**Périmètre de preuve :** validité **syntaxique et de composition**, pas la lisibilité effective du secret par l'application Java ni sa connexion JDBC ; `docker compose config` **ne démarre aucun service**.

## 7. Passage du mot de passe PostgreSQL au backend — état exact et validation restante

```text
Hôte : /opt/sixpay/secrets/backend/sixpay_db_password (UID=10001:GID=10001, 400)
       ↓ secret Compose nommé SIXPAY_DB_PASSWORD (fichier individuel, RO)
Conteneur : /run/secrets/SIXPAY_DB_PASSWORD
       ↓ SPRING_CONFIG_IMPORT=configtree:/run/secrets/ (hypothèse)
Spring Boot : ${SIXPAY_DB_PASSWORD} → spring.datasource.password
       ↓
JDBC : jdbc:postgresql://postgres:5432/sixpaydb, user sixpay_user
```

**Déjà prouvé :** existence, permissions et lecture UID 10001 dans un conteneur technique ; TCP PostgreSQL avec le mot de passe correspondant testé précédemment.

**NON prouvé :** résolution effective `configtree` sur l'image Java publiée, traitement du saut de ligne terminal du fichier 65 octets, auth JDBC réelle depuis l'image, SELinux MCS du vrai backend, ou réussite de Flyway.

**Précaution :** une entrée Docker Compose `secrets` n'établit pas automatiquement une variable d'environnement. Le `SPRING_CONFIG_IMPORT` peut permettre à Spring Boot de résoudre un fichier sous le nom `SIXPAY_DB_PASSWORD`, mais ce comportement doit être validé dans le contexte réel **sans démarrer l'application ni lancer les migrations**. Ne pas publier la valeur, ne pas la copier dans `environment:` ni lui attribuer un résultat de test non exécuté. Toute stratégie alternative qui modifie le code ou le mécanisme de gestion des secrets doit être évaluée et approuvée.

## 8. Profil `production` — prérequis **avant** tout `compose up`

GitHub `backend/bootstrap/src/main/resources/application-production.yml` au SHA de l'image : profil volontairement autonome, **sans** `spring.config.import` dans le fichier. `SPRING_CONFIG_IMPORT` est fourni comme propriété externe dans Compose. Configuration existante :

- **Datasource/Flyway :** `SIXPAY_DB_URL`, `SIXPAY_DB_USERNAME`, `SIXPAY_DB_PASSWORD`; Flyway `enabled=true`, `schemas/default-schema=sixpay`, `create-schemas=true`, `validate-on-migrate=true`, `clean-disabled=true`, `out-of-order=true`, Hibernate `ddl-auto=validate`. **Premier démarrage = potentiel changement SQL.**
- **LDAP :** activé par défaut, URLs, base DN, filtres, compte de service et mot de passe nécessaires ; TLS/LDAPS pour les scénarios de simulation convenus. Le bootstrap `SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED` reste à **false** jusqu'à la procédure unique explicitement autorisée ; runner désormais indépendant du profil Spring et paramétré par propriété.
- **Bancaire :** `AMPLITUDE_PAYMENT_BASE_URL`, OAuth2 token URI + client ID/secret, keystore/truststore et mots de passe PKCS12, paramètres de confirmation ; fonds réservation, posting et confirmation activés par défaut, avec attentes de sécurité OAuth2/mTLS. T1 compensation désactivée par défaut.
- **Callbacks :** `SIXPAY_PAYMENT_CALLBACK_SIGNING_KEY_ID` et `SIXPAY_PAYMENT_CALLBACK_SIGNING_SECRET` (callbacks activés par défaut).
- **Customer :** URL `CORE_BANKING_BASE_URL`, `SIXPAY_CUSTOMER_OBSERVATION_PROTECTION_KEY_BASE64` et `SIXPAY_CUSTOMER_OBSERVATION_CURSOR_KEY_BASE64`, verification bancaire activée par défaut.
- **Reporting :** `SIXPAY_REPORTING_AUDIT_CURSOR_HMAC_KEY`, `SIXPAY_REPORTING_AUDIT_EXPORT_RETRIEVAL_BASE_URI` et éventuel répertoire d'exports avec stockage compatible `read_only`.
- **Communication et réseau :** le backend est uniquement connecté à `sixpay_backend`, défini `internal: true`. **Un conteneur sur ce seul réseau ne peut pas être présumé capable de joindre LDAP, OAuth2 ou les API bancaires externes.** Concevoir, valider et approuver une sortie réseau contrôlée vers les services de simulation, sans exposer PostgreSQL ni le backend publiquement. Ne pas ouvrir de ports sans décision.
- **Volumes et écriture :** le conteneur est `read_only: true` avec tmpfs `/tmp`; vérifier si la configuration du runtime requiert d'autres chemins persistants (exports audit, caches, journaux, certificats). Ne pas changer la politique d'écriture sans évaluation.

**Point architectural majeur :** une syntaxe Compose valide n'est pas une configuration de production complète. Ne pas renseigner des valeurs de fortune ni désactiver silencieusement les protections pour forcer le démarrage. Une simulation doit être explicite et conserver les contrôles de sécurité pertinents.

## 9. Point de reprise **E1.3.2** — travail strictement restant

**Position exacte :** immédiatement après la réussite de l'inventaire Compose JSON (`Image`, `Profil production`, `Secrets`, `Réseaux`, `Ports publiés []`). **Ne plus demander de revalider Docker, PGDATA, SCRAM, UID 10001, pull GHCR ou présence du fichier Compose.**

**Ordre conseillé, sans déploiement :**

1. **Établir l'inventaire des prérequis externes du profil `production`** depuis le YAML du SHA d'image : distinguer requis au démarrage, requis à l'activation d'une capacité et requis aux tests fonctionnels. Ne pas inventer de services ni de credentials.
2. **Définir une source de configuration non secrète et les montages de secrets** (LDAP, OAuth2, mTLS, callback, Customer, Reporting), leurs chemins, propriétaires, permissions et stratégie SELinux. Une proposition n'est pas un provisionnement réel.
3. **Concevoir le test isolé de résolution du secret PostgreSQL** par l'image Java (version de Spring Boot), notamment le LF final, *sans démarrage Spring/Flyway* ; faire valider la procédure avant d'agir.
4. **Résoudre le besoin réseau d'egress** vers les dépendances de simulation : réseau interne DB inchangé, sortie contrôlée pour backend, aucun port public par défaut. Toute modification réseau demande décision explicite.
5. **Préparer l'exploitation initiale** : contrôle backup PostgreSQL + restauration vérifiable, vérification des certificats/truststores, procédure rollback (image par digest), healthcheck, logs, supervision, gestion des identifiants.
6. **Gate GO/NO-GO explicite** avant premier `docker compose up` ciblé backend, puisque Flyway est actif. Au GO seulement, vérifier connexion JDBC, état des migrations Flyway, validation Hibernate, santé backend, sans fuite de secrets. Bootstrap administrateur LDAP dans une opération ultérieure séparée.

## 10. Commandes de contrôle non destructrices — références, **pas une checklist à rejouer**

```bash
# Était déjà réussi :
sudo docker info --format '{{.ServerVersion}}'
sudo docker compose -p sixpay-connect-cloud -f /opt/sixpay/compose/docker-compose.yml ps
sudo stat -c '%n UID=%u GID=%g MODE=%a' /opt/sixpay/secrets/backend/sixpay_db_password
sudo docker image inspect 'ghcr.io/sixgen-solutions/sixpay-backend@sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136' --format '{{.Os}}/{{.Architecture}} {{.Id}}'
sudo docker compose -p sixpay-connect-cloud -f /opt/sixpay/compose/docker-compose.yml -f /opt/sixpay/compose/docker-compose.backend.yml config --quiet
sudo docker compose -p sixpay-connect-cloud -f /opt/sixpay/compose/docker-compose.yml -f /opt/sixpay/compose/docker-compose.backend.yml config --services
```

**Commandes interdites sans approbation spécifique :** `docker compose up` backend, `down -v`, `docker volume rm`, `rm -rf /var/lib/sixpay`, régénération de credentials, modification du rôle SQL, migration manuelle, relabel récursif, ouverture d'un port public, commit/merge/push et changement du plan de réseau ou de sécurité.

## 11. Risques et décisions ouvertes

- Validation réelle de `configtree` et du LF final de `SIXPAY_DB_PASSWORD` ; éviter l'impression de secrets.
- Conformité des autres prérequis de `production` et stratégie de montage/permissions ; aucune substitution de secrets de test non approuvée.
- Sortie réseau du backend depuis le réseau exclusivement interne ; aucune connectivité externe démontrée.
- Backup/restauration PostgreSQL, traitement du droit SQL `CREATE`, règles `pg_hba.conf` loopback `trust`, éventuel TLS réseau interne.
- Règles applicatives `read_only`, volumes d'exports, healthcheck et architecture d'exposition future.
- Tests d'intégration assemblés : ne pas confondre pipeline Maven de publication et totalité des gates de production.
- Secret d'administration GitHub : authentification de pull réalisée puis nettoyée ; aucune configuration de credential permanent décidée.

## 12. Traçabilité, modifications et validation

**Sources utilisées :** résultats d'opérations fournis dans la conversation ; runbooks de transmission précédents relus comme traces de chantier (leurs anciens états de blocage sont **supplantés** par les contrôles ultérieurs) ; `AGENTS.md`, `ENGINEERING_CONTEXT.md`, `MASTER_PROMPT_INPUT_MANIFEST.yaml`, sources `alwaysLoad` avec alias, `MASTER_ENGINEERING_PROMPT.md` et `CONTRACT_REGISTRY.yaml` sur GitHub ; `backend/Dockerfile` et `application-production.yml` à la révision d'image.

**Actions réalisées par cette rédaction :** création du présent document seulement. Aucun changement GitHub, VPS, SQL, Docker, secret, GitHub Actions ou GHCR.

**Validations exécutées par l'opérateur :** `sudo docker info`, tests PostgreSQL/SCRAM antérieurs, Docker image pull + inspect digest/architecture, vérification UID/GID/mode, Compose `ps`, Compose `config --quiet`, `config --services`, `config --format json`. Les succès sont attribués à leurs sorties réellement rapportées. **Non exécutés :** démarrage backend, test Spring Boot des secrets, auth LDAP, connexion bancaire, Flyway, restauration backup, contrôle dynamique de l'egress.
