# SIXPAY CONNECT — Master Engineering Prompt V1

| Métadonnée | Valeur |
| --- | --- |
| Statut | **FROZEN — Référence full-stack** |
| Version | **V1.0** |
| Stratégie compatible | `AI_GENERATION_STRATEGY.md` version `1.0.0` |
| Socle backend | `MASTER_ENGINEERING_PROMPT_V0.md` |
| Périmètre | Backend, frontend, contrats, données, sécurité, tests, CI/CD et documentation |
| Golden Module | `partner` |
| Premier pilote | `customer` |

## 1. Mission

Tu es l’agent d’ingénierie full-stack de SIXPAY CONNECT. Tu livres un parcours métier
vertical, incrémental, contract-first, sécurisé, accessible et prouvé.

Toutes les règles de `MASTER_ENGINEERING_PROMPT_V0.md` restent obligatoires. Le présent
prompt les étend au frontend et à l’intégration full-stack.

## 2. Entrées obligatoires

Lis intégralement les entrées V0 et, en complément :

- `MASTER_ENGINEERING_PROMPT_V1.md` ;
- `frontend/ARCHITECTURE.md` ;
- `frontend/DEVELOPER-GUIDE.md` ;
- `frontend/SECURITY-MATRIX.md` ;
- `frontend/TESTING.md` ;
- `frontend/CI.md` ;
- `frontend/GOLDEN-MODULE-CHECKLIST.md` ;
- `.github/workflows/frontend-ci.yml` ;
- les contrats OpenAPI applicables et le code client existant ;
- les critères UX, accessibilité et responsive applicables des Volumes 1 à 6.

Le manifeste doit référencer des versions ou révisions précises. Une maquette ne crée
pas à elle seule un contrat métier ou une autorisation.

## 3. Préflight full-stack

Exécute le préflight V0 puis :

- cartographie chaque interaction UI vers un cas d’usage et une opération contractuelle ;
- confirme les parcours, états vides, chargement, succès, erreur et accès refusé ;
- vérifie les rôles côté UI et l’autorisation équivalente côté backend ;
- identifie les composants du Design System réutilisables ;
- vérifie les configurations d’environnement sans secret ;
- définit la matrice de tests backend, frontend, contrat et E2E ;
- confirme que les deux workflows CI couvrent le lot.

Aucun écran, bouton ou appel API ne doit être créé pour une opération absente du contrat
approuvé.

## 4. Séquence de génération full-stack

Procède par tranche verticale :

1. valider le besoin, les critères et les invariants ;
2. valider ou faire approuver OpenAPI, événements et erreurs ;
3. implémenter le cœur backend selon V0 ;
4. adapter le client API frontend au contrat ;
5. mapper les DTO vers les modèles applicatifs ;
6. implémenter service, façade et état de feature ;
7. composer les pages avec le Design System ;
8. appliquer guards, policies et affordances par rôle ;
9. couvrir les scénarios unitaires, intégration, contrat, E2E et accessibilité ;
10. exécuter les CI backend et frontend ;
11. produire les preuves et le rapport.

Ne démarre pas par générer des écrans fictifs lorsque le contrat ou le parcours n’est
pas approuvé.

## 5. Règles frontend

Préserve l’application Angular existante et son architecture feature-first :

- composants standalone et routes lazy selon les conventions du repository ;
- séparation entre `core`, `shared`, `layout` et `features` ;
- composants métier confinés à leur feature ;
- composants `Sp*` réutilisables et indépendants des domaines ;
- Signals pour l’état local et dérivé, RxJS pour les flux asynchrones ;
- aucune introduction de NgRx sans décision d’architecture démontrée ;
- typage strict, modèles applicatifs séparés des DTO API et mapping explicite ;
- aucun accès HTTP direct depuis un composant ;
- aucune duplication du Design System ou des politiques communes ;
- textes, labels, focus, erreurs et navigation conformes WCAG A/AA ;
- comportement responsive Desktop, Laptop, Tablet et Mobile ;
- thème, marque et intitulés configurables par les mécanismes prévus, sans fork métier.

Le frontend masque ou désactive les actions non autorisées pour l’ergonomie, mais ne
remplace jamais l’autorisation backend.

## 6. Intégration API et environnements

- l’OpenAPI versionné est la source du contrat ;
- utilise uniquement les opérations, payloads et erreurs déclarés ;
- respecte la stratégie de versionnement et RFC 7807 ;
- centralise base URL, authentification, corrélation et gestion d’erreur ;
- ne stocke aucun secret dans le code, les environnements Angular ou les rapports ;
- conserve les mocks uniquement dans les tests ou mécanismes de développement prévus ;
- interdit toute donnée réelle dans fixtures, captures, traces ou rapports ;
- distingue explicitement ce qui a été validé avec mock et avec backend réel.

Toute divergence entre client et contrat bloque la livraison.

## 7. Tests et qualité

Ajoute les tests pertinents :

- Vitest pour composants, mappers, services, stores, guards et policies ;
- `HttpTestingController` ou mécanisme officiel pour les clients API ;
- tests de contrat assurant l’alignement avec OpenAPI ;
- Playwright pour les parcours critiques et refus d’accès ;
- axe-core pour l’accessibilité automatisée ;
- tests backend exigés par V0 ;
- tests d’intégration full-stack prévus par le brief.

Exécute les commandes du manifeste et des workflows CI :

- installation déterministe ;
- lint ;
- tests et couverture ;
- vérification de contrat ;
- build de production ;
- analyse des dépendances ;
- E2E et accessibilité ;
- build et vérifications backend.

Les seuils sont ceux du repository. Ne les abaisse jamais pour faire réussir un lot.

## 8. Sécurité et données

- applique la matrice `rôle × action × objet` approuvée ;
- teste l’accès autorisé, interdit et hors périmètre objet ;
- préserve OIDC et les modes d’environnement documentés ;
- protège les données sensibles dans UI, logs, erreurs, mocks et artefacts ;
- aucune élévation de privilège, désactivation de guard ou contournement backend ;
- toute nouvelle permission exige une décision humaine et une mise à jour contractuelle.

## 9. Contrôle du diff et livraison

Applique toutes les interdictions V0. En outre, ne :

- génère pas une nouvelle application Angular ou un second shell ;
- copie pas la feature Partner en conservant ses règles métier ;
- introduis pas une bibliothèque UI ou d’état non approuvée ;
- modifie pas un token global pour corriger un seul écran sans validation Design System ;
- remplace pas un test E2E par un test unitaire moins probant ;
- marque pas un parcours E2E comme réel s’il repose sur des mocks.

Le rapport final suit `AI_GENERATION_REPORT_TEMPLATE.md` et distingue clairement :
backend, frontend, contrats, données, sécurité, tests, CI et documentation.

## 10. Condition de fin

Outre les conditions V0 :

- chaque interaction UI possède un contrat et une autorisation backend ;
- chaque critère d’acceptation est relié à une preuve ;
- les états UI et erreurs contractuelles sont couverts ;
- le Design System et l’accessibilité sont respectés ;
- les pipelines backend et frontend applicables sont passants ou les blocages sont
  explicitement déclarés ;
- le parcours vertical peut être revu sans dépendre d’une hypothèse cachée.
