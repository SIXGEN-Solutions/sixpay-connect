# SIXPAY CONNECT — Guide de connectivité LDAP / Microsoft Active Directory

## 1. Objet

Ce guide décrit les éléments opérationnels à préparer lorsque SIXPAY CONNECT sera raccordé au Microsoft Active Directory de La Régionale. Il s'appuie sur l'architecture LDAP actuellement implémentée et sur les enseignements de la validation preproduction avec Samba AD.

La règle d'architecture reste :

> Active Directory authentifie l'identité ; SIXPAY gère les rôles, permissions et autorisations applicatives.

Les groupes ou attributs AD ne deviennent donc pas automatiquement des autorités SIXPAY.

## 2. Topologie cible

```text
SIXPAY backend
      |
      | LDAPS / TCP 636
      v
Microsoft Active Directory
La Régionale
```

En production, le Samba AD utilisé en preproduction disparaît. SIXPAY doit joindre directement les Domain Controllers autorisés par La Régionale.

## 3. Informations à obtenir de La Régionale

Avant le déploiement, obtenir et valider avec l'équipe infrastructure/sécurité :

- les URL LDAPS des Domain Controllers ;
- le Base DN ;
- le User Search Base ;
- le filtre de recherche utilisateur ;
- l'attribut de login ;
- l'attribut username ;
- l'attribut d'identité stable ;
- le DN du compte technique SIXPAY ;
- le mode de gestion/rotation de son secret ;
- la chaîne de certificats ou CA institutionnelle ;
- les noms DNS des Domain Controllers ;
- les règles réseau/firewall ;
- les exigences de haute disponibilité et de bascule entre DC.

La configuration SIXPAY actuelle est conçue autour de valeurs de type :

```text
urls             = ldaps://<dc>:636
user-search      = (sAMAccountName={0})
login-attribute  = sAMAccountName
username         = sAMAccountName
subject          = objectGUID
```

Les valeurs réelles doivent être fournies et approuvées par La Régionale.

## 4. Compte technique LDAP

SIXPAY doit disposer d'un compte technique dédié permettant la recherche des utilisateurs nécessaires à l'authentification.

Ce compte :

- doit être distinct des comptes humains ;
- ne doit pas être Domain Admin ;
- ne doit recevoir que les permissions de lecture nécessaires ;
- ne doit pas avoir son secret stocké dans Git ;
- doit suivre le mécanisme de gestion et rotation des secrets retenu par La Régionale.

## 5. LDAPS et confiance TLS

La connexion de production doit utiliser LDAPS.

Le certificat présenté par chaque Domain Controller doit être validé par la JVM SIXPAY. Le truststore doit donc contenir les autorités nécessaires à la validation de la chaîne de certificats de La Régionale, tout en préservant les autorités de confiance standard nécessaires à la JVM.

Le nom DNS utilisé dans l'URL LDAPS doit correspondre au certificat du serveur. Une adresse IP ne doit pas être utilisée pour contourner une erreur de résolution DNS ou de certificat.

## 6. Gestion de `objectGUID`

SIXPAY utilise `objectGUID` comme identité externe stable lorsque `subject-attribute=objectGUID`.

Microsoft Active Directory expose cet attribut sous forme binaire. La configuration JNDI doit donc explicitement demander sa restitution comme attribut binaire :

```java
if ("objectGUID".equalsIgnoreCase(properties.subjectAttribute())) {
    environment.put(
            "java.naming.ldap.attributes.binary",
            properties.subjectAttribute()
    );
}
```

SIXPAY convertit ensuite les octets AD vers la représentation UUID canonique utilisée pour la liaison d'identité.

Il ne faut pas remplacer cette clé stable par `sAMAccountName` : un login peut évoluer alors que la liaison d'identité doit rester stable.

## 7. Résolution vers le compte SIXPAY

Après authentification AD, SIXPAY construit une identité externe composée notamment de :

```text
identityType = LDAP
provider     = trust-domain configuré
subject      = objectGUID canonique
```

Cette identité est résolue vers un compte canonique SIXPAY.

Le compte SIXPAY reste propriétaire :

- du statut applicatif ;
- des rôles ;
- des permissions ;
- des autorisations.

Une authentification AD réussie ne suffit donc pas à donner accès à SIXPAY si aucune liaison canonique n'existe ou si le compte SIXPAY est désactivé.

## 8. Premier administrateur

Le mécanisme de bootstrap LDAP one-shot peut être utilisé uniquement pour initialiser un realm SIXPAY vide.

Séquence :

1. obtenir l'`objectGUID` du premier administrateur ;
2. choisir son UUID canonique SIXPAY ;
3. activer temporairement le bootstrap ;
4. démarrer SIXPAY et vérifier la création de l'identité et du rôle administrateur ;
5. désactiver immédiatement le bootstrap ;
6. redémarrer l'application.

Le bootstrap ne doit pas devenir le mécanisme normal de provisioning des administrateurs.

## 9. Haute disponibilité

Si plusieurs Domain Controllers sont fournis, configurer les URL approuvées et tester :

- la résolution DNS ;
- la confiance TLS de chaque DC ;
- le comportement en cas d'indisponibilité du premier DC ;
- les temps de connexion et de lecture ;
- la cohérence de réplication AD nécessaire au parcours d'authentification.

Les timeouts de production doivent être calibrés avec La Régionale. Les valeurs augmentées temporairement lors d'un diagnostic ne doivent pas être considérées comme une référence de production.

## 10. Ordre de diagnostic opérationnel

En cas d'échec LDAP, diagnostiquer dans cet ordre :

1. résolution DNS du Domain Controller ;
2. connectivité TCP/636 ;
3. négociation TLS et certificat ;
4. truststore JVM ;
5. bind du compte technique ;
6. recherche de l'utilisateur ;
7. état du compte AD ;
8. bind de l'utilisateur ;
9. récupération et conversion de `objectGUID` ;
10. correspondance `identityType/provider/subject` dans SIXPAY ;
11. statut du compte canonique SIXPAY ;
12. rôles et permissions SIXPAY ;
13. création/réutilisation de la session HTTP.

Ne pas modifier la base, les rôles ou le code avant d'avoir identifié la frontière exacte de l'échec.

## 11. Sécurité des logs

Ne jamais journaliser :

- les mots de passe ;
- les secrets du compte technique ;
- les credentials utilisateur ;
- les valeurs binaires brutes d'attributs AD ;
- des informations permettant inutilement l'énumération des comptes.

La réponse publique d'authentification doit continuer à masquer la distinction entre mauvais credentials, identité non liée et compte SIXPAY désactivé lorsque cette distinction exposerait des informations sensibles.

## 12. Checklist avant mise en production

- [ ] URLs LDAPS validées avec La Régionale.
- [ ] DNS des DC résolu depuis l'environnement SIXPAY.
- [ ] Firewall TCP/636 ouvert uniquement selon les flux approuvés.
- [ ] Certificats/CA importés dans le truststore.
- [ ] Compte technique créé avec privilèges minimaux.
- [ ] Secret fourni par le mécanisme sécurisé retenu.
- [ ] Base DN et Search Base validés.
- [ ] `sAMAccountName` ou attribut de login réel validé.
- [ ] `objectGUID` validé comme identité stable.
- [ ] Traitement binaire JNDI de `objectGUID` conservé.
- [ ] Première liaison administrateur réalisée puis bootstrap désactivé.
- [ ] Rôles et permissions contrôlés exclusivement par SIXPAY.
- [ ] Failover multi-DC testé si applicable.
- [ ] Timeouts calibrés.
- [ ] Aucun secret ou artefact de session présent dans Git.
