from pathlib import Path
import sys

ROOT = Path.cwd()
ENGINEERING = ROOT / "ENGINEERING_CONTEXT.md"
EXPECTED_BRANCH = "feat/repository-baseline-consolidation"

DOC = (
    ROOT
    / "documentation/architecture/configuration/"
    / "FS-2.5.4_SECURITY_AUTHENTICATION_CONFIGURATION.md"
)

TEST = (
    ROOT
    / "backend/bootstrap/src/test/java/com/sixpay/bootstrap/architecture/"
    / "SecurityAuthenticationConfigurationArchitectureTest.java"
)

SECURITY_CONFIG = (
    ROOT
    / "backend/security/src/main/java/com/sixpay/security/configuration"
)

BOOTSTRAP_RESOURCES = (
    ROOT
    / "backend/bootstrap/src/main/resources"
)

REPORT = '# FS-2.5.4 — Security / Authentication Configuration Consolidation\n\n**Branch:** `feat/repository-baseline-consolidation`  \n**Phase:** `FS-2.5 — Configuration consolidation`  \n**Golden module:** Partner\n\n## Purpose\n\nFS-2.5.4 separates authentication runtime composition from Security-owned\npolicy configuration without changing existing authentication behavior.\n\n## Ownership split\n\n### Bootstrap runtime-owned\n\nBootstrap owns runtime assembly for:\n\n```text\nspring.security.*\nserver.servlet.session.*\n```\n\nThis includes:\n\n- OAuth2 resource-server / OIDC provider wiring;\n- issuer/JWK runtime endpoints;\n- HTTP session timeout;\n- session-cookie runtime attributes;\n- activation of `local-auth` / `hybrid-auth` profiles.\n\n### Security module-owned\n\nSecurity owns the semantics, defaults and validation of:\n\n```text\nsixpay.security.authentication.*\nsixpay.security.local.password.*\n```\n\nCurrent canonical binders are:\n\n```text\nAuthenticationCapabilitiesProperties\nPasswordPolicyProperties\n```\n\nSecurity therefore owns:\n\n- local authentication enabled/disabled capability;\n- OIDC enabled/disabled capability and registration id;\n- maximum failed attempts;\n- lock duration;\n- BCrypt strength;\n- password minimum/maximum length;\n- password history size;\n- password expiration policy.\n\n## Existing profile semantics\n\n`application-local-auth.yml`:\n\n```text\nlocal = enabled\noidc  = disabled\n```\n\n`application-hybrid-auth.yml`:\n\n```text\nlocal = enabled\noidc  = enabled\n```\n\nThese semantics remain unchanged.\n\n## Important distinction\n\nThe physical presence of `sixpay.security.*` values in Bootstrap profile YAML\ndoes not make Bootstrap their semantic owner.\n\n```text\nBootstrap YAML\n    = runtime value source / profile composition\n\nSecurity @ConfigurationProperties\n    = semantic owner + defaults + validation\n```\n\n## Password policy\n\n`PasswordPolicyProperties` validates external values against the Security domain\n`PasswordPolicy`.\n\nTherefore Bootstrap must not duplicate password-policy validation or defaults.\n\n## Authentication capability policy\n\n`AuthenticationCapabilitiesProperties` owns the defaults and normalization for:\n\n```text\nmaximum-failed-attempts = 5\nlock-duration           = 15m\nbcrypt-strength         = 12\n```\n\nBootstrap profiles may override them through existing environment variables,\nbut must not introduce competing defaults elsewhere in Java.\n\n## Non-regression rules\n\nFS-2.5.4 does not:\n\n- rename `sixpay.security.*` keys;\n- rename auth environment variables;\n- change local/OIDC profile semantics;\n- change session/cookie defaults;\n- change password-policy defaults;\n- move Security configuration classes;\n- change authentication flows.\n\n## Gate rules\n\nThe architecture gate enforces that:\n\n1. Security owns `AuthenticationCapabilitiesProperties`.\n2. Security owns `PasswordPolicyProperties`.\n3. the canonical `@ConfigurationProperties` prefixes remain stable;\n4. local-auth remains local=true / oidc=false;\n5. hybrid-auth remains local=true / oidc=true;\n6. OAuth2 runtime wiring remains under `spring.security.*`;\n7. session runtime configuration remains under `server.servlet.session.*`;\n8. no non-Security business module consumes `sixpay.security.*` directly.\n\n## Exit criteria\n\nFS-2.5.4 is DONE when the ownership split is documented and the gate is green,\nwith no functional authentication behavior changed.\n'
JAVA = 'package com.sixpay.bootstrap.architecture;\n\nimport org.junit.jupiter.api.Test;\n\nimport java.nio.file.Files;\nimport java.nio.file.Path;\nimport java.util.ArrayList;\nimport java.util.List;\nimport java.util.regex.Matcher;\nimport java.util.regex.Pattern;\n\nimport static org.junit.jupiter.api.Assertions.assertTrue;\n\nclass SecurityAuthenticationConfigurationArchitectureTest {\n\n    private static final Path BACKEND_ROOT =\n            Path.of("..");\n\n    private static final Path BOOTSTRAP_RESOURCES =\n            Path.of("src/main/resources");\n\n    private static final List<String> BUSINESS_MODULES =\n            List.of(\n                    "partner",\n                    "customer",\n                    "payment",\n                    "accounting",\n                    "reporting",\n                    "notification",\n                    "administration"\n            );\n\n    private static final Pattern SECURITY_PROPERTY_REFERENCE =\n            Pattern.compile(\n                    "sixpay\\\\.security\\\\.[A-Za-z0-9_.-]+"\n            );\n\n    @Test\n    void securityOwnsAuthenticationCapabilityProperties()\n            throws Exception {\n\n        Path source =\n                BACKEND_ROOT.resolve(\n                        "security/src/main/java/com/sixpay/security/"\n                                + "configuration/"\n                                + "AuthenticationCapabilitiesProperties.java"\n                );\n\n        assertTrue(Files.isRegularFile(source));\n\n        String text = Files.readString(source);\n\n        assertTrue(\n                text.contains(\n                        "@ConfigurationProperties("\n                                + "\\"sixpay.security.authentication\\""\n                                + ")"\n                ),\n                "Security must own authentication capability binding"\n        );\n\n        assertTrue(text.contains("DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5"));\n        assertTrue(text.contains("Duration.ofMinutes(15)"));\n        assertTrue(text.contains("DEFAULT_BCRYPT_STRENGTH = 12"));\n    }\n\n    @Test\n    void securityOwnsPasswordPolicyProperties()\n            throws Exception {\n\n        Path source =\n                BACKEND_ROOT.resolve(\n                        "security/src/main/java/com/sixpay/security/"\n                                + "configuration/"\n                                + "PasswordPolicyProperties.java"\n                );\n\n        assertTrue(Files.isRegularFile(source));\n\n        String text = Files.readString(source);\n\n        assertTrue(\n                text.contains(\n                        "@ConfigurationProperties("\n                                + "\\"sixpay.security.local.password\\""\n                                + ")"\n                ),\n                "Security must own password-policy binding"\n        );\n\n        assertTrue(text.contains("DEFAULT_MIN_LENGTH = 12"));\n        assertTrue(text.contains("DEFAULT_MAX_LENGTH = 200"));\n        assertTrue(text.contains("DEFAULT_HISTORY_SIZE = 5"));\n        assertTrue(text.contains("DEFAULT_EXPIRATION_DAYS = 90"));\n\n        assertTrue(\n                text.contains("new PasswordPolicy("),\n                "External password configuration must be validated "\n                        + "through Security domain invariants"\n        );\n    }\n\n    @Test\n    void authenticationProfilesPreserveReviewedSemantics()\n            throws Exception {\n\n        String local =\n                Files.readString(\n                        BOOTSTRAP_RESOURCES.resolve(\n                                "application-local-auth.yml"\n                        )\n                );\n\n        String hybrid =\n                Files.readString(\n                        BOOTSTRAP_RESOURCES.resolve(\n                                "application-hybrid-auth.yml"\n                        )\n                );\n\n        assertLocalAndOidc(local, true, false, "local-auth");\n        assertLocalAndOidc(hybrid, true, true, "hybrid-auth");\n    }\n\n    @Test\n    void hybridProfileKeepsRuntimeOauthAndSessionAssemblyInBootstrap()\n            throws Exception {\n\n        String hybrid =\n                Files.readString(\n                        BOOTSTRAP_RESOURCES.resolve(\n                                "application-hybrid-auth.yml"\n                        )\n                );\n\n        assertTrue(\n                hybrid.contains("spring:")\n                        && hybrid.contains("security:")\n                        && hybrid.contains("oauth2:")\n                        && hybrid.contains("resourceserver:"),\n                "OAuth2 runtime assembly must remain in Bootstrap profile"\n        );\n\n        assertTrue(\n                hybrid.contains("server:")\n                        && hybrid.contains("servlet:")\n                        && hybrid.contains("session:"),\n                "HTTP session runtime assembly must remain in Bootstrap"\n        );\n    }\n\n    @Test\n    void otherBusinessModulesDoNotConsumeSecurityConfigurationDirectly()\n            throws Exception {\n\n        List<String> violations =\n                new ArrayList<>();\n\n        for (String module : BUSINESS_MODULES) {\n\n            Path javaRoot =\n                    BACKEND_ROOT.resolve(\n                            module + "/src/main/java"\n                    );\n\n            if (!Files.isDirectory(javaRoot)) {\n                continue;\n            }\n\n            try (var files = Files.walk(javaRoot)) {\n                for (Path javaFile :\n                        files.filter(Files::isRegularFile)\n                                .filter(path ->\n                                        path.toString()\n                                                .endsWith(".java")\n                                )\n                                .toList()) {\n\n                    String source =\n                            Files.readString(javaFile);\n\n                    Matcher matcher =\n                            SECURITY_PROPERTY_REFERENCE.matcher(source);\n\n                    while (matcher.find()) {\n                        violations.add(\n                                module\n                                        + " directly consumes "\n                                        + matcher.group()\n                                        + " in "\n                                        + javaFile\n                        );\n                    }\n                }\n            }\n        }\n\n        assertTrue(\n                violations.isEmpty(),\n                () -> "Security configuration leaked into other "\n                        + "business modules: "\n                        + violations\n        );\n    }\n\n    private void assertLocalAndOidc(\n            String source,\n            boolean localExpected,\n            boolean oidcExpected,\n            String profile\n    ) {\n\n        int localIndex =\n                source.indexOf("local:");\n\n        int oidcIndex =\n                source.indexOf("oidc:");\n\n        assertTrue(localIndex >= 0, profile + " missing local section");\n        assertTrue(oidcIndex > localIndex, profile + " missing OIDC section");\n\n        String localSection =\n                source.substring(\n                        localIndex,\n                        oidcIndex\n                );\n\n        String oidcSection =\n                source.substring(oidcIndex);\n\n        assertTrue(\n                localSection.contains(\n                        "enabled: " + localExpected\n                ),\n                profile + " local semantics changed"\n        );\n\n        assertTrue(\n                oidcSection.contains(\n                        "enabled: " + oidcExpected\n                ),\n                profile + " OIDC semantics changed"\n        );\n    }\n}\n'

def fail(message):
    print(f"ERROR: {message}")
    sys.exit(1)

def require(path):
    if not path.is_file():
        fail(f"Missing required file: {path}")
    return path.read_text(encoding="utf-8")

def main():
    engineering = require(ENGINEERING)

    if EXPECTED_BRANCH not in engineering:
        fail(
            "ENGINEERING_CONTEXT.md does not declare "
            + EXPECTED_BRANCH
        )

    require(
        ROOT
        / "documentation/architecture/configuration/"
        / "FS-2.5.3_PROFILES_CONSOLIDATION.md"
    )

    capabilities = require(
        SECURITY_CONFIG
        / "AuthenticationCapabilitiesProperties.java"
    )

    password = require(
        SECURITY_CONFIG
        / "PasswordPolicyProperties.java"
    )

    local = require(
        BOOTSTRAP_RESOURCES
        / "application-local-auth.yml"
    )

    hybrid = require(
        BOOTSTRAP_RESOURCES
        / "application-hybrid-auth.yml"
    )

    required_capability_tokens = [
        '@ConfigurationProperties("sixpay.security.authentication")',
        "DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5",
        "Duration.ofMinutes(15)",
        "DEFAULT_BCRYPT_STRENGTH = 12",
    ]

    for token in required_capability_tokens:
        if token not in capabilities:
            fail(
                "Authentication capability semantic changed: "
                + token
            )

    required_password_tokens = [
        '@ConfigurationProperties("sixpay.security.local.password")',
        "DEFAULT_MIN_LENGTH = 12",
        "DEFAULT_MAX_LENGTH = 200",
        "DEFAULT_HISTORY_SIZE = 5",
        "DEFAULT_EXPIRATION_DAYS = 90",
        "new PasswordPolicy(",
    ]

    for token in required_password_tokens:
        if token not in password:
            fail(
                "Password policy semantic changed: "
                + token
            )

    if "local:" not in local or "oidc:" not in local:
        fail("local-auth profile structure changed")

    if "local:" not in hybrid or "oidc:" not in hybrid:
        fail("hybrid-auth profile structure changed")

    if "spring:" not in hybrid or "oauth2:" not in hybrid:
        fail("hybrid OAuth2 runtime assembly missing")

    if "server:" not in hybrid or "session:" not in hybrid:
        fail("hybrid session runtime assembly missing")

    DOC.parent.mkdir(parents=True, exist_ok=True)
    TEST.parent.mkdir(parents=True, exist_ok=True)

    if DOC.exists():
        fail(
            "Documentation already exists: "
            + str(DOC.relative_to(ROOT))
        )

    if TEST.exists():
        fail(
            "Architecture gate already exists: "
            + str(TEST.relative_to(ROOT))
        )

    DOC.write_text(REPORT, encoding="utf-8")
    TEST.write_text(JAVA, encoding="utf-8")

    print(
        "FS-2.5.4 Security/authentication configuration installed."
    )
    print("Created:")
    print(" -", DOC.relative_to(ROOT))
    print(" -", TEST.relative_to(ROOT))
    print()
    print("Ownership:")
    print(" - Bootstrap: spring.security.*, session/profile runtime assembly")
    print(" - Security: sixpay.security.* semantics/defaults/validation")
    print()
    print("Behavior changes: 0")
    print("Property renames: 0")
    print("Default changes: 0")
    print()
    print("Run from backend:")
    print(
        "  mvn -pl bootstrap -am "
        "-Dtest=SecurityAuthenticationConfigurationArchitectureTest test"
    )
    print()
    print("Then:")
    print("  mvn verify")

if __name__ == "__main__":
    main()
