# SIXPAY CONNECT — Lot 3 : transport bancaire Regional

Révision de départ : `feat/repository-baseline-consolidation-cucumber` @
`dd46dfb2cfabcf3c7379d14670f794ff0b587911`.

## Flux

**Partenaire → SIXPAY** conserve l'authentification entrante définie par les
contrats SIXPAY, notamment JWT/M2M lorsque applicable.
**SIXPAY → Regional** utilise HTTPS/mTLS, **sans** `Authorization: Bearer`.
Les en-têtes de corrélation, d'institution et d'idempotence restent inchangés.

Le keystore PKCS12 SIXPAY contient sa clé privée et son certificat client.
Le truststore PKCS12 contient la CA qui authentifie le serveur Regional.
Le serveur vérifie la chaîne client et l'empreinte autorisée, puis applique
ses contrôles applicatifs. TLS 1.3 protège le transport, pas la décision métier.

## Modes

- **Local/mock isolé** : HTTP éventuellement autorisé uniquement par les
  configurations locales explicites ; ne pas l'exposer à un réseau non fiable.
- **Mode sécurisé** : `SIXPAY_BANKING_MTLS_ENABLED=true`, URL HTTPS,
  PKCS12 externe, clé privée, truststore, mot de passe ; pas de fallback HTTP.

Variables : `SIXPAY_BANKING_MTLS_ENABLED`, `AMPLITUDE_PAYMENT_BASE_URL`,
`AMPLITUDE_PAYMENT_SSL_BUNDLE`, `AMPLITUDE_PAYMENT_KEYSTORE_LOCATION`,
`AMPLITUDE_PAYMENT_KEYSTORE_PASSWORD`, `AMPLITUDE_PAYMENT_TRUSTSTORE_LOCATION`,
`AMPLITUDE_PAYMENT_TRUSTSTORE_PASSWORD`.

## Scénarios

1. Bundle valide et URL HTTPS : création du client et handshake mTLS.
2. mTLS activé et magasins absents : échec fail-closed.
3. URL HTTP en mode sécurisé : refus, sans repli.
4. Certificat serveur erroné / SAN inadéquat : échec TLS.
5. Requête sortante : aucune transmission de Bearer ; preuve HTTP à collecter.
6. La réussite du handshake ne vaut pas acceptation fonctionnelle du paiement.

## Commandes locales — non exécutées ici

```powershell
cd backend
mvn -pl bootstrap -am "-Dtest=CoreBankingMtlsAlignmentArchitectureTest,RegionalSecurityContractAlignmentTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn verify
```

La modification des contrats obsolètes accompagne l'implémentation courante.
Sa nouvelle approbation bancaire/sécurité reste à enregistrer : les anciens
statuts `APPROVED` ne constituent pas une preuve de ratification du changement.
