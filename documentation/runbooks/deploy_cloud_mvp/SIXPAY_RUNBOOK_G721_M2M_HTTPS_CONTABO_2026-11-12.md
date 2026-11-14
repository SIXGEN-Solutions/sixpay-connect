# SIXPAY CONNECT — Runbook de clôture : Keycloak M2M, JWT, réseaux Docker et exposition HTTPS

**État terrain :** 12 novembre 2026 · **Périmètre :** continuation G7.21–G7.26, travaux M2M G45.x consignés dans la conversation · **Usage :** environnement Contabo de simulation / tests développeurs, **pas une homologation de production**.

| Attribut | Valeur |
|---|---|
| Dépôt | `SIXGEN-Solutions/sixpay-connect` |
| Branche sélectionnée | `feat/repository-baseline-consolidation-cucumber` |
| SHA de référence demandé | `b212c4bb67c8cdc63711428e0453933b3d2d7ac3` |
| HEAD GitHub observé à la rédaction | `04808a26984e4919a29f9e36493abcf9f102d960` (2 commits en avance, modifications frontend uniquement dans la comparaison) |
| Révision de l'image backend observée dans les étiquettes de conteneur | `e8a528bc18340b40c04e62a60e0730f8401a5779` ; ne pas confondre avec le HEAD GitHub |
| VPS | Contabo, `vmi3671192`, IP publique `45.85.146.52`, AlmaLinux 10.2 selon runbook G7.18–G7.20 |
| Utilisateur d'exploitation | `sixpayadmin`, SSH à clé et `sudo` |
| Mode de production de ce document | Reconstitution des commandes et sorties communiquées par l'opérateur, complétée par la lecture GitHub ; pas d'accès SSH direct ni d'exécution de commandes sur VPS pendant la rédaction |
| Statut | **PHASE_CLOSED_BY_USER** ; connectivité HTTPS/JWT testée ; plusieurs exigences contractuelles et de production explicitement reportées |

## 1. Règles de preuve et périmètre

**[OBSERVÉ]** : commande ou résultat reproduit dans les échanges, ou configuration affichée par l'opérateur. **[DOCUMENTÉ]** : contenu lu dans un runbook du dépôt, sans reconfirmation runtime pendant la phase. **[RECONSTITUÉ]** : procédure reproductible correspondant à l'architecture, mais dont l'exécution exacte n'est pas attestée. **[REPORTÉ]** : décision de remettre à après MVP.

Ce runbook complète les fichiers du dépôt `documentation/runbooks/deploy_cloud_mvp/` ; il ne les remplace pas et ne prétend pas réinventorier des commandes E1.1/E1.2/E1.3 ou G7.18–G7.20 qui n'ont pas été exécutées dans cette phase. Ne jamais utiliser d'anciens handoffs comme état actif lorsqu'ils sont contredits par les observations récentes.

## 2. Architecture effectivement obtenue

```text
CLIENT INTERNET / Windows (sans tunnel)
     | HTTPS :443 + Authorization: Bearer JWT + X-Correlation-ID
     v
api.sixgensol.com -> Caddy (service systemd sur hôte Contabo)
     | liste blanche de 5 couples méthode/chemin ; défaut 403
     v
127.0.0.1:8081 -> publication Docker vers backend:8080
     | Spring Boot profile production, Spring Security M2M
     +-- sixpay_backend (bridge, internal=true) -> PostgreSQL, Samba LDAP, frontend
     +-- sixpay_m2m_bridge (bridge, internal=true) -> Keycloak
     +-- sixpay_frontend_access (bridge, internal=false) -> réseau de publication

sixpayapp.sixgensol.com -> Caddy -> 127.0.0.1:8088 -> Nginx Angular :80
  -> /api/*, /internal/api/* -> backend:8080 par DNS Docker, sur réseau privé

auth.sixgensol.com -> Caddy -> 127.0.0.1:8180 -> Keycloak :8080
  -> chemins OIDC publics strictement limités

45.85.146.52 -> Caddy -> 127.0.0.1:8088 (configuration IP existante)
```

**[OBSERVÉ]** `frontend/proxy.conf.json` en développement vise `http://localhost:8081` ; cela **ne** décrit **pas** le proxy de production Angular. Le Nginx déployé utilise `set $sixpay_backend_upstream "backend:8080"`, avec `proxy_pass` pour `/api/` et `/internal/api/`. Le frontend a son accès externe indépendant sur `8088`. Il n'a **pas** été nécessaire d'intercaler Nginx Angular entre Caddy et l'API publique.

### 2.1 Matrice de ports et réseaux

| Composant | Port conteneur | Publication hôte observée | Réseau(x) |
|---|---|---|---|
| Caddy | Hôte | `:80` redirection HTTPS, `:443` HTTPS | Hôte Linux |
| Frontend Angular/Nginx | `80/tcp` | `127.0.0.1:8088->80` | `sixpay_backend` (interne), `sixpay_frontend_access` (non interne) |
| Backend Spring Boot | `8080/tcp` | `127.0.0.1:8081->8080` **après correction** | `sixpay_backend`, `sixpay_m2m_bridge`, `sixpay_frontend_access` |
| Keycloak | `8080/tcp` | `127.0.0.1:8180->8080` | `sixpay-m2m-keycloak-cloud_default` et `sixpay_m2m_bridge` |
| PostgreSQL SIXPAY | `5432/tcp` | Aucune | `sixpay_backend` |
| Samba AD LDAP | Ports AD/LDAP internes | Aucune | `sixpay_backend` |
| Caddy admin | Hôte | `127.0.0.1:2019` **[DOCUMENTÉ G7.18–G7.20]** | Hôte Linux |

**[OBSERVÉ]** Docker Engine `29.9.3`, storage driver `overlayfs` ; SELinux actif dans les options Docker. Réseaux bridge `sixpay-connect-cloud_sixpay_backend` : `172.18.0.0/16`, `internal=true` ; `sixpay_m2m_bridge` : `172.21.0.0/16`, `internal=true` ; `sixpay-connect-cloud_sixpay_frontend_access` : `internal=false`, initialement frontend seul. Ces adresses sont des constats temporaires, **pas des endpoints stables à inscrire dans une configuration**.

## 3. Dossiers, fichiers, identité de déploiement

| Chemin | Rôle / état |
|---|---|
| `/opt/sixpay/compose/docker-compose.yml` | Composition de base |
| `/opt/sixpay/compose/docker-compose.backend.yml` | Overlay backend, port `8081` et réseaux ; **modifié pendant cette phase** |
| `/opt/sixpay/compose/docker-compose.frontend.yml` | Overlay frontend |
| `/opt/sixpay/compose/docker-compose.ldap.yml` | Overlay LDAP |
| `/opt/sixpay/compose/docker-compose.backend.pre-m2m-network.yml` | Sauvegarde antérieure à la phase de liaison M2M, rapportée |
| `/opt/sixpay/compose/docker-compose.backend.pre-api-network.yml` | Sauvegarde de la version avant le troisième réseau ; `cmp -s` réussi |
| `/opt/sixpay/config/backend.env` | Configuration runtime backend, secrets à protéger ; paramètres M2M ajoutés antérieurement |
| `/opt/sixpay/config/backend.env.pre-m2m-oauth2` | Sauvegarde préalable à l'activation M2M, signalée |
| `/opt/sixpay/config/frontend/runtime-config.json` | Config Angular externalisée en lecture seule [DOCUMENTÉ] |
| `/opt/sixpay/keycloak/docker-compose.yml` | Stack Keycloak autonome déjà mise en place pendant la phase, contenu intégral non recopié dans l'échange |
| `/opt/sixpay/keycloak/backups/docker-compose.pre-m2m-network.yml` | Sauvegarde Keycloak antérieure au réseau M2M partagé, signalée |
| `/etc/caddy/Caddyfile` | Configuration Caddy active ; bloc `api.sixgensol.com` ajouté |
| `/etc/caddy/Caddyfile.pre-sixpay-api` | Sauvegarde initiale Caddy, vérifiée présente |
| `/etc/caddy/Caddyfile.api-candidate` | Candidat validé puis copié sur `/etc/caddy/Caddyfile` |
| `/tmp/sixpay-api-response.json` | Réponse de diagnostic `400` avec JWT sans correlation ID ; **ne pas conserver des diagnostics sensibles durablement** |
| `/tmp/sixpay-api-response-with-correlation.json` | Réponse de diagnostic `404` avec corrélation ; purge à décider avec opérateur |
| Windows `%USERPROFILE%\sixpay-m2m-test.ps1` | Script local manuel OAuth2/curl sans tunnel ; secret demandé à l'exécution |

Les **commandes initiales de création** de certains répertoires Keycloak et de persistance n'apparaissent pas intégralement dans les échanges consultables. Ne pas fabriquer de `mkdir`, `chown`, montages, règles SELinux ou valeurs `chmod` et les déclarer exécutés ; reconstituer ces étapes depuis les runbooks et l'état du VPS lors de la rédaction d'un guide de machine complet.

## 4. Keycloak M2M — configuration effective et limitations

### 4.1 Identité et exposition

**[OBSERVÉ / RAPPORTÉ]**

- Conteneur : `sixpay-m2m-keycloak-cloud-keycloak-1`, Compose `/opt/sixpay/keycloak/docker-compose.yml`.
- Realm : `sixpay-m2m-local`.
- Client OAuth2 confidentiel : `sixpay-partner-test`, `grant_type=client_credentials`.
- Endpoint token externe : `https://auth.sixgensol.com/realms/sixpay-m2m-local/protocol/openid-connect/token`.
- Endpoint discovery : `https://auth.sixgensol.com/realms/sixpay-m2m-local/.well-known/openid-configuration`.
- JWKS interne : `http://keycloak:8080/realms/sixpay-m2m-local/protocol/openid-connect/certs` ; DNS `keycloak` sur `sixpay_m2m_bridge`.
- Issuer JWT : `https://auth.sixgensol.com/realms/sixpay-m2m-local`, audience attendue `sixpay-api`.
- JWTs testés avec scopes `payment.read` et `payment.initiate`, validité constatée autour de 300 secondes.
- Le realm et la stack sont de **simulation** : Keycloak `start-dev`, stockage H2 persistant monté sur hôte, et non un déploiement Keycloak durci pour production.
- Le `client_secret` reste hors documentation et n'est jamais reproduit ; de même pour tout token, clé de signature ou mot de passe.

### 4.2 Configuration backend M2M

Paramètres d'environnement identifiés dans le conteneur avant sa dernière recréation :

```dotenv
SIXPAY_M2M_ENABLED=true
SIXPAY_M2M_ISSUER_URI=https://auth.sixgensol.com/realms/sixpay-m2m-local
SIXPAY_M2M_JWK_SET_URI=http://keycloak:8080/realms/sixpay-m2m-local/protocol/openid-connect/certs
SIXPAY_M2M_AUDIENCE=sixpay-api
```

**Nuance de sécurité :** `issuer` public HTTPS vérifié, JWKS interne HTTP sur bridge Docker privé. Ne pas présenter le JWKS HTTP comme une interface publique. Les paramètres ont été vérifiés dans un conteneur en exécution avant la recréation réseau ; revalidation post-redéploiement non documentée séparément.

### 4.3 Contrôles techniques Keycloak réalisés

- Depuis Windows et VPS, discovery OIDC public : HTTP 200.
- Depuis un conteneur curl temporaire sur le réseau `sixpay_m2m_bridge`, JWKS interne : HTTP 200.
- Token `client_credentials` : acquisition réussie ; issuer et audience inspectés lors des tests précédents.
- Client `sixpay-partner-test` avec scope `payment.read` : consultation de référence fictive → `404` après `X-Correlation-ID`.
- Même consultation avec scope `payment.initiate` : `403`.
- JWT absent → `401` ; JWT malformé → `401` ; JWT expiré → `401` observés lors des tests par tunnel précédents.
- Secret Keycloak saisi par prompt masqué ; pas de publication de JWT dans le chat.

**Procédure de lecture non intrusive :**

```bash
sudo docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Networks}}'
sudo docker network inspect sixpay_m2m_bridge --format 'INTERNAL={{.Internal}} OPTIONS={{json .Options}}'
curl -sS -o /dev/null -w '%{http_code}\n' 'https://auth.sixgensol.com/realms/sixpay-m2m-local/.well-known/openid-configuration'
```

**Ne pas supposer** les commandes exactes de création de realm, client, rôles, scope mappers ou volumes H2 : les résultats en sont rapportés, mais le détail CLI/UI est à récupérer dans le journal d'exploitation ou l'export realm approuvé, après retrait des secrets.

## 5. Diagnostic approfondi du port Docker `8081` — échec puis résolution

### 5.1 Incident initial

**[OBSERVÉ]** Après ajout :

```yaml
services:
  backend:
    ports:
      - '127.0.0.1:8081:8080'
    networks:
      - sixpay_backend
      - sixpay_m2m_bridge
```

Puis `docker compose ... up -d --no-deps --force-recreate backend` → `Started`, mais :

```text
HostConfig.PortBindings = {"8080/tcp":[{"HostIp":"127.0.0.1","HostPort":"8081"}]}
NetworkSettings.Ports = {"8080/tcp":null}
docker port backend 8080/tcp -> no public port published
curl http://127.0.0.1:8081/... -> curl (7), HTTP 000
```

Le backend fonctionnait depuis l'hôte via IP Docker interne : `/actuator/health` → HTTP 200. L'absence d'un socket `ss` sur le port 8081 ne suffit pas toujours à prouver un défaut de NAT, mais le faisceau `docker port`, inspect et `curl` confirmait le défaut de publication.

### 5.2 Commandes diagnostiques exécutées — résultat

```bash
# Ports et conteneurs
sudo docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Networks}}'
sudo docker port sixpay-connect-ldap-backend-1 8080/tcp
sudo docker inspect --format '{{json .HostConfig.PortBindings}} | {{json .NetworkSettings.Ports}}' sixpay-connect-ldap-backend-1
sudo ss -ltnp '( sport = :8081 )'
systemctl list-sockets --all --no-pager | grep -E '8081|LISTEN' || true

# Conteneur et daemon
sudo docker inspect --format 'NETWORK_MODE={{.HostConfig.NetworkMode}} EXPOSED_PORTS={{json .Config.ExposedPorts}} PUBLISH_ALL={{.HostConfig.PublishAllPorts}}' sixpay-connect-ldap-backend-1
sudo docker version --format 'DOCKER_SERVER_VERSION={{.Server.Version}}'
sudo docker network inspect sixpay-connect-cloud_sixpay_backend --format 'INTERNAL={{.Internal}} OPTIONS={{json .Options}}'
sudo docker network inspect sixpay-connect-cloud_sixpay_frontend_access --format 'DRIVER={{.Driver}} INTERNAL={{.Internal}} OPTIONS={{json .Options}}'
sudo docker network inspect sixpay-connect-cloud_sixpay_frontend_access --format '{{range .Containers}}{{.Name}}{{println}}{{end}}'
sudo docker network inspect sixpay_m2m_bridge --format 'INTERNAL={{.Internal}} OPTIONS={{json .Options}}'

# Événements et état du service
sudo docker events --since 30m --until 0s --filter container=sixpay-connect-ldap-backend-1 --format '{{.Time}} {{.Action}} {{json .Actor.Attributes}}'
systemctl is-active docker
```

**Résultats :** Docker Engine `29.9.3` ; `sixpay_backend` et `sixpay_m2m_bridge` : tous deux `internal=true` ; `sixpay_frontend_access` : `bridge internal=false` ; seul le frontend en était membre initialement ; port `8081` libre selon `ss`, `docker ps` et sockets systemd ; service Docker `active`, conteneur `running`, `EXPOSED_PORTS={"8080/tcp":{}}`, `PUBLISH_ALL=false` (normal), options de réseaux `{}`. Paramètres daemon (`iptables`, `ip6tables`, `userland-proxy`, `firewall-backend`) tous `default`.

**Échec de diagnostic mineur :** une commande Python `print(f"{key}={config.get(key, \"default\")}")` a produit `SyntaxError`; remplacée par `"{}={}".format(key, value)`. Ce défaut venait du script de diagnostic, pas de Docker.

**Pistes finalement abandonnées :** `socat` non installé (`SOCAT_NOT_INSTALLED`) ; `/usr/lib/systemd/systemd-socket-proxyd` disponible mais **aucun relais installé**. Réutilisation du Nginx frontend sur `8088` envisagée en dernier recours, **non retenue**. Pas d'ouverture du port 8081 sur toutes les interfaces, pas de désactivation SELinux ou du pare-feu.

### 5.3 Correction autorisée et exécutée

Sauvegarde validée :

```bash
sudo bash -c '
set -e
SOURCE=/opt/sixpay/compose/docker-compose.backend.yml
BACKUP=/opt/sixpay/compose/docker-compose.backend.pre-api-network.yml
test ! -e "$BACKUP"
cp -p "$SOURCE" "$BACKUP"
cmp -s "$SOURCE" "$BACKUP"
echo SIXPAY_BACKEND_COMPOSE_BACKUP_VALID
'
```

Modification ciblée `docker-compose.backend.yml` :

```yaml
services:
  backend:
    ports:
      - '127.0.0.1:8081:8080'
    networks:
      - sixpay_backend
      - sixpay_m2m_bridge
      - sixpay_frontend_access
```

**La définition du réseau** `sixpay_frontend_access` existait déjà dans la composition du frontend ; elle n'a pas été recréée. La modification a été faite par remplacement Python à occurrence unique, puis validée :

```bash
cd /opt/sixpay/compose
sudo docker compose \
  -f docker-compose.yml \
  -f docker-compose.backend.yml \
  -f docker-compose.frontend.yml \
  -f docker-compose.ldap.yml \
  config --quiet
```

Sorties : `SIXPAY_COMPOSE_CONFIGURATION_VALID`, puis validation JSON des trois réseaux et du port `BACKEND_PORT_8081_VALID: True`, `SIXPAY_BACKEND_NETWORK_CONFIGURATION_VALID`.

Application **au seul backend** :

```bash
cd /opt/sixpay/compose
sudo docker compose \
  -f docker-compose.yml \
  -f docker-compose.backend.yml \
  -f docker-compose.frontend.yml \
  -f docker-compose.ldap.yml \
  up -d --no-deps --force-recreate backend
```

Sortie : `Container sixpay-connect-ldap-backend-1 Started`.

Contrôles post-correction :

```bash
sudo docker port sixpay-connect-ldap-backend-1 8080/tcp
sudo docker inspect --format '{{json .NetworkSettings.Ports}}' sixpay-connect-ldap-backend-1
curl -sS --connect-timeout 5 --max-time 15 -o /dev/null -w 'HTTP=%{http_code}\n' http://127.0.0.1:8081/actuator/health
```

Sorties : `127.0.0.1:8081` ; `{"8080/tcp":[{"HostIp":"127.0.0.1","HostPort":"8081"}]}` ; `HTTP=200`. **Cause pratique établie par correction contrôlée :** rattacher le backend à un réseau bridge non interne préexistant a rétabli la publication ; ne pas en déduire une propriété universelle de toutes les versions Docker.

**Conséquence reconnue :** ajout d'un réseau non interne = élargissement potentiel de la connectivité sortante et de la surface de communication du backend, accepté pour ce périmètre de test et reporté pour réévaluation après MVP.

## 6. Caddy — configuration, sauvegarde, validation, activation

### 6.1 Configuration conservée

Avant la phase, `/etc/caddy/Caddyfile` définissait :

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
sixpayapp.sixgensol.com {
    encode zstd gzip
    header {
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }
    reverse_proxy 127.0.0.1:8088
}
auth.sixgensol.com {
    encode zstd gzip
    header {
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }
    @oauth2_allowed {
        path /realms/sixpay-m2m-local/.well-known/openid-configuration
        path /realms/sixpay-m2m-local/protocol/openid-connect/token
        path /realms/sixpay-m2m-local/protocol/openid-connect/certs
    }
    handle @oauth2_allowed {
        reverse_proxy 127.0.0.1:8180
    }
    handle {
        respond "Forbidden" 403
    }
}
```

La route publique Keycloak ne donne donc **pas** par défaut accès à l'administration Keycloak.

### 6.2 Nouveau bloc API effectivement installé

```caddyfile
api.sixgensol.com {
    encode zstd gzip
    header {
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }

    @payment_initiate {
        method POST
        path /v1/payments/initiate
    }
    @payment_consult {
        method GET
        path_regexp ^/api/v1/integrations/payments/[^/]+$
    }
    @challenge_consult {
        method GET
        path_regexp ^/v1/payments/[^/]+/confirmation-challenge$
    }
    @challenge_verify {
        method POST
        path_regexp ^/v1/payments/[^/]+/confirmation-challenge/verify$
    }
    @challenge_resend {
        method POST
        path_regexp ^/v1/payments/[^/]+/confirmation-challenge/resend$
    }

    route {
        handle @payment_initiate {
            reverse_proxy 127.0.0.1:8081
        }
        handle @payment_consult {
            reverse_proxy 127.0.0.1:8081
        }
        handle @challenge_consult {
            reverse_proxy 127.0.0.1:8081
        }
        handle @challenge_verify {
            reverse_proxy 127.0.0.1:8081
        }
        handle @challenge_resend {
            reverse_proxy 127.0.0.1:8081
        }
        handle {
            respond "Forbidden" 403
        }
    }
}
```

**Temporalité :** chemin de consultation encore actif `/api/v1/integrations/payments/{paymentReference}` ; chemin cible après développement `/v1/payments/{paymentReference}`. Ce changement **avant le futur déploiement de production** est le seul chantier explicitement conservé avant production par le propriétaire ; les autres sujets ci-dessous sont reportés, sans que ce report vaille approbation normative.

### 6.3 Déroulé de la modification

1. **[OBSERVÉ]** Sauvegarde existante `/etc/caddy/Caddyfile.pre-sixpay-api` vérifiée présente.
2. **[OBSERVÉ]** `caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile` : `Valid configuration`, `systemctl is-active caddy` actif.
3. **[OBSERVÉ]** Création `/etc/caddy/Caddyfile.api-candidate` par script Python qui copie la configuration existante et ajoute seulement le bloc API.
4. **[OBSERVÉ]** `caddy validate` candidat : succès. Matcher `@payment_allowed` inutile identifié et supprimé (aucune règle ne l'utilisait) ; deuxième validation réussie.
5. **[OBSERVÉ]** `diff -u /etc/caddy/Caddyfile /etc/caddy/Caddyfile.api-candidate` : ajout du bloc `api.sixgensol.com` uniquement ; aucun changement des trois autres sites.
6. **[OBSERVÉ]** Copie validée `cp -p Caddyfile.api-candidate Caddyfile` + `cmp -s` : `SIXPAY_CADDY_API_CONFIG_INSTALLED`.
7. **[OBSERVÉ]** `sudo systemctl reload caddy` après validation ; `systemctl is-active --quiet caddy` OK et `SIXPAY_CADDY_API_RELOAD_SUCCESS`.
8. **[OBSERVÉ]** Avertissement `Caddyfile input is not formatted` (ligne 2) ; **non bloquant**, pas de `caddy fmt --overwrite` exécuté.

Commandes utiles :

```bash
sudo sed -n '1,120p' /etc/caddy/Caddyfile
sudo diff -u /etc/caddy/Caddyfile.pre-sixpay-api /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
sudo systemctl is-active caddy
# Ne recharger que lors d'un changement approuvé et après validation :
# sudo systemctl reload caddy
```

**Rollback documentaire à préparer et tester séparément, non exécuté dans cette phase :** restaurer la sauvegarde du Caddyfile après vérification de son adéquation, valider, recharger ; le rollback du réseau Compose nécessiterait une décision et une recréation du backend. Ne jamais considérer un rollback non exécuté comme validé.

## 7. Validation locale, via VPS et depuis Internet — preuves

### 7.1 Tests depuis l'hôte Contabo

| Action | Résultat fourni |
|---|---|
| `curl http://127.0.0.1:8081/actuator/health` | `200` |
| `curl https://api.sixgensol.com/actuator/health` | `403`, vérification TLS `0` |
| GET consultation `/api/v1/integrations/payments/<référence fictive>` sans JWT | `401` ; TLS `0` |
| GET même route avec JWT `payment.read` **sans** `X-Correlation-ID` | `400 INVALID_REQUEST` ; `detail=Payment request is invalid`, `violations=[]` |
| GET même route avec JWT `payment.read` **et** UUID `X-Correlation-ID` | `404` (référence fictive), TLS `0` |
| Cinq méthodes/routes testées sans JWT | `401` pour chacune ; voir nuance ci-dessous |

**Nuance sur les « cinq routes » :** le test réel de consultation dans le lot était `GET /api/v1/integrations/payments/{ref}`, **pas** `GET /v1/payments/{ref}` qui n'existait pas encore. L'HTTP `401` seul ne prouve pas qu'une route métier est disponible.

### 7.2 Exemples de tests reproductibles côté VPS

```bash
curl -sS --connect-timeout 5 --max-time 20 -o /dev/null \
  -w 'HTTP=%{http_code} TLS=%{ssl_verify_result}\n' \
  https://api.sixgensol.com/actuator/health

curl -sS --connect-timeout 5 --max-time 20 -o /dev/null \
  -w 'HTTP=%{http_code} TLS=%{ssl_verify_result}\n' \
  https://api.sixgensol.com/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV

# Vérification du corps 400, sans imprimer les secrets :
python3 - <<'PY'
import json
from pathlib import Path
p=Path('/tmp/sixpay-api-response.json')
if p.exists():
    d=json.loads(p.read_text())
    print({k:d.get(k) for k in ('status','code','type')})
    print('violations_count=',len(d.get('violations') or []))
PY
```

### 7.3 Tests depuis Windows, **hors VPS, sans SSH/tunnel**

**[OBSERVÉ]** Dans PowerShell :

```powershell
curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "HTTP_STATUS=%{http_code}; TLS_VERIFY=%{ssl_verify_result}`n" https://api.sixgensol.com/actuator/health
# HTTP_STATUS=403; TLS_VERIFY=0

curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "SIXPAY_EXTERNAL_UNAUTH_STATUS=%{http_code}; TLS_VERIFY=%{ssl_verify_result}`n" -H "X-Correlation-ID: $([guid]::NewGuid())" https://api.sixgensol.com/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV
# SIXPAY_EXTERNAL_UNAUTH_STATUS=401; TLS_VERIFY=0
```

**[OBSERVÉ]** Un collage PowerShell multilignes est arrivé à l'envers, avec invite `>>` ; abandon `Ctrl+C` puis recours à `notepad "$HOME\sixpay-m2m-test.ps1"` suivi de `powershell.exe -NoProfile -File "$HOME\sixpay-m2m-test.ps1"`. Script local : saisie masquée du secret, POST OAuth2 `client_credentials` via `Invoke-RestMethod`, JWT en mémoire, GET de consultation via `curl.exe` avec `Authorization: Bearer` et `X-Correlation-ID`, suppression des références variables à la fin du processus.

**Réussites Windows :**

```text
SIXPAY_WINDOWS_PAYMENT_READ_TOKEN_OBTAINED
SIXPAY_EXTERNAL_AUTH_STATUS=404  # scope payment.read, référence fictive
SIXPAY_EXTERNAL_AUTH_STATUS=403  # scope payment.initiate uniquement, consultation interdite
```

Le message de script `PAYMENT_READ_TOKEN_OBTAINED` n'avait pas été renommé lors du test `payment.initiate` ; ce libellé n'est pas une preuve de scope, mais la modification de `scope` par l'opérateur est attestée dans l'échange.

**Script de test sûr à recréer pour futures validations (PROCÉDURE, pas artefact déployé) :**

```powershell
$ErrorActionPreference = 'Stop'
$Secure = Read-Host 'Secret Keycloak sixpay-partner-test' -AsSecureString
$Plain = [System.Net.NetworkCredential]::new('', $Secure).Password
try {
    $Body = @{ grant_type='client_credentials'; client_id='sixpay-partner-test'; client_secret=$Plain; scope='payment.read' }
    $Result = Invoke-RestMethod -Method POST -Uri 'https://auth.sixgensol.com/realms/sixpay-m2m-local/protocol/openid-connect/token' -ContentType 'application/x-www-form-urlencoded' -Body $Body
    if (-not $Result.access_token) { throw 'Aucun JWT retourné' }
    $Correlation = [guid]::NewGuid().ToString()
    curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w 'HTTP=%{http_code}; TLS=%{ssl_verify_result}\n' -H "Authorization: Bearer $($Result.access_token)" -H "X-Correlation-ID: $Correlation" 'https://api.sixgensol.com/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV'
} finally {
    $Result=$null; $Body=$null; $Plain=$null; $Secure=$null
}
```

Ne pas committer ni conserver un secret client dans ce script. Ne pas utiliser une commande contenant le JWT ou le secret en ligne de commande avec journalisation détaillée.

### 7.4 Signification et limites des réponses

| HTTP | Preuve obtenue dans ce parcours | Ne prouve pas |
|---|---|---|
| `200` health loopback | Spring Boot répond via la publication Docker | Résilience de production |
| `403` Caddy `/actuator/health` | Liste blanche bloque cette URL | Que tous les autres chemins interdits ont été exhaustivement testés |
| `401` API sans JWT | Authentification exigée | Existence du contrôleur et droits objet |
| `400` JWT sans corrélation | Requête rejetée `INVALID_REQUEST` | Un diagnostic contractuel exhaustif du validateur |
| `404` JWT read + corrélation | Requête franchit suffisamment de filtres pour obtenir une réponse de ressource absente | Paiement existant, intégration bancaire ou autorisation d'un vrai objet partenaire |
| `403` JWT initiate sur GET read | Refus de permission pour ce scénario | Droits sur les trois routes confirmation et initiation en production |

## 8. Incidents, décisions, non-régressions et sorties

| Incident / décision | Fait et résolution |
|---|---|
| `docker port ... 8080/tcp` = aucun port | Deux réseaux internes uniquement ; correction approuvée : rattacher le backend à `sixpay_frontend_access`, port publié et health `200` |
| Port 8081 soupçonné occupé | `ss`, Docker et systemd sockets : aucune occupation constatée |
| DNS/domain HTTPS absent initialement | Bloc Caddy créé séparément, `validate` et `reload` réussis ; HTTPS externe validé |
| Besoin de nouveau composant | Refus explicite de l'opérateur ; pas de `socat`, pas de `systemd-socket-proxyd`, pas de proxy via frontend |
| Caddyfile `fmt` warning | Avertissement non bloquant, pas de modification cosmétique |
| `@payment_allowed` inutile | Retiré du candidat avant installation |
| JWT valide, HTTP 400 | `X-Correlation-ID` manquant ; même requête avec UUID → `404` |
| Recherche logs `docker logs ... grep` | Aucun résultat, donc aucune cause prouvée par logs |
| Recherche Java/YAML sur `/opt/sixpay` | Aucune occurrence, pas de conclusion sur le code absent |
| Collage PowerShell multilignes inversé | Script `.ps1` via Notepad, exécution Windows réussie |
| Refus sur mauvais scope | `payment.initiate` sur consultation retourne `403` depuis Windows |
| Frontend et LDAP après dernière recréation backend | Fonctionnement rapporté plus tôt, mais **aucun nouveau test post-correction spécifiquement consigné** |

## 9. Sécurité, état contractuel et travaux reportés

Les contrats GitHub consultés indiquent que `payment-consultation-api-v1` est **`ACTIVE_MVP` / `PENDING_APPROVAL` / `REFERENCE_ONLY` / `codeGenerationAllowed:false`**, avec OAuth2 client credentials et mTLS dans le profil de sécurité. `payment-command-api-v1` et `tresorpay-payment-confirmation-api-v1` restent **APPROVED** avec mTLS requis dans leurs profils contractuels. Le domaine HTTPS actuellement exposé **n'est pas une preuve de conformité mTLS**. Ne pas créer/modifier contrats ou APIs depuis ce runbook.

**Décision explicite de clôture :** ne pas relancer les travaux ci-dessous dans la suite immédiate du MVP ; ils sont **REPORTÉS après le MVP**, pas « validés », « supprimés des contrats » ou « sans risque » :

1. Mise en conformité mTLS contractuelle et éventuelle décision officielle de changement du profil.
2. Tests exhaustifs de permissions/object ownership sur les routes initiation et confirmation (read/verify/resend).
3. Réévaluation du troisième réseau Docker non interne et de l'egress backend.
4. Durcissement Keycloak : abandon de `start-dev`/H2 pour une configuration production, protection des secrets, sauvegarde/restauration et supervision.

**Une intervention est maintenue AVANT le déploiement en production**, quand le code sera prêt : aligner la consultation de paiement sur `GET /v1/payments/{paymentReference}`, puis changer **uniquement** le matcher Caddy correspondant ; valider avec contrôle positif JWT+`X-Correlation-ID` et refus négatifs. L'actuelle URL `GET /api/v1/integrations/payments/{paymentReference}` reste le chemin réellement testé.

**Limites décisionnelles :** la décision de report du propriétaire ne transforme pas une violation du contrat ou un dispositif de test en conformité production. Toute mise en production réelle, utilisation de données bancaires réelles ou transaction financière exige les approbations et gates applicables.

## 10. Références internes et provenance

- `AGENTS.md`, `ENGINEERING_CONTEXT.md`, `MASTER_PROMPT_INPUT_MANIFEST.yaml`, `MASTER_ENGINEERING_PROMPT.md`, `documentation/contracts/CONTRACT_REGISTRY.yaml`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_E1_1_PREPARATION_CONTABO_ALMALINUX.md`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_E1_2_POSTGRESQL_CONTABO.md`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_E1_3_CONTABO_POSTGRES_GHCR_BACKEND_2026-11-10.md`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_RUNBOOK_CONTABO_LDAP_PROVISIONNEMENT.md`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_G718_G720_RUNBOOK_CONTABO_2026-11-12.md`.
- `documentation/runbooks/deploy_cloud_mvp/SIXPAY_PROMPT_REPRISE_G721_G726_2026-11-12.md`.
- `documentation/runbooks/deploy_cloud_mvp/KEYCLOAK_PRODUCTION_PARAMETERS.md` : **placeholders**, pas configuration exécutée.
- Échanges opérateur de la présente phase M2M G45.33 à G45.81.4 et contexte de la continuation G7.21–G7.26.

**Validation de la rédaction :** ce fichier est une consolidation documentaire ; aucun SSH, contrôle Git du VPS, test Caddy, Docker ou bancaire nouveau n'a été exécuté pour sa fabrication. Aucun commit, push, PR ou déploiement GitHub n'a été effectué.
