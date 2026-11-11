# SIXPAY CONNECT — Runbook de préproduction Contabo : LDAP, configuration externalisée et provisionnement

**Date de consolidation :** 11 novembre 2026  
**Périmètre :** opérations G7 réalisées/rapportées dans la discussion ; environnement Contabo, Samba AD, backend Spring Boot, PostgreSQL et provisioning des utilisateurs humains.  
**Référence de code vérifiée lors de l’analyse :** `SIXGEN-Solutions/sixpay-connect`, branche `feat/repository-baseline-consolidation-cucumber`, SHA `e8a528bc18340b40c04e62a60e0730f8401a5779`.  
**Mode :** documentation de travaux passés ; **aucune commande de ce runbook n’a été exécutée lors de sa rédaction**. Les sorties d’exploitation sont celles rapportées par l’opérateur, et non une revalidation en temps réel du VPS.  
**Statut :** parcours LDAP/provisionnement fonctionnel vérifié pour les deux utilisateurs testés ; accès navigateur HTTPS et frontend encore à livrer. Ne pas assimiler ce résultat à une qualification de production bancaire.

## 1. Synthèse et limites des preuves

Les validations rapportées sont : déploiement de PostgreSQL, du contrôleur Samba AD et du backend épinglé par digest ; backend `/actuator/health` = `UP` ; LDAP avec authentification réussie ; liaison d'identité `objectGUID` ; bootstrap initial de `sixpay.admin` exécuté puis **désactivé** ; provisioning contrôlé de `sixpay.testuser` en `MANAGER` ; login/session et séparation ADMIN/MANAGER testés. Les valeurs d'environnement sensibles et mots de passe sont exclus de ce document.

Certaines commandes de préparation et de diagnostic ne sont pas intégralement conservées dans l’historique disponible. Elles sont identifiées ci-dessous comme **commandes de contrôle reproductibles**, non comme transcription certifiée d'une exécution antérieure. Ne pas inventer le résultat d'un contrôle absent.

## 2. Topologie et conventions observées

| Élément | Valeur observée |
|---|---|
| VPS | Contabo `vmi3671192`, AlmaLinux 10.2 |
| Exploitant shell | `sixpayadmin` |
| Répertoire Compose | `/opt/sixpay/compose` |
| Compose base | `docker-compose.yml` |
| Compose backend | `docker-compose.backend.yml` |
| Compose LDAP | `docker-compose.ldap.yml` |
| Fichier externe backend | `/opt/sixpay/config/backend.env` (contient des secrets : **ne pas afficher**) |
| Truststore LDAP | `/opt/sixpay/secrets/ldap/` (fichier `.p12`, secret et accès à protéger) |
| Backend container | `sixpay-connect-ldap-backend-1` |
| Base container | `sixpay-connect-cloud-postgres-1` |
| Contrôleur AD container | `sixpay-cloud-ldap` |
| Domaine AD de test | `sixpay.test` |
| DC LDAPS | `dc.sixpay.test:636` |
| Base DN | `DC=sixpay,DC=test` |
| User search base | `CN=Users` |
| Service account DN | `CN=Administrator,CN=Users,DC=sixpay,DC=test` (compte de laboratoire, à remplacer par un compte de service à privilèges minimaux en production) |
| Provider/trust domain de liaison | `sixpay-preproduction-ldap` |
| Profil Spring | `production` |
| Session | 30 minutes ; cookie `Secure=true` |
| Certificat CA Samba | Alias `sixpay-samba-ad-ca` dans truststore `.p12` |
| Frontend / reverse proxy HTTPS | Non déployés au moment du bilan |

### Commande Compose commune (référence)

```bash
cd /opt/sixpay/compose
sudo docker compose \
  -f docker-compose.yml \
  -f docker-compose.backend.yml \
  -f docker-compose.ldap.yml ps
```

**Attention :** ne pas lancer `down -v`, ni recréer PostgreSQL/LDAP pour un diagnostic HTTP. Ne jamais substituer un tag mutable au digest d'une image déjà validée.

## 3. Images immuables et déploiement backend

Le backend actuellement retenu a été publié puis téléchargé depuis GHCR avec le digest :

```text
ghcr.io/sixgen-solutions/sixpay-backend@sha256:1958f3bf753129ea6fde73e9e7dba2a666fcc5d6d6c04205411a148dd512951b
```

L'image antérieure référencée dans Compose était `...@sha256:a2d8d035c940f3e02d1ccf198ddaad62a0999e2b211e00d641dba36e58f64d49`. Le changement a été fait dans `docker-compose.backend.yml`, avec sauvegarde `docker-compose.backend.yml.bak-g716`, puis validation de configuration et recréation ciblée du seul backend. L’inventaire des images `docker image ls` montrait plusieurs anciennes images backend et **aucune image frontend**.

Contrôles reproductibles, à exécuter seulement si nécessaire :

```bash
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml config --quiet
sudo docker compose -f docker-compose.yml -f docker-compose.backend.yml -f docker-compose.ldap.yml ps
sudo docker inspect -f '{{.Image}}' sixpay-connect-ldap-backend-1
sudo docker exec sixpay-connect-ldap-backend-1 wget -qO- http://127.0.0.1:8080/actuator/health
```

**Résultat observé du health check :** `{"groups":["liveness","readiness"],"status":"UP"}`. Le digest du conteneur doit être recoupé avec la référence immuable lors de futures vérifications.

## 4. Externalisation de la configuration

La configuration de l'environnement Contabo est **externalisée**, notamment dans `/opt/sixpay/config/backend.env`, plutôt qu'en codant les secrets et adresses dans l'image. Ne pas confondre les paramètres effectivement observés avec la totalité des variables disponibles dans `application-production.yml`.

| Groupe | Paramètres importants | Politique |
|---|---|---|
| Profil | `SPRING_PROFILES_ACTIVE=production` | Valeur constatée |
| LDAP | `SIXPAY_LDAP_ENABLED=true`, `SIXPAY_LDAP_URLS=ldaps://dc.sixpay.test:636`, `SIXPAY_LDAP_BASE_DN`, `SIXPAY_LDAP_USER_SEARCH_BASE`, `SIXPAY_LDAP_TRUST_DOMAIN` | Valeurs d'environnement |
| LDAP service account | `SIXPAY_LDAP_SERVICE_ACCOUNT_DN`, `SIXPAY_LDAP_SERVICE_ACCOUNT_PASSWORD` | Mot de passe secret, jamais dans Git/log |
| Recherche et identité | `SIXPAY_LDAP_USER_SEARCH_FILTER`, `SIXPAY_LDAP_LOGIN_ATTRIBUTE`, `SIXPAY_LDAP_USERNAME_ATTRIBUTE`, `SIXPAY_LDAP_SUBJECT_ATTRIBUTE` | Sémantique `sAMAccountName` / `objectGUID` à valider |
| TLS Java | Truststore PKCS12 monté hors de l'image, propriétés JVM/truststore | CA du DC et CA Java habituelles conservées |
| LDAP bootstrap | `SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false` | **Ne pas réactiver** après initialisation |
| Session | `SIXPAY_SESSION_TIMEOUT` (30m), `SIXPAY_SESSION_COOKIE_SECURE=true` | Conserver `Secure=true` pour HTTPS |
| PostgreSQL | `SIXPAY_DB_URL`, `SIXPAY_DB_USERNAME`, `SIXPAY_DB_PASSWORD` | Secret DB hors Git |
| M2M (à venir) | `SIXPAY_M2M_ENABLED`, `SIXPAY_M2M_ISSUER_URI`, `SIXPAY_M2M_JWK_SET_URI`, `SIXPAY_M2M_AUDIENCE` | **Ne pas activer** avant Keycloak/JWT et mTLS validés |

L'inventaire complet des paramètres de production dépend du SHA analysé : contrôler `backend/bootstrap/src/main/resources/application-production.yml` sur la révision ciblée avant tout changement. Le fichier d’inventaire local antérieur `sixpay-production-variable-inventory.md` a été établi à un autre SHA : il est indicatif, pas preuve de la configuration réellement chargée sur le VPS.

**Contrôles de sécurité suggérés :** vérifier propriétaire/permissions des fichiers `.env` et `.p12` sans en afficher le contenu ; ne jamais utiliser `docker inspect` pour afficher les variables contenant des secrets dans un partage de logs ; exporter uniquement le nom des clés pour inventaire ; protéger les sauvegardes et copies temporaires. L'existence d'un fichier env ne remplace pas un secret manager en production.

## 5. Samba AD, certificat LDAPS et truststore Java

L'annuaire de test est Samba AD, conteneur `sixpay-cloud-ldap`. SIXPAY s'y connecte en LDAPS (TCP/636). L'utilisateur est recherché sous `CN=Users,DC=sixpay,DC=test`. Le compte technique fait la recherche, puis la vérification des credentials de l'utilisateur se fait par bind LDAP. La liaison d'identité stable repose sur `objectGUID`, pas sur un login mutable.

### Truststore : preuve fournie

Un truststore PKCS12 de remplacement a été examiné avec un conteneur temporaire Java 21 sans réseau et l'outil `keytool` :

```bash
sudo docker run --rm \
  --network none \
  --user 10001:10001 \
  --entrypoint /opt/java/openjdk/bin/keytool \
  --mount type=bind,src=/opt/sixpay/secrets/ldap/sixpay-ldap-truststore-new.p12,dst=/tmp/ldap-truststore.p12,readonly \
  eclipse-temurin:21-jdk \
  -list \
  -keystore /tmp/ldap-truststore.p12 \
  -storetype PKCS12 \
  -storepass '<MOT_DE_PASSE_TRUSTSTORE>'
```

**Résultat rapporté :** `Keystore type: PKCS12`, **119 entrées**, dont `sixpay-samba-ad-ca` (`trustedCertEntry`) et des CA standard du JDK. La vérification de présence de l'alias ne suffit pas à prouver seule la confiance TLS bout-en-bout : le bind LDAPS et la connexion applicative ont ensuite fonctionné. Dans l'opération historique, un mot de passe de laboratoire a été utilisé ; ne pas le recopier comme secret de production.

Règles : ne pas désactiver la validation TLS, ne pas utiliser `ldap://` pour contourner LDAPS, ne pas remplacer `dc.sixpay.test` par une IP pour contourner l'identité du certificat. Réimporter une CA renouvelée seulement après contrôle de son empreinte avec l'administrateur AD. Faire une sauvegarde avant remplacement du truststore et appliquer des permissions minimales.

## 6. Authentification et liaison LDAP : résolution de l'incident HTTP 401

### Incident

Sur le VPS, `POST /api/v1/auth/login/ldap` avec `sixpay.admin` retournait **HTTP 401**, alors qu'un parcours local Docker Desktop avec login court fonctionnait. Les contrôles ont distingué résolution DNS/LDAPS, compte AD, identité stable, et étape de bind utilisateur.

### Correction du code

Sur la révision `e8a528bc...`, l'adaptateur LDAP conserve le **DN complet** du profil annuaire et utilise ce DN pour le bind utilisateur plutôt que seulement le `sAMAccountName`. Des corrections de constructeurs dans les tests Java ont été nécessaires avant publication de l'image. Le diagnostic « bind au login court » est une explication forte mais ne doit pas être présenté comme une preuve cryptographique exclusive de la cause initiale.

### Résultat après nouveau déploiement

`LOGIN_STATUS=200` a été observé pour `sixpay.admin` puis pour `sixpay.testuser`. La vérification de `/api/v1/auth/me` s'est faite dans un script de diagnostic qui transmet explicitement le cookie de session sur le réseau Docker privé (voir incident HTTP/Secure plus bas). **Ne pas reproduire ce contournement comme mode d'exploitation public.**

## 7. Bootstrap ADMIN (one-shot) — achevé et désactivé

Le bootstrap LDAP a servi **une seule fois** à initialiser un compte administrateur SIXPAY à partir d'une identité AD existante ; il n'est pas le mécanisme permanent de gestion des utilisateurs.

Identité observée :

```text
AD username: sixpay.admin
AD DN: CN=sixpay.admin,CN=Users,DC=sixpay,DC=test
AD objectGUID: 09d2761b-127b-4ed4-b385-a5ec584f1931
SIXPAY UUID: 32e414be-c323-49ab-b400-6673db835e13
SIXPAY role: ADMIN
Permissions: 20
Status: ACTIVE
Auth method: LDAP
```

Les contrôles DB et l'endpoint `/api/v1/auth/me` ont confirmé la liaison canonique, le statut, le rôle `ADMIN` et 20 permissions. **État durable confirmé : `SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false`** après redémarrage. Ne pas le réactiver pour ajouter un autre utilisateur ; utiliser l'API administrative avec une session ADMIN authentifiée.

## 8. Provisionnement MANAGER — réalisé et validé

### Précondition

Le compte `sixpay.testuser` existait déjà dans l'annuaire :

```text
AD DN: CN=sixpay.testuser,CN=Users,DC=sixpay,DC=test
objectGUID: cbd1f5cf-e2f2-42bb-8a6a-08f9a8170792
userAccountControl: 512 (compte actif)
accountExpires: 9223372036854775807
pwdLastSet: non nul
```

L'exploitant a renouvelé le mot de passe AD avec :

```bash
sudo docker exec -it sixpay-cloud-ldap samba-tool user setpassword sixpay.testuser
```

**Sortie rapportée :** `Changed password OK`. Le secret n'est pas consigné dans ce runbook.

### Provisionnement via API sécurisée

Une session de `sixpay.admin` a été créée avec `POST /api/v1/auth/login/ldap`. Pour la requête d'écriture, le diagnostic a transmis le cookie `JSESSIONID`, le cookie CSRF et l'en-tête `X-XSRF-TOKEN`. L'opération métier a été :

```http
POST /internal/api/v1/administration/directory-users/sixpay.testuser/provisioning
Content-Type: application/json
Cookie: JSESSIONID=<SESSION>; XSRF-TOKEN=<CSRF>
X-XSRF-TOKEN: <CSRF>

{"roles":["MANAGER"],"permissions":["accounting.read","accounting.t1.execute","observed-customer.read","payment.audit","payment.read","payment.write"]}
```

**Attention :** le schéma exact de session/CSRF peut évoluer ; ne jamais envoyer les cookies sur un HTTP public. Ici, les tests directs se limitaient à une liaison HTTP interne Docker.

**Résultat rapporté :** `HTTP 201 Created` ; compte SIXPAY `d6a4354f-b3ce-4d89-bcbf-89b589b95989`, `ACTIVE`, `MANAGER` et **six permissions**, corroborées en base. L'identité canonique a `identityType=LDAP`, `provider=sixpay-preproduction-ldap`, `subject=objectGUID` ci-dessus.

Un septième droit envisagé `payment.reverse` n'existe **pas** dans l'énumération canonique des permissions SIXPAY. Il a été retiré **avant provisioning**, avec validation de l'utilisateur ; ne jamais l'ajouter arbitrairement. La politique métier `PaymentRolePolicy` distingue les autorisations de rôle et les permissions applicatives : ne pas confondre les deux.

### Contrôle session et autorisation

Après connexion directe avec `sixpay.testuser` :

```text
LOGIN_STATUS=200
ME_STATUS=200
USERNAME=sixpay.testuser
AUTHENTICATION_METHOD=LDAP
HAS_MANAGER_ROLE=True
HAS_ADMIN_ROLE=False
PERMISSION_COUNT=6
```

Les autorités exposées par `/api/v1/auth/me` portent le préfixe Spring `SCOPE_` :

```text
SCOPE_accounting.read
SCOPE_accounting.t1.execute
SCOPE_observed-customer.read
SCOPE_payment.audit
SCOPE_payment.read
SCOPE_payment.write
```

Un premier contrôle d'égalité de listes a renvoyé `EXPECTED_PERMISSIONS_MATCH=False` uniquement parce qu'il comparait les noms de permissions sans `SCOPE_` aux autorités avec `SCOPE_`. Après normalisation, l'égalité était exacte : **ce n'était pas un échec de provisioning**.

Enfin, un appel d'administration réservé à ADMIN depuis la session MANAGER a produit :

```text
LOGIN_STATUS=200
ADMIN_API_STATUS=403
ACCESS_CONTROL=PASSED
```

La séparation de privilèges a donc été vérifiée sur ce cas négatif précis. Le compte `sixpay.admin` n'a pas été modifié lors du provisioning.

## 9. Incident de session : HTTP 200 login puis `/auth/me` HTTP 401

Un client Python avec jar de cookies a reçu `HTTP 200` au login LDAP mais `HTTP 401` sur `GET /api/v1/auth/me`. Analyse des cookies :

```text
JSESSIONID: Secure=True, Path=/, DomainSpecified=False
XSRF-TOKEN: Secure=False, Path=/, DomainSpecified=False
```

**Cause du comportement de test :** le client cookie jar respectait `Secure`, refusant de renvoyer `JSESSIONID` sur `http://<IP_DOCKER>:8080`. Ce n'était pas une preuve d'échec LDAP. Le contournement diagnostique a consisté à envoyer manuellement `Cookie: JSESSIONID=...` sur la liaison Docker privée, afin d'isoler le contrôle d'accès applicatif. Il ne constitue **pas** une solution de production.

**Décision :** maintenir `SIXPAY_SESSION_COOKIE_SECURE=true` sur Contabo ; apporter frontend et HTTPS via reverse proxy avant les véritables tests navigateur ; vérifier alors `X-Forwarded-Proto`, la politique cookies/CSRF et l'accès same-origin. En local Docker Desktop, la configuration préproduction possède `SIXPAY_SESSION_COOKIE_SECURE=false`, ce qui explique la différence de comportement sans justifier sa reprise sur Contabo. Vérifier aussi l'attribut `Secure` de `XSRF-TOKEN` sur le futur parcours HTTPS.

## 10. Contrôles techniques et résultats

| Contrôle | Résultat observé | Portée |
|---|---|---|
| `docker compose config` lors du redéploiement | Configuration acceptée selon compte rendu | N'implique pas validation fonctionnelle |
| Backend health | `UP` avec groupes liveness/readiness | Runtime backend |
| Truststore PKCS12 | 119 entrées, `sixpay-samba-ad-ca` | Contenu truststore |
| Login admin LDAP | HTTP 200 | Identité humaine |
| `/auth/me` admin avec cookie manuel local | HTTP 200, ADMIN, 20 autorités | Session et mapping |
| API détail de l'utilisateur annuaire par ADMIN | HTTP 200 | Recherche annuaire admin |
| Provisioning MANAGER | HTTP 201 | Création/lien/autorités |
| PostgreSQL identity/roles/permissions | Concordance rapportée | Persistance |
| Login MANAGER + `/auth/me` | HTTP 200/200, rôle MANAGER, 6 autorités | Auth et autorisation |
| Tentative API ADMIN par MANAGER | HTTP 403 | Refus attendu |
| DNS `sixpay-connect.com` / `cloud.sixpay-connect.com` | Pas de résolution IPv4 depuis VPS | Infrastructure à compléter |
| OS/proxy/ports | AlmaLinux 10.2 ; pas de nginx/caddy/httpd ; 80/443 libres ; firewalld actif | Préparation HTTPS |
| Images frontend | Aucune sur Contabo | Publication manquante |

### Commandes d'observation réutilisables

```bash
# Réseau interne backend (l'IP change après recréation)
sudo docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' sixpay-connect-ldap-backend-1

# Inventaire images (ne prouve pas la disponibilité dans GHCR)
sudo docker image ls --format '{{.Repository}}:{{.Tag}} | {{.ID}}' | grep -Ei 'sixpay|frontend'

# OS et firewall
cat /etc/os-release | grep -E '^(NAME|VERSION|ID)='
sudo firewall-cmd --state
sudo ss -lntp | grep -E ':(80|443)\s' || echo 'PORTS_AVAILABLE'

# DNS (résultats historiques : aucun IPv4)
getent ahostsv4 cloud.sixpay-connect.com | awk '{print $1}' | sort -u
```

Une commande telle que `docker manifest inspect ...` renvoyant `FRONTEND_IMAGE_NOT_ACCESSIBLE` ne distingue pas image absente, tag incorrect ou authentification manquante ; l’opérateur a ensuite confirmé qu'aucune image frontend n'était encore publiée.

## 11. Incidents et mesures de prévention

| Incident | Symptôme | Résolution/état |
|---|---|---|
| Truststore LDAPS | Nécessité de faire confiance à la CA Samba tout en conservant les CA Java | Truststore PKCS12 contenant CA Samba + CA standards, contrôle keytool |
| Login court AD sur VPS | Login LDAP 401 alors que test local fonctionnait | Adaptateur modifié pour bind utilisateur via DN complet ; nouvelle image déployée, login 200 |
| Tests Java suite à l'évolution de l'adaptateur | Constructeurs tests incompatibles | Corrigés avant publication |
| Cookie `Secure` envoyé en HTTP | Login 200, `/auth/me` 401 avec jar Python | Diagnostic cookie explicite seulement en réseau privé ; HTTPS à livrer |
| CSRF lors du provisioning | POST exige jeton CSRF et session admin | Cookie `XSRF-TOKEN` + header `X-XSRF-TOKEN` utilisés dans le diagnostic |
| Permission inexistante | `payment.reverse` absent de l'énumération | Exclue ; 6 permissions canoniques provisionnées |
| Comparaison des authorities | `EXPECTED_PERMISSIONS_MATCH=False` | Préfixe `SCOPE_` normalisé ; droits exacts |
| Backend GHCR | Révision précédente de l'image | Digest immuable remplacé, Compose validé, backend ciblé recréé |
| Frontend GHCR | Pas d'image publiée | Workflow `frontend-image-publish.yml` à concevoir, non réalisé |
| DNS | Aucun A résolu ; propriété du domaine non connue | Reporter DNS/certificat, sans bloquer la préparation frontend/Keycloak |

## 12. Sécurité et exploitation

1. Maintenir LDAP `LDAPS://` avec validation du certificat ; pas de bind anonyme ou contournement TLS.
2. Ne jamais publier `backend.env`, secrets AD, secrets PostgreSQL, truststore et jetons/cookies dans Git ou les échanges.
3. Pour la banque, utiliser un **compte technique AD non-admin à privilèges minimaux**, des DC approuvés, une CA institutionnelle et la rotation des secrets ; ne pas recopier le compte `Administrator` de laboratoire.
4. Préserver `SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false` ; n'effectuer les provisions ordinaires que depuis le parcours administratif contrôlé.
5. Ne pas exposer PostgreSQL, Samba AD/LDAPS, le backend direct ou le Keycloak admin sur Internet par défaut.
6. Ne pas désactiver les cookies `Secure` pour contourner l'absence actuelle de HTTPS.
7. Avant une modification réseau/certificat/CI/sécurité/déploiement, prendre une décision explicite et préserver un rollback.
8. Pour un reverse proxy ou un futur gateway multi-réplicas, vérifier les entêtes forwardés, session sticky/shared session, CSRF et cookies.

## 13. Prochaine validation / critères de clôture cloud

- Frontend Angular publié sous image GHCR (profil `preproduction`) et épinglé par digest ; frontend déployé sur un réseau Docker approprié.
- Reverse proxy Nginx et routage same-origin frontend/API ; DNS et certificat HTTPS à compléter une fois le contrôle du domaine confirmé.
- Parcours **navigateur** ADMIN/MANAGER, cookies `Secure`, CSRF et refus ADMIN 403 revalidés via HTTPS réel.
- Keycloak M2M persistant, client_credentials, émission et validation de JWT, liaison partenaire et mTLS gateway validés **avant activation des endpoints sensibles**.
- Tests réseau/firewall, sauvegardes/restauration, rotation des secrets, redémarrages, logs/supervision ; dossier des exigences La Régionale en dernier.

## 14. Traçabilité des sources

- GitHub @ `e8a528bc...` : `AGENTS.md`, `ENGINEERING_CONTEXT.md`, `MASTER_PROMPT_INPUT_MANIFEST.yaml`, `MASTER_ENGINEERING_PROMPT.md`, `documentation/contracts/CONTRACT_REGISTRY.yaml`, `infrastructure/docker/preproduction/SIXPAY_LDAP_AD_PRODUCTION_GUIDE.md`, `frontend/Dockerfile`, `frontend/nginx.conf`, `.github/workflows/frontend-ci.yml`, `.github/workflows/backend-image-publish.yml`, `infrastructure/docker/m2m-keycloak/README.md`.
- Sorties d'exploitation fournies dans la discussion : login, session, provisioning, comparaison des permissions, 403, health check, images Docker, DNS, OS/firewall/ports et analyse des cookies.
- Sortie additionnelle jointe : `keytool -list` sur `/opt/sixpay/secrets/ldap/sixpay-ldap-truststore-new.p12`, 119 entrées.

**Ne pas interpréter ce runbook comme une preuve que tous les cas d'échec LDAP, de renouvellement des certificats, de HA ou de sécurité M2M ont été testés.**
