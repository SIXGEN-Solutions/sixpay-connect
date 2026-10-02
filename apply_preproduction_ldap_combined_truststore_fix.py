#!/usr/bin/env python3
from pathlib import Path

ROOT = Path.cwd()
COMPOSE = ROOT / "infrastructure/docker/preproduction/docker-compose.yml"
README = ROOT / "infrastructure/docker/preproduction/README.md"

if not COMPOSE.is_file():
    raise SystemExit("ERROR: run from sixpay-connect repository root; docker-compose.yml not found.")

compose = COMPOSE.read_text(encoding="utf-8")

old_entrypoint = '    entrypoint: ["/bin/sh","-ec","for i in $$(seq 1 60); do [ -s /samba/private/tls/ca.pem ] && break; sleep 2; done; test -s /samba/private/tls/ca.pem; rm -f /truststore/ldap-truststore.p12; keytool -importcert -noprompt -alias samba-ca -file /samba/private/tls/ca.pem -keystore /truststore/ldap-truststore.p12 -storetype PKCS12 -storepass changeit"]'
new_entrypoint = '    entrypoint: ["/bin/sh","-ec","for i in $$(seq 1 60); do [ -s /samba/private/tls/ca.pem ] && break; sleep 2; done; test -s /samba/private/tls/ca.pem; rm -f /truststore/sixpay-cacerts; cp \"$$JAVA_HOME/lib/security/cacerts\" /truststore/sixpay-cacerts; keytool -importcert -noprompt -trustcacerts -alias samba-ca -file /samba/private/tls/ca.pem -keystore /truststore/sixpay-cacerts -storepass changeit; keytool -list -keystore /truststore/sixpay-cacerts -storepass changeit -alias samba-ca"]'

old_java = '      JAVA_TOOL_OPTIONS: "-Djavax.net.ssl.trustStore=/opt/sixpay/trust/ldap-truststore.p12 -Djavax.net.ssl.trustStoreType=PKCS12 -Djavax.net.ssl.trustStorePassword=changeit"'
new_java = '      JAVA_TOOL_OPTIONS: "-Djavax.net.ssl.trustStore=/opt/sixpay/trust/sixpay-cacerts -Djavax.net.ssl.trustStorePassword=changeit"'

for label, needle in (("ldap-truststore entrypoint", old_entrypoint), ("JAVA_TOOL_OPTIONS", old_java)):
    n = compose.count(needle)
    if n != 1:
        raise SystemExit(f"ERROR: expected exactly one {label}, found {n}; no change applied.")

compose = compose.replace(old_entrypoint, new_entrypoint, 1)
compose = compose.replace(old_java, new_java, 1)
COMPOSE.write_text(compose, encoding="utf-8", newline="\n")

if README.is_file():
    readme = README.read_text(encoding="utf-8")
    marker = "Human auth is LDAP-only: LOCAL=false, OIDC=false, LDAP=true. Samba AD is used because SIXPAY consumes AD attributes (`sAMAccountName`, `objectGUID`, `userAccountControl`)."
    paragraph = marker + "\n\nThe pre-production LDAP truststore is built from the Temurin 21 default JVM `cacerts` and augmented with the Samba AD CA. This preserves the JVM standard trusted roots while adding trust for the pre-production LDAPS endpoint."
    if paragraph not in readme:
        if marker not in readme:
            raise SystemExit("ERROR: README structure differs from expected baseline; compose changed, README unchanged.")
        README.write_text(readme.replace(marker, paragraph, 1), encoding="utf-8", newline="\n")

print("Applied pre-production LDAP combined truststore fix.")
print("Modified:", COMPOSE)
if README.is_file():
    print("Modified:", README)
print("No Git operation, build, test, or gate was executed.")
