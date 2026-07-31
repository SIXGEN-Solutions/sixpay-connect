# Gate IA-0.5P — Revue du contrat Amplitude Customer Verification

## 1. Décision

Le contrat
`documentation/contracts/amplitude/amplitude-customer-verification-api-v1.yaml`
est **confirmé pour une utilisation par le parcours Payment**, avec le statut
`REFERENCE_MVP`, l’approbation `PENDING_APPROVAL` et la politique
`REFERENCE_ONLY`.

Cette confirmation ne rend pas le contrat actif pour la génération de code.
`codeGenerationAllowed` reste à `false` jusqu’à l’approbation complète du
Payment Contract Pack.

## 2. Périmètre confirmé

Après persistance durable de la demande de paiement, SIXPAY peut utiliser
l’opération composite `verifyAmplitudeCustomer` afin d’obtenir des faits
bancaires frais sur :

- l’existence du client dans Amplitude ;
- la correspondance de l’institution financière ;
- la concordance du NIU ;
- la concordance des attributs d’identité fournis ;
- l’existence du compte débiteur ;
- l’appartenance du compte au client ;
- le statut actif du compte ;
- l’absence de blocage de débit ;
- l’absence d’opposition ;
- la présence et la vérification des champs KYC requis.

Amplitude reste le système de référence du client et du compte. Le module
`integration` porte le contrat de transport, le module `customer` normalise les
faits bancaires et le module `payment` décide si ces faits permettent la
progression de l’agrégat.

## 3. Limites impératives

Ce contrat ne couvre pas :

- l’existence, le statut ou la validation d’un abonnement TRESOR PAY ;
- la validation du token d’autorisation TRESOR PAY ;
- la disponibilité des fonds, les plafonds ou les limites de paiement ;
- le débit du client ou le crédit du CUT ;
- la recherche d’un résultat de posting ;
- l’extourne ;
- la confirmation TFJ.

Le contrôle des fonds reste à définir dans
`amplitude-payment-verification-api-v1.yaml`. Les écritures et leur récupération
restent à définir dans `amplitude-payment-posting-api-v1.yaml`.

## 4. Corrections apportées pendant la revue

La revue 1.3 a :

- rendu explicite l’utilisation du contrat après persistance du Payment ;
- ajouté les contrôles `FINANCIAL_INSTITUTION_MATCHES` et `ACCOUNT_EXISTS` ;
- rendu cohérent le résultat composite avec les vérifications négatives :
  l’identité et le compte peuvent être absents lorsque le fait bancaire
  correspondant n’existe pas ;
- ajouté un résultat global `VERIFIED`, `REJECTED` ou `INDETERMINATE` ;
- défini les absences client/compte comme des résultats métier négatifs en
  HTTP 200, distincts des erreurs techniques ;
- précisé que les attributs de téléphone et d’adresse électronique sont
  facultatifs dans les assertions entrantes, tout en restant demandables comme
  faits KYC ;
- remplacé le fallback lié à l’activation d’abonnement par un fail-closed
  propre à la vérification Payment.

## 5. Règles d’utilisation par Payment

1. La demande Payment doit être durablement persistée avant l’appel.
2. `X-Correlation-ID` est propagé sans modification et réutilisé lors d’un
   retry technique.
3. Les RIB/IBAN entrants sont protégés, non journalisés et jamais renvoyés en
   clair.
4. Aucune donnée Amplitude obsolète ne peut autoriser un paiement.
5. Seul `outcome: VERIFIED` autorise la progression vers le contrôle des fonds.
6. `REJECTED` provoque un rejet métier sans écriture financière.
7. `INDETERMINATE`, un timeout épuisé ou une indisponibilité Amplitude
   provoquent un arrêt sûr ; ils ne valent jamais approbation.
8. Le payload Amplitude brut n’entre ni dans l’agrégat Payment ni dans les
   événements métier. Seul un résultat canonique minimisé est conservé.

## 6. Verdict de l’étape 1.3

```text
AMPLITUDE CUSTOMER VERIFICATION CONTRACT REUSED BY PAYMENT
SCOPE: CUSTOMER AND ACCOUNT VERIFICATION ONLY
EXTERNAL APPROVAL PENDING
CODE GENERATION FORBIDDEN
```
