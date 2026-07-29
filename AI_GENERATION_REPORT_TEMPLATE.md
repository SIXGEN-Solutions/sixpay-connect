# SIXPAY CONNECT — Rapport de génération IA

> Décrire uniquement des faits, décisions et preuves. Ne jamais inclure de raisonnement
> interne, secret, jeton, donnée personnelle ou donnée de production.

## 1. Identification

| Champ | Valeur |
| --- | --- |
| Campagne | `<generation-id>` |
| Domaine/capacité | `<domain>/<capability>` |
| Statut | `COMPLETED / PARTIAL / BLOCKED / REJECTED` |
| Stratégie | `1.0.0` |
| Master Prompt | `V0 / V1 / V1.1` |
| Brief | `<path>@<version>` |
| Manifeste | `<path>@<revision>` |
| Branche | `<branch>` |
| Commit de base | `<full-sha>` |
| Commit livré | `<full-sha-or-not-created>` |
| Agent/outillage | `<tool-and-version>` |
| Date | `<ISO-8601>` |

## 2. Résultat

- Objectif : `<objective>`
- Résultat observable : `<outcome>`
- Périmètre livré : `<scope>`
- Hors périmètre confirmé : `<non-goals>`

## 3. Sources d’autorité lues

| Source | Version/révision | Sections | Résultat |
| --- | --- | --- | --- |
| `<path-or-reference>` | `<version>` | `<sections>` | `READ / MISSING / CONFLICT` |

## 4. Préflight

- État initial du worktree : `<clean-or-described>`
- Chemins autorisés : `<paths>`
- Chemins interdits : `<paths>`
- Autonomie autorisée : `<level>`
- Ambiguïtés initiales : `<none-or-list>`
- Décision de démarrage : `GO / NO-GO`

## 5. Changements

| Fichier | Action | Justification | Exigence/contrat |
| --- | --- | --- | --- |
| `<path>` | `CREATED/MODIFIED/DELETED` | `<reason>` | `<AC/INV/operation>` |

### Résumé par couche

- Domaine : `<changes-or-none>`
- Application : `<changes-or-none>`
- API/événements : `<changes-or-none>`
- Infrastructure/données : `<changes-or-none>`
- Frontend : `<changes-or-none>`
- Sécurité/observabilité : `<changes-or-none>`
- CI/documentation : `<changes-or-none>`

## 6. Contrats et migrations

| Élément | Changement | Compatibilité | Approbation |
| --- | --- | --- | --- |
| `<OpenAPI/event/migration>` | `<none-or-description>` | `<compatible/breaking>` | `<reference>` |

## 7. Décisions et interventions humaines

| ID | Sujet | Décision | Autorité | Référence |
| --- | --- | --- | --- | --- |
| `<id>` | `<subject>` | `<outcome>` | `<approver>` | `<reference>` |

Hypothèses non approuvées introduites : **aucune** / `<explain-and-mark-blocked>`.

## 8. Traçabilité

| Exigence | Code | Test | Preuve | Statut |
| --- | --- | --- | --- | --- |
| `<AC/INV/NFR>` | `<path>` | `<test>` | `<artifact-or-log>` | `PASS/FAIL/NOT_RUN` |

## 9. Validations exécutées

| Commande exacte | Environnement | Durée | Résultat | Preuve |
| --- | --- | --- | --- | --- |
| `<command>` | `<runtime-versions>` | `<duration>` | `PASS/FAIL` | `<artifact-or-log>` |

### Contrôles non exécutés

| Contrôle | Motif | Risque | Action requise |
| --- | --- | --- | --- |
| `<check>` | `<reason>` | `<impact>` | `<owner/action>` |

Un contrôle non exécuté ne doit jamais être déclaré réussi.

## 10. CI et qualité

| Check | Statut | Exécution/référence | Artefacts |
| --- | --- | --- | --- |
| Backend CI | `PASS/FAIL/NOT_RUN` | `<run>` | `<reports>` |
| Frontend CI | `PASS/FAIL/NOT_RUN` | `<run>` | `<reports>` |
| Couverture | `<value/status>` | `<run>` | `<report>` |
| Dépendances/sécurité | `<status>` | `<run>` | `<report>` |

Seuil modifié : **non** / `<approved-reference>`.
Test ou règle désactivé : **non** / `<approved-reference>`.

## 11. Sécurité et données

- Matrice des rôles vérifiée : `<yes/no/not-applicable>`
- Autorisation objet testée : `<yes/no/not-applicable>`
- Secret ou donnée réelle utilisé : **non**
- Journaux/artefacts vérifiés : `<yes/no>`
- Vulnérabilités ou risques : `<none-or-list>`

## 12. Écarts, incidents et dette

| ID | Écart/incident | Impact | Contournement | Propriétaire | Échéance |
| --- | --- | --- | --- | --- | --- |
| `<id>` | `<description>` | `<impact>` | `<none-or-action>` | `<owner>` | `<date>` |

## 13. Revue du diff

- Changements hors périmètre : **aucun** / `<list>`
- Fichiers locaux préexistants préservés : `<yes/no>`
- Dépendances ajoutées : **aucune** / `<list-and-approval>`
- Compatibilité publique : `<preserved-or-approved-change>`
- Procédure de rollback : `<procedure>`

## 14. Conclusion et Gate suivant

- Conclusion : `<delivery-assessment>`
- Gate atteint : `<IA-n>`
- Blocages : `<none-or-list>`
- Risques résiduels : `<none-or-list>`
- Action humaine requise : `<review/approval/decision>`
- Prochaine étape : `<next-step>`

## 15. Attestation

- [ ] Le rapport correspond au diff livré.
- [ ] Toutes les commandes indiquées ont réellement été exécutées.
- [ ] Les échecs et contrôles non exécutés sont visibles.
- [ ] Aucun secret, raisonnement interne ou donnée réelle n’est exposé.
- [ ] Les approbations structurantes sont référencées.
- [ ] Le lot est prêt pour une review humaine, sans auto-approbation ni auto-merge.
