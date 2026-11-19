# SIXPAY CONNECT — Runbook consolidé G7.18 à G7.20

**Date d'état terrain :** 12 novembre 2026 (journaux du VPS en CET/UTC, poste de l'opérateur en Amérique du Nord).  
**Repository :** `SIXGEN-Solutions/sixpay-connect`  
**Branche de référence observée sur GitHub :** `feat/repository-baseline-consolidation-cucumber`  
**HEAD GitHub contrôlé :** `116e8130d30b73a859d07bc21ce64b21848bfca1`  
**Mode de rédaction :** lecture seule GitHub + consolidation des commandes et sorties communiquées lors des travaux ; **aucun nouveau test VPS exécuté pendant la rédaction**.  
**Statut :** G7.18–G7.20 **opérationnels pour tests développeurs** ; ce document n'est ni une homologation bancaire ni un `REPOSITORY_VALIDATED`.

> **Règle de lecture.** « Confirmé » = preuve GitHub inspectée ou sortie d'exécution communiquée ; « rapporté » = résultat fourni par l'opérateur, sans logs complets ; « non démontré » = ne pas inférer. Les anciens handoffs peuvent signaler des étapes ouvertes désormais clôturées ; ils restent des instantanés historiques, pas l'état runtime courant.

## 1. Périmètre, décision d'architecture et état final

Objectif : publier **une image frontend Angular unique et immuable**, configurable par un JSON de runtime (sans recompilation par environnement), démarrant même si le backend est indisponible ; déployer frontend + backend existant sur Contabo ; exposer **uniquement** un reverse proxy HTTPS public et conserver PostgreSQL, LDAP et backend privés. Environnement **de test développeurs**, données fictives seulement. Pour La Régionale, Caddy est un choix Contabo **non prescriptif** : une capacité reverse proxy/ingress/TLS existante chez la banque est réutilisable si elle respecte les exigences.

Architecture effective :

```text
Développeur navigateur HTTPS → 45.85.146.52:443/TCP
                                      │
                           Caddy 2.11.7 (hôte)
                           :80 → :443, TLS LE
                                      │ proxy HTTP loopback
                              127.0.0.1:8088
                                      │
                Docker sixpay_frontend_access (bridge non interne)
                                      │
                         frontend Nginx 1.27 + Angular
                           ├── fichiers Angular, runtime-config.json
                           └── /api/*, /internal/api/* → backend:8080
                                      │
                 Docker sixpay_backend (bridge, internal:true)
                           ├── Backend Spring Boot :8080
                           ├── PostgreSQL :5432
                           └── Samba AD LDAP/LDAPS interne
```

**Réseaux et contraintes observés :** `sixpay-connect-cloud_sixpay_backend` préexistant, `internal: true` sans gateway ; `sixpay-connect-cloud_sixpay_frontend_access` ajouté pour le seul frontend ; Caddy sur hôte et frontend publié exclusivement sur `127.0.0.1:8088`. Réseaux backend/PostgreSQL/LDAP non modifiés lors du déploiement frontend. L'image du frontend est épinglée par digest. Le port d'administration Caddy `127.0.0.1:2019` reste loopback.

Images approuvées/observées dans la séquence :

- Frontend : `ghcr.io/sixgen-solutions/sixpay-frontend@sha256:84a493644def5dc51128f984c8b1073ab74be9f984616a81d2d2561703c33b89` (source `116e8130d30b73a859d07bc21ce64b21848bfca1`, tag `sha-116e8130d30b73a859d07bc21ce64b21848bfca1`).
- Backend : `ghcr.io/sixgen-solutions/sixpay-backend@sha256:1958f3bf753129ea6fde73e9e7dba2a666fcc5d6d6c04205411a148dd512951b` (constat de l'handoff antérieur, **ne pas remplacer sans réinspection**).
- Images PostgreSQL/Samba : garder les digests existants ; aucune modification réalisée dans G7.18–G7.20.

## 2. G7.18 — Frontend indépendant de l'environnement et du backend

### 2.1 Sources GitHub vérifiées au HEAD

- `frontend/Dockerfile` : builder `node:22.23.1-alpine`, `npm ci --no-audit --no-fund`, `npm run build` **sans `BUILD_CONFIGURATION`** ; runtime `nginx:1.27-alpine`, copie sous `/usr/share/nginx/html`, port 80 ; `ENV SIXPAY_BACKEND_UPSTREAM=backend:8080` ; `nginx.conf` copié dans `/etc/nginx/templates/default.conf.template`.
- `frontend/nginx.conf` : resolver Docker `127.0.0.11 valid=10s ipv6=off`; variable Nginx `set $sixpay_backend_upstream "${SIXPAY_BACKEND_UPSTREAM}"`; `proxy_pass http://$sixpay_backend_upstream` **sans suffixe URI** pour préserver `/api/...`; proxy `/api/` et `/internal/api/`; `location = /actuator` et `location ^~ /actuator/` retournent `404` ; `location = /runtime-config.json` (JSON, `Cache-Control: no-store, max-age=0`, `nosniff`), `try_files ... =404`; fallback SPA sur `/index.html`.
- `frontend/src/main.ts` : import dynamique de `environment.runtime`, appel `loadRuntimeEnvironment()` **avant** l'import Angular, des providers et composants ; refus de démarrage avec message neutre si configuration invalide (pas de fallback mock).
- Les scripts locaux de préparation observés : `apply_sixpay_frontend_runtime.py`, `apply_sixpay_frontend_bootstrap_fix.py`, `apply_sixpay_frontend_backend_independent.py`. Ce sont des correctifs préparatoires locaux ; ils ne prouvent pas seuls une commande réussie, mais le code présent au HEAD confirme le résultat.

### 2.2 Incidents fonctionnels et corrections

| Incident / test | Observation | Correction / preuve |
|---|---|---|
| Image liée au profil Angular par `BUILD_CONFIGURATION` | Pas de garantie « build once, deploy everywhere » | `npm run build` sans argument de profil ; JSON runtime externalisé ; Dockerfile courant inspecté |
| Bootstrap Angular évaluant potentiellement des providers avant config | Risque d'initialisation avec environnement incorrect | Chargement séquentiel de `environment.runtime`, puis imports dynamiques Angular et application ; `main.ts` inspecté |
| Nginx résolvant `backend` au démarrage | Backend absent → démarrage Nginx impossible | Variable upstream, DNS Docker différé à l'appel ; backend absent → 502 sur l'API, frontend accessible |
| Deux containers d'environnement différents | Test local rapporté : même image `sha256:b2e7a39e44dfc27a26d07c7eac0096a722fd630e20997256b58570ca3521f13d` ; configurations LDAP et LOCAL distinctes | Écrans de connexion distincts avec **même** image ; test de non-régression rapporté |

**Attention :** la configuration LDAP du VPS est différente de l'exemple LOCAL, mais l'image frontend est identique par construction.

### 2.3 Workflow de publication GHCR

Fichier GitHub **inspecté** : `.github/workflows/frontend-image-publish.yml`.

- Déclenchement **manuel** `workflow_dispatch`; `contents: read`, `packages: write`.
- Checkout SHA sélectionné, Node depuis `frontend/.node-version`, `npm ci --no-audit --no-fund`.
- Gates : `verify:angular-environments`, `verify:runtime-datasource-policy`, `verify:contract-consolidation`, `verify:full-stack-conformance`, `verify:integration-contract-backed`, `npm test`, `npm run build`, `verify:runtime-image`.
- Docker Buildx `linux/amd64`, GitHub token vers GHCR, tag immuable `sha-${GITHUB_SHA}`, sortie du digest validée sur `sha256:[hex]{64}`.
- Image publiée : `ghcr.io/sixgen-solutions/sixpay-frontend@sha256:84a493644def5dc51128f984c8b1073ab74be9f984616a81d2d2561703c33b89`.
- Le succès du workflow et la publication sont **rapportés dans l'historique du chantier**, mais l'exécution exacte Actions/job ID n'a pas été revérifiée dans cette rédaction ; conserver l'URL du run si nécessaire à une preuve d'audit CI.

**Méthode de réutilisation :** pour toute nouvelle version, revalider branche/HEAD, lancer manuellement le workflow approuvé, relever son digest, déployer ce digest, jamais un tag flottant.

## 3. G7.19 — Déploiement frontend sur VPS

### 3.1 Localisation, comptes, sécurité

- VPS `vmi3671192` (`45.85.146.52`), AlmaLinux **10.2**, SELinux **Enforcing**.
- Utilisateur admin `sixpayadmin` via SSH à clé, `sudo` pour les commandes Docker, firewalld et fichiers root.
- Racine Compose : `/opt/sixpay/compose` ; configuration frontend externe : `/opt/sixpay/config/frontend/runtime-config.json`.
- Le parent `/opt/sixpay/config` appartient à root avec permissions restrictives ; une écriture directe par `sixpayadmin` a été refusée. Correction : création des dossiers avec privilèges, `sudo tee` pour écrire le fichier. Ne pas ouvrir les droits du parent de manière globale ; conserver l'accès minimal.
- Runtime JSON ne contient pas de secrets, fichier monté `:ro`; permissions fichier constatées `0644` et répertoire frontend `0750` dans le parcours. Vérifier labels SELinux si un futur montage échoue ; ne pas désactiver SELinux.

Configuration runtime effectivement testée :

```json
{
  "production": true,
  "apiBaseUrl": "",
  "backend": { "mode": "api" },
  "authentication": {
    "standalone": false,
    "local": { "enabled": false },
    "oidc": { "enabled": false },
    "ldap": { "enabled": true }
  }
}
```

Configuration Compose ajoutée sur le VPS (`docker-compose.frontend.yml`, reconstruction de l'état final observé) :

```yaml
services:
  frontend:
    image: ghcr.io/sixgen-solutions/sixpay-frontend@sha256:84a493644def5dc51128f984c8b1073ab74be9f984616a81d2d2561703c33b89
    restart: unless-stopped
    environment:
      SIXPAY_BACKEND_UPSTREAM: backend:8080
    ports:
      - "127.0.0.1:8088:80"
    volumes:
      - /opt/sixpay/config/frontend/runtime-config.json:/usr/share/nginx/html/runtime-config.json:ro
    networks:
      - sixpay_backend
      - sixpay_frontend_access

networks:
  sixpay_frontend_access:
    external: true
    name: sixpay-connect-cloud_sixpay_frontend_access
```

Le réseau logique `sixpay_backend` provient des Compose existants ; la définition du réseau est **combinée** au moyen des quatre fichiers, et non autonome dans cet overlay.

### 3.2 Commandes exécutées ou utiles, résultat

```bash
# Vérification syntaxique des quatre Compose — aucune erreur rapportée
cd /opt/sixpay/compose
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml -f docker-compose.frontend.yml config --quiet

# Premier déploiement frontend, préservation des dépendances existantes
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml -f docker-compose.frontend.yml up -d --no-deps --no-recreate frontend
# Retour : Container sixpay-connect-ldap-frontend-1 Started

# Investigation du port publié déclaré mais non effectif
sudo docker inspect sixpay-connect-ldap-frontend-1 --format '{{json .HostConfig.PortBindings}}'
# {"80/tcp":[{"HostIp":"127.0.0.1","HostPort":"8088"}]}
sudo docker inspect sixpay-connect-ldap-frontend-1 --format '{{json .NetworkSettings.Ports}}'
# {"80/tcp":null} — échec de publication effective
curl --noproxy '*' -i --max-time 10 http://127.0.0.1:8088/
# curl: (7) Failed to connect ...

# Inventaire des réseaux — aucun bridge d'accès dédié
sudo docker network ls --format 'table {{.Name}}\t{{.Driver}}\t{{.Scope}}'
# Réseaux bridge/host/none et sixpay-connect-cloud_sixpay_backend (internal:true)

# Création d'un bridge d'accès indépendant — Docker a retourné un ID
sudo docker network create --driver bridge --label com.sixpay.purpose=frontend-access sixpay-connect-cloud_sixpay_frontend_access
# 04f25ebab2fbae9e78d405293925024ee9ff9ddb14c9fa9c6a5174c526d1e550

# Ajout de sixpay_frontend_access au seul frontend dans Compose, puis validation
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml -f docker-compose.frontend.yml config --quiet
# Aucune erreur

# Recréation du seul frontend pour prise en compte des deux réseaux
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml -f docker-compose.frontend.yml up -d --no-deps --force-recreate frontend

# Test réussi après correction
curl --noproxy '*' -i --max-time 10 http://127.0.0.1:8088/
# HTTP/1.1 200 OK ; Server: nginx/1.27.5 ; HTML SIXPAY CONNECT
curl --noproxy '*' -i --max-time 10 http://127.0.0.1:8088/runtime-config.json
# 200, application/json, Cache-Control: no-store, max-age=0 ; ldap=true
curl --noproxy '*' -i --max-time 10 http://127.0.0.1:8088/api/v1/auth/me
# 401 sans session ; Set-Cookie JSESSIONID Secure HttpOnly SameSite=Strict
```

**Diagnostic réseau :** le bridge Docker interne était la cause **probable** du mapping de port inopérant ; le test concluant après ajout du second réseau établit la correction empirique, mais aucune preuve exhaustive du moteur Docker n'a été recueillie. Ne pas rendre `sixpay_backend` public.

### 3.3 Accès temporaire depuis Windows par SSH — échecs et correction

```powershell
# ÉCHEC : aucune clé présentée acceptée
ssh -N -L 127.0.0.1:8088:127.0.0.1:8088 sixpayadmin@45.85.146.52
# Permission denied (publickey,gssapi-keyex,gssapi-with-mic)

# Inventaire des noms de clés (sans afficher le contenu)
Get-ChildItem "$env:USERPROFILE\.ssh" -Force | Select-Object Name
# Présence de sixpay_contabo_ed25519 et id_rsa (privées et .pub)

# ÉCHEC malgré bonne clé : port local Windows 8088 refusé
ssh -i "$env:USERPROFILE\.ssh\sixpay_contabo_ed25519" -o IdentitiesOnly=yes -N -L 127.0.0.1:8088:127.0.0.1:8088 sixpayadmin@45.85.146.52
# bind [127.0.0.1]:8088: Permission denied; Could not request local forwarding

# SUCCÈS : autre port local et fail-fast
ssh -i "$env:USERPROFILE\.ssh\sixpay_contabo_ed25519" -o IdentitiesOnly=yes -o ExitOnForwardFailure=yes -N -L 127.0.0.1:18088:127.0.0.1:8088 sixpayadmin@45.85.146.52
# Navigateur : http://127.0.0.1:18088 ; login LDAP rapporté réussi
```

**Ne pas confondre** chiffrement SSH du tunnel et HTTPS côté navigateur ; conserver le cookie `Secure`. Le tunnel est maintenant facultatif puisque HTTPS est public.

## 4. G7.20 — Caddy, SELinux, Firewalld, ACME et HTTPS public

### 4.1 Audit initial et installation

```bash
sudo ss -lntp
# Avant Caddy : SSH 0.0.0.0:22 ; frontend 127.0.0.1:8088 ; rien sur 80/443
sudo firewall-cmd --state
# running
sudo firewall-cmd --list-all
# zone public, eth0 ; dhcpv6-client ssh ; ports vides
command -v caddy || echo "Caddy non installé"
# Caddy non installé
cat /etc/os-release
# AlmaLinux 10.2 (Lavender Lion), platform:el10
sudo dnf info caddy
# Premier essai : Error: No matching Packages to list
sudo dnf copr --help
# Plugin COPR fonctionnel, enable/disable disponibles
sudo dnf copr enable @caddy/caddy
# Confirmation interactive y ; Repository successfully enabled
sudo dnf info caddy
# Disponible : caddy 2.11.7-1.el10 x86_64 (COPR @caddy/caddy)
sudo dnf install caddy
# Confirmation transaction puis clé GPG y ; Installed: caddy-2.11.7-1.el10.x86_64 ; Complete!
```

**Messages non bloquants durant RPM :** `restorecon: lstat(/var/log/caddy) failed: No such file or directory`, `Port udp/80 already defined, modifying instead`, `Port udp/443 already defined, modifying instead`. Ces messages ne prouvent ni une ouverture Firewalld ni un conflit de bind. Service après installation : `inactive (dead)`, `disabled`.

### 4.2 Sauvegarde et configuration Caddy

```bash
sudo systemctl status caddy --no-pager
# Avant configuration : Loaded ... disabled ; Active: inactive (dead)
sudo cat /etc/caddy/Caddyfile
# Fichier de démonstration : :80 { root * /usr/share/caddy ; file_server }
sudo cp -a /etc/caddy/Caddyfile /etc/caddy/Caddyfile.before-sixpay-g7.20
# Sans sortie ; sauvegarde conservée
```

Configuration réellement créée et validée :

```caddyfile
{
    default_sni 45.85.146.52
}

45.85.146.52 {
    tls {
        issuer acme {
            profile shortlived
        }
    }

    encode zstd gzip

    header {
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }

    reverse_proxy 127.0.0.1:8088
}
```

Écriture effectuée via `sudo tee /etc/caddy/Caddyfile > /dev/null <<'EOF' ... EOF` (ne pas écraser le fichier sans revue préalable lors d'une reprise).

```bash
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
# Valid configuration ; WARN de formatage « Caddyfile input is not formatted » non bloquant
getenforce
# Enforcing
sudo systemctl cat caddy
# User=caddy Group=caddy ; ProtectSystem=full ; PrivateTmp=true ; CAP_NET_BIND_SERVICE
ls -Z /usr/bin/caddy /etc/caddy/Caddyfile
# httpd_exec_t et httpd_config_t
sudo getsebool httpd_can_network_connect
# httpd_can_network_connect --> on
sudo systemctl start caddy
sudo systemctl status caddy --no-pager
# Active: active (running) ; erreurs ACME visibles mais initialement tronquées
sudo journalctl -u caddy -n 80 --no-pager -o cat
```

**Incidents ACME :** `tls-alpn-01` sur TCP 443 puis `http-01` sur TCP 80 refusés (`Error getting validation data`) parce que l'autorité ne pouvait pas joindre l'IP, en cohérence avec Firewalld non ouvert. Ne pas accuser SELinux sans AVC. Caddy réessaie automatiquement. La première tentative production puis staging a échoué ; **ne pas confondre certificat staging avec certificat publiquement reconnu**.

### 4.3 Ouverture réseau contrôlée et certificat

```bash
# Autorisation active, initialement temporaire : success
sudo firewall-cmd --zone=public --add-service=http --add-service=https

# Journaux après nouvelle tentative
sudo journalctl -u caddy -n 40 --no-pager -o cat
# http-01 : served key authentication ; authz_status=valid
# validation succeeded ; certificate obtained successfully
# issuer : acme-v02.api.letsencrypt.org-directory (production)

# Permanent : success
sudo firewall-cmd --permanent --zone=public --add-service=http --add-service=https
sudo firewall-cmd --permanent --zone=public --list-all
# services: dhcpv6-client http https ssh
sudo firewall-cmd --zone=public --list-all
# zone public active eth0, mêmes services, aucun port/rich rule supplémentaire

# Activation au boot de Caddy
sudo systemctl enable caddy
# Created symlink ...multi-user.target.wants/caddy.service
sudo systemctl is-enabled caddy && sudo systemctl is-active caddy
# enabled / active
```

**Certificat observé** :

```bash
echo | openssl s_client -connect 127.0.0.1:443 -servername 45.85.146.52 2>/dev/null | openssl x509 -noout -issuer -dates -ext subjectAltName
# issuer=C=US, O=Let's Encrypt, CN=YE2
# notBefore=Nov 11 23:08:42 2026 GMT
# notAfter=Nov 18 15:08:41 2026 GMT
# IP Address:45.85.146.52
sudo journalctl -u caddy --no-pager -o cat | grep -E 'got renewal info|certificate obtained successfully|renewing certificate|renewal'
# got renewal info (fenêtres ACME), certificate obtained successfully
```

**Limite :** Caddy dispose d'informations ACME de renouvellement ; **aucun renouvellement effectivement abouti** n'a encore été observé. Le certificat à durée courte expire le **18/11/2026 à 15:08:41 UTC**. Il faut suivre sa rotation. Ne pas annoncer de renouvellement réussi anticipativement.

### 4.4 Tests externes depuis Windows, résultats effectifs

```powershell
curl.exe -i --max-time 15 https://45.85.146.52/
# HTTP/1.1 200 OK ; HTML SIXPAY CONNECT ; Via: 1.1 Caddy ; Server: nginx/1.27.5

curl.exe -i --max-time 15 https://45.85.146.52/api/v1/auth/me
# HTTP/1.1 401 Unauthorized sans session
# Set-Cookie: JSESSIONID=...; Path=/; Secure; HttpOnly; SameSite=Strict

curl.exe -I --max-time 15 http://45.85.146.52/
# HTTP/1.1 308 Permanent Redirect ; Location: https://45.85.146.52/

curl.exe -i --max-time 15 https://45.85.146.52/actuator/health
# 404 Not Found, HTML Nginx, aucune donnée Actuator

curl.exe -i --max-time 15 https://45.85.146.52/swagger-ui/index.html
# 200 MAIS contenu Angular index.html — fallback SPA, PAS Swagger UI

curl.exe -i --max-time 15 https://45.85.146.52/v3/api-docs
# 200 MAIS contenu Angular index.html — PAS de JSON OpenAPI

curl.exe -i --max-time 15 https://45.85.146.52/api/v3/api-docs
# 401 Unauthorized, aucun JSON anonyme

curl.exe -i --max-time 15 https://45.85.146.52/api/actuator/health
# 401 Unauthorized, aucune information de santé anonyme
```

**Authentification navigateur :** connexion `sixpay.admin` réussie via HTTPS public ; après redémarrage intégral du VPS, connexion `sixpay.testuser` réussie également. Aucune information de mot de passe/session n'est conservée ici.

**Point résiduel HTTP :** `X-Content-Type-Options: nosniff` observé **deux fois** sur la réponse backend traversant Caddy/Nginx ; déduplication envisageable après revue, non bloquante, pas de modification réalisée. Les chemins Swagger sans `/api` renvoient le fallback SPA (HTTP 200), à considérer comme une amélioration d'hygiène potentielle si des règles explicites 404 sont souhaitées ; **pas une exposition Swagger démontrée**.

### 4.5 Audit des sockets et isolation Docker

```bash
sudo ss -lntup
# SSH :22 public ; Caddy :80 TCP/:443 TCP+UDP ; admin Caddy 127.0.0.1:2019
# frontend 127.0.0.1:8088 ; chronyd loopback :323/udp
sudo docker ps --format 'table {{.Names}}\t{{.Ports}}'
# frontend 127.0.0.1:8088->80/tcp
# backend 8080/tcp ; postgres 5432/tcp ; Samba ports internes sans host publication
sudo firewall-cmd --zone=public --list-all
# services: dhcpv6-client http https ssh ; forward: yes
```

**Nuance importante :** `ss` montre les sockets de l'hôte et `docker ps` les publications Docker ; cette combinaison ne remplace pas un pentest ni un audit complet NAT/IPv6. HTTP/3 écoute en UDP 443, mais l'autorisation UDP de Firewalld ne peut pas être déduite du service `https` sans inspection détaillée ; elle n'est pas un prérequis pour HTTP/1.1 et HTTP/2.

## 5. Résilience et redémarrage complet — validés

Le frontend et PostgreSQL avaient déjà `restart: unless-stopped`. Le backend et LDAP avaient `restart: "no"` **dans les fichiers Compose et les conteneurs** ; la politique a été corrigée pour conserver le service après reboot.

```bash
# Diagnostic initial
sudo docker inspect sixpay-connect-ldap-backend-1 sixpay-connect-cloud-postgres-1 sixpay-cloud-ldap --format '{{.Name}} : {{.HostConfig.RestartPolicy.Name}}'
# backend=no ; postgres=unless-stopped ; ldap=no

# Localisation initiale des directives
sudo grep -nE '^[[:space:]]*(backend|ldap|restart):' docker-compose.yml docker-compose.backend.yml docker-compose.ldap.yml docker-compose.frontend.yml
# backend.yml ligne 9 restart: "no" ; ldap.yml ligne 9 restart: "no"

# Mise à jour ciblée des deux Compose par script Python, résultat rapporté :
# Mis à jour : docker-compose.backend.yml
# Mis à jour : docker-compose.ldap.yml
# Les deux contiennent désormais restart: unless-stopped

sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml -f docker-compose.frontend.yml config --quiet
# Résultat rapporté OK ; absence d'erreur

# Modification effective sur les conteneurs EXISTANTS sans redémarrage
sudo docker update --restart unless-stopped sixpay-connect-ldap-backend-1
# sixpay-connect-ldap-backend-1
sudo docker update --restart unless-stopped sixpay-cloud-ldap
# sixpay-cloud-ldap

sudo docker inspect sixpay-connect-ldap-frontend-1 sixpay-connect-ldap-backend-1 sixpay-connect-cloud-postgres-1 sixpay-cloud-ldap --format '{{.Name}} : {{.HostConfig.RestartPolicy.Name}}'
# 4 × unless-stopped
sudo systemctl is-enabled docker && sudo systemctl is-active docker
# enabled / active

# Reboot autorisé explicitement par l'opérateur
sudo systemctl reboot
# SSH interrompu normalement
```

Reconnexion Windows :

```powershell
ssh -i "$HOME\.ssh\sixpay_contabo_ed25519" -o IdentitiesOnly=yes sixpayadmin@45.85.146.52
# Bannière Contabo, invite [sixpayadmin@vmi3671192 ~]$
```

Contrôles après reboot :

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Status}}'
# frontend Up 38 seconds
# backend Up 38 seconds
# ldap Up 38 seconds
# postgres Up 37 seconds (healthy)
sudo systemctl is-enabled caddy && sudo systemctl is-active caddy
# enabled / active
```

Puis côté Windows : `curl.exe -i --max-time 15 https://45.85.146.52/api/v1/auth/me` → HTTP 401 attendu, certificat accepté, `Via: 1.1 Caddy`, cookie sécurisé. Connexion navigateur LDAP `sixpay.testuser` **réussie après reboot**. Ceci constitue la preuve de reprise ciblée de G7.20 ; **pas** un test de restauration de données ou de reprise après perte de stockage.

## 6. Contrôles validés, risques, décisions et limites

| Domaine | État / preuve | Reste à réaliser |
|---|---|---|
| Build frontend agnostique | Code GitHub inspecté et scénarios LDAP/LOCAL sur même image rapportés | Vérifier les gates pour tout nouveau commit |
| GHCR frontend | Workflow inspecté ; digest de publication identifié et déployé | Conserver run ID et traces de gates pour audit |
| HTTPS IP publique | `200`, redirection `308`, LE public, LDAP réussi | Contrôle périodique TLS ; futur DNS après décision |
| Sécurité accès | SELinux Enforcing ; `httpd_can_network_connect=on`; Firewalld 80/443/SSH ; ports backend/DB/LDAP non publiés | Vérifier sécurité réseau IPv6/NAT et scanner externe approuvé |
| Protection endpoints | `/actuator/health` 404 ; `/api/v3/api-docs`, `/api/actuator/health` 401 | Revue exhaustive des routes publiques/ACL, y compris utilisateurs authentifiés |
| Sessions | Cookie Secure/HttpOnly/SameSite=Strict ; LDAP admin/testuser fonctionnel | CSRF, expiration, verrouillage, permissions E2E et en-têtes forwarded complets |
| Résilience | reboot VPS + frontend/backend/PG/LDAP + Caddy OK | Tests backup/restore reportés, audit de rollback |
| Certificats | LE IP `notAfter=2026-11-18 15:08:41Z`, ARI enregistré | **Renouvellement réel non encore observé** |
| M2M / paiements | Non déployé/validé en cloud | G7.21+, ne pas activer endpoints financiers prématurément |

**Décision explicite :** sauvegardes/restaurations PostgreSQL et LDAP **mises en pause** pour la suite à la demande du responsable projet. Il s'agit d'un **risque accepté temporairement pour l'environnement de tests**, pas d'un gate validé. Interdire données réelles, paiements réels, ou promotion en production tant que restauration, gestion de secrets, mTLS, M2M et sécurité métier ne sont pas validés. Keycloak persistant doit disposer d'une stratégie de sauvegarde documentée avant toute configuration non jetable.

**Ce qui n'a pas été fait dans la rédaction du runbook :** aucune modification GitHub, aucun commit/push, aucune commande VPS, aucune réexécution des gates CI/Maven/Angular ni validation dynamique du renouvellement TLS.

## 7. Références repo et convention de reprise

- `AGENTS.md`, `ENGINEERING_CONTEXT.md`, `MASTER_PROMPT_INPUT_MANIFEST.yaml`, `MASTER_ENGINEERING_PROMPT.md` (ordre de chargement obligatoire, registres de contrats et statuts).
- `frontend/Dockerfile`, `frontend/nginx.conf`, `frontend/src/main.ts`, `.github/workflows/frontend-image-publish.yml` — vérifiés au HEAD indiqué.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_CLOUD_MVP_RUNBOOK.md` — programme général.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_PROMPT_REPRISE_DEPLOIEMENT_CONTABO.md` — ancien handoff à actualiser, ne pas traiter ses « non déployé » comme une vérité actuelle.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_CONTABO_LDAP_PROVISIONNEMENT.md` — preuves LDAP déjà acquises.
- `infrastructure/docker/m2m-keycloak/docker-compose.yml` — **local `start-dev` sans DB persistante ; à ne pas installer sur VPS tel quel**.
- `/opt/sixpay/compose/{docker-compose.yml,docker-compose.backend.yml,docker-compose.ldap.yml,docker-compose.frontend.yml}` ; `/etc/caddy/Caddyfile` ; `/etc/caddy/Caddyfile.before-sixpay-g7.20` ; `/opt/sixpay/config/frontend/runtime-config.json` — chemins VPS observés, leur contenu doit être réinspecté avant une modification future.

**Préservation :** une seule commande VPS à la fois ; conserver volumes, réseaux et secrets ; ne pas exécuter `down -v`, `flyway clean`, ni relancer bootstrap admin, ni configurer de ports publics backend/LDAP/PostgreSQL. Différencier tests locaux, assertions GitHub et preuves VPS.
