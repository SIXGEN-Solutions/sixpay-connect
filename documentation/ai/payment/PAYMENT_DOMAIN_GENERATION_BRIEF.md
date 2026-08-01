# SIXPAY CONNECT — Payment Domain Generation Brief

> **Document ID:** `SIXPAY-PAYMENT-DOMAIN-GENERATION-BRIEF`  
> **Gate:** `IA-1_PAYMENT_DOMAIN_BRIEF`  
> **Branch:** `feat/payment-domain-generation-brief`  
> **Version:** `1.0.0`  
> **Status:** `IN_REVIEW`  
> **Normative:** `true`  
> **Global code generation:** `FORBIDDEN_PENDING_APPROVALS`

---

## 1. Métadonnées

| Attribut | Valeur |
| --- | --- |
| Domaine | `Payment` |
| Aggregate Root | `Payment` |
| Module cible | `backend/payment` |
| Java | `21` |
| Framework cible hors domaine | `Spring Boot 4` |
| Modèle d’architecture | Modular monolith aligné sur le Golden Module `partner` |
| Gate | `IA-1 — Payment Domain Brief` |
| Statut du modèle | Gelé et validé |
| Statut du brief | `IN_REVIEW` |
| Génération globale | Interdite |
| Branche normative | `feat/payment-domain-generation-brief` |

Le statut `IN_REVIEW` signifie que le modèle métier est complet, mais que les
approbations Product, Architecture et Engineering ne sont pas encore toutes
enregistrées et que des bloqueurs de génération externes subsistent.

## 2. Objectif et valeur métier

Payment protège le cycle de vie complet d’un ordre de paiement reçu de
TRESOR PAY jusqu’à sa finalité bancaire et TFJ. Il garantit :

- une seule intention Payment logique par référence externe ;
- aucune double opération financière lors d’un replay ;
- des décisions bancaires fondées sur des preuves immuables ;
- une représentation explicite des outcomes financiers incertains ;
- la conservation des preuves du posting original après reversal ;
- une traçabilité sûre, ordonnée et reproductible ;
- une séparation nette entre état financier et livraison Notification.

La valeur métier est de permettre une exécution de paiement déterministe,
auditable et réconciliable, même en présence de retries, callbacks dupliqués,
timeouts et concurrence.

## 3. Périmètre

### Inclus

- réception et identification du Payment ;
- autorisation TRESOR PAY ;
- vérification bancaire et contrôle de fonds ;
- résolution des comptes Treasury/CUT ;
- autorisation et suivi du posting ;
- résolution des outcomes inconnus ;
- finalité TFJ ;
- autorisation et suivi du reversal ;
- échecs, rejets, preuves, version et Domain Events ;
- règles conceptuelles d’idempotence, audit, Outbox, persistance et concurrence.

### Hors périmètre

- cycle de vie de l’abonnement TRESOR PAY ;
- gestion des credentials, JWT, PIN ou API keys ;
- transport HTTP, Kafka ou broker ;
- exécution technique des appels Amplitude ;
- livraison des notifications ;
- logique comptable TFJ d’Accounting ;
- entités JPA, migrations et schéma physique final ;
- controllers, adapters et configuration Spring ;
- frontend Payment dédié pour le MVP.

## 4. Sources d’autorité

Ordre de précédence :

1. branche `feat/payment-domain-generation-brief` ;
2. `documentation/architecture/` ;
3. `documentation/requirements/` ;
4. `documentation/contracts/` ;
5. `documentation/ai/payment/` ;
6. manifeste et actifs de génération ;
7. `ENGINEERING_CONTEXT.md`.

Sources Payment principales :

- `PAYMENT_DOMAIN_MODEL.md` ;
- `PAYMENT_AGGREGATE_ROOT.md` ;
- `PAYMENT_VALUE_OBJECT_CATALOGUE.md` ;
- `PAYMENT_EVIDENCE_SNAPSHOT_CATALOGUE.md` ;
- `PAYMENT_INVARIANT_CATALOGUE.yaml` ;
- `PAYMENT_COMMAND_CATALOGUE.yaml` ;
- `PAYMENT_STATE_MACHINE.yaml` ;
- `PAYMENT_EVENT_CATALOG.yaml` ;
- `PAYMENT_POLICY_DOMAIN_SERVICE_CATALOGUE.yaml` ;
- `PAYMENT_CONCEPTUAL_PERSISTENCE.yaml` ;
- `PAYMENT_TEST_TRACEABILITY.yaml` ;
- contrats Payment et documentation TRESOR PAY applicables.

Toute divergence est résolue selon la priorité du dépôt, sans inventer une
nouvelle règle.

## 5. Langage ubiquitaire

| Terme | Définition normative |
| --- | --- |
| Payment | Aggregate Root portant l’intention, le cycle de vie et les preuves minimales d’un paiement |
| Paiement reçu | Intention canonique authentifiée créée en état `RECEIVED` |
| Autorisation TRESOR PAY | Décision prouvant que la source et l’abonnement autorisent le paiement |
| Vérification bancaire | Preuve fraîche de cohérence institution, client et compte débiteur |
| Contrôle de fonds | Décision bancaire sur la disponibilité exacte des fonds requis |
| Posting bancaire | Instruction logique unique produisant le débit et le crédit CUT attendus |
| Débit confirmé | Effet débiteur prouvé sans confirmation complète du crédit CUT |
| Crédit CUT confirmé | Posting complet en attente de finalité TFJ |
| Finalité TFJ | Preuve `INTEGRATED` appariée de manière unique |
| Notification | Livraison externe d’un résultat, sans propriété sur l’état financier |
| Rejeu | Répétition équivalente d’une même intention ou preuve |
| Reprise | Nouvelle tentative technique autorisée sans nouvelle opération financière logique |
| Outcome inconnu | Résultat externe non concluant exigeant un lookup autoritatif |
| Reversal | Nouvelle instruction bancaire autorisée compensant un effet financier |
| Échec métier | Refus conclusif sans effet financier |
| Échec technique | Incident non métier classifié et sûr |
| Rejet définitif | État terminal `REJECTED`, sans reprise métier |

## 6. Frontière du domaine

Payment possède :

- son identité et ses références ;
- son montant et sa banque ;
- son état financier ;
- ses transitions ;
- sa version métier ;
- ses preuves acceptées ;
- ses instructions de posting et reversal ;
- son échec courant ;
- ses Domain Events ordonnés.

Payment ne possède pas :

- Subscription ;
- Customer/KYC ;
- configuration bancaire source de vérité ;
- transport et clients externes ;
- Notification delivery ;
- Accounting/TFJ processing ;
- repository, Outbox ou audit génériques ;
- sécurité technique des tokens.

La collaboration inter-domaines se fait exclusivement par ports applicatifs,
preuves typées et événements d’intégration versionnés.

## 7. Modèle métier

```text
Aggregate Roots       : 1
États                  : 17
États terminaux        : 4
Commandes              : 16
Opérations Aggregate   : 17
Transitions légales    : 38
Invariants             : 76
Domain Events          : 33
Policies               : 14
Policy Profiles        : 12
Domain Services        : 4
```

États terminaux :

```text
REJECTED
FAILED
TREASURY_INTEGRATED
REVERSED
```

Garanties structurantes :

- `Payment.receive` crée `RECEIVED` ;
- `Payment.reconstitute` ne crée ni version ni événement ;
- une mutation réussie incrémente `businessVersion` une seule fois ;
- les événements partagent cette version et utilisent `eventSequence = 1..N` ;
- un replay identique est un no-op ;
- une preuve contradictoire produit un conflit sans mutation ;
- un outcome inconnu interdit toute resoumission financière aveugle ;
- `NOTIFIED` n’est pas un `PaymentStatus`.

## 8. Cas d’usage et critères d’acceptation

Cas d’usage métier :

1. recevoir une intention Payment ;
2. démarrer et enregistrer l’autorisation ;
3. enregistrer la vérification bancaire ;
4. enregistrer le contrôle de fonds ;
5. résoudre les comptes Treasury ;
6. autoriser le posting ;
7. enregistrer ou résoudre le résultat du posting ;
8. enregistrer la confirmation TFJ appariée ;
9. autoriser le reversal ;
10. enregistrer ou résoudre le résultat du reversal ;
11. rejeter ou différer sans mutation partielle ;
12. échouer de façon terminale uniquement sans effet financier.

Critères communs :

- toute commande a un résultat déterministe dans chaque état ;
- transition valide : mutation atomique, version +1 et événements ordonnés ;
- transition invalide : exception stable, aucun changement ;
- replay identique : résultat original ou no-op ;
- conflit : erreur stable et aucune opération financière supplémentaire ;
- état terminal : aucune transition sortante.

## 9. Sécurité

Données interdites dans Payment, erreurs, audit et événements :

```text
Bearer Token
Subscription Key
PIN
API key, password, secret ou private key
JWT brut, claims, signature ou JWKS
RIB ou numéro de compte complet en clair
KYC et identité client brute
solde bancaire
payload brut TRESOR PAY, Amplitude ou TFJ
message fournisseur libre
stack trace, SQL, endpoint ou topologie interne
```

Les références de compte utilisent trois représentations séparées :

- référence technique protégée ou tokenisée ;
- valeur masquée de consultation ;
- empreinte de binding/recherche autorisée.

Les paramètres JWT — TTL, skew et algorithmes — restent une configuration
externe approuvée ; ils ne changent pas le modèle Payment.

## 10. Contrats API applicables

Contrats applicables :

- contrat externe d’initiation Payment TRESOR PAY ;
- endpoint `InitiateDebit` de l’API d’Ordre de Virement ;
- contrats de vérification et posting Amplitude après approbation ;
- contrats internes :
  - Payment Query API ;
  - Observed Customer Query API ;
  - Payment Audit Query API.

Règles :

- aucun DTO de contrat ne traverse directement le domaine ;
- mapping explicite vers commandes, Value Objects et preuves ;
- les contrats en `approvalStatus=PENDING_APPROVAL` ne permettent pas la
  génération des adapters correspondants ;
- toute modification OpenAPI exige une approbation explicite.

## 11. Événements et intégrations

Les 33 Domain Events sont les faits canoniques de Payment.

```text
com.sixpay.payment.domain.event
→ faits internes immuables

com.sixpay.payment.events
→ futurs contrats d’intégration versionnés
```

Règles :

- aucun appel Kafka depuis l’Aggregate Root ;
- aucune publication directe d’un Domain Event ;
- mapping par allowlist explicite ;
- déduplication par `eventId` ;
- ordre par `aggregateVersion,eventSequence` ;
- partition logique par `paymentId` ;
- publication Outbox `AT_LEAST_ONCE` ;
- un échec Kafka après commit ne rollbacke jamais Payment ;
- une republication conserve l’identité et le payload de l’événement.

## 12. Persistance et migrations

Le Lot IA-1 définit des capacités, pas un schéma physique final.

Capacités obligatoires :

- persistance et reconstitution de `PaymentState` ;
- unicité de `source + externalPaymentReference` ;
- unicité de `publicPaymentReference` ;
- optimistic locking par `businessVersion` ;
- échec courant sûr et historique optionnel ;
- audit append-only ;
- idempotency store ;
- Outbox transactionnelle ;
- replay protection d’autorisation selon profil ;
- corrélation durable des outcomes posting/reversal inconnus.

Frontière transactionnelle :

```text
Payment state/version
+ audit records
+ Outbox rows
+ idempotency result when applicable
= one database transaction
```

Les futures entités JPA et migrations doivent être conçues dans un lot
explicitement autorisé et alignées sur les conventions du Golden Partner.

## 13. Frontend

`NOT_APPLICABLE_FOR_IA1_MVP`

Justification :

- IA-1 définit le noyau Payment et ses frontières backend ;
- aucun workflow utilisateur Payment direct n’est requis ;
- consultations opérationnelles futures consommeront les APIs internes ;
- toute UI d’administration ou de réconciliation aura son propre contrat,
  ses rôles et son lot d’approbation.

## 14. Exigences non fonctionnelles

- idempotence distribuée ;
- aucune double opération financière ;
- concurrence déterministe ;
- optimistic locking ;
- audit append-only ;
- confidentialité by design ;
- événements ordonnés et reproductibles ;
- Outbox transactionnelle ;
- résilience aux callbacks dupliqués ;
- lookup autoritatif après timeout ;
- aucun I/O dans le domaine ;
- Java 21 ;
- domaine sans Spring/JPA/Kafka/HTTP ;
- observabilité future sans donnée sensible ;
- rétention conforme définie avec Compliance, Security, Accounting et
  Operations.

## 15. Plan de tests et traçabilité

Couverture normative :

```text
76 invariants avec scénario nommé
38 transitions avec scénario nommé
33 événements catalogués
17 opérations Aggregate Root
15 scénarios verticaux futurs
```

Chaîne obligatoire :

```text
source requirement
→ invariant
→ transition
→ method
→ event
→ test
→ future file
```

Les tests futurs application/persistence sont marqués explicitement
`FUTURE_VERTICAL_TEST` et ne sont pas présentés comme déjà exécutés.

Tests permanents :

- Value Objects et références invalides ;
- transitions légales et interdites ;
- atomicité en cas d’échec ;
- terminalité ;
- posting/reversal incertain ;
- confidentialité des événements ;
- architecture domain-only ;
- cohérence générique du manifeste ;
- complétude de la matrice de traçabilité.

## 16. Périmètre de fichiers

Autorisé dans IA-1 :

```text
backend/payment/src/main/java/com/sixpay/payment/PaymentModule.java
backend/payment/src/main/java/com/sixpay/payment/domain/**
backend/payment/src/test/java/com/sixpay/payment/domain/**
backend/payment/src/test/java/com/sixpay/payment/architecture/**
backend/payment/*.md
documentation/ai/payment/**
```

Interdit sans activation explicite :

```text
backend/payment/src/main/java/com/sixpay/payment/api/**
backend/payment/src/main/java/com/sixpay/payment/application/**
backend/payment/src/main/java/com/sixpay/payment/infrastructure/**
backend/payment/src/main/java/com/sixpay/payment/configuration/**
backend/payment/src/main/java/com/sixpay/payment/events/**
backend/payment/src/main/resources/db/**
backend/payment/src/main/resources/openapi/**
```

## 17. Gates et commandes

Gate courant :

```text
IA-1_PAYMENT_DOMAIN_BRIEF
Status: IN_REVIEW
```

Validation locale :

```bash
cd backend
mvn --batch-mode --no-transfer-progress -pl payment -am test
```

Validation documentaire :

```bash
python - <<'PY'
from pathlib import Path
import re
import yaml

brief = Path(
    "documentation/ai/payment/"
    "PAYMENT_DOMAIN_GENERATION_BRIEF.md"
).read_text()

headings = re.findall(r"^## ([0-9]+)\.", brief, re.MULTILINE)
assert headings == [str(i) for i in range(1, 21)]

for marker in (
    "T" + "ODO",
    "T" + "BD",
    "{" + "{",
    "}" + "}",
    "<" + "PLACEHOLDER" + ">"
):
    assert marker not in brief

manifest = yaml.safe_load(Path(
    "documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml"
).read_text())
assert manifest["context"]["lot"] == "9_BRIEF_CONSOLIDATION"
print("IA-1 brief validation: PASS")
PY
```

La génération globale reste interdite tant que les approbations et contrats
bloquants ne sont pas enregistrés.

## 18. Décisions ouvertes

Aucune décision ouverte ne bloque la **forme du modèle Payment**.

Décisions externes/configuration encore requises :

1. approbation complète du Payment Contract Pack ;
2. approbation du contrat de vérification Amplitude ;
3. réconciliation finale du manifeste historique des contrats ;
4. validation bancaire de la référence principale et des références de jambes ;
5. approbation Operations/Bank des états sources du reversal ;
6. approbation Security des paramètres JWT ;
7. approbation Accounting/Operations des fenêtres cutoff, réconciliation et SLA.

Ces éléments bloquent la génération ou l’activation de couches externes, mais
ne réouvrent pas le modèle IA-1 sans change control approuvé.

## 19. Definition of Ready

Le modèle IA-1 satisfait les critères suivants :

- tous les types du modèle sont définis ;
- les 17 états et 38 transitions sont explicites ;
- les 76 invariants ont un mécanisme de contrôle ;
- les données sensibles sont classifiées ;
- les frontières inter-domaines sont explicites ;
- les 33 Domain Events sont définis ;
- idempotence et outcomes inconnus sont complets ;
- persistance conceptuelle et concurrence sont décrites ;
- le plan de tests est complet ;
- le brief contient exactement 20 sections ;
- aucun placeholder non résolu n’est présent.

Le passage à `APPROVED` exige en plus :

- zéro bloqueur de génération ;
- Product approval enregistrée ;
- Architecture approval enregistrée ;
- Engineering approval enregistrée.

## 20. Approbations

| Autorité | Statut | Preuve enregistrée |
| --- | --- | --- |
| Product | `NOT_RECORDED` | Aucune approbation Product présente dans le dépôt |
| Architecture | `NOT_RECORDED` | Aucune approbation Architecture finale présente dans le dépôt |
| Engineering | `NOT_RECORDED` | Aucune approbation Engineering finale présente dans le dépôt |

Décision de statut :

```text
DRAFT       → dépassé : le contenu est consolidé
IN_REVIEW   → statut actuel
APPROVED    → interdit tant que les trois approbations et les bloqueurs
              externes ne sont pas résolus
```

## Verdict du Gate IA-1

```text
MODEL COMPLETENESS : PASS
DOCUMENT CONSOLIDATION: PASS
PLACEHOLDERS       : NONE
REQUIRED APPROVALS : NOT_RECORDED
GENERATION BLOCKERS: PRESENT
GATE STATUS        : IN_REVIEW
GLOBAL GENERATION  : FORBIDDEN
```
