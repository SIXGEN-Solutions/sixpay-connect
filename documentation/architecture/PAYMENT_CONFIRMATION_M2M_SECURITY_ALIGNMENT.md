# Payment Confirmation — alignement M2M des trois endpoints

Statut : correctif local à valider. Révision de base : `095302f140f1678372639fa471aeda55bf8ab850`.

## Invariants

- `GET /v1/payments/{paymentReference}/confirmation-challenge` requiert `payment.confirmation.read`.
- `POST .../verify` requiert `payment.confirmation.verify`.
- `POST .../resend` requiert `payment.confirmation.resend`.
- Les quatre surfaces publiques (`initiate` et les trois confirmations) utilisent le même validateur JWT M2M : issuer, audience, signature, durée et `azp`.
- Les clients OIDC humains ne sont pas autorisés par repli sur ces routes.
- La résolution `azp -> Partner` est réutilisée et comparée à l'identité propriétaire enregistrée dans le Payment avant tout appel bancaire.
- Absence de principal, Partner non enregistré/inactif, paiement absent ou propriétaire différent : refus fermé avant tout effet Core Banking.
- Le mTLS est une obligation de la frontière de déploiement; son application au gateway doit être vérifiée séparément.
- Aucun endpoint, payload, rôle, table ou permission nouveau; les scopes sont déjà approuvés dans le contrat.

## Validation attendue

Tests de filtre : quatre routes exactes, scopes distincts, JWT invalide/absent, audience/issuer via decoder, `azp` manquant et route étrangère non interceptée.
Tests de garde : identité absente, Payment absent, Partner actif correspondant, Partner différent, Partner désactivé. Tests HTTP Spring Method Security et contrôle non-appel bancaire après refus à ajouter/contrôler dans l'environnement complet.

Le contrôleur est conditionné par l'activation de la confirmation Amplitude, comme avant.
