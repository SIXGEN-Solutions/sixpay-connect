# SIXPAY CONNECT — Guide maître d’exploitation de l’infrastructure VPS Contabo (MVP simulation)

**Édition documentaire :** 12 novembre 2026 · **Mode :** lecture seule des sources GitHub, synthèse de preuves fournies, aucune intervention VPS · **Statut documentaire :** `INCOMPLETE_CONTEXT` pour une clôture exhaustive (deux apports de phase non accessibles) · **Qualification runtime :** état *rapporté/attesté*, non revalidé sur machine.

**Comparaison de révisions contrôlée :** `b212c4bb…` est ancêtre de `04808a269…` ; GitHub confirme exactement **2 commits** d’avance, **0** en retard et **6 fichiers modifiés**, exclusivement sous `frontend/src/app/features/partners/` et `frontend/src/app/layout/navigation/navigation.config.ts`. Ces différences n’établissent aucun changement d’infrastructure dans le dépôt.

**Référentiel :** `SIXGEN-Solutions/sixpay-connect` · branche `feat/repository-baseline-consolidation-cucumber` · **HEAD GitHub observé par lecture du point de branche :** `04808a26984e4919a29f9e36493abcf9f102d960` · ancien jalon de phase `b212c4bb67c8cdc63711428e0453933b3d2d7ac3` (ne pas imposer l’égalité). **Aucune révision de worktree Git local ni de label OCI du VPS n’est observable ici.**

**Périmètre :** VPS `vmi3671192`, `45.85.146.52`, AlmaLinux 10.2, utilisateur opérateur `sixpayadmin`. Exclus : déploiement bancaire réel, migration, durcissement en production et exécution opérationnelle. **Ce document ne constitue ni homologation sécurité ni autorisation d’intervention.**

## 0. Couverture documentaire et disponibilité des sources

| ID | Source canonique (ou classe) | Disponibilité au moment de l’analyse | Emploi et limite |
|---|---|---|---|
| GOV | `AGENTS.md` → `ENGINEERING_CONTEXT.md` → `MASTER_PROMPT_INPUT_MANIFEST.yaml` → 18 sources `alwaysLoad` → `MASTER_ENGINEERING_PROMPT.md` | **Consultées au HEAD** via GitHub, dans l’ordre prescrit ; alias du projet également disponibles pour plusieurs fichiers | Gouvernance active ; le manifeste GitHub est plus récent que la copie jointe au projet ; priorité au HEAD sélectionné |
| CTR | `documentation/contracts/CONTRACT_REGISTRY.yaml` ; contrats physiques Payment | **Consultés au HEAD** | Statuts de contrats distincts de l’état d’une route déployée |
| R01 | `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_E1_1_PREPARATION_CONTABO_ALMALINUX.md` | **Consulté au HEAD** | Installation initiale AlmaLinux/SSH/Docker ; certaines étapes sont des procédures et non des preuves de réalisation |
| R02 | `…/SIXPAY_RUNBOOK_E1_2_POSTGRESQL_CONTABO.md` | **Consulté au HEAD** | Conserve l’échec SELinux initial ; l’état final est supersédé par R03 |
| R03 | `…/SIXPAY_RUNBOOK_E1_3_CONTABO_POSTGRES_GHCR_BACKEND_2026-11-10.md` | **Consulté au HEAD** | PostgreSQL sain, credentials SQL non divulgués, premier digest GHCR ; backend non démarré **à cette date de jalon** |
| R04 | `…/SIXPAY_RUNBOOK_CONTABO_LDAP_PROVISIONNEMENT.md` | **Consulté au HEAD** | Backend/LDAP, identités, sessions, provisioning ; ses anciens constats « frontend non installé » sont historiques |
| R05 | `…/SIXPAY_G718_G720_RUNBOOK_CONTABO_2026-11-12.md` | **Consulté au HEAD** | Frontend, Nginx, Caddy, TLS, reboot et résultats Windows ; antérieur à la clôture G7.21 M2M décrite dans la demande |
| R06 | `…/SIXPAY_CLOUD_MVP_RUNBOOK.md` | **Consulté au HEAD** | Plan initial, pas inventaire de l’état runtime final |
| R07 | `…/KEYCLOAK_PRODUCTION_PARAMETERS.md` | **Consulté au HEAD** | Placeholders seulement ; **ne prouve aucun paramètre de production** |
| R08 | `…/SIXPAY_PROMPT_REPRISE_DEPLOIEMENT_CONTABO.md` | **Consulté au HEAD** | Handoff daté, valeur historique uniquement |
| R09 | `…/SIXPAY_PROMPT_REPRISE_G721_G726_2026-11-12.md` | **Consulté au HEAD** | Plan de continuation, ne prouve pas l’exécution G7.21 |
| P10 | `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_G721_M2M_HTTPS_CONTABO_2026-11-12.md` | **Absent du HEAD et non joint matériellement à cette session** ; basename/alias identiques recherchés dans le dossier GitHub | État G7.21 fourni par le présent cahier des charges, **pas** par ce fichier |
| P11 | `documentation/runbooks/deploy_cloud_mvp/SIXPAY_DECISIONS_REPORTS_APRES_MVP_2026-11-12.md` | **Absent du HEAD et non joint matériellement à cette session** ; basename/alias identiques recherchés dans le dossier GitHub | Reports repris comme **décisions communiquées**, à rapprocher du document lors de sa réception |
| CODE | `frontend/nginx.conf`, `frontend/Dockerfile`, `infrastructure/docker/m2m-keycloak/docker-compose.yml` | **Inspectés au HEAD** | Code dépôt ≠ configuration effective sur le VPS |
| VPS | `/etc/caddy/Caddyfile`, `/opt/sixpay/…`, `docker inspect`, journaux | **Non accessibles en direct** | Données runtime reprises uniquement des preuves et sorties reproduites dans les sources ou fournies dans la demande |

**Convention de preuve :** `OBS-Rxx` = résultat d’opérateur inscrit dans un runbook ; `DECL-12NOV` = constat ou décision explicitement communiqué dans la demande ; `CODE-HEAD` = code lu au HEAD ; `A-CONFIRMER` = contrôle encore absent. Les commandes de ce guide sont **des références de diagnostic**, pas une liste d’opérations à exécuter automatiquement. Ne jamais recopier la sortie brute de secrets ou de tokens.

## 1. Architecture et inventaire

### 1.1 Vue physique

```text
Client navigateur / partenaire Windows
             │  HTTPS public (DNS ou IP)
             ▼
Internet ── Contabo VPS 45.85.146.52 / vmi3671192 ──────────────────────┐
             │ firewalld, SELinux Enforcing, sshd:22, Caddy:80/443       │
             │ Caddy (service systemd sur hôte)                          │
             ├─ sixpayapp.sixgensol.com ─► 127.0.0.1:8088 ─► Nginx:80  │
             │                                             │             │
             │                                     /api/, /internal/api/ │
             │                                             ▼             │
             │                                         backend:8080      │
             ├─ auth.sixgensol.com ───► 127.0.0.1:8180 ─► Keycloak:8080 │
             ├─ api.sixgensol.com ────► 127.0.0.1:8081 ─► backend:8080  │
             └─ https://45.85.146.52 ─► 127.0.0.1:8088                 │
                                          │                              │
                           Docker PostgreSQL:5432 + Samba AD LDAPS:636 ─┘
```

**Attention :** `127.0.0.1:8081` n’est pas une exposition Internet directe. Caddy publie uniquement les routes explicitement autorisées. Les éléments « cinq routes », les sites `auth` et `api` et le réseau M2M proviennent de `DECL-12NOV` ; la configuration Caddy courante **exacte** n’est pas accessible ici.

### 1.2 Flux logiques

```text
Utilisateur humain → HTTPS sixpayapp → Angular/Nginx → /api/v1/auth/login/ldap
         → Spring Security/session → recherche/bind LDAPS Samba AD
         → mapping identité objectGUID / droits internes → PostgreSQL

Partenaire M2M → HTTPS auth → Keycloak token client_credentials (JWT)
       → HTTPS api (ACL Caddy méthode+chemin) → Spring JWT / audience / scopes
       → contrôle propriétaire Partner / identité initiatrice → Payment → PostgreSQL
       [échanges Core Banking réels : non validés par ces tests de route]
```

`backend/partner` demeure le golden module pour le code, `bootstrap` compose l’application, `integration` reste provider-neutral. La capacité `CustomerSubscription` appartient à `customer`, sans module autonome `subscription`.

### 1.3 Inventaire attesté vs à recontrôler

| Objet | Valeur rapportée | Autorité et limite |
|---|---|---|
| OS | AlmaLinux 10.2 « Lavender Lion », `platform:el10` | OBS-R05 ; noyau/patchlevel courant : A-CONFIRMER |
| Docker Engine | `29.9.3`, storage `overlayfs`, SELinux | DECL-12NOV ; version Compose exacte : A-CONFIRMER |
| Caddy | `2.11.7-1.el10` (COPR `@caddy/caddy`) | OBS-R05 à l’installation ; version binaire actuelle : A-CONFIRMER |
| Frontend HTTP | Nginx réponse `1.27.5` ; Dockerfile base `nginx:1.27-alpine` | OBS-R05 / CODE-HEAD ; digest déployé : A-CONFIRMER |
| Java / Spring | baseline dépôt Java 21, Spring Boot 4.1.0 | Matrice gouvernance ; runtime JVM/JAR exact : A-CONFIRMER |
| PostgreSQL | image `postgres:16.15-trixie`, sain (`healthy`) | OBS-R03/R05 ; digest ancien validé §5 |
| Samba AD | conteneur `sixpay-cloud-ldap`, LDAPS `dc.sixpay.test:636` | OBS-R04/R05 ; version Samba et digest : A-CONFIRMER |
| Keycloak | realm `sixpay-m2m-local`, `start-dev`, H2 persistant sur hôte | DECL-12NOV ; version/image/labels VPS : A-CONFIRMER (compose local GitHub utilise `26.8.1` **sans** H2 persistant) |
| SSH / firewall | `sixpayadmin`, public SSH:22 ; firewalld `ssh,http,https` | OBS-R01/R05 ; état instantané : A-CONFIRMER |
| SELinux | `Enforcing`, Caddy `httpd_can_network_connect=on` | OBS-R05 ; AVC/fcontexts actuels : A-CONFIRMER |
| Temps/hôte | `vmi3671192` ; chronyd UDP 323 loopback relevé | OBS-R05 ; noyau, UTC/fuseau, synchro NTP active : A-CONFIRMER |

## 2. Hôte Linux : identité, SSH, accès et audit non intrusif

**Précondition générale :** opérateur autorisé, session SSH existante, lecture seule ; une seule commande à la fois, sans afficher les fichiers `secrets`, `.env`, les clés ou des cookies. Les commandes suivantes ne sont **pas exécutées dans cette production documentaire** :

```bash
hostnamectl
cat /etc/os-release
uname -r
timedatectl
systemctl is-active chronyd
getenforce
sestatus
sudo firewall-cmd --get-active-zones
sudo firewall-cmd --zone=public --list-all
sudo ss -lntup
sudo dnf repolist
rpm -q docker-ce caddy firewalld container-selinux
sudo systemctl is-enabled sshd docker caddy
sudo systemctl is-active sshd docker caddy
id sixpayadmin
sudo sshd -T | grep -E '^(port|permitrootlogin|pubkeyauthentication|passwordauthentication|kbdinteractiveauthentication) '
```

**Lecture des droits sans exposition de données :**

```bash
sudo stat -c '%n UID=%u GID=%g MODE=%a' /opt/sixpay /opt/sixpay/compose /opt/sixpay/config /opt/sixpay/secrets /var/lib/sixpay/postgres/data
sudo ls -ldZ /opt/sixpay /opt/sixpay/compose /opt/sixpay/config /opt/sixpay/secrets /var/lib/sixpay/postgres/data
sudo matchpathcon -V /opt/sixpay/secrets/postgres_admin_password
sudo semanage fcontext -l -C
sudo systemctl cat caddy
```

**Historique attesté :** compte administrateur non-root et clé SSH Ed25519 selon R01 ; les étapes `sshd -t`, règles `firewalld`, Docker Engine et premiers tests sont décrites dans R01. Ne pas inférer que chaque commande de préparation a été réussie : vérifier les sorties consignées pour chacune. R02/R03 attestent des droits de PGDATA `999:999` mode `700`, dossier secrets root, copie du secret SQL applicatif `10001:10001` mode `400`; ne jamais ouvrir ou lister le contenu du secret. Le contexte de fichiers `container_file_t` et les catégories MCS PostgreSQL avaient été alignés après incident via montage Docker `:Z` ; **pas** par désactivation SELinux.

**Sudo/Docker :** `sudo` pour l’administration ; les personnes pouvant piloter le daemon Docker disposent potentiellement de pouvoirs équivalents à root : inventorier les membres du groupe `docker` et les sudoers lors d’un audit habilité, ne pas ajouter de membre dans ce chantier. Les permissions et montages exacts de Keycloak H2, Compose, PKCS12 et Caddy sont A-CONFIRMER.

## 3. Arborescence, services et persistance

```text
/opt/sixpay/
├── compose/
│   ├── docker-compose.yml                    # PostgreSQL (R03)
│   ├── docker-compose.backend.yml            # backend (R03, R04 et DECL)
│   ├── docker-compose.ldap.yml               # Samba AD (R04)
│   ├── docker-compose.frontend.yml           # frontend (R05)
│   └── docker-compose.yml.pre-selinux-fix    # sauvegarde R03
├── config/
│   ├── backend.env                           # PRIVÉ : NE PAS AFFICHER
│   └── frontend/runtime-config.json          # runtime JSON frontend (R05)
├── secrets/                                  # PRIVÉ : contenu interdit
│   ├── backend/sixpay_db_password            # R03
│   └── ldap/…                                # truststore PKCS12 (R04)
└── keycloak/docker-compose.yml               # DECL-12NOV ; inspecter le fichier sur VPS

/var/lib/sixpay/postgres/data                  # données persistantes PostgreSQL
/etc/caddy/Caddyfile                           # version courante NON lue
/etc/caddy/Caddyfile.before-sixpay-g7.20        # sauvegarde OBS-R05
```

Les chemins des volumes Samba et H2 ainsi que les autres sauvegardes sont **à inventorier**, non à deviner. L’existence de `docker-compose.backend.yml.bak-g716` est documentée dans R04. Le nom exact des volumes Docker (`docker volume ls`) et le montage effectif de H2 sont A-CONFIRMER. Caddy est **un service systemd hôte**, non un service Docker. Docker est activé au boot ; `restart: unless-stopped` pour les quatre conteneurs PostgreSQL/backend/LDAP/frontend fut confirmé et un redémarrage intégral validé en R05. Keycloak : politique restart à confirmer.

**Lecture sûre des métadonnées sans afficher les configurations secrètes :**

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
sudo docker volume ls
sudo docker network ls
sudo docker inspect sixpay-connect-ldap-backend-1 --format '{{.Name}} image={{.Image}} restart={{.HostConfig.RestartPolicy.Name}}'
sudo systemctl is-active caddy docker
sudo ls -l /etc/caddy/Caddyfile /etc/caddy/Caddyfile.before-sixpay-g7.20
```

Ne pas faire un `docker inspect` intégral ni `docker compose config` sans filtrage/masquage : `Env`, chemins secrets et interpolation peuvent divulguer des credentials. Garder les preuves en coffre d’audit restreint, ne publier qu’une version expurgée.

## 4. Réseaux Docker : structure et incident de publication

| Réseau | Isolation | Services/usage rapportés | Validation résiduelle |
|---|---|---|---|
| `sixpay-connect-cloud_sixpay_backend` | `internal:true`, `172.18.0.0/16` | backend, frontend, PostgreSQL, Samba selon raccordement | Gateway et adresses/IP effectives |
| `sixpay_m2m_bridge` | `internal:true`, `172.21.0.0/16` | Keycloak ↔ backend, JWKS interne | Gateway, DNS `keycloak`, isolation restante |
| `sixpay-connect-cloud_sixpay_frontend_access` | `internal:false` | frontend **et backend** après correctif ; publication des loopbacks | Subnet/gateway et analyse de surface latérale |
| Réseau stack Keycloak | bridge, statut à vérifier | Keycloak, composition locale VPS | Nom, subnet, accès, publications |

**Topologie déclarée :** backend 3 réseaux ; frontend 2 ; Keycloak 2 ; bases et Samba privés. La provenance de ce schéma complet est `DECL-12NOV`, non une nouvelle inspection directe.

**Incident NET-01 (OBS-R05 puis DECL-12NOV).** Sur frontend puis backend, la déclaration `HostConfig.PortBindings` ciblait `127.0.0.1:8088`/`8081`, mais `NetworkSettings.Ports` était `null`, donc écoute loopback indisponible. Le frontend a été relié à un bridge d’accès existant/créé selon R05 puis recréé seul. Pour le backend, la demande atteste l’ajout du **réseau non interne déjà existant** et la recréation ciblée du backend : `docker port` a alors retourné `127.0.0.1:8081`, `/actuator/health` = HTTP 200. La causalité complète du moteur réseau Docker n’a pas été prouvée ; correction empirique réussie, audit du changement d’isolation **différé**. Ne pas ajouter `socat`, un service systemd de relais, un nouveau bridge ou un proxy API Nginx non justifié.

**Diagnostic ciblé A-CONFIRMER (sans changement réseau) :**

```bash
sudo docker network inspect sixpay-connect-cloud_sixpay_backend --format '{{json .Internal}} {{json .IPAM.Config}}'
sudo docker network inspect sixpay_m2m_bridge --format '{{json .Internal}} {{json .IPAM.Config}}'
sudo docker network inspect sixpay-connect-cloud_sixpay_frontend_access --format '{{json .Internal}} {{json .IPAM.Config}}'
sudo docker port sixpay-connect-ldap-backend-1
sudo docker port sixpay-connect-ldap-frontend-1
sudo docker inspect sixpay-connect-ldap-backend-1 --format '{{json .NetworkSettings.Networks}}'
```

Ne pas corriger à l’aveugle un port par une nouvelle publication `0.0.0.0`, ni désactiver l’isolation de PostgreSQL/Samba.

## 5. Images immuables, versions, provenance

**Chaîne prouvée à un jalon passé :** GitHub Actions `backend-image-publish.yml`, run `42842816703` réussi le 10/11 (R03), digest backend initial `ghcr.io/sixgen-solutions/sixpay-backend@sha256:2e0675ffba768d093d30a0f59df91279f5444bf1671cc36eef09397cacdf5136`, image associée dans R03 au commit `11f106a289d6d018a575506284a03c78bb701cd5`. R04 documente **un autre digest backend** `sha256:1958f3bf753129ea6fde73e9e7dba2a666fcc5d6d6c04205411a148dd512951b` pour le chantier LDAP. Aucune de ces références historiques ne prouve le digest actuel du conteneur G7.21. PostgreSQL R03 : `docker.io/library/postgres:16.15-trixie@sha256:43f9275df91e012c3da3d86bea5d7bb5e67a473296cc906c5f9e80b8176b1a37`.

**Procédure d’identification actuelle à autoriser uniquement comme diagnostic :**

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.ID}}'
sudo docker inspect sixpay-connect-ldap-backend-1 --format '{{.Image}}'
sudo docker image inspect IMAGE_ID --format '{{index .Config.Labels "org.opencontainers.image.revision"}}'
sudo docker image inspect IMAGE_ID --format '{{.Id}} {{json .RepoDigests}}'
```

Remplacer `IMAGE_ID` par **l’ID observé**. Appliquer aux services backend, frontend, Keycloak, Samba, PostgreSQL. Si un label n’existe pas, marquer `NON FOURNI`, et non attribuer le HEAD GitHub au conteneur. Récupérer séparément `docker compose version`, `java -version` (si disponible sans exposition), `caddy version`, version Samba/Keycloak par image/manifest. **La version Keycloak 26.8.1 du compose de développement GitHub n’est pas une preuve de la version VPS.** Conserver pour audit : date UTC, nom conteneur, ID image, RepoDigest, label OCI, source de publication, méthode de vérification.

## 6. Caddy : surfaces publiques et TLS

### 6.1 Matrix DNS et ports

| Hôte | Entrée publique | Destination loopback | Destination réelle | Politique |
|---|---|---|---|---|
| `sixpayapp.sixgensol.com` | 443/TCP HTTPS | `127.0.0.1:8088` | Nginx frontend :80 | UI ; proxy Nginx `/api/`, `/internal/api/` vers `backend:8080` |
| `auth.sixgensol.com` | 443/TCP HTTPS | `127.0.0.1:8180` | Keycloak :8080 | seulement discovery OIDC, token, JWKS en public ; administration non publique |
| `api.sixgensol.com` | 443/TCP HTTPS | `127.0.0.1:8081` | Spring Boot :8080 | allowlist **méthode et chemin** ci-dessous, défaut `403` |
| `45.85.146.52` | 443/TCP HTTPS | `127.0.0.1:8088` | Nginx | certificat IP courte durée, R05 |
| VPS | 22/TCP SSH | sshd | hôte | accès administrateur contrôlé |
| VPS | 80/TCP HTTP | Caddy | redirection / ACME | `308` observé pour IP |
| hôte | 2019/TCP | `127.0.0.1:2019` | admin Caddy | **non public** |
| Docker | 5432/TCP / 636/TCP | aucun host port revendiqué | PostgreSQL / Samba | privé |

**Allowlist actuellement communiquée (`DECL-12NOV`) :**

```text
POST /v1/payments/initiate
GET  /api/v1/integrations/payments/{paymentReference}  # transitoire
GET  /v1/payments/{paymentReference}/confirmation-challenge
POST /v1/payments/{paymentReference}/confirmation-challenge/verify
POST /v1/payments/{paymentReference}/confirmation-challenge/resend
```

**Caddyfile exact :** seule la première version **historique** à site IP est entièrement reproduite dans R05 ; la version G7.21 à quatre sites et ACL précises **n’est pas consultable** sans P10 ou lecture protégée VPS. Il serait trompeur de présenter une reconstruction comme « Caddyfile exact ». **Extraction future :** `sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` sans modification ; audit du fichier seulement après **masquage** des tokens/entêtes secrets, puis comparaison de son empreinte avec un artefact d’audit. Ne pas publier le fichier brut si des secrets y figurent. `sudo caddy reload --config /etc/caddy/Caddyfile` est une **commande de changement non autorisée par cette demande**, à n’utiliser qu’après approbation.

**Certificat IP (OBS-R05) :** Let’s Encrypt production, CN émetteur YE2, `notBefore=2026-11-11T23:08:42Z`, `notAfter=2026-11-18T15:08:41Z`, SAN IP `45.85.146.52`. Échec initial des défis ACME `tls-alpn-01`/`http-01` tant que le pare-feu ne laissait pas entrer 443/80 ; succès après ouverture permanente, redirection 308 et HTTP 200 depuis Windows. Caddy a reçu des informations de renouvellement ; **aucun renouvellement effectif n’était encore prouvé** à la rédaction R05. Il faut distinguer expiration IP de celle des certificats des noms DNS (A-CONFIRMER).

**Contrôle externe non intrusif (proposé, pas exécuté ici) :**

```powershell
curl.exe -I --max-time 15 https://sixpayapp.sixgensol.com/
curl.exe -i --max-time 15 https://api.sixgensol.com/actuator/health
curl.exe -i --max-time 15 https://auth.sixgensol.com/admin/
curl.exe -i --max-time 15 https://45.85.146.52/
```

Le test de l’admin Keycloak doit respecter les ACL et ne nécessite aucune tentative d’authentification. Il ne faut pas confondre réponse Angular `200` (fallback SPA) avec Swagger fonctionnel : R05 a observé `/swagger-ui/index.html` et `/v3/api-docs` répondant par **HTML Angular**. `X-Content-Type-Options` dupliqué a été remarqué, sans correction.

## 7. Keycloak M2M / LDAP humain : deux frontières distinctes

### 7.1 M2M, état communiqué

Realm : `sixpay-m2m-local` ; client : `sixpay-partner-test` ; grant : `client_credentials` ; scopes démontrés `payment.read` et `payment.initiate` ; audience attendue `sixpay-api`; jetons courts ~300 secondes ; issuer `https://auth.sixgensol.com/realms/sixpay-m2m-local` ; JWKS backend privé `http://keycloak:8080/realms/sixpay-m2m-local/protocol/openid-connect/certs`.

**Configuration backend observée, sans secrets (`DECL-12NOV`) :**

```text
SIXPAY_M2M_ENABLED=true
SIXPAY_M2M_ISSUER_URI=https://auth.sixgensol.com/realms/sixpay-m2m-local
SIXPAY_M2M_JWK_SET_URI=http://keycloak:8080/realms/sixpay-m2m-local/protocol/openid-connect/certs
SIXPAY_M2M_AUDIENCE=sixpay-api
```

La configuration `start-dev` + H2 persisté hôte est **une installation de simulation**. Le compose `infrastructure/docker/m2m-keycloak/docker-compose.yml` du dépôt est un **compose local jetable** et ne contient pas la persistance hôte rapportée : ne pas l’utiliser comme description du VPS. Les commandes exactes de création du realm/client, les mappers audience, scopes client et permissions service account **ne sont pas attestées** dans une source P10 disponible. À inventorier depuis une exportation **expurgée** de la configuration réelle, sans secret client, JWT, clés, comptes ou identifiants personnels. Ne jamais supposer le rôle des mappers non décrits.

### 7.2 Authentification interactive et LDAP

Samba AD `sixpay-cloud-ldap` ; domaine test `DC=sixpay,DC=test` ; base utilisateurs `CN=Users,DC=sixpay,DC=test` ; bind authentifié LDAPS `dc.sixpay.test:636` ; mapping identité stable `objectGUID`. La révision corrigée de l’adaptateur a utilisé le **DN complet** pour le bind plutôt que le seul login court ; `sixpay.admin` HTTP 200, bootstrap unique **désactivé** (`SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false`), `sixpay.testuser` provisionné `MANAGER`, ADMIN/MANAGER distincts (OBS-R04). Truststore PKCS12 contenant 119 entrées au contrôle R04, dont alias `sixpay-samba-ad-ca` ; **ne pas ouvrir, exporter ni recopier le truststore**. Une session login HTTP 200 suivie d’un `/auth/me` 401 en diagnostic HTTP privé provenait du cookie `Secure` non renvoyé sur HTTP. Le parcours navigateur HTTPS a fonctionné après G7.20 (OBS-R05). Le contournement de cookie par envoi manuel était une méthode diagnostique **non réutilisable en production**.

## 8. Backend, frontend et données

**Backend :** conteneur Spring Boot `backend:8080` ; connexion JDBC par DNS Docker `postgres:5432/sixpaydb` (R03) ; usager SQL applicatif à privilèges limités ; secret monté depuis `/opt/sixpay/secrets/backend/sixpay_db_password` ; `SIXPAY_M2M_*` comme §7 ; habilitations utilisateur LDAP dans le module Security. Le profil, l’assemblage Spring, le provisioning M2M, l’audit et les contrôles Partner doivent être déduits du code correspondant **à la révision de l’image**, non simplement du HEAD GitHub. Aucune migration Flyway ou requête SQL n’est relancée ici.

**Frontend :** `frontend/Dockerfile` au HEAD construit Angular avec `node:22.23.1-alpine` puis sert depuis `nginx:1.27-alpine`, upstream par défaut `SIXPAY_BACKEND_UPSTREAM=backend:8080`. `frontend/nginx.conf` route `/api/` et `/internal/api/` sans retirer le préfixe, avec DNS Docker `127.0.0.11`, masque `/actuator` (404), sert `/runtime-config.json` sans cache et fallback Angular. R05 a observé `200` sur UI et runtime-config et `401` sans session sur `/api/v1/auth/me`. **Le code n’implique pas automatiquement que l’image runtime actuelle porte le HEAD**.

**PostgreSQL :** service `postgres:5432`, base `sixpaydb`, rôle `sixpay_user` non superuser, authentification SCRAM testée (R03), PGDATA monté avec étiquette SELinux adaptée. Son contenu, ses mots de passe, ses tables bancaires et les bases Samba/Keycloak ne font pas partie des preuves à exporter. La sauvegarde/restauration réelle reste reportée.

**Journalisation opérationnelle, à employer au besoin sans fuite :**

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
sudo docker logs --tail 50 sixpay-connect-ldap-backend-1
sudo journalctl -u caddy -n 50 --no-pager
sudo systemctl status docker caddy --no-pager
```

**Prudence :** même les logs peuvent contenir des identifiants ou données sensibles ; filtrer/expurger avant partage. `docker compose config` peut révéler des secrets interpolés : ne pas l’inclure dans les annexes de preuves non expurgées.

## 9. Journal d’exploitation chronologique : commande, précondition, sortie, statut, retour arrière

| ID / origine | Précondition et commande de référence | Sortie observée dans source | Statut à la date de preuve | Retour arrière / règle de sûreté |
|---|---|---|---|---|
| OP01 R01 | SSH avec clé autorisée ; `ssh … sixpayadmin@45.85.146.52` | accès opérateur, services préparés ; paramètres exacts selon R01 | **Historique exécuté** | conserver console Contabo en cas d’erreur de règles SSH ; ne pas fermer dernière session avant test |
| OP02 R02 | SELinux actif, compose PG initial ; `docker compose … up -d --no-deps postgres` | `Exited (1)`, AVC SELinux MCS | **Échec initial prouvé** | arrêt ciblé du conteneur, pas de `down -v` ni `setenforce 0` |
| OP03 R03 | bind PGDATA `:Z`, volumes autorisés ; `docker compose … ps` | PostgreSQL `healthy`, SCRAM SQL `SIXPAY_SCRAM_AUTH_OK` | **Correction acquise à R03** | conserver backup `docker-compose.yml.pre-selinux-fix` ; pas de retour à un montage SELinux défectueux |
| OP04 R03 | workflow GitHub sur branche ; build/push image | run Actions `42842816703` success, digest GHCR vérifié | **Publication initiale prouvée** | garder digest antérieur, ne pas substituer `latest` |
| OP05 R04 | DC, truststore PKCS12 et backend alignés ; login LDAPS | 401 initial puis 200 après bind DN complet | **Correction prouvée** | revenir au précédent digest seulement sous change approuvé, sans modifier AD/DB |
| OP06 R04 | ADMIN autorisé ; bootstrap one-shot / provisionnement | ADMIN validé, utilisateur MANAGER provisionné ; bootstrap false | **Opération achevée** | aucun second bootstrap ; ajustements via API autorisée, auditables |
| OP07 R04 | Tests de session directs HTTP interne ; login puis `/auth/me` | 200 puis 401 dû au cookie Secure non renvoyé | **Diagnostic prouvé** | ne pas abaisser la sécurité du cookie ; utiliser HTTPS public |
| OP08 R05 | Frontend sur seul bridge interne ; `docker inspect` / curl loopback | binding demandé mais port `null`, curl échec | **Échec prouvé** | ne pas rendre le réseau DB public |
| OP09 R05 | Bridge frontend_access, recreate frontend seulement | `127.0.0.1:8088` → 200 UI/config, API 401 sans session | **Correction prouvée** | réversibilité via sauvegarde Compose et digest si approuvée |
| OP10 R05 | Caddy installé depuis COPR ; `caddy validate` | syntaxe valide ; service actif ; ACME 80/443 d’abord refusés | **Échec TLS initial expliqué** | garder `/etc/caddy/Caddyfile.before-sixpay-g7.20`, ne pas désactiver SELinux |
| OP11 R05 | Firewalld `http`,`https` autorisés ; ACME nouvelle tentative | certificat LE production obtenu, HTTPS IP 200, HTTP 308 | **Succès daté** | rollback Caddyfile uniquement sur autorisation et avec validation préalable |
| OP12 R05 | Conteneurs `restart: unless-stopped`, Caddy enabled ; reboot opérateur | 4 services up, PG healthy, Caddy active, LDAP HTTPS réussi | **Reboot et reprise prouvés** | pas une preuve de restauration après corruption de disque |
| OP13 DECL-12NOV | Backend 8081 sur deux réseaux internes ; `docker port` | `NetworkSettings.Ports=null`, loopback absent | **Incident postérieur attesté** | ne pas ajouter relais public improvisé |
| OP14 DECL-12NOV | Ajout bridge non interne existant + recreate backend **seul** | `127.0.0.1:8081` puis backend health HTTP 200 | **Correction attestée**, audit réseau différé | conserver composition pré-correctif/état réseau ; pas d’application inverse automatique |
| OP15 DECL-12NOV | `api.sixgensol.com` ; requête sans JWT, JWT scopes, header UUID | 403 Actuator ; 401 sans JWT ; 400 sans corrélation ; 404 avec read + UUID ; 403 scope initiate sur GET | **Contrôles VPS et Windows attestés** | aucune transaction réelle prouvée, aucune autorisation d’objet exhaustive |
| OP16 DECL-12NOV | Script Windows `powershell.exe -NoProfile -File $HOME\sixpay-m2m-test.ps1` | échec de collage multiligne évité avec fichier `.ps1` | **Correction de procédure attestée** | garder script local sans secret permanent, pas de tokens en sortie |

**Note rollback :** ce tableau identifie une **possibilité de restauration sous changement approuvé**, et non une autorisation de la déclencher. Les procédures de retour arrière ne doivent jamais remplacer les volumes de données par des versions anciennes non restaurées/testées.

## 10. Régressions et preuves fonctionnelles

| Régression | Résultat antérieur | Portée prouvée | Non prouvé / à planifier |
|---|---|---|---|
| Frontend GET `/` et runtime-config | 200 (R05) | Nginx fonctionne, configuration runtime servie | E2E complet toutes pages, refresh de config sans cache intermédiaire |
| LDAP HTTPS | connexion admin et testuser validée après reboot (R05) | authenticité session navigateur sur parcours testé | matrice exhaustive CSRF/permissions/expiration |
| PostgreSQL | healthy et test SCRAM R03 | connectivité à la date du contrôle | backup/restore et intégrité des données après sinistre |
| Keycloak token | JWT read/initiate obtenu (DECL) | émission du token M2M pour les cas testés | rotation keys, restrictions client/scopes/mappers complètes |
| Caddy HTTPS | routes publiques testées (DECL) | chemin, TLS vérifié sur tests cités | ACL fuzzing complet, IPv6/NAT, headers et mTLS |
| Backend M2M GET sans JWT | 401 (DECL) | authentification requise sur requête testée | existence de toutes les routes : **un 401 n’en est pas la preuve** |
| GET read avec UUID | 404 paiement fictif (DECL) | validation de header/chemin jusqu’à la couche applicable | présence paiement réel ou isolation interpartenaires : **non prouvées** |
| GET scope initiate | 403 (DECL) | refus d’une portée insuffisante sur ce GET | granularité permissions initiation/challenge complète |
| `POST initiate`, challenge GET/verify/resend | routes déclarées dans allowlist | routes listées, non E2E | tests positifs/négatifs détaillés différés |
| Paiement bancaire réel | **aucune validation** | sans objet | Core Banking, OTP/SMS, mTLS et transactions réelles hors périmètre |

La consultation M2M nécessite `payment.read` et `X-Correlation-ID` au format UUID. Le contrat `documentation/contracts/external/payment-consultation-api-v1.yaml` est `ACTIVE_MVP` mais **`PENDING_APPROVAL` / `REFERENCE_ONLY` / `codeGenerationAllowed:false`** ; sa route physique au HEAD est `/api/v1/integrations/payments/{paymentReference}`. Le contrat `payment-command-api-v1` est `ACTIVE_MVP` / `APPROVED` / `ACTIVE` / `true` et expose `/v1/payments/initiate`. L’allowlist actuelle reprend la consultation transitoire ; le passage à `GET /v1/payments/{paymentReference}` est une **évolution avant production, non déployée ni présumée contractuellement approuvée**. Elle exige alignement code, registre/contrat et Caddy avec décision humaine préalable. Ne pas modifier Caddy avant de confirmer la route dans l’image réellement déployée.

## 11. Exploitation courante et diagnostics non destructifs

### 11.1 Chemin d’investigation à suivre par un opérateur, une commande à la fois

1. Vérifier SSH et machine (`hostnamectl`, `uptime`, `timedatectl`) puis SELinux/firewalld **sans changer** les règles.
2. Vérifier services (`systemctl is-active docker caddy`), sockets (`ss -lntup`).
3. Vérifier conteneurs (`docker ps`) et politiques restart ; relever noms et ID d’images.
4. Vérifier bridges et port publications (`docker network inspect` ciblé, `docker port`).
5. Vérifier **uniquement** frontend `/` sur `127.0.0.1:8088`, backend Actuator loopback `127.0.0.1:8081` et Caddy `validate`, sans connexion à la DB ni extraction d’env.
6. Vérifier DNS/HTTPS externes et redirections, puis la **bonne** route M2M ; refuser d’utiliser un résultat 404 comme preuve de droit objet.
7. En cas d’échec, lire un nombre borné de logs et les **expurger** avant conservation.
8. Escalader avant toute recréation, rechargement, changement ACL/reseau, rotation de secrets ou réinitialisation de base.

**Exemples sûrs selon provenance (non exécutés maintenant) :**

```bash
curl --noproxy '*' -sS -o /dev/null -w '%{http_code}\n' --max-time 10 http://127.0.0.1:8088/
curl --noproxy '*' -sS -o /dev/null -w '%{http_code}\n' --max-time 10 http://127.0.0.1:8081/actuator/health
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

### 11.2 Windows — une ligne par commande

```powershell
ssh -i "$HOME\.ssh\sixpay_contabo_ed25519" -o IdentitiesOnly=yes sixpayadmin@45.85.146.52
curl.exe -sS -o NUL -w "%{http_code} %{ssl_verify_result}`n" --max-time 15 https://api.sixgensol.com/actuator/health
powershell.exe -NoProfile -File "$HOME\sixpay-m2m-test.ps1"
```

**Script `.ps1` :** fichier local déjà utilisé ; aucune reconstruction de son contenu sensible. Les tokens doivent rester en mémoire de session et ne pas être écrits en clair dans journaux/historique. Ne pas créer de paiement réel pour un contrôle réseau.

### 11.3 Rechargement, sauvegarde et restauration (références, non autorisées)

* **Caddy :** la copie `Caddyfile.before-sixpay-g7.20` est attestée. Pour une modification approuvée : faire une sauvegarde complémentaire, contrôler le diff expurgé, `caddy validate`, puis seulement `caddy reload` ; vérifier HTTP/TLS et disposer de la copie précédente. **Aucun changement ici.**
* **Docker/Compose :** préserver les fichiers actifs et leur sauvegarde ; comparer la configuration fusionnée **sans exposer variables interpolées**. Recréation ciblée d’un service uniquement après autorisation et évaluation d’impact sur les sessions/connexions.
* **PostgreSQL, LDAP, H2 :** sauvegardes/restaurations non prouvées. Documenter destinations, chiffrement, rétention, RPO/RTO, droits d’accès et preuve de restauration **avant** données non jetables. **Ne pas** proposer une commande de restauration factice. Ne jamais exécuter `down -v`, purge, `flyway clean`, export de secrets ou H2 wipe.

## 12. Risques, reports, décisions humaines

| ID | Situation au 12/11/2026 | Disposition |
|---|---|---|
| DEC-01 | **mTLS / conformité de gateway** reportés après MVP simulation | Blocage pour réel bancaire ; respect des contrats existants inchangé |
| DEC-02 | **Tests exhaustifs de permissions** initiation/challenges et propriété d’objet reportés | Ne pas assimiler quatre codes HTTP à couverture sécurité complète |
| DEC-03 | **Audit de l’ouverture réseau non interne** du backend reporté | Surface latérale Docker non homologuée ; ne pas élargir sans revue |
| DEC-04 | **Durcissement Keycloak `start-dev`/H2**, politiques et exploitation reportés | Non adapté production ; prévoir DB/support, sauvegarde, rotation, accès admin après décision |
| DEC-05 | **Sauvegardes et restaurations PostgreSQL/LDAP** déjà reportées séparément | Risque de perte de données de test accepté temporairement, pas gate réussi |
| DEC-06 | Route consultation à harmoniser **avant production** vers `GET /v1/payments/{paymentReference}` | N’agir qu’après preuve de code/runtime, approbation des contrats et mise à jour synchronisée de Caddy/tests |
| DEC-07 | Certificat IP court, renouvellement futur non observé | Contrôle de validité/renouvellement approuvé avant échéance, sans accès aux clefs privées |
| DEC-08 | Différence versions GitHub et images déployées ; P10/P11 manquants | Récupérer preuves expurgées avant approbation finale du guide |

**Distinction impérative :** décision de clôture *simulation MVP* ≠ acceptation d’un risque pour l’exploitation bancaire réelle. Aucun contrat différé n’est activé par ce guide. Aucun nouveau composant, contrat, endpoint, table, rôle ou permission n’est introduit.

## 13. Annexes et traçabilité

### 13.1 Matrice chemins / propriétaire / confidentialité

| Chemin | Propriétaire attendu/observé | Partage autorisé |
|---|---|---|
| `/etc/caddy/Caddyfile` | root / Caddy selon ACL système, **à confirmer** | version expurgée après revue |
| `/etc/caddy/Caddyfile.before-sixpay-g7.20` | sauvegarde root historique R05 | pas de partage brut sans revue |
| `/opt/sixpay/compose/docker-compose*.yml` | root/administration à confirmer | uniquement champs non sensibles expurgés |
| `/opt/sixpay/config/backend.env` | **configuration sensible** | **INTERDIT** de copier ou afficher |
| `/opt/sixpay/config/frontend/runtime-config.json` | config runtime non nécessairement secrète | filtrer valeurs avant partage |
| `/opt/sixpay/secrets/**` | secrets DB/LDAP/JVM, droits restreints | **INTERDIT** de copier, lire ou afficher |
| `/var/lib/sixpay/postgres/data` | UID/GID `999:999`, mode 700 R03 | pas d’accès au contenu |
| persistance Keycloak H2 | chemin à confirmer sur VPS | aucun dump/export non approuvé |
| répertoires Docker volumes | daemon Docker, à inventorier | métadonnées seulement |

### 13.2 Matrice de traçabilité des 12 exigences du guide

| Exigence | Sections | Sources | Couverture |
|---|---|---|---|
| 1 OS et logiciels | 1.3, 2 | R01, R05, DECL | partielle : noyau, heure, mises à jour à confirmer |
| 2 comptes/permissions/SELinux | 2, 13.1 | R01–R04 | partielle : ACL Keycloak et sudoers à confirmer |
| 3 arborescence/services/sauvegardes | 3, 11.3 | R03–R05, DECL | partielle : volumes H2/Samba à confirmer |
| 4 réseaux et flux | 1, 4, 6.1 | R05, DECL | logique couverte ; gateway/subnets restants à confirmer |
| 5 GHCR/digests/versions | 1.3, 5 | R03–R05, CODE | anciens digests prouvés ; labels actuels à confirmer |
| 6 Caddy/TLS | 6 | R05, DECL | routes déclarées ; Caddyfile G7.21 exact manquant |
| 7 Keycloak/realm | 7.1 | DECL, CODE, R07 | configuration fonctionnelle déclarée ; création exacte non prouvée |
| 8 Backend/LDAP/PG | 7.2, 8 | R03–R04, CODE, DECL | couverte avec limites de secrets et des versions |
| 9 chronologie avec échecs | 9 | R01–R05, DECL | couverte par identifiants OP01–OP16 ; toutes les lignes de commande originales restent aux runbooks |
| 10 tests positifs/négatifs | 10 | R04–R05, DECL | tests réels vs reportés distingués |
| 11 exploitation/risques | 11–12 | R01–R05, DECL | diagnostic non destructif, aucun changement |
| 12 annexes matrices/PowerShell | 6.1, 11.2, 13 | R01–R05, DECL | couverte ; script original G7.21 indisponible |

### 13.3 Références auditables

* GitHub HEAD : `https://github.com/SIXGEN-Solutions/sixpay-connect/tree/04808a26984e4919a29f9e36493abcf9f102d960`.
* Dossier runbooks : `https://github.com/SIXGEN-Solutions/sixpay-connect/tree/04808a26984e4919a29f9e36493abcf9f102d960/documentation/runbooks/deploy_cloud_mvp`.
* Publication GHCR initiale : `https://github.com/SIXGEN-Solutions/sixpay-connect/actions/runs/42842816703` (R03).
* Registry : `documentation/contracts/CONTRACT_REGISTRY.yaml` ; contrats externes `payment-command-api-v1.yaml`, `payment-consultation-api-v1.yaml`.
* Code consulté : `frontend/Dockerfile`, `frontend/nginx.conf`, `infrastructure/docker/m2m-keycloak/docker-compose.yml`.
* Preuves terrain G7.21 : **uniquement `DECL-12NOV`** en attendant P10/P11 et relevés expurgés.

### 13.4 Registre des preuves à recueillir lors d’un audit autorisé

| ID | Preuve recherchée, sans secret | Relevé à produire | État |
|---|---|---|---|
| EV01 | `hostnamectl`, `uname -r`, `timedatectl`, `rpm -q` | date/heure UTC, versions et code retour | NON EXÉCUTÉ |
| EV02 | `id`, `stat`, `ls -ldZ`, `ss`, `firewall-cmd` | UID/groupes/modes/sockets expurgés | NON EXÉCUTÉ |
| EV03 | Docker `ps`, network inspect ciblé, `docker port` | ports, bridges, gateway et subnets, sans Env | NON EXÉCUTÉ |
| EV04 | image inspect IDs/digests/labels | matrix composants ↔ SHA OCI | NON EXÉCUTÉ |
| EV05 | validation Caddy + Caddyfile revu | hash, routes et ACL, sans secrets | NON EXÉCUTÉ |
| EV06 | TLS de chacun des quatre hosts | SAN, émetteur, dates, verify result | NON EXÉCUTÉ |
| EV07 | client M2M realm/scopes/mappers expurgés | structure seulement | NON EXÉCUTÉ |
| EV08 | scripts Windows + réponses HTTP sans token | verdict, pas de payload de paiement | DÉCLARÉ, ORIGINAL NON FOURNI |
| EV09 | preuve backup/restore PostgreSQL/LDAP/H2 | RPO/RTO + restauration sur système jetable | DIFFÉRÉ |

## 14. Bilan et critères d’approbation

**Faits constatés dans cette analyse :** branche GitHub consultée à `04808a…`, gouvernance et contrats lus au HEAD, runbooks R01–R09 disponibles, code frontend/Nginx et compose Keycloak *local* consultés. **Faits terrain :** ceux de `OBS-Rxx` et `DECL-12NOV`, sans nouvelle connexion au VPS. **Conclusions :** le parcours simulation UX/LDAP/HTTPS/M2M à droits minimaux est documenté avec limites ; aucune preuve nouvelle d’état runtime ou de conformité de production. **Modifications réalisées :** création du **présent fichier documentaire local de livraison uniquement**, aucune écriture GitHub, aucune intervention VPS, aucune branche/commit/push/PR/déploiement. **Validations exécutées :** lectures GitHub uniquement, aucun test automatisé/gate/curl/SSH/commande VPS. **Validations bloquées :** revalidation terrain (absence d’accès), Caddyfile exact G7.21, labels images et deux nouveaux runbooks P10/P11 (non fournis dans les sources consultables). **Statut : `INCOMPLETE_CONTEXT`** pour l’exhaustivité documentaire ; **pas `REPOSITORY_VALIDATED`**. **Action humaine restante :** apporter P10/P11 et preuves expurgées, valider le classement sécurité/risques, approuver ce guide avant réutilisation hors simulation.

**Règle finale :** aucune procédure de ce document ne doit déclencher une mutation sans autorisation explicite, préconditions vérifiées et fenêtre de changement dédiée.
