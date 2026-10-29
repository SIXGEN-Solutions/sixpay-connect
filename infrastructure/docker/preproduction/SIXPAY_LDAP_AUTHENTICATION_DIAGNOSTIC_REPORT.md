# SIXPAY CONNECT — Rapport de diagnostic de l'authentification LDAP

## 1. Objet

Ce document retrace le diagnostic ayant conduit à l'identification et à la correction de l'échec d'authentification LDAP observé dans l'environnement Docker de preproduction.

Le symptôme initial était un `HTTP 401` sur le login LDAP alors que l'utilisateur de test et sa liaison SIXPAY semblaient correctement configurés.

## 2. Symptôme initial

L'appel :

```http
POST /api/v1/auth/login/ldap
```

retournait :

```http
HTTP/1.1 401
Content-Type: application/problem+json
```

avec :

```json
{
  "detail": "Invalid credentials",
  "status": 401,
  "title": "Authentication failed"
}
```

Cette réponse était volontairement non discriminante. Le handler LDAP masque publiquement la différence entre :

- credentials LDAP rejetés ;
- identité externe non liée ;
- compte SIXPAY désactivé.

Il n'était donc pas possible de conclure à un mauvais mot de passe à partir du seul `401`.

## 3. Vérification du directory et des credentials

Un bind LDAP a été exécuté indépendamment depuis le réseau Docker avec le même utilisateur.

Le bind a réussi.

Cette vérification a confirmé :

- la validité des credentials utilisateur ;
- l'activation du compte AD ;
- l'accessibilité du directory ;
- le fonctionnement de LDAPS depuis le réseau concerné.

L'hypothèse « mauvais mot de passe » a été écartée.

## 4. Vérification réseau et DNS

Le backend et Samba AD appartenaient au même réseau Docker.

Depuis le backend, le nom du serveur AD était correctement résolu vers son adresse Docker.

La connectivité réseau/DNS n'expliquait donc pas le `401`.

## 5. Vérification TLS et truststore

Une difficulté de confiance TLS avait été rencontrée pendant la mise en place.

Le truststore preproduction a été construit en conservant le `cacerts` standard de la JVM et en y ajoutant la CA du Samba AD.

Le backend a ensuite démarré correctement avec ce truststore.

Cette étape a permis d'écarter les erreurs de chaîne de confiance/PKIX comme cause du `401` final.

## 6. Diagnostic direct de l'adapter LDAP

Un test temporaire a exécuté directement `ActiveDirectoryLdapAuthenticationAdapter` contre le vrai directory de preproduction.

Il a confirmé le fonctionnement du parcours :

```text
service-account bind
    -> recherche utilisateur
    -> lecture de l'état du compte
    -> user bind
    -> création du résultat LDAP
```

Ce test était uniquement diagnostique et ne doit pas rester dans la suite de tests finale.

## 7. Vérification de la liaison canonique SIXPAY

La base SIXPAY contenait une identité LDAP liée à un compte canonique actif.

Valeurs vérifiées :

```text
identity_type    = LDAP
provider         = sixpay-preproduction-ldap
provider_subject = 0d308194-5949-4b3c-8946-28523a3d7828
```

Le compte canonique associé était actif et disposait du rôle `ADMIN`.

Le `objectGUID` du compte AD avait également été vérifié indépendamment et correspondait à :

```text
0d308194-5949-4b3c-8946-28523a3d7828
```

La donnée persistée n'était donc pas arbitraire ou incohérente avec le directory.

## 8. Identification du type réel d'échec

Le handler LDAP a été temporairement instrumenté pour journaliser uniquement le type interne d'exception, sans credentials.

Le runtime a produit :

```text
LDAP_RUNTIME_DIAGNOSTIC authenticationFailureType=ExternalIdentityNotLinkedException
```

Cette observation a localisé l'échec après l'authentification LDAP.

Le parcours réel était donc :

```text
LDAP authentication        OK
ExternalIdentity creation  OK
canonical identity lookup  KO
```

La recherche s'est alors concentrée sur la clé utilisée par `LinkedExternalIdentityResolver`.

## 9. Observation de la clé d'identité runtime

Une instrumentation temporaire supplémentaire a observé la clé utilisée pour la résolution :

```text
identityType
issuer
subject
```

La sortie contenait des caractères binaires illisibles autour du provider/subject.

Ce comportement indiquait qu'une valeur LDAP binaire était transformée en texte brut et que des caractères de contrôle perturbaient même l'affichage du log.

Les logs diagnostiques ont ensuite été retirés.

## 10. Analyse de `objectGUID`

L'adapter contenait déjà une conversion spécifique :

```java
if ("objectGUID".equalsIgnoreCase(attributeName)
        && raw instanceof byte[] bytes) {
    return objectGuid(bytes);
}
```

Cependant, au runtime, la valeur ne passait pas par cette branche.

Sans configuration JNDI spécifique, `objectGUID` n'était pas restitué dans le format `byte[]` attendu. Le traitement générique finissait donc par interpréter la valeur comme une chaîne.

Conséquence :

```text
Valeur canonique attendue :
0d308194-5949-4b3c-8946-28523a3d7828

Valeur utilisée au runtime :
représentation issue des octets binaires bruts
```

La recherche exacte de l'identité persistée ne pouvait pas aboutir et levait `ExternalIdentityNotLinkedException`.

## 11. Cause racine

La cause racine était l'absence de déclaration JNDI demandant explicitement que `objectGUID` soit traité comme un attribut LDAP binaire.

Ce n'était pas :

- un mauvais mot de passe ;
- un compte AD désactivé ;
- une panne DNS ;
- une panne réseau ;
- un timeout LDAP ;
- un défaut de rôle SIXPAY ;
- un compte SIXPAY inactif ;
- une mauvaise valeur `objectGUID` volontairement persistée en base.

## 12. Correctif

Le correctif fonctionnel ajouté dans la création du contexte LDAP est :

```java
if ("objectGUID".equalsIgnoreCase(properties.subjectAttribute())) {
    environment.put(
            "java.naming.ldap.attributes.binary",
            properties.subjectAttribute()
    );
}
```

JNDI restitue ainsi `objectGUID` comme attribut binaire. Le code existant peut convertir correctement les octets Active Directory vers l'UUID canonique.

## 13. Validation runtime après correctif

Après reconstruction et redémarrage du backend, le même login a retourné :

```http
HTTP/1.1 200
```

avec une session créée et une réponse indiquant notamment :

```json
{
  "authenticated": true,
  "username": "sixpay.testuser",
  "roles": ["ADMIN"],
  "authenticationMethod": "LDAP",
  "passwordChangeRequired": false,
  "capabilities": {
    "localEnabled": false,
    "oidcEnabled": false,
    "ldapEnabled": true
  }
}
```

Le serveur a également émis un `JSESSIONID` HttpOnly et un token CSRF.

Cela confirme le parcours :

```text
credentials AD
    -> bind LDAP
    -> objectGUID binaire
    -> conversion UUID canonique
    -> ExternalIdentity LDAP
    -> résolution du compte SIXPAY
    -> rôles/permissions SIXPAY
    -> session authentifiée
```

## 14. Artefacts diagnostiques à retirer

Les éléments suivants ont été créés uniquement pour le diagnostic et ne doivent pas être embarqués dans la version finale :

```text
backend/security/src/test/java/com/sixpay/security/infrastructure/authentication/ldap/ActiveDirectoryLdapAuthenticationDiagnosticTest.java

backend/security/src/test/java/com/sixpay/security/infrastructure/authentication/ldap/LdapCanonicalAuthenticationDiagnosticTest.java

cookies.txt
```

Les logs temporaires `LDAP_RUNTIME_DIAGNOSTIC` et `LDAP_IDENTITY_DIAGNOSTIC` ont déjà été retirés du code au SHA analysé.

`cookies.txt` doit être ignoré par Git afin d'éviter de versionner de nouveaux identifiants de session.

## 15. Éléments fonctionnels à conserver

Doivent rester :

- le traitement JNDI binaire de `objectGUID` ;
- l'adapter Active Directory ;
- la résolution canonique des identités ;
- le handler public non discriminant ;
- les tests unitaires/architecture normaux ;
- `LdapSensitiveLoggingArchitectureTest` ;
- la configuration LDAP commune ;
- la simulation Samba AD de preproduction ;
- la construction du truststore preproduction ;
- le bootstrap one-shot du premier administrateur.

## 16. Enseignement opérationnel

Pour tout incident LDAP futur, il faut localiser l'échec frontière par frontière avant de modifier l'application :

```text
DNS
 -> TCP/636
 -> TLS/truststore
 -> service bind
 -> user search
 -> account state
 -> user bind
 -> stable identity attribute
 -> canonical identity lookup
 -> SIXPAY account status
 -> SIXPAY authorization
 -> HTTP session
```

Cette méthode a permis ici d'éviter de corriger à tort les credentials, les données PostgreSQL ou les rôles alors que le défaut réel se trouvait dans le traitement JNDI d'un attribut Active Directory binaire.
