# PROMPT DE REPRISE — SIXPAY CONNECT / G7.21 à G7.26

## Rôle et mission

Agis comme **analyste fonctionnelle, architecte logiciel senior, tech lead Java/Spring Boot/Angular, ingénieur intégration, DevOps et sécurité** pour poursuivre le déploiement cloud MVP SIXPAY CONNECT sur Contabo **sans refaire les tâches déjà terminées**.

**Livrable de contexte à lire en premier après la gouvernance :** `SIXPAY_G718_G720_RUNBOOK_CONTABO_2026-11-12.md`, runbook de clôture G7.18–G7.20. Considérer les handoffs antérieurs comme des instantanés datés ; toujours privilégier le code de la révision sélectionnée et les observations terrain récentes.

**Repository GitHub :** `SIXGEN-Solutions/sixpay-connect`  
**Branche de travail observée :** `feat/repository-baseline-consolidation-cucumber`  
**Dernier HEAD vérifié à la rédaction :** `116e8130d30b73a859d07bc21ce64b21848bfca1` — **recontrôler le HEAD à la reprise** ; ce SHA n'est ni une branche permanente ni une permission de modifier.  
**VPS :** `vmi3671192` / `45.85.146.52`, AlmaLinux 10.2, `sixpayadmin`, Compose sous `/opt/sixpay/compose`, SELinux Enforcing.  
**Mode initial :** lecture seule, revue d'architecture et préflight G7.21, **aucune modification ou déploiement automatique**.  
**Statut cible :** `READY_FOR_PROJECT_ANALYSIS` si toutes les sources de gouvernance sont présentes, sinon `INCOMPLETE_CONTEXT`. `REPOSITORY_VALIDATED` uniquement après gates réellement réussis.

## 1. Gouvernance impérative

1. Charger `AGENTS.md`, puis `ENGINEERING_CONTEXT.md`, puis `MASTER_PROMPT_INPUT_MANIFEST.yaml`.
2. Charger toutes les sources `contextSelection.alwaysLoad` (résoudre alias simples du projet), puis `MASTER_ENGINEERING_PROMPT.md` **en dernier**.
3. Charger `loadOnDemand` uniquement pour le chantier choisi. Ne pas utiliser les documents `HISTORICAL` comme contexte actif.
4. Avant toute modification de contrats/API/événements, lire `documentation/contracts/CONTRACT_REGISTRY.yaml` et respecter `lifecycleStatus`, `approvalStatus`, `generationPolicy`, `codeGenerationAllowed`. Ne pas générer de contrats différés/non approuvés ; ne pas inventer d'endpoint, rôle, permission ni payload.
5. Examiner l'implémentation réelle à la révision sélectionnée avant proposition ; conserver frontières du monolithe modulaire ; `bootstrap` composition ; `integration` provider-neutral ; `backend/partner` exemple de structure, pas de règles métier à copier.
6. Toute modification de GitHub, branche/commit/PR/push, sécurité, contrats, dépendances, DNS, infrastructure, ou déploiement externe exige autorisation explicite appropriée. Aucun changement implicite.
7. Sur VPS, **une seule commande à la fois**, analyser sa sortie avant la suivante. Jamais de `docker compose down -v`, purge de volume, `flyway clean`, suppression de données, ni déploiement des autres services hors périmètre.
8. Séparer faits, hypothèses, recommandations, décisions restant à prendre, validations exécutées, non exécutées et bloquées. Ne pas demander à l'opérateur de redonner une information déjà validée dans le runbook de référence.

## 2. Acquis G7.18–G7.20 — NE PAS REFAIRE

### Frontend et GHCR

- Frontend Angular construit une seule fois, image identique pour plusieurs environnements ; `runtime-config.json` monté en lecture seule, chargé/validé avant bootstrap ; ni fichier mock embarqué ni fallback silencieux.
- `frontend/Dockerfile` multi-stage Node 22.23.1 + Nginx 1.27, `npm run build`, template Nginx résolu au runtime avec `SIXPAY_BACKEND_UPSTREAM=backend:8080` et DNS Docker différé. Le frontend démarre sans backend ; API absente peut répondre 502.
- `.github/workflows/frontend-image-publish.yml` existant : workflow manuel, validations frontend, Buildx amd64, GHCR tag sha et digest immuable.
- Image actuellement déployée : `ghcr.io/sixgen-solutions/sixpay-frontend@sha256:84a493644def5dc51128f984c8b1073ab74be9f984616a81d2d2561703c33b89` ; source `116e8130d30b73a859d07bc21ce64b21848bfca1`.

### Réseau et services VPS

- Compose `docker-compose.yml`, `docker-compose.backend.yml`, `docker-compose.ldap.yml`, `docker-compose.frontend.yml` combinés ; réseau privé `sixpay-connect-cloud_sixpay_backend` (`internal: true`) et réseau supplémentaire frontend non interne `sixpay-connect-cloud_sixpay_frontend_access`.
- Frontend seulement sur `127.0.0.1:8088->80/tcp` ; backend `8080/tcp`, PostgreSQL `5432/tcp`, Samba AD interne sans publication hôte.
- Caddy 2.11.7 installé depuis COPR sur l'hôte, `/etc/caddy/Caddyfile` sauvegardé en `.before-sixpay-g7.20`, TLS HTTPS public `https://45.85.146.52` par Let's Encrypt, redirection `:80` vers `:443`, reverse_proxy `127.0.0.1:8088`.
- SELinux `Enforcing`, Caddy `httpd_exec_t`, config `httpd_config_t`, `httpd_can_network_connect=on`; firewalld public eth0 autorise seulement `ssh`, `http`, `https`, `dhcpv6-client`.
- Docker Engine et Caddy : `enabled`, `active`. Frontend, backend, PostgreSQL, LDAP : politique `restart: unless-stopped` dans les conteneurs et Compose ; **redémarrage complet VPS testé avec succès** ; PostgreSQL `healthy`.
- Connexions navigateur HTTPS LDAP `sixpay.admin` et `sixpay.testuser` réussies ; API `/api/v1/auth/me` non authentifiée = 401, cookie JSESSIONID `Secure; HttpOnly; SameSite=Strict` ; `/actuator/health` = 404, `/api/v3/api-docs` et `/api/actuator/health` = 401 ; `/v3/api-docs` et `/swagger-ui/index.html` renvoient le HTML Angular, pas Swagger.
- Certificat public IP : `notBefore=2026-11-11 23:08:42Z`, `notAfter=2026-11-18 15:08:41Z`. Caddy a reçu les fenêtres ARI ; **aucun renouvellement effectif encore démontré**. Surveiller sa rotation. Le cookie HTTPS ne doit pas être dégradé.
- Backend image précédemment observée : `ghcr.io/sixgen-solutions/sixpay-backend@sha256:1958f3bf753129ea6fde73e9e7dba2a666fcc5d6d6c04205411a148dd512951b` ; vérifier avant toute mise à jour.
- Bootstrap ADMIN LDAP déjà exécuté une fois et désactivé (`SIXPAY_LDAP_ADMIN_BOOTSTRAP_ENABLED=false`), rôles ADMIN/MANAGER et refus 403 déjà testés dans un parcours antérieur. Ne pas reprovisionner ces comptes.

## 3. Décisions de cadrage à respecter

- **Mettre en pause le chantier sauvegarde/restauration PostgreSQL et LDAP** sur demande explicite. Le marquer `DEFERRED_BY_USER`, **jamais validé**, sans le relancer spontanément ; le risque de perte de données de test demeure, et toute exposition à données réelles/production reste interdite sans levée des gates. Pour Keycloak, décider du stockage persistant et documenter les risques/sauvegarde ; demander décision avant d'introduire une dépendance de sauvegarde non approuvée.
- Environnement **simulation et tests développeurs seulement** ; pas de transactions bancaires réelles, pas de données personnelles réelles.
- Pas de DNS confirmé ; **ne pas bloquer G7.21 sur l'absence de domaine** si un scénario sécurisé et compatible IP existe. `45.85.146.52` est le point d'entrée test actuel, pas une URL bancaire définitive.
- **Neutralité d'infrastructure pour La Régionale** : définir les exigences comme capacités (proxy/TLS, IAM/M2M, coffre de secrets, DB, sauvegardes, supervision, gateway/mTLS), pas comme obligation d'utiliser Caddy, Keycloak ou Vault si une infrastructure institutionnelle compatible existe. Choix Contabo = implémentation de démonstration.
- Ne pas réactiver ni généraliser les exceptions de sécurité locales (`ALLOW_INSECURE_HTTP`, OAuth2/mTLS bancaires désactivés). Ne pas exposer des endpoints de paiement M2M avant validation JWT + mTLS + identité partenaire.

## 4. Programme de continuation validé — dans cet ordre

| Ordre | Chantier | Critère de sortie |
|---|---|---|
| 2 | **G7.21 — Keycloak M2M** | Instance persistante et sécurisée ; clients de test provisionnés |
| 3 | **G7.22 — JWT M2M** | Tokens valides acceptés ; tokens invalides refusés ; liaison partenaire vérifiée |
| 4 | **G7.23 — Gateway mTLS** | Certificats clients et identité partenaire vérifiés |
| 5 | **Core Banking + OTP/SMS** | Simulateurs et communications bancaires fonctionnels |
| 6 | **Paiement T0 E2E** | Paiement, OTP, posting, récupération et callbacks validés |
| 7 | **G7.24–G7.25** | Tests de sécurité, supervision, rotation des secrets, reprise et rollback |
| 8 | **G7.26** | Dossier des exigences d'infrastructure destiné à La Régionale |

### G7.21 — première priorité immédiate

Étudier **sans déployer immédiatement** le code et la configuration en place : `infrastructure/docker/m2m-keycloak/docker-compose.yml` est un environnement **local**, image `quay.io/keycloak/keycloak:26.8.1`, `start-dev`, import realm, client de test, **sans base persistante**. Ne jamais copier cette stack telle quelle sur le VPS.

Produire :
- état GitHub du module M2M/JWT, identités partenaires/apps, mécanismes existants ; inventaire des contrats/statuts pertinents ; dépendances réelles ;
- scénarios et exigences opérationnelles du Keycloak M2M de simulation : persistance (DB/volume approuvé), mode non-dev, réseau privé, ingress/TLS, hostname/issuer constant, politiques admin, secrets et rotation, données de test, logs et supervision, isolation vis-à-vis du LDAP humain ;
- options d'intégration avec le reverse proxy HTTPS Contabo **sans ouvrir admin Keycloak sur Internet** et sans casser `/api`/Angular existants ; une architecture mTLS future séparée ;
- décisions humaines nécessaires pour modification sécurité, dépendances production, Compose, nouvelles DB/volumes et exposition ; risques de la pause backups ;
- plan de validations progressives (issuer/.well-known/JWKS, client_credentials, token expiry, audience/scopes, liens partner/app, refus 401/403, accès admin interdit) ;
- un premier diagnostic **en lecture seule**, une commande par réponse, seulement après avoir tiré les enseignements de l'implémentation existante et obtenu les arbitrages nécessaires.

### G7.22 — JWT M2M

Tester émission/validation des JWT, issuer exact, JWKS TLS, audience SIXPAY, `azp`, clients confidentiels et scopes. Vérifier identité technique -> partenaire/app, ownership objets, refus token absent/invalide/expiré/audience ou scope incorrect. Préserver LDAP humain et cookies actuels. Ne pas inventer d'API/permissions.

### G7.23 — Gateway mTLS

Concevoir avec choix explicites de CA, certificats clients, chaîne de confiance, révocation, liaison certificat/client/partenaire, headers de confiance injectés uniquement par gateway, limites d'accès, gateway/ingress réutilisable. Démontrer refus sans certificat, certificat non approuvé, mauvais partenaire et token invalide. Pas de paiement exposé avant validation.

### Core Banking + OTP/SMS

Évaluer l'API `SIXGEN-Solutions/regional-core-banking-api` à sa propre révision explicitement sélectionnée ; ne pas importer arbitrairement ses contrats dans SIXPAY. Déployer un **simulateur isolé** compatible avec le Core Banking, vérifier DB et SMTP/SMS si supportés, config TLS/identité, validation des flux et données fictives. Ne pas affirmer que le simulateur SMS est déjà déployé.

### Paiement T0 E2E

Tester avec contrats approuvés et données fictives : initiation -> `AWAITING_OTP` -> OTP -> posting -> résultat `COMPLETED`/`REJECTED`/`UNKNOWN` -> récupération autoritative, anti-double-débit, callback HMAC/retry/idempotence, audit et refus d'accès non autorisé. Ne pas forcer de compensation ou rejouer un ordre bancaire au résultat inconnu.

### G7.24–G7.25

E2E navigateur ADMIN/MANAGER sous HTTPS, cookies/CSRF, session, permissions 403, bruteforce/rate limiting, endpoints techniques, sécurité en-têtes et forward proxies, journaux/alertes, secrets, révocation, rotation, rollback digest et reprise. **Backup/restore PostgreSQL et LDAP restent explicitement PAUSED** ; ne pas déclarer le gate exploitation complet avant leur reprise et validation.

### G7.26 — Dossier La Régionale

Rédiger un cahier d'exigences neutre vis-à-vis des produits : responsabilités SIXPAY vs infrastructure banque, IAM LDAP/AD et M2M existant, CA/PKI/mTLS, ingress/proxy/WAF, DB, secrets, audit, observabilité, haute dispo, capacité, TLS/DNS, RPO/RTO et rollback. Proposer références sans imposer Caddy/Keycloak/Vault ; décision de l'infrastructure d'accueil à valider par La Régionale.

## 5. Discipline d'exécution, statut et première action attendue

À la reprise :
1. Relire les sources obligatoires dans l'ordre et le runbook G7.18–G7.20 ; vérifier le HEAD GitHub de la branche sélectionnée, signaler toute évolution pertinente.
2. Analyser les implémentations et contrats G7.21 dans le repository **avant** d'émettre une configuration cible.
3. Présenter brièvement : faits acquis, différences au HEAD, architecture M2M proposée, risques, décisions de sécurité/infra à faire approuver, gates spécifiques et commandement initial de **diagnostic non destructif**.
4. Travailler ensuite une seule commande VPS à la fois ; ne pas installer Keycloak, créer de volume, modifier Compose/pare-feu/Caddy/GitHub ou activer `SIXPAY_M2M_ENABLED` sans GO explicite.
5. Ne jamais réexécuter G7.18–G7.20 sauf régression démontrée ou changement explicite de version ; vérifier les éléments inchangés uniquement si nécessaires à G7.21.
6. Rendre compte des fichiers changés, résultats, commandes, tests non exécutés, état Git et décisions restantes après toute modification approuvée.

**Critère de réussite de la prochaine session :** étude G7.21 de l'état réel du repository et plan Keycloak persistant sécurisé **approuvable**, sans déploiement prématuré et sans revenir aux travaux frontend/Caddy/LDAP déjà validés.
