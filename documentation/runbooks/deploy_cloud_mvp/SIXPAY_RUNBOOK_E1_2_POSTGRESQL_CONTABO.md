# SIXPAY CONNECT — Runbook E1.2 : Préparation PostgreSQL 16.15 sur VPS Contabo

**Version :** 1.0 — état arrêté au contrôle **E1.2.3.C.11.C**, 10 novembre 2026, heure observée dans les journaux du VPS (CET).  
**Statut opérationnel :** **INCIDENT SELINUX MCS — POSTGRESQL ARRÊTÉ — NO-GO pour toute relance**.  
**Statut de contexte documentaire du projet :** `INCOMPLETE_CONTEXT` pour une analyse *autoritative intégrale* : les sources obligatoires `MASTER_ENGINEERING_PROMPT.md` (alias `SIXPAY_MASTER_ENGINEERING_PROMPT.md`) et `documentation/contracts/CONTRACT_REGISTRY.yaml` (alias `CONTRACT_REGISTRY.yaml`) n'étaient pas accessibles dans les pièces disponibles lors de la rédaction. Ce document est une transcription technique des actions VPS effectivement rapportées, pas une validation complète du repository.  
**Révision applicative de référence, précédemment vérifiée :** `SIXGEN-Solutions/sixpay-connect`, branche `feat/repository-baseline-consolidation-cucumber`, SHA `f0bbf910ca84f4a1ba636469289ea6651335f963`. Ce SHA n'a pas été revérifié pendant cette rédaction. **Aucun worktree Git accessible sur le VPS n'a été présenté.**  
**Nature :** runbook historique + procédure de diagnostic et reproduction, rédigé hors du VPS ; aucune commande de ce document n'a été exécutée par le rédacteur.  
**Avertissement :** Les commandes d'installation/création ci-dessous retracent les étapes déjà exécutées. **Ne les rejouez pas sur le VPS actuel**, surtout celles qui créent fichiers, secrets ou conteneurs. Ne reprendre que la section « Point de reprise ».

## 0. Périmètre et principes

- Le déploiement est celui du **MVP / environnement de simulation SIXPAY CONNECT**, et non de Regional Core Banking API. Le Core Banking est une stack indépendante et ne doit être ni installé ni co-déployé avec SIXPAY.
- Serveur : VPS Contabo, AlmaLinux 10.2 x86_64 ; Docker Engine `29.9.3` ; Compose `v5.7.0` ; PostgreSQL officiel `16.15-trixie` sur `linux/amd64` ; SELinux **Enforcing**, Docker SELinux activé, `container-selinux 2.248.0` installé.
- Accès opérateur : `sixpayadmin` avec `sudo`. Pour la préparation générale SSH, pare-feu et durcissement de la machine, consulter le document distinct `SIXPAY_RUNBOOK_E1_1_PREPARATION_CONTABO_ALMALINUX(1).md`. Ce runbook E1.2 complète E1.1 et ne prétend pas rejouer l'ensemble des procédures SSH/pare-feu.
- Aucun mot de passe, secret ou contenu du fichier secret n'est reproduit ici. **Ne pas copier des secrets dans un prompt, une issue ou un dépôt Git.**

## 1. Architecture de stockage et isolation recherchées

```text
/opt/sixpay/                         755 0:0
├── compose/                         755 0:0
│   └── docker-compose.yml           644 0:0
└── secrets/                         700 0:0
    └── postgres_admin_password      600 0:0, container_file_t:s0
    # sixpay_app_password            NON CRÉÉ

/var/lib/sixpay/                     755 0:0
└── postgres/                        755 0:0
    └── data/                        700 999:999, container_file_t:s0:c201,c399

Docker Compose project: sixpay-connect-cloud
Docker network: sixpay-connect-cloud_sixpay_backend (bridge, internal:true)
Docker service: postgres
Database prévue: sixpaydb
```

**Attention à `999` sur AlmaLinux :** UID/GID `999` est nommé `systemd-oom` sur le VPS (`getent passwd 999` et `getent group 999`) mais correspond à `postgres` dans l'image Docker. Sur l'hôte, aucun processus avec EGID 999 n'était apparu au contrôle du 10 novembre ; cela ne garantit pas son absence future.

### Séparation des comptes et privilèges : décision approuvée

| Identité | Usage | Permissions cibles | État |
|---|---|---|---|
| `root` hôte + `sixpayadmin` via sudo | Administration OS, Docker et secrets | Accès contrôlé aux fichiers hôtes | En usage |
| `postgres` (rôle SQL administrateur) | Initialiser et administrer la base `sixpaydb`; provisionner `sixpay_user` | Superutilisateur PostgreSQL, **jamais** datasource de SIXPAY | Secret créé ; rôle non encore vérifié en fonctionnement |
| `postgres` (UID/GID 999 dans le conteneur) | Processus système PostgreSQL | Propriétaire de `PGDATA`; distinct du rôle SQL même si homonyme | UID/GID vérifié |
| `sixpay_user` (rôle SQL applicatif) | Datasource Spring Boot et Flyway **initialement partagée** | `LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION`; `CONNECT` à `sixpaydb`; `CREATE` sur base seulement pour la création initiale du schéma `sixpay`, puis réduction des droits à décider/tester | **Non créé, secret non créé** |

**Option B approuvée :** initialiser d'abord PostgreSQL avec l'entrypoint officiel et le rôle administrateur `postgres`, puis provisionner séparément `sixpay_user` après vérification du service. **Aucun script `initdb` n'a été déployé** et aucun wrapper custom n'est retenu. Le mécanisme sécurisé de transmission du mot de passe applicatif au provisionnement reste **à définir et valider**. Ne pas inclure le mot de passe SQL dans des arguments CLI, l'historique shell ou les logs.

### Séquence prévue après résolution de l'incident

1. Corriger et valider le label SELinux privé de PGDATA, sans détruire les fichiers.
2. Démarrer PostgreSQL, attendre le statut `healthy`, vérifier `sixpaydb` et les journaux.
3. Créer le secret `sixpay_app_password` avec permissions/relabel persistants, de façon contrôlée.
4. Provisionner `sixpay_user` par une procédure SQL approuvée et protégée contre la divulgation de secret ; vérifier que le rôle n'est pas superutilisateur.
5. Vérifier la connexion avec ce rôle depuis le réseau interne ; garder PostgreSQL non exposé sur des ports hôtes.
6. Démarrer SIXPAY seulement ensuite : datasource `SIXPAY_DB_URL`, `SIXPAY_DB_USERNAME`, `SIXPAY_DB_PASSWORD`; Flyway crée le schéma `sixpay` et applique les migrations (config connue : `create-schemas:true`, `default-schema:sixpay`, `validate-on-migrate:true`, `clean-disabled:true` ; Hibernate `ddl-auto:validate`). Ces propriétés sont fondées sur l'analyse de travail antérieure, non revérifiées au SHA courant dans ce rapport.

## 2. Préparation Docker et SELinux — opérations déjà accomplies

**Observations rapportées :** Docker 29.9.3 et Compose 5.7.0 opérationnels ; Docker SELinux activé et système `Enforcing`. Un fichier `/etc/docker/daemon.json` contenant `{"selinux-enabled":true}` a été configuré, vérifié, puis le daemon redémarré ; `hello-world` a fonctionné. Les paramètres de confinement Docker observés comprennent SELinux et seccomp. Ce rapport ne prétend pas reproduire le log intégral de l'installation Docker (couvert par la préparation E1.1).

**Contrôles utiles non destructifs :**

```bash
docker --version
docker compose version
sudo docker info --format '{{json .SecurityOptions}}'
getenforce
rpm -q container-selinux
sudo cat /etc/docker/daemon.json
```

**Image choisie et contrôlée :**

```text
docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37
```

La sélection a été confirmée pour `linux/amd64` via manifest et `docker image inspect` ; `id postgres` dans l'image a montré `999:999`. Le `PGDATA` de l'image 16 utilisée est `/var/lib/postgresql/data`.

**Commandes de référence (utilisées lors de la préparation ; ne pas les interpréter comme nécessaires à rejouer) :**

```bash
sudo docker manifest inspect postgres:16.15-trixie --verbose
sudo docker pull docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37
sudo docker run --rm --network none --entrypoint /bin/sh \
  docker.io/library/postgres@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37 \
  -c 'id postgres; echo "PGDATA=$PGDATA"'
```

**Stockage hôte déjà créé** (commandes de création de référence, pas à rejouer) :

```bash
# Les répertoires parents avaient été créés séparément.
sudo install -d -m 700 -o 999 -g 999 /var/lib/sixpay/postgres/data
sudo install -d -m 755 -o root -g root /opt/sixpay/compose
sudo install -d -m 700 -o root -g root /opt/sixpay/secrets

sudo stat -c '%a %u:%g %n' \
  /opt/sixpay /opt/sixpay/compose /opt/sixpay/secrets \
  /var/lib/sixpay /var/lib/sixpay/postgres /var/lib/sixpay/postgres/data
sudo ls -ldZ /var/lib/sixpay/postgres/data
```

Les tests de bind-mount antérieurs ont validé l'écriture temporaire en conteneur avec `:Z` et la suppression du marqueur de test. **Ceci n'assure pas que les catégories MCS du volume soient compatibles avec un nouveau conteneur ultérieur ; l'incident C.11 le démontre.**

## 3. Investigation des secrets sous SELinux — tests fictifs réalisés

Les essais ont utilisé uniquement :

```text
/tmp/sixpay-secret-test/             700 root:root
/tmp/sixpay-secret-test/test_secret  valeur fictive SIXPAY_SECRET_TEST_ONLY
```

Résultats successifs observés :

| Test | Résultat |
|---|---|
| Fichier fictif `600 root:root`, type `user_tmp_t` | Refus SELinux même sous UID 0 ; AVC `container_t` → `user_tmp_t` |
| Après `sudo chcon -t container_file_t /tmp/sixpay-secret-test/test_secret` | UID 0 lit le secret avec succès |
| `600 root:root`, UID 999 | Refus attendu, code de test `0` |
| `640 root:999`, UID 999 | Lecture réussie, mais collision potentielle avec groupe hôte `systemd-oom` |
| Retour à `600 root:root` | Configuration retenue pour le secret source |
| tmpfs `64k`, `700 root:root` | Montage et écriture UID 0 validés sous SELinux `Enforcing` |
| tmpfs fichier `600 999:999` mais répertoire `700 root:root` | Lecture UID 999 refusée ; ancien test affichait un **faux succès** à cause du manque de `set -e` dans le shell imbriqué |
| tmpfs répertoire `710 root:999`, fichier `600 999:999` | UID 999 lit réellement après `gosu postgres`, test corrigé |
| Source Docker Compose `600 root:root` → copie tmpfs → UID 999 → suppression | `UID999_SECRET_TRANSFER_READ_OK`, `SECRET_TRANSFER_CLEANUP_OK`, `SECRET_FULL_TRANSFER_EXIT=0` |

**Important :** ces tests prouvent un mécanisme technique pour fichiers fictifs, **pas** la robustesse du nettoyage en cas de crash ni l'intégration à l'entrypoint permanent. Puisque l'option B de provisionnement post-initialisation est approuvée, **aucun wrapper `tmpfs` n'a été mis en place pour PostgreSQL**.

### Entrypoint officiel PostgreSQL — vérifié dans l'image

```bash
sudo docker run --rm --network none --read-only --entrypoint /bin/sh \
  docker.io/library/postgres@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37 \
  -c 'sed -n "330,385p" /usr/local/bin/docker-entrypoint.sh; sed -n "169,205p" /usr/local/bin/docker-entrypoint.sh'
```

Faits observés : `docker_setup_env` / `file_env POSTGRES_PASSWORD` initialement sous UID 0, `exec gosu postgres "$BASH_SOURCE" "$@"`, initialisation sous UID 999 ; scripts `*.sh` exécutables exécutés, non exécutables sourcés ; `*.sql` traités par `docker_process_sql`. Les scripts `initdb` **ne** servent **pas** au provisionnement applicatif retenu.

## 4. Fichier Docker Compose permanent — CRÉÉ et validé

**Chemin sur le VPS :** `/opt/sixpay/compose/docker-compose.yml` ; `644 root:root`, type SELinux `usr_t:s0`, 1 017 octets observés. Le contenu ci-dessous correspond à la configuration validée ; pour toute modification ultérieure, considérer le fichier réel comme source prioritaire :

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
      - type: bind
        source: /var/lib/sixpay/postgres/data
        target: /var/lib/postgresql/data
        bind:
          create_host_path: false
          selinux: Z
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

**Validations rapportées :**

```bash
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml config --quiet
echo "COMPOSE_FILE_VALIDATION_EXIT=$?"   # observé 0

sudo docker compose -f /opt/sixpay/compose/docker-compose.yml config --format json
# Inspection réelle : image, restart, ports=[], variables POSTGRES_DB et POSTGRES_USER,
# secret monté, bind(selinux Z, create_host_path false), réseau, no-new-privileges.
# observé COMPOSE_FINAL_INSPECTION_EXIT=0
```

Le contrôle avant initialisation a confirmé `PGDATA_EMPTY_CONFIRMED` et `Enforcing`. **Attention : `config --quiet` est une validation de syntaxe, pas un test dynamique de montage ou de labels MCS.**

## 5. Secret administrateur — CRÉÉ ; contexte SELinux persistant validé

**Fichier réel existant :** `/opt/sixpay/secrets/postgres_admin_password` ; `600 0:0`, 65 octets, type `container_file_t:s0`. Il a été créé par un script shell root avec `umask 077`, refus d'écrasement (`set -C`), `openssl rand -base64 48`, puis `chown 0:0`, `chmod 600`, `chcon -t container_file_t`. **NE PAS REJOUER LA GÉNÉRATION : cela écraserait / changerait le mot de passe et risquerait de rendre les données inaccessibles.**

Commandes d'observation sûres :

```bash
sudo stat -c '%a %u:%g %s %n' /opt/sixpay/secrets/postgres_admin_password
sudo ls -lZ /opt/sixpay/secrets/postgres_admin_password
getenforce
```

**Validation effective du secret :** UID 0 dans image PostgreSQL avec montage Docker, lecture autorisée, `POSTGRES_ADMIN_SECRET_READ_OK`, `ADMIN_SECRET_RUNTIME_EXIT=0`; après `restorecon`, `POSTGRES_ADMIN_SECRET_FINAL_READ_OK`, `POSTGRES_ADMIN_FINAL_CHECK_EXIT=0`. Le contenu n'a pas été affiché.

**Persistance SELinux :** package `policycoreutils-python-utils-3.10-2.el10_2.noarch` installé avec DNF, `SEMANAGE_INSTALL_EXIT=0`, `/usr/sbin/semanage`, `Enforcing`. Règle locale créée **uniquement pour le fichier existant** :

```bash
# Déjà exécutée : ne pas ré-ajouter la même règle (erreur de doublon).
sudo semanage fcontext -a -t container_file_t \
  '/opt/sixpay/secrets/postgres_admin_password'
```

Contrôles observés :

```bash
sudo semanage fcontext -l -C
sudo matchpathcon /opt/sixpay/secrets/postgres_admin_password
sudo restorecon -v /opt/sixpay/secrets/postgres_admin_password
sudo matchpathcon -V /opt/sixpay/secrets/postgres_admin_password
```

Résultats : règle `system_u:object_r:container_file_t:s0`, `SEMANAGE_RULE_EXIT=0`, `RESTORECON_EXIT=0`, `MATCHPATHCON_VERIFY_EXIT=0`. `restorecon` a indiqué « not reset as customized by admin » sans modifier type ni permissions. **Ne pas étendre cette règle globalement au dossier secret sans décision de sécurité.**

## 6. Premier démarrage — ÉCHEC À CONSERVER DANS LE JOURNAL

Commande **déjà exécutée** (ne pas rejouer tant que l'incident n'est pas résolu) :

```bash
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml up -d --no-deps postgres
```

Résultat : `POSTGRES_FIRST_START_EXIT=0`, conteneur `sixpay-connect-cloud-postgres-1` créé, puis `Restarting (1)` ; logs :

```text
chmod: changing permissions of '/var/lib/postgresql/data': Permission denied
find: '/var/lib/postgresql/data': Permission denied
```

Le `0` de `docker compose up` signifie seulement que la commande Docker s'est terminée, **pas** que la base a démarré. Le redémarrage automatique a été interrompu par :

```bash
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml stop postgres
# POSTGRES_STOP_EXIT=0
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml ps -a postgres
# STATUS: Exited (1)
```

**Dernière observation dynamique :** `RestartCount=12`, `ExitCode=1`, service **arrêté** ; aucun port publié ; le fichier admin est monté en lecture seule ; PGDATA en bind RW. Le `find` sur le contenu de PGDATA n'a retourné aucune entrée à ce moment : **vide au dernier contrôle** ; le conteneur permanent existe toujours mais arrêté.

### Cause immédiate avérée — AVC SELinux avec catégories MCS différentes

Audit (`sudo ausearch -m AVC,USER_AVC -ts recent`) :

```text
comm="chmod"  avc: denied { setattr }  tclass=dir  permissive=0
scontext=system_u:system_r:container_t:s0:c202,c319
tcontext=system_u:object_r:container_file_t:s0:c201,c399
```

| Objet | Catégories SELinux |
|---|---|
| Processus PostgreSQL permanent (observé dans AVC) | `c202,c319` |
| Répertoire `/var/lib/sixpay/postgres/data` | `c201,c399` |

**Conclusion factuelle :** incompatibilité des catégories MCS, refus de `chmod` (`setattr`). **Hypothèse non encore confirmée :** des montages antérieurs sous `selinux: Z` ont laissé un label privé différent ; il faut inspecter `ProcessLabel`, `MountLabel` et les options effectives du conteneur pour comprendre pourquoi Docker n'a pas réétiqueté correctement ce bind au lancement.

### Interdictions tant que l'incident n'est pas diagnostiqué

- **Ne pas lancer** `docker compose up`, `start` ou `restart` pour PostgreSQL.
- **Ne pas** lancer `docker compose down -v`, `rm -rf`, `initdb`, ou nettoyage de PGDATA.
- **Ne pas** utiliser `setenforce 0`, `--privileged`, `chmod 777`, modification récursive de `/var/lib` ou manipulation des catégories MCS sans avoir inspecté les labels du conteneur.
- **Ne pas** créer `sixpay_user`, lancer SIXPAY ni Flyway ; la base n'a pas été validée.

## 7. POINT DE REPRISE IMMÉDIAT : E1.2.3.C.11.D — lecture seule

**C'est la seule étape recommandée dans le prochain échange.** Lancer sur le VPS :

```bash
echo '=== LABELS DU CONTENEUR POSTGRES ==='
sudo docker inspect sixpay-connect-cloud-postgres-1 \
  --format 'ProcessLabel={{.ProcessLabel}}
MountLabel={{.MountLabel}}'

echo '=== MONTAGES ==='
sudo docker inspect sixpay-connect-cloud-postgres-1 \
  --format '{{range .Mounts}}{{println .Type .Source "->" .Destination "RW=" .RW "Mode=" .Mode}}{{end}}'

echo '=== PGDATA ==='
sudo ls -ldZ /var/lib/sixpay/postgres/data
sudo find /var/lib/sixpay/postgres/data -mindepth 1 -maxdepth 1 -printf '%f\n'

echo '=== ETAT POSTGRES ==='
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml ps -a postgres

echo '=== SELINUX ==='
getenforce
```

**Résultat recherché :** documenter `ProcessLabel`, `MountLabel`, label de PGDATA et état `Exited (1)` ; **aucune modification**. Ne pas préjuger de la meilleure correction avant les résultats. Les valeurs MCS exactes peuvent changer à la recréation d'un conteneur : ne pas les figer dans un script permanent.

## 8. Contrôles d'exploitation et critères de GO ultérieurs (non exécutés)

Après correction autorisée de SELinux et redémarrage unique contrôlé :

```bash
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml ps postgres
sudo docker compose -f /opt/sixpay/compose/docker-compose.yml logs --tail=80 postgres
# Puis requêtes SQL strictement ciblées avec méthode d'authentification validée.
```

Attendre de constater **effectivement** : sortie des logs indiquant fin d'initialisation, état `healthy`, présence de `sixpaydb`, connexion admin contrôlée, aucune erreur AVC liée à PGDATA, absence de ports publiés. `pg_isready` et `healthy` mesurent la disponibilité, **pas** une authentification applicative complète. Les opérations de création du rôle `sixpay_user`, la gestion de son secret et les migrations Flyway ne sont pas exécutées à ce jour.

### Journal de validation synthétique

| Lot | État |
|---|---|
| Vérification Docker/SELinux/image officielle | Validée |
| Répertoires et permissions | Validés |
| Tests secrets fictifs UID0/UID999/tmpfs | Validés dans les scénarios détaillés ; un faux positif initial corrigé |
| Entrypoint officiel / `gosu` / scripts initdb | Inspectés |
| Option B post-initialisation | Approuvée |
| Compose `config --quiet`, inspection JSON | `0` / validé statiquement |
| Secret admin + règle SELinux persistante + lecture Docker | Validés |
| Premier `compose up -d` | **Commande retournée 0, PostgreSQL en échec, 12 redémarrages** |
| Arrêt du service | Validé (`Exited (1)`) |
| Diagnostic AVC MCS | Cause immédiate identifiée |
| Inspection labels C.11.D | **À effectuer maintenant** |
| Création `sixpay_user`, tests SQL, Flyway | Non exécutés |

## 9. Maintenance, sauvegarde et reprise

- Avant toute opération avec effet persistant sur PGDATA, décider explicitement d'une sauvegarde adaptée (snapshot, backup PostgreSQL cohérent) selon le niveau de données réel et la gouvernance. À cet instant le dossier était vide au dernier contrôle, mais ne jamais extrapoler cette observation à une date ultérieure.
- Le secret administrateur réel existe uniquement sur le VPS dans le périmètre observé ; aucune sauvegarde sécurisée ni rotation n'a été validée dans ce runbook. **Ne pas perdre ni régénérer ce mot de passe** après initialisation.
- La sécurité doit rester en `Enforcing` ; contrôler les AVC après toute correction.
- Pour les futures modifications du Compose, vérifier `docker compose config --quiet`, puis les montages et labels réels. Ne pas supposer qu'une option Compose valide syntaxiquement est appliquée comme prévu au runtime.
