# SIXPAY CONNECT — Registre des contrats

Ce dossier contient les contrats d’intégration versionnés de SIXPAY CONNECT.
Leur présence dans le dépôt ne signifie pas qu’ils appartiennent tous au MVP.

La classification normative et exploitable par l’IA se trouve dans
[`CONTRACT_REGISTRY.yaml`](./CONTRACT_REGISTRY.yaml). Chaque fichier OpenAPI
répète sa classification dans l’extension `info.x-sixpay-contract`.

## Classement au Gate IA-0R

| Contrat | Classement | Usage MVP | Génération |
| --- | --- | --- | --- |
| `amplitude-customer-verification-api-v1.yaml` | `REFERENCE_MVP` | Vérification bancaire en support du paiement | Référence uniquement |
| `tresorpay-authorization-request-api-v1.yaml` | `DEFERRED_FUTURE` | Aucun | Exclue |
| `tresorpay-authorization-decision-webhook-v1.yaml` | `DEFERRED_FUTURE` | Aucun | Exclue |

Le contrat Amplitude existant ne couvre ni le contrôle du solde disponible, ni
le débit du client, ni le crédit du CUT, ni la confirmation après TFJ. Ces
capacités doivent être définies dans le Contract Pack Payment.

Les deux contrats TRESOR PAY d’autorisation sont conservés à leurs chemins
actuels pour la traçabilité et une évolution future du parcours d’abonnement.
Pour le MVP, TRESOR PAY reste maître de l’abonnement et SIXPAY ne gère aucun
cycle de vie local d’abonnement.

## Règle de gouvernance

Toute évolution de classement doit mettre à jour dans le même changement :

1. `CONTRACT_REGISTRY.yaml`;
2. l’extension `info.x-sixpay-contract` du contrat;
3. `documentation/ai/customer/AI_CONTEXT_MANIFEST.yaml`;
4. le document de Gate concerné.
