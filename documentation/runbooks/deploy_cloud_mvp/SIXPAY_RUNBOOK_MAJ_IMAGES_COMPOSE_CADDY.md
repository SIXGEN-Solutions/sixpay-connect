# SIXPAY CONNECT — Runbook d’exploitation : mise à jour des images Docker Compose et des routes Caddy

**Périmètre :** VPS Contabo AlmaLinux, environnement SIXPAY de simulation/MVP.  
**Type :** procédure opératoire reproductible (ne constitue pas un ordre de déploiement).  
**Révision applicative de référence de la dernière intervention :** `feat/repository-baseline-consolidation-cucumber` @ `49d6bb854a0ec2ebcdd6aeb3e2a3bcfbf4db8bce` ; **vérifier le commit et les images de la nouvelle livraison**.  
**État observé lors de la dernière intervention :** backend par digest `sha256:12b37cc63f4b49fe1c2c5d1dc5ee300fad81b4f0202da3c24df0e7ad85555b1f`, `127.0.0.1:8081/actuator/health` = HTTP 200 ; cinq routes Caddy harmonisées, recharge réussie ; consultation publique non authentifiée 401, authentifiée `payment.read` avec référence fictive 404, ancien chemin 403.  
**Gouvernance :** lire les instructions actives du dépôt avant changement d'API ou de configuration de sécurité. Éviter les données bancaires réelles et les modifications hors périmètre.

> **Important :** les commandes ci-dessous sont des procédures *à exécuter lors d'une future livraison*. Les exemples de références correspondent à une livraison passée. Les procédures de rollback sont préparées mais n’ont pas été exécutées ni éprouvées lors de cette intervention. Ne pas réutiliser aveuglément un ancien digest ou un ancien backup.

## A. Préparation commune — avant toute mise à jour

### A1. Périmètre, fenêtre et prérequis

1. Identifier le composant : `backend` ou `frontend`. Ne pas mettre à jour les deux si un seul a changé et que la compatibilité de l'ensemble est validée.
2. Connaître la nouvelle référence **par digest** `ghcr.io/sixgen-solutions/sixpay-<composant>@sha256:<64 caractères hexadécimaux>` et le SHA Git source attendu. Le digest et le SHA Git sont différents.
3. Confirmer l'autorisation de changement et prévoir une courte interruption du composant recréé. Aucun `down`, `down -v`, `docker system prune`, `flyway clean`, redémarrage général ni suppression de volume.
4. Avoir accès à `sixpayadmin` via SSH avec `sudo` ; **Docker exige `sudo`** dans l'état observé (sans `sudo` : `permission denied` sur `/var/run/docker.sock`). Ne pas élargir les droits Docker uniquement pour éviter `sudo`.
5. Vérifier que les sauvegardes du fichier Compose concerné et, si les routes changent, du Caddyfile, ont été créées et comparées avant modification.
6. Ne pas afficher le résultat de `docker compose config` sans `--quiet` : la configuration résolue peut contenir des secrets.

### A2. Variables pratiques (dans la même session Bash du VPS)

```bash
COMPOSE_DIR=/opt/sixpay/compose
COMPOSE_BASE="$COMPOSE_DIR/docker-compose.yml"
COMPOSE_BACKEND="$COMPOSE_DIR/docker-compose.backend.yml"
COMPOSE_FRONTEND="$COMPOSE_DIR/docker-compose.frontend.yml"
COMPOSE_LDAP="$COMPOSE_DIR/docker-compose.ldap.yml"
STAMP=$(date -u +%Y%m%dT%H%M%SZ)
```

Toutes les commandes `docker compose` doivent assembler les **quatre** fichiers, faute de quoi la définition réseau, les publications de ports ou les dépendances pourraient différer de l'exploitation actuelle.

### A3. État initial, sans mutation

```bash
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" ps
sudo docker inspect sixpay-connect-ldap-backend-1 --format 'IMAGE={{.Config.Image}} STATUS={{.State.Status}}'
FRONTEND_ID=$(sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" ps -q frontend)
[ -n "$FRONTEND_ID" ] && sudo docker inspect "$FRONTEND_ID" --format 'IMAGE={{.Config.Image}} STATUS={{.State.Status}}'
sudo ss -ltnp | grep -E ':(443|8081|8088|8180)\b' || true
```

L'identifiant frontend est résolu dynamiquement par Compose ; si le conteneur n'existe pas, la commande d'inspection est ignorée et l'opérateur doit vérifier `docker compose ps`. Les endpoints de santé n'attestent pas les fonctionnalités métier.

---

## B. Mise à jour d’une image Docker Compose

### B1. Choisir le composant et le fichier

| Composant | Service Compose | Fichier overlay | Port loopback observé | Contrôle sans effet métier |
|---|---|---|---|---|
| Backend Spring Boot | `backend` | `/opt/sixpay/compose/docker-compose.backend.yml` | `127.0.0.1:8081 → 8080` | `GET http://127.0.0.1:8081/actuator/health` : attendu 200 |
| Frontend Angular/Nginx | `frontend` | `/opt/sixpay/compose/docker-compose.frontend.yml` | `127.0.0.1:8088 → 80` | `GET http://127.0.0.1:8088/` : attendu 200 si application prête |

Exemple **backend** ci-dessous. Pour le frontend, affecter `SERVICE=frontend`, `OVERLAY="$COMPOSE_FRONTEND"`, `IMAGE_REPOSITORY=ghcr.io/sixgen-solutions/sixpay-frontend` et sélectionner le digest correspondant.

```bash
SERVICE=backend
OVERLAY="$COMPOSE_BACKEND"
IMAGE_REPOSITORY=ghcr.io/sixgen-solutions/sixpay-backend
NEW_DIGEST='sha256:<COLLER_DIGEST_COMPLET_VALIDÉ>'
NEW_IMAGE="$IMAGE_REPOSITORY@$NEW_DIGEST"
```

**Ne pas exécuter tel quel** : le placeholder `<COLLER_DIGEST_COMPLET_VALIDÉ>` doit être remplacé. Exemple historique backend validé : `sha256:12b37cc63f4b49fe1c2c5d1dc5ee300fad81b4f0202da3c24df0e7ad85555b1f`.

### B2. Contrôler l'image candidate (sans déployer)

```bash
if printf '%s\n' "$NEW_IMAGE" | grep -Eq '^ghcr\.io/sixgen-solutions/sixpay-(backend|frontend)@sha256:[[:xdigit:]]{64}$'; then echo 'IMAGE_REFERENCE_FORMAT_OK'; else echo 'INVALID_IMAGE_REFERENCE — STOPPER LA PROCÉDURE'; fi
sudo docker pull "$NEW_IMAGE"
sudo docker image inspect "$NEW_IMAGE" --format 'IMAGE_ID={{.Id}} REPO_DIGESTS={{json .RepoDigests}}'
```

**Gate obligatoire :** continuer uniquement si le format affiche `IMAGE_REFERENCE_FORMAT_OK` et si `docker pull` puis `inspect` réussissent ; sinon arrêter la procédure. Ne pas interpréter un `pull` réussi comme une preuve de provenance Git. Les métadonnées OCI et les preuves GitHub Actions permettent de rapprocher l'image du SHA Git ; la simple ressemblance entre tag et digest ne le démontre pas.

### B3. Sauvegarder le Compose et vérifier l'original

```bash
BACKUP="$OVERLAY.pre-update-$STAMP"
sudo cp -p "$OVERLAY" "$BACKUP"
sudo cmp -s "$OVERLAY" "$BACKUP" && echo 'SIXPAY_COMPOSE_BACKUP_OK'
sudo grep -n -C 4 'image:' "$OVERLAY"
```

Le résultat de `cmp -s` doit être nul ; ne pas poursuivre en cas d'erreur. Conserver le chemin exact de `BACKUP` dans le journal d'exploitation.

### B4. Modifier seulement `image:`

Éditer le fichier ciblé (sans changer réseaux, ports, `env_file`, utilisateurs, volumes, dépendances et politiques de redémarrage) :

```bash
sudoedit "$OVERLAY"
```

Remplacer uniquement la ligne `image:` du service concerné :

```yaml
services:
  backend:
    image: ghcr.io/sixgen-solutions/sixpay-backend@sha256:<DIGEST_COMPLET>
```

Cet extrait **n'est pas** un fichier Compose complet : les propriétés existantes doivent être conservées à l'identique.

### B5. Diff et validation Compose

```bash
sudo diff -u "$BACKUP" "$OVERLAY"
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" config --quiet && echo 'SIXPAY_COMPOSE_CONFIG_VALID'
```

**Gate obligatoire :** le diff ne contient que le digest attendu (sauf changement explicitement autorisé). Ne pas interpréter `diff` code de sortie `1` comme un échec métier : il signifie qu’il y a des différences ; il faut les inspecter. `config --quiet` doit se terminer avec un code `0`.

### B6. Recréer uniquement le composant cible

```bash
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" up -d --no-deps --force-recreate "$SERVICE"
```

**Effet :** interruption possible du composant ciblé ; pas de recréation volontaire de PostgreSQL, LDAP, Keycloak ou de l'autre composant applicatif. `Started` signifie que le conteneur a démarré, **pas** que l’application est saine.

### B7. Contrôles après recréation

```bash
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" ps "$SERVICE"
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" images "$SERVICE"
```

**Backend uniquement :**

```bash
sudo docker inspect sixpay-connect-ldap-backend-1 --format 'IMAGE={{.Config.Image}} STATUS={{.State.Status}}'
curl --noproxy '*' -sS --connect-timeout 5 --max-time 15 -o /dev/null -w 'BACKEND_HEALTH_HTTP=%{http_code}\n' http://127.0.0.1:8081/actuator/health
```

Attendu : référence de la nouvelle image, `running` et HTTP `200`.

**Frontend uniquement :**

```bash
curl --noproxy '*' -sS --connect-timeout 5 --max-time 15 -o /dev/null -w 'FRONTEND_HTTP=%{http_code}\n' http://127.0.0.1:8088/
curl --noproxy '*' -sS --connect-timeout 5 --max-time 15 -o /dev/null -w 'RUNTIME_CONFIG_HTTP=%{http_code}\n' http://127.0.0.1:8088/runtime-config.json
```

Attendu : HTTP `200` pour ces ressources si la configuration est correcte. Vérifier aussi que le montage `runtime-config.json` reste présent en lecture seule, sans afficher de secrets. Ne pas supposer qu'un simple 200 valide l'authentification ou tous les parcours utilisateur.

En cas d'échec :

```bash
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" ps "$SERVICE"
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" logs --tail=100 "$SERVICE"
```

**Attention :** les logs peuvent contenir des données sensibles ; les filtrer avant partage. Ne jamais publier de secrets dans un ticket ou une conversation.

### B8. Contrôles HTTPS de bout en bout

Depuis un poste externe :

```powershell
curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "TLS_VERIFY=%{ssl_verify_result}; HTTP=%{http_code}`n" https://api.sixgensol.com/actuator/health
```

Attendu dans la configuration Caddy documentée : `TLS_VERIFY=0`, `HTTP=403`.

**Après mise à jour backend des chemins Payment :**

```powershell
curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "PAYMENT_UNAUTH_HTTP=%{http_code}; TLS_VERIFY=%{ssl_verify_result}`n" -H "X-Correlation-ID: $([guid]::NewGuid())" https://api.sixgensol.com/api/v1/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV
```

Attendu : `401`, TLS `0`, si la route Caddy active correspond à la nouvelle application. Pour un jeton OAuth2 `payment.read` et un identifiant de corrélation valide, le dernier test historique a renvoyé `404` sur cette référence fictive. Ne pas afficher le secret Keycloak ni le JWT ; ne pas déclencher POST d'initiation, vérification OTP ou renvoi sans scénario explicitement autorisé.

### B9. Retour arrière du composant (préparé ; pas de rollback déjà attesté)

**Condition :** ancien artefact toujours disponible, schémas/configurations compatibles et décision opérateur. Attention : le retour d'image ne restaure pas une migration de base de données.

```bash
sudo cp -p "$BACKUP" "$OVERLAY"
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" config --quiet
sudo docker compose -f "$COMPOSE_BASE" -f "$COMPOSE_BACKEND" -f "$COMPOSE_FRONTEND" -f "$COMPOSE_LDAP" up -d --no-deps --force-recreate "$SERVICE"
```

Puis répéter les contrôles de B7 et les tests externes de B8. Si l'ancien digest a été retiré de GHCR et n'est pas présent localement, la restauration peut être bloquée. Si le Caddyfile a évolué conjointement avec le backend, **coordonner les deux rollbacks** ; restaurer une ancienne configuration Caddy incompatible risque de maintenir l'API publique indisponible.

---

## C. Mise à jour des routes Caddy — seulement si les chemins publics changent

### C1. Déterminer si une intervention Caddy est nécessaire

**Ne pas modifier Caddy** pour un changement d'image seul si les méthodes/chemins exposés ne changent pas. Une mise à jour Caddy est nécessaire lorsqu'un chemin public ou une méthode autorisée change, ou quand une règle de sécurité/proxy approuvée est effectivement modifiée.

Caddy est installé sur l'hôte VPS (service `caddy` sous systemd), fichier actif `/etc/caddy/Caddyfile`. Le site API `api.sixgensol.com` relaie vers `127.0.0.1:8081`. Les sites frontend et Keycloak doivent rester intacts.

### C2. Lire l'état actuel avant la moindre modification

```bash
sudo systemctl is-active caddy
sudo grep -n -A 85 '^api\.sixgensol\.com {' /etc/caddy/Caddyfile
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
```

Lister précisément les couples méthode/chemin actuels et ceux du nouveau backend. Vérifier au préalable les contrats approuvés, les mappings Java et la politique d'authentification ; ne pas inventer de route ni élargir la liste blanche.

### C3. Sauvegarder la configuration active

```bash
CADDY_ACTIVE=/etc/caddy/Caddyfile
CADDY_BACKUP="/etc/caddy/Caddyfile.pre-route-update-$STAMP"
CADDY_CANDIDATE="/etc/caddy/Caddyfile.route-candidate-$STAMP"
sudo cp -p "$CADDY_ACTIVE" "$CADDY_BACKUP"
sudo cmp -s "$CADDY_ACTIVE" "$CADDY_BACKUP" && echo 'SIXPAY_CADDY_BACKUP_OK'
```

Si l’opérateur lance une **nouvelle session SSH**, initialiser `STAMP` à nouveau avant l'étape. Ne pas écraser des sauvegardes existantes.

### C4. Préparer un candidat sans toucher au fichier actif

```bash
sudo cp -p "$CADDY_ACTIVE" "$CADDY_CANDIDATE"
sudoedit "$CADDY_CANDIDATE"
```

**Exemple de bloc cible historique validé en novembre 2026 :**

```caddyfile
api.sixgensol.com {
    encode zstd gzip

    header {
        X-Content-Type-Options "nosniff"
        Referrer-Policy "strict-origin-when-cross-origin"
    }

    @payment_initiate {
        method POST
        path /api/v1/payments/initiate
    }
    @payment_consult {
        method GET
        path_regexp ^/api/v1/payments/[^/]+$
    }
    @challenge_consult {
        method GET
        path_regexp ^/api/v1/payments/[^/]+/confirmation-challenge$
    }
    @challenge_verify {
        method POST
        path_regexp ^/api/v1/payments/[^/]+/confirmation-challenge/verify$
    }
    @challenge_resend {
        method POST
        path_regexp ^/api/v1/payments/[^/]+/confirmation-challenge/resend$
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

**Attention :** ce bloc illustre **l'état cible de la dernière migration**, pas une instruction de remplacer l'ensemble du Caddyfile. Conserver tous les autres sites et ne modifier que les règles nécessaires. Dans la dernière intervention, un premier `sed` n'avait modifié qu'**une** des cinq routes, faute de correspondance entre `^/` et la ligne réelle `path_regexp ^/...`. Le diff de la candidate a détecté l'erreur, puis une seconde modification l'a corrigée. Ce retour d'expérience impose la revue du diff, plutôt que de croire qu'un `sed` terminé a tout changé.

### C5. Diff et validation sans effet sur le service actif

```bash
sudo diff -u "$CADDY_ACTIVE" "$CADDY_CANDIDATE"
sudo caddy validate --config "$CADDY_CANDIDATE" --adapter caddyfile && echo 'SIXPAY_CADDY_CANDIDATE_VALID'
```

**Gate obligatoire :** uniquement les changements autorisés ; aucune modification des domaines frontend et Keycloak ; Caddy répond `Valid configuration`. Un avertissement `Caddyfile input is not formatted` a été observé et n'était pas bloquant ; ne pas reformater tout le fichier pour une modification de routage ciblée.

### C6. Installer et recharger

```bash
sudo cp -p "$CADDY_CANDIDATE" "$CADDY_ACTIVE" && \
sudo caddy validate --config "$CADDY_ACTIVE" --adapter caddyfile && \
sudo systemctl reload caddy && \
echo 'SIXPAY_CADDY_ROUTES_RELOAD_OK'
```

Recharger Caddy, pas `systemctl restart`, pour appliquer les nouvelles routes. Vérifier que la commande se termine effectivement avec succès. Si la validation échoue avant le reload, le fichier actif peut être modifié sur disque mais l'ancienne configuration demeure en mémoire : **restaurer immédiatement le fichier sauvegardé après diagnostic**.

### C7. Vérifications externes après reload

```bash
sudo systemctl is-active caddy
sudo caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
```

Depuis Windows :

```powershell
# Route nouvelle, sans JWT : attente HTTP 401 / TLS 0
curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "NEW_HTTP=%{http_code}; TLS_VERIFY=%{ssl_verify_result}`n" -H "X-Correlation-ID: $([guid]::NewGuid())" https://api.sixgensol.com/api/v1/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV

# Ancien chemin, refus Caddy : attente HTTP 403 / TLS 0
curl.exe -sS --connect-timeout 5 --max-time 20 -o NUL -w "OLD_HTTP=%{http_code}; TLS_VERIFY=%{ssl_verify_result}`n" https://api.sixgensol.com/api/v1/integrations/payments/PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV
```

Compléter avec le test OAuth2 `payment.read` en utilisant un client M2M de test, un secret saisi de manière sécurisée, un JWT de courte durée et `X-Correlation-ID` ; attendu lors de l’intervention précédente : HTTP `404` avec référence fictive. Ne pas prendre `404` comme preuve de fonctionnement de tous les cas fonctionnels ni du contrôle d'accès objet.

Si les routes modifiées concernent `POST`, les tests sans JWT pourront vérifier des refus HTTP `401` sans effectuer d'opérations métier. Ne jamais envoyer de demande valide d'initiation ou d'OTP sans jeu d'essai et autorisation spécifiques.

### C8. Retour arrière Caddy (préparé ; non exécuté lors du chantier)

```bash
sudo cp -p "$CADDY_BACKUP" "$CADDY_ACTIVE" && \
sudo caddy validate --config "$CADDY_ACTIVE" --adapter caddyfile && \
sudo systemctl reload caddy && \
echo 'SIXPAY_CADDY_ROLLBACK_RELOAD_OK'
```

**Avant d’exécuter :** vérifier que l'ancien Caddyfile cible bien les endpoints de la version backend effectivement en service. Répéter les contrôles C7 avec **les chemins de cette version**. Un rollback de Caddy seul ne suffit pas si les chemins backend ont été supprimés : il faut planifier un rollback cohérent backend + Caddy.

---

## D. Checklist de clôture de chaque intervention

- [ ] Autorisation, image(s), commit(s), composants et fenêtre confirmés.
- [ ] État initial et digests précédents consignés.
- [ ] Image par digest téléchargée, disponible et provenance vérifiée.
- [ ] Sauvegarde horodatée du Compose et `cmp` réussis.
- [ ] Diff Compose limité aux changements approuvés.
- [ ] `docker compose config --quiet` : code de sortie nul.
- [ ] Recréation du seul service prévu : retour réussi.
- [ ] Image réellement configurée identique à l'image attendue.
- [ ] Service `running`, santé locale satisfaisante et logs sans anomalie bloquante.
- [ ] Caddy inchangé si aucune route n'a changé.
- [ ] Si Caddy change : backup, candidate, diff, `caddy validate`, reload réussis.
- [ ] Tests HTTPS/TLS et refus des chemins obsolètes réussis.
- [ ] Les autres sites (frontend, Keycloak) restent accessibles.
- [ ] Procédure de retour arrière documentée et compatibilité contrôlée, sans prétendre l'avoir exécutée.
- [ ] Journal d'exploitation : date, opérateur, SHA Git, digests avant/après, chemins de sauvegarde, commandes et résultats, réserves.

## E. Récapitulatif de la dernière intervention (preuves rapportées)

| Opération | Preuve rapportée |
|---|---|
| Backup Compose backend | `SIXPAY_BACKEND_COMPOSE_BACKUP_OK` |
| Changement image backend | Une seule ligne `image:` modifiée dans le diff |
| Validation Compose | `SIXPAY_COMPOSE_CONFIG_VALID` |
| Recréation backend | `Container sixpay-connect-ldap-backend-1 Started` |
| Vérification backend | Digest `12b37cc...`, `STATUS=running`, `BACKEND_HEALTH_HTTP=200` |
| Backup Caddy | `SIXPAY_CADDY_BACKUP_OK` |
| Candidat Caddy | Diff des cinq routes contrôlé ; première substitution partielle détectée et corrigée |
| Validation Caddy | `Valid configuration` ; `SIXPAY_CADDY_PAYMENT_API_VALID` |
| Reload Caddy | `SIXPAY_CADDY_PAYMENT_API_RELOAD_SUCCESS` |
| Nouveau GET sans JWT | `401`, `TLS_VERIFY=0` depuis Windows |
| Nouveau GET avec JWT `payment.read` | `404` avec référence fictive |
| Ancien GET | `403`, `TLS_VERIFY=0` |

**Limites :** ni le rollback Docker ni le rollback Caddy n'ont été exécutés pendant cette intervention ; aucune transaction bancaire réelle, aucun test E2E des trois opérations de confirmation et de l'initiation n'ont été réalisés. Ce runbook ne constitue pas une validation générale de conformité production.
