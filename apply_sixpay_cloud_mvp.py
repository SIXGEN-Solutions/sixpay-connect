#!/usr/bin/env python3
"""Préparation locale réversible du profil SIXPAY MVP cloud.
Ne lit ni ne copie le .env local; ne lance ni Git, ni tests, ni Docker.
"""
import argparse
import datetime as dt
import os
from pathlib import Path
import shutil
import sys

PROD = 'backend/bootstrap/src/main/resources/application-production.yml'
FRONT = 'frontend/src/environments/environment.ts'

OLD_PROD = '''spring:
  config:
    import:
      - classpath:config/security/ldap-common.yml
      - classpath:config/security/oidc-common.yml
  datasource:
    url: ${SIXPAY_DB_URL}
    username: ${SIXPAY_DB_USERNAME}
    password: ${SIXPAY_DB_PASSWORD}
    hikari:
      schema: sixpay
      maximum-pool-size: ${SIXPAY_DB_MAX_POOL_SIZE:20}
      minimum-idle: ${SIXPAY_DB_MIN_IDLE:5}
      connection-timeout: ${SIXPAY_DB_CONNECTION_TIMEOUT:20s}
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        default_schema: sixpay
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
management:
  endpoints:
    web:
      exposure:
        include: health,info
sixpay:
  security:
    authentication:
      policy:
        require-provider: true
      local:
        enabled: ${SIXPAY_LOCAL_ENABLED:false}
      oidc:
        enabled: ${SIXPAY_OIDC_ENABLED:false}
      ldap:
        enabled: ${SIXPAY_LDAP_ENABLED:false}
server:
  servlet:
    session:
      timeout: ${SIXPAY_SESSION_TIMEOUT:30m}
      cookie:
        http-only: true
        same-site: strict
        secure: ${SIXPAY_SESSION_COOKIE_SECURE:true}
'''
OLD_FRONT = '''import { AppEnvironment } from './environment.model';

export const environment = {
  production: true,
  apiBaseUrl: '',
  backend: {
    mode: 'api',
  },
  authentication: {
    standalone: false,
    local: {
      enabled: true,
    },
    oidc: {
      enabled: true,
      authority: 'https://identity.sixpay.example',
      clientId: 'sixpay-connect-frontend',
      scope: 'openid profile email roles offline_access',
    },
    ldap: {
      enabled: false,
    },
  },
} satisfies AppEnvironment;
'''

NEW_PROD = OLD_PROD.replace(
    '      - classpath:config/security/oidc-common.yml\n',
    '      - classpath:application-customer-banking.yml\n'
    '      - classpath:application-payment-banking-capabilities.yml\n'
    '      - classpath:application-notification-operational.yml\n'
    '      - classpath:application-customer-projection-outbox.yml\n'
)
NEW_PROD = NEW_PROD.replace('enabled: ${SIXPAY_LDAP_ENABLED:false}', 'enabled: ${SIXPAY_LDAP_ENABLED:true}')
NEW_PROD += '''
# Production cloud simulation: business capabilities preserved from the local .env.
# Never copy local HMAC keys, callback secrets, HTTP relaxations or test bank accounts.
  # marker intentionally NOT used: YAML entries below are top-level `sixpay` additions
'''.replace('  # marker intentionally NOT used: YAML entries below are top-level `sixpay` additions\n','')
# Add under the existing sixpay: block, before server:.
NEW_PROD = NEW_PROD.replace('server:\n  servlet:', '''  payment:
    treasury:
      enabled: ${SIXPAY_PAYMENT_TREASURY_ENABLED:true}
    banking:
      amplitude:
        # The imported capability fragment owns the normal provider configuration.
        # Transport security must be configured externally for the deployed provider.
        confirmation:
          enabled: ${AMPLITUDE_CONFIRMATION_ENABLED:true}
          connect-timeout: ${AMPLITUDE_CONFIRMATION_CONNECT_TIMEOUT:2s}
          read-timeout: ${AMPLITUDE_CONFIRMATION_READ_TIMEOUT:10s}
        reservation:
          enabled: ${AMPLITUDE_FUNDS_RESERVATION_ENABLED:true}
        posting:
          enabled: ${AMPLITUDE_POSTING_ENABLED:true}
        compensation:
          enabled: ${AMPLITUDE_COMPENSATION_ENABLED:false}
        status:
          enabled: ${AMPLITUDE_POSTING_STATUS_ENABLED:false}
    callback:
      enabled: ${SIXPAY_PAYMENT_CALLBACK_ENABLED:true}
      signing-key-id: ${SIXPAY_PAYMENT_CALLBACK_SIGNING_KEY_ID}
      signing-secret: ${SIXPAY_PAYMENT_CALLBACK_SIGNING_SECRET}
  reporting:
    audit-query:
      cursor-hmac-key: ${SIXPAY_REPORTING_AUDIT_CURSOR_HMAC_KEY}
    audit-export:
      storage-directory: ${SIXPAY_REPORTING_AUDIT_EXPORT_STORAGE_DIRECTORY:/var/lib/sixpay/audit-exports}
      retrieval-base-uri: ${SIXPAY_REPORTING_AUDIT_EXPORT_RETRIEVAL_BASE_URI}
  customer:
    observation:
      persistence:
        enabled: true
        protection-key-base64: ${SIXPAY_CUSTOMER_OBSERVATION_PROTECTION_KEY_BASE64}
      query:
        enabled: true
        cursor-key-base64: ${SIXPAY_CUSTOMER_OBSERVATION_CURSOR_KEY_BASE64}
      audit:
        persistence:
          enabled: true
  notification:
    email:
      mode: ${SIXPAY_NOTIFICATION_EMAIL_MODE:logging}
  security:
    m2m:
      # Enable only after verified mTLS gateway, Keycloak JWT and partner binding.
      enabled: ${SIXPAY_M2M_ENABLED:false}
      issuer-uri: ${SIXPAY_M2M_ISSUER_URI:}
      jwk-set-uri: ${SIXPAY_M2M_JWK_SET_URI:}
      audience: ${SIXPAY_M2M_AUDIENCE:}
server:
  servlet:''')
# Correct duplicate sixpay.security key (merge m2m into the first security mapping).
NEW_PROD = NEW_PROD.replace('''  security:
    m2m:
      # Enable only after verified mTLS gateway, Keycloak JWT and partner binding.
      enabled: ${SIXPAY_M2M_ENABLED:false}
      issuer-uri: ${SIXPAY_M2M_ISSUER_URI:}
      jwk-set-uri: ${SIXPAY_M2M_JWK_SET_URI:}
      audience: ${SIXPAY_M2M_AUDIENCE:}
server:''', 'server:')
NEW_PROD = NEW_PROD.replace('''  security:
    authentication:
''', '''  security:
    m2m:
      # Enable only after verified mTLS gateway, Keycloak JWT and partner binding.
      enabled: ${SIXPAY_M2M_ENABLED:false}
      issuer-uri: ${SIXPAY_M2M_ISSUER_URI:}
      jwk-set-uri: ${SIXPAY_M2M_JWK_SET_URI:}
      audience: ${SIXPAY_M2M_AUDIENCE:}
    authentication:
''')
NEW_PROD = NEW_PROD.replace('''# Production cloud simulation: business capabilities preserved from the local .env.
# Never copy local HMAC keys, callback secrets, HTTP relaxations or test bank accounts.
''', '')
NEW_PROD = '# Production cloud simulation: no local insecure HTTP/OAuth2/mTLS overrides.\n' + NEW_PROD
NEW_FRONT = '''import { AppEnvironment } from './environment.model';

export const environment = {
  production: true,
  apiBaseUrl: '',
  backend: { mode: 'api' },
  authentication: {
    standalone: false,
    local: { enabled: false },
    oidc: { enabled: false },
    ldap: { enabled: true },
  },
} satisfies AppEnvironment;
'''

FILES = {PROD: (OLD_PROD, NEW_PROD), FRONT: (OLD_FRONT, NEW_FRONT)}

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    mode = ap.add_mutually_exclusive_group(required=True)
    mode.add_argument('--dry-run', action='store_true')
    mode.add_argument('--apply', action='store_true')
    ap.add_argument('--repo', type=Path, default=Path.cwd(), help='Racine SIXPAY locale')
    args = ap.parse_args()
    root = args.repo.resolve()
    if not (root / 'AGENTS.md').is_file() or not (root / 'backend/bootstrap').is_dir():
        ap.error(f"Répertoire SIXPAY invalide: {root}")
    updates = []
    for relative, (old, new) in FILES.items():
        file = root / relative
        if not file.is_file():
            ap.error(f"Fichier obligatoire absent : {relative}")
        actual = file.read_text(encoding='utf-8')
        if actual == new:
            print(f"DÉJÀ APPLIQUÉ : {relative}")
        elif actual == old or actual.replace('\r\n','\n') == old:
            updates.append((file, actual, new))
            print(f"À MODIFIER : {relative}")
        else:
            ap.error(f"Contenu inattendu : {relative}. Aucune écriture. Réexaminer la révision locale et les modifications existantes.")
    if args.dry_run:
        print(f"DRY RUN : {len(updates)} fichier(s) candidat(s), aucun changement.")
        return
    if not updates:
        print('Idempotent : aucun fichier à modifier.')
        return
    stamp = dt.datetime.now(dt.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
    backup_dir = root.parent / ('sixpay-cloud-mvp-backup-' + stamp)
    if backup_dir.exists():
        ap.error(f'Dossier de sauvegarde déjà présent : {backup_dir}')
    backup_dir.mkdir(mode=0o700)
    written = []
    try:
        for path, old, new in updates:
            relative = path.relative_to(root)
            copy = backup_dir / relative
            copy.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, copy)
        for path, old, new in updates:
            tmp = path.with_name(path.name + '.sixpay-cloud.tmp')
            try:
                with open(tmp, 'x', encoding='utf-8', newline='\n') as handle:
                    handle.write(new)
                os.replace(tmp, path)
                written.append((path, old))
            finally:
                if tmp.exists():
                    tmp.unlink()
    except Exception as exc:
        for path, old in reversed(written):
            copy = backup_dir / path.relative_to(root)
            shutil.copy2(copy, path)
        ap.error(f'Application annulée, restauration tentée : {exc}')
    print(f"Sauvegarde : {backup_dir}")
    print('Correctif appliqué localement. Aucun test ou déploiement exécuté.')

if __name__ == '__main__':
    main()
