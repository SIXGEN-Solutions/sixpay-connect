# SIXPAY CONNECT — Runbook E1.1 : Préparation et sécurisation du VPS Contabo

**Version :** 1.0 — 8 novembre 2026 (date de référence du chantier ; certains journaux système affichent le 9 novembre, fuseau CET)  
**Objet :** procédure de reproduction, d'exploitation et de transmission de connaissances, étape préalable à E1.2.  
**Statut :** étapes de préparation validées à partir des sorties fournies par l'opérateur ; contrôles complémentaires signalés séparément.  
**Mode :** documentation, aucune opération distante exécutée par le rédacteur.  
**Repository applicatif :** `SIXGEN-Solutions/sixpay-connect` ; branche de travail précédemment observée `feat/repository-baseline-consolidation-cucumber` ; dernier SHA GitHub observé `f0bbf910ca84f4a1ba636469289ea6651335f963`. Le SHA est un constat, **pas** une branche imposée ni une révision à réutiliser sans la revérifier.

## 0. Périmètre et architecture

- **VPS :** Contabo Cloud VPS 8 ; AlmaLinux 10.2 (Lavender Lion), x86_64 ; 8 vCPU ; 24 Go RAM commerciaux (environ 23 Gio détectés) ; disque 300 Go (environ 299 Gio sur `/`) ; 600 Mbit/s ; trois snapshots proposés par Contabo.
- **Stockage observé :** `/dev/sda4` monté sur `/`, système de fichiers XFS ; environ 292 Go libres à l'inventaire. **Swap :** 0.
- **Sécurité initiale :** SSH `root` + mots de passe autorisés, port 22 accessible IPv4/IPv6 ; SELinux `Enforcing` ; `firewalld` et Docker absents.
- **Finalité :** héberger la **simulation** SIXPAY (Angular/Nginx, Spring Boot, PostgreSQL, puis services d'identité et gateway de test), avec séparation technique appropriée.
- **Exclusion absolue :** le **Regional Core Banking API est une stack totalement indépendante** : aucun service Core Banking, base, secret, volume ou procédure de déploiement Core Banking n'entre dans la composition SIXPAY. Les interactions sont des appels API externes contractuels et sécurisés.
- **Principe :** ne pas confondre environnement de simulation et production. Aucun paiement réel ni donnée réelle dans cet environnement sans autorisation explicite.

### Préconditions de sécurité

1. Avoir une fenêtre SSH existante **ouverte** et une seconde fenêtre de test.
2. Disposer de l'accès de secours Contabo (console/VNC, non chiffré : usage exceptionnel et fermeture après intervention).
3. Ne pas partager clés privées, mots de passe, fichiers `.env` sensibles, jetons, certificats privés ni inventaires réseau complets avec des tiers.
4. Tester les règles IPv4 **et IPv6** ; SSH écoute sur les deux familles.
5. Ne jamais désactiver SELinux pour contourner une erreur Docker ou de montage.
6. Conserver un journal des commandes exécutées, versions et preuves de validation ; ne pas enregistrer de secrets dans le journal.

---

## E1.1.1 — Identification de la machine et inventaire système

**Objectif :** confirmer OS, architecture, capacité, réseau et état initial avant toute modification.

```bash
cat /etc/os-release
uname -m
nproc
free -h
lsblk -f
df -h
systemctl is-active firewalld
firewall-cmd --list-all
getenforce
systemctl is-active docker
docker --version
```

**Résultat observé :** AlmaLinux 10.2 ; x86_64 ; 8 vCPU ; ~23 Gio RAM, ~22 Gio disponibles ; pas de swap ; XFS `/` ~299 Go, ~292 Go libres ; SELinux `Enforcing` ; `firewalld` initialement absent ; Docker initialement absent. Une commande « command not found » est normale pour un paquet non installé.

**Validation :** ✅ inventaire réalisé. **À surveiller plus tard :** mémoire/CPU réels, saturation disque, consommation des logs ; 8 vCPU ne signifie pas huit cœurs dédiés garantis.

## E1.1.2 — Audit initial SSH, pare-feu, utilisateurs et dépôts

```bash
sshd -T | grep -E '^(port|permitrootlogin|passwordauthentication|pubkeyauthentication|kbdinteractiveauthentication) '
ss -tulpen
rpm -q firewalld
getent group wheel
dnf repolist
dnf check-update
```

**Observations initiales :** port SSH 22 ; `permitrootlogin yes`, `pubkeyauthentication yes`, `passwordauthentication yes` ; écoute TCP 22 IPv4 et IPv6 ; groupe `wheel` sans membre affiché ; dépôts AlmaLinux BaseOS, AppStream, CRB, Extras ; mises à jour disponibles. `dnf check-update` peut retourner **100** si des mises à jour sont disponibles : ne pas interpréter 100 comme un échec de dépôt.

**Validation :** ✅ audit réalisé.

## E1.1.3 — Création d'un compte administrateur non-root

**Sur une session root existante :**

```bash
useradd -m -s /bin/bash sixpayadmin
passwd sixpayadmin
usermod -aG wheel sixpayadmin
id sixpayadmin
```

Choisir un mot de passe fort et distinct du mot de passe root ; il sert à `sudo`. Ne pas réexécuter `useradd` si le compte existe déjà. Alternative idempotente pour contrôler l'existant : `id sixpayadmin`.

**Sur Windows PowerShell**, générer une paire de clés **uniquement si elle n'existe pas déjà** :

```powershell
Get-ChildItem "$HOME\.ssh" -ErrorAction SilentlyContinue
ssh-keygen -t ed25519 -f "$HOME\.ssh\sixpay_contabo_ed25519" -C "sixpay-contabo-admin"
Get-Content "$HOME\.ssh\sixpay_contabo_ed25519.pub"
```

Protéger la clé privée par phrase de passe. **Ne jamais l'envoyer au VPS.** Le fichier `.pub` contient la seule clé à installer côté serveur.

**Sur le VPS root :**

```bash
install -d -m 700 -o sixpayadmin -g sixpayadmin /home/sixpayadmin/.ssh
# Ouvrir le fichier avec l'éditeur disponible :
vi /home/sixpayadmin/.ssh/authorized_keys
# ou installer nano et l'utiliser : dnf install -y nano
chown -R sixpayadmin:sixpayadmin /home/sixpayadmin/.ssh
chmod 700 /home/sixpayadmin/.ssh
chmod 600 /home/sixpayadmin/.ssh/authorized_keys
restorecon -RFv /home/sixpayadmin/.ssh
ssh-keygen -lf /home/sixpayadmin/.ssh/authorized_keys
```

Copier **toute** la ligne publique `ssh-ed25519 ...` dans `authorized_keys`, sans coupure. Dans `vi` : `i`, coller, `Esc`, `:wq` ; dans `nano` : `Ctrl+O`, Entrée, `Ctrl+X`.

**Incidents rencontrés et solutions :**

- `nano: command not found` : installer `nano` via `dnf` ou utiliser `vi`.
- Depuis **CMD**, `$HOME` n'est pas interprété comme en PowerShell : utiliser `%USERPROFILE%\.ssh\...` ou passer à PowerShell.
- `authorized_keys is not a public key file` : la ligne copiée était invalide/incomplète ; corriger avec le contenu complet du `.pub`. Une ligne Ed25519 complète est typiquement plus longue que les 69 octets observés lors de l'incident.
- Comparer les empreintes sans transmettre les clés privées : `ssh-keygen -lf ...` sur serveur et sur fichier `.pub` local.

**Test PowerShell, nouvelle fenêtre :**

```powershell
ssh -i "$HOME\.ssh\sixpay_contabo_ed25519" -o IdentitiesOnly=yes -o PreferredAuthentications=publickey -o PasswordAuthentication=no sixpayadmin@ADRESSE_IPV4_DU_VPS
```

**Test dans la session SSH :**

```bash
whoami
id
sudo whoami
```

**Résultats observés :** `sixpayadmin`, groupe `wheel`, puis `root` après saisie du mot de passe `sudo`. **Validation :** ✅ accès non-root par clé et `sudo` validés. Garder l'ancienne session ouverte pendant la transition.

## E1.1.4 — Durcissement du démon SSH

**Objectif :** refuser les connexions SSH directes root et les mots de passe, conserver les clés publiques et le port 22.

**Préconditions :** nouvelle connexion `sixpayadmin` par clé **et** `sudo whoami → root` réussis ; session de secours toujours ouverte.

```bash
sudo install -d -m 755 /etc/ssh/sshd_config.d
sudo tee /etc/ssh/sshd_config.d/01-sixpay-hardening.conf > /dev/null <<'SSHCONF'
PermitRootLogin no
PubkeyAuthentication yes
PasswordAuthentication no
KbdInteractiveAuthentication no
SSHCONF
sudo sshd -t
sudo sshd -T | grep -E '^(port|permitrootlogin|pubkeyauthentication|passwordauthentication|kbdinteractiveauthentication) '
```

**Attention :** un fichier de configuration SSH inclus peut ne pas avoir la priorité attendue. Si les valeurs effectives divergent, **ne pas recharger** ; inspecter les fichiers `/etc/ssh/sshd_config` et `/etc/ssh/sshd_config.d/` et leur ordre effectif avant toute correction.

**Valeurs effectives attendues :**

```text
port 22
permitrootlogin no
pubkeyauthentication yes
passwordauthentication no
kbdinteractiveauthentication no
```

**Uniquement après validation de syntaxe et des valeurs effectives :**

```bash
sudo systemctl reload sshd
sudo systemctl is-active sshd
```

Tester une **nouvelle** connexion SSH par clé, sans mot de passe, et `sudo whoami`. Ne pas fermer la session de secours avant confirmation.

**Repli si perte de nouvelle connexion :** depuis une session encore ouverte/console de secours :

```bash
sudo rm -f /etc/ssh/sshd_config.d/01-sixpay-hardening.conf
sudo sshd -t && sudo systemctl reload sshd
```

Cette commande restaure le comportement précédent de configuration SSH ; elle doit être suivie d'une vérification et d'une analyse du motif d'échec.

**Résultat observé :** `sshd -T` a affiché exactement les valeurs attendues ; une nouvelle connexion `sixpayadmin` et `sudo` ont réussi. **Validation :** ✅ durcissement SSH confirmé.

## E1.1.5 — Installation et configuration du pare-feu

**Objectif :** protéger `eth0` (IPv4 et IPv6), n'autoriser que SSH à ce stade, éviter l'exposition de Cockpit.

```bash
sudo dnf install -y firewalld
sudo firewall-offline-cmd --get-default-zone
sudo firewall-offline-cmd --list-all
ip -br addr
ip route
```

**Observé avant activation :** zone par défaut `public`, services `cockpit dhcpv6-client ssh`, interface réseau `eth0` avec IPv4 et IPv6 publiques.

L'ajout hors ligne de `ssh` a renvoyé `ALREADY_ENABLED`, donc l'accès était déjà autorisé :

```bash
sudo firewall-offline-cmd --zone=public --add-service=ssh
```

**Incident rencontré :** `firewall-offline-cmd --zone=public --remove-service=cockpit` a échoué avec `Can't use lokkit options with other options.` Le retrait n'a pas été appliqué. Ne pas confondre cet échec avec une suppression réussie.

Après vérification préalable de l'accès de secours et de SSH autorisé, le pare-feu a été activé :

```bash
sudo systemctl enable --now firewalld
sudo systemctl is-active firewalld
sudo firewall-cmd --get-active-zones
sudo firewall-cmd --zone=public --list-all
```

**Observed :** service `active`, zone `public` active sur `eth0`, SSH encore accessible après une **nouvelle** connexion par clé.

**Retrait de Cockpit avec la commande adaptée au service actif :**

```bash
sudo firewall-cmd --permanent --zone=public --remove-service=cockpit
sudo firewall-cmd --check-config
sudo firewall-cmd --reload
sudo firewall-cmd --zone=public --list-all
sudo firewall-cmd --zone=public --query-service=ssh
sudo firewall-cmd --zone=public --query-service=cockpit
```

**Résultats observés :** `success`, services `dhcpv6-client ssh`, requête SSH `yes`, requête Cockpit `no`. **Validation :** ✅ pare-feu activé, retrait Cockpit confirmé, nouvelle connexion SSH confirmée.

**Consignes futures :** ne pas ouvrir PostgreSQL (5432), LDAP, Keycloak admin, ports backend ou Docker API au public. Docker gère les règles des ports publiés : le pare-feu seul **ne suffit pas** à protéger un `ports: "8080:8080"`. Prévoir bind `127.0.0.1` pour les interfaces internes et une gateway dédiée ; vérifier IPv6.

## E1.1.6 — Mises à jour AlmaLinux et maintenance

**Préparer une fenêtre de maintenance :** un update peut toucher OpenSSH, NetworkManager, `glibc` et le noyau ; garder le secours Contabo accessible.

```bash
sudo dnf upgrade --refresh
```

Inspecter les paquets et confirmer avec `y` uniquement si le périmètre est approuvé. L'opération observée a installé **1 paquet noyau** et mis à jour **27 paquets**, notamment OpenSSH, NetworkManager, certificats, `glibc`, outils noyau. La transaction s'est terminée sur `Complete!`.

**Validation :** ✅ mise à jour terminée dans les journaux fournis ; pas de commande `dnf` lancée par le rédacteur.

## E1.1.7 — Redémarrage et contrôle après redémarrage

**Avant redémarrage :**

```bash
uname -r
rpm -q kernel
sudo systemctl is-enabled firewalld
sudo systemctl is-enabled sshd
sudo sshd -t
```

Vérifier la console Contabo avant interruption SSH.

```bash
sudo reboot
```

**Après redémarrage et reconnexion par clé :**

```bash
uname -r
sudo systemctl is-active sshd
sudo systemctl is-active firewalld
sudo firewall-cmd --get-active-zones
sudo firewall-cmd --zone=public --list-services
getenforce
sudo whoami
```

**Résultats observés :** noyau `6.12.0-211.74.1.el10_2.x86_64`, SSH `active`, firewalld `active`, zone `public` sur `eth0`, services `dhcpv6-client ssh`, SELinux `Enforcing`, `sudo whoami → root`. **Validation :** ✅ contrôles système post-redémarrage validés. Le dernier extrait est en console `root`; la connexion non-root avec `sudo` avait été prouvée auparavant.

## E1.1.8 — Installation de Docker Engine, Compose et test fonctionnel

**Objectif :** exécuter les images SIXPAY, sans installer Java/Maven/Node sur l'hôte simplement pour l'exécution.

**Préparation :**

```bash
sudo dnf install -y dnf-plugins-core
```

**Incident rencontré :** la syntaxe suivante **n'était pas acceptée** par le `dnf config-manager` installé :

```text
sudo dnf config-manager addrepo --from-repofile=https://download.docker.com/linux/rhel/docker-ce.repo
```

**Syntaxe compatible avec l'aide affichée sur le VPS :**

```bash
sudo dnf config-manager --add-repo https://download.docker.com/linux/rhel/docker-ce.repo
sudo dnf repolist | grep docker
sudo dnf list docker-ce --showduplicates
```

Vérifier l'origine du dépôt et la signature GPG auprès de la documentation officielle Docker. AlmaLinux est un dérivé de RHEL : vérifier que la version des paquets et ses dépendances sont compatibles EL10 avant acceptation.

**Installation :**

```bash
sudo dnf install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

**Tests :**

```bash
sudo docker version
sudo docker compose version
sudo systemctl is-active docker
sudo systemctl is-enabled docker
getenforce
sudo docker run --rm hello-world
```

**Résultats effectivement transmis par l'opérateur :**

- Docker Engine Community **29.9.3** côté client et serveur, `linux/amd64`.
- `containerd` **v2.4.2**, `runc` **1.5.2**.
- Docker Compose **v5.7.0**.
- Docker `active` et `enabled`.
- SELinux `Enforcing`.
- `hello-world` téléchargé et exécuté avec le message **« Hello from Docker! »**.

**Validation :** ✅ Docker fonctionnel, Compose présent, démarrage automatique activé. Le test d'assemblage d'une véritable stack Compose SIXPAY reste à faire en E1.2.

**Sécurité Docker :** ne pas ajouter `sixpayadmin` au groupe `docker` sans décision explicite ; `sudo docker` évite l'octroi permanent d'un accès équivalent root. Limiter les images par version/digest approuvé. Ne pas rendre le socket Docker accessible à des services non fiables.

---

## 3. Checklist de réception de l'étape E1.1

| Contrôle | État et preuve |
|---|---|
| OS AlmaLinux 10.2, x86_64, 8 vCPU, 24 Go, 300 Go | ✅ `os-release`, `uname`, `free`, `df` |
| Séparation utilisateur `sixpayadmin` / root | ✅ `id`, `sudo whoami` |
| Connexion administrateur SSH par clé | ✅ nouvelle session établie |
| Connexion SSH directe root interdite | ✅ `sshd -T` = `no` |
| Authentification SSH par mot de passe interdite | ✅ `sshd -T` = `no` |
| `firewalld` actif et attaché à `eth0` | ✅ `systemctl`, `firewall-cmd` |
| SSH autorisé, Cockpit retiré | ✅ services et `query-service` |
| SELinux `Enforcing` | ✅ `getenforce` |
| Mises à jour DNF terminées | ✅ `Complete!` |
| Nouveau noyau chargé après reboot | ✅ `uname -r` |
| Docker Engine opérationnel | ✅ `hello-world` |
| Docker au démarrage | ✅ `active`, `enabled` |
| Plugin Compose présent | ✅ `docker compose version` |
| Sauvegarde externe et restauration du VPS | ⬜ Non démontrées ; nécessaires avant la phase de déploiement |
| Test détaillé d'exposition réseau Docker IPv4/IPv6 | ⬜ À faire lors de E1.2 |
| Tests de déploiement SIXPAY et supervision | ⬜ Étape E1.2 et suivantes |

**Critère de sortie E1.1 :** machine prête comme **hôte Docker de simulation**. Cette validation ne signifie **ni** configuration de production achevée **ni** disponibilité de SIXPAY ou Core Banking.

## 4. Procédure d'exploitation courante (socle)

```bash
# Contrôles non destructifs
uname -r
free -h
df -h
sudo systemctl status sshd --no-pager
sudo systemctl status firewalld --no-pager
sudo systemctl status docker --no-pager
sudo firewall-cmd --zone=public --list-all
getenforce
sudo docker info
sudo docker compose version
```

**Recommandations non encore exécutées :** sécuriser les sauvegardes hors VPS, réserver le DNS et les certificats, définir les limites de logs et de volumes Docker, évaluer le besoin de swap sans compromettre la mémoire disponible, automatiser l'inventaire et la supervision. Trois snapshots fournisseur ne remplacent pas des sauvegardes applicatives cohérentes ni un test de restauration.

## 5. Handoff — Prochaine étape E1.2

**E1.2 : Préparation de l'environnement SIXPAY sur Contabo**, à traiter dans une conversation distincte :

1. Reconfirmer le HEAD GitHub et charger les sources de gouvernance applicables, sans considérer un ancien SHA comme autoritatif.
2. Inventorier les configurations Compose et Dockerfiles du repository avant de proposer leur adaptation.
3. Définir l'organisation sécurisée des répertoires `/opt/sixpay` et du stockage persistant `/var/lib/sixpay` ; **ne pas créer** de fichiers sur la machine sans accord explicite.
4. Définir les stacks logiques SIXPAY (application, identité de test, gateway), réseaux, volumes, secrets, configuration et stratégie DNS/TLS.
5. Préparer une installation progressive PostgreSQL → backend → frontend → authentification → supervision, avec tests et retour arrière à chaque étape.
6. Maintenir Core Banking **hors VPS SIXPAY / hors Compose SIXPAY**, communiquer uniquement via API externe sécurisée et endpoint configuré.
7. Traiter les configurations dangereuses existantes avant toute exposition : ports PostgreSQL/backend, profil Spring, secrets, Keycloak `start-dev`, routes Nginx `/v1/payments/` et exposition `/actuator/`.
8. Exécuter les tests Docker, réseau, sécurité, fonctionnement, persistance, sauvegarde/restauration et résilience qui seront explicitement autorisés.

**Opérations non réalisées dans ce runbook :** aucun changement de repository, aucun commit, push, PR, déploiement applicatif ou intervention Core Banking.
