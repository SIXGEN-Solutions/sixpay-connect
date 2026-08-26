# SIXPAY CONNECT — IA-0R / 0.3 Décisions bloquantes

Ce registre ferme les ambiguïtés d’architecture nécessaires au préflight
Payment. Les validations organisationnelles et l’approbation des futurs
contrats ne sont pas déclarées acquises : elles restent des conditions de Gate.

Le fichier normatif exploitable par l’IA est
[`IA_0R_BLOCKING_DECISIONS.yaml`](./IA_0R_BLOCKING_DECISIONS.yaml).

## Décisions fermées

| ID | Sujet | Décision MVP |
| --- | --- | --- |
| IA0R-D01 | Identité ObservedCustomer | UUID stable ; clé bancaire institution + référence client ; NIU non identifiant |
| IA0R-D02 | Rapprochement interbancaire | Aucun merge automatique entre institutions |
| IA0R-D03 | Accès aux données | Comptes masqués, RBAC strict, lectures et exports audités |
| IA0R-D04 | Rétention | Paiement/audit 10 ans ; clés d’idempotence 13 mois ; payload outbox 90 jours |
| IA0R-D05 | Authentification TRESOR PAY | Bearer token + Subscription Key obligatoires pour le MVP |
| IA0R-D06 | Résilience | Valeurs configurables avec defaults techniques non contractuels |
| IA0R-D07 | Écriture partielle | Aucun rejeu aveugle ; état incertain, rapprochement et extourne explicite |
| IA0R-D08 | Confirmation TFJ | Confirmation Amplitude asynchrone, requête de rapprochement en fallback |

## Précisions majeures

### ObservedCustomer

Le NIU est un attribut de rapprochement, jamais la clé technique. Une observation
provisoire peut être créée dès la première demande de paiement. Elle n’est
consolidée qu’après une vérification Amplitude fraîche. Deux profils issus de
banques différentes ne sont jamais fusionnés automatiquement.

### Écritures comptables

L’opération cible du Core Banking doit être atomique : débit client et crédit du
CUT. Si le résultat est inconnu ou partiel, SIXPAY ne renvoie pas aveuglément
l’ordre. Il interroge l’opération avec sa clé d’idempotence ou sa référence
bancaire, ouvre un cas de rapprochement et alerte les opérations. Une extourne
requiert une instruction bancaire ou un runbook approuvé.

### Confirmation TFJ

La notification de débit/crédit immédiate ne constitue pas la finalité TFJ.
SIXPAY attend une confirmation Amplitude corrélée. Une confirmation sans
paiement correspondant est mise en quarantaine. En l’absence de confirmation,
le paiement reste `PENDING_END_OF_DAY_CONFIRMATION` et aucune notification
finale de succès n’est envoyée.

### Résilience et SLA

Les valeurs définitives restent fournies par la banque. Des defaults techniques
configurables permettent cependant de construire et tester le système :
connexion 2 s, réponse 10 s, opération 15 s, trois tentatives maximum et backoff
exponentiel avec jitter. Ces valeurs ne constituent pas un engagement SLA.

## Éléments encore soumis à Gate

- production et approbation du Contract Pack Payment ;
- validation des valeurs SLA par la banque ;
- signatures Product, Architecture, Security, Integration et Operations.

Ces éléments sont des validations externes, pas des ambiguïtés d’architecture.
La génération de code reste interdite jusqu’au Gate prévu.
