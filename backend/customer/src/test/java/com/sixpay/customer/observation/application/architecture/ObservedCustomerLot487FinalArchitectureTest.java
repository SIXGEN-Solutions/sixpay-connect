package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot487FinalArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    /**
     * Types applicatifs connus pour contenir des données personnelles
     * ou des critères sensibles.
     *
     * <p>Ces types doivent déclarer explicitement un toString protégé,
     * car le toString généré automatiquement par un record exposerait
     * toutes ses composantes.</p>
     */
    private static final Set<String> SENSITIVE_TYPES = Set.of(
            "ObserveCustomerCommand.java",
            "ObservedCustomerDetailView.java",
            "ObservedCustomerSummaryView.java",
            "SearchObservedCustomersQuery.java"
    );

    private static final List<String> PROTECTION_MARKERS = List.of(
            "[PROTECTED]",
            "[REDACTED]",
            "[MASKED]"
    );

    @Test
    void finalCustomerObservationStructureExists() {

        for (String directory : List.of(
                "api/controller",
                "api/dto",
                "api/error",
                "api/mapper",
                "api/observability",
                "application/audit",
                "application/port/input",
                "application/port/output/audit",
                "application/port/output/query",
                "application/query",
                "application/service",
                "domain",
                "infrastructure/audit",
                "infrastructure/health",
                "infrastructure/observability",
                "infrastructure/persistence",
                "infrastructure/query",
                "configuration"
        )) {
            Path expectedDirectory =
                    ROOT.resolve(directory);

            assertTrue(
                    Files.isDirectory(expectedDirectory),
                    () -> "Missing final directory: "
                            + expectedDirectory.toAbsolutePath()
            );
        }
    }

    @Test
    void customerNeverDependsOnPaymentOrAmplitude()
            throws Exception {

        assertNoTokens(
                ROOT,
                List.of(
                        "import com.sixpay.payment.",
                        "PaymentOutboxEntity",
                        "Amplitude",
                        "amplitude"
                )
        );
    }

    @Test
    void domainAndApplicationRemainFreeOfSpringJpaAndHttp()
            throws Exception {

        for (Path layer : List.of(
                ROOT.resolve("application"),
                ROOT.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "import org.springframework.",
                            "import jakarta.persistence.",
                            "import org.hibernate.",
                            "RestClient",
                            "WebClient",
                            "HttpClient",
                            "@Entity",
                            "@Repository",
                            "@Service",
                            "@Component",
                            "@Transactional",
                            "@RestController"
                    )
            );
        }
    }

    @Test
    void sensitiveTypesDeclareProtectedToString()
            throws Exception {

        try (var paths = Files.walk(ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            SENSITIVE_TYPES.contains(
                                    path.getFileName().toString()
                            )
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            String toStringBody =
                                    extractMethodBody(
                                            source,
                                            "String toString()"
                                    );

                            if (toStringBody == null) {
                                return java.util.stream.Stream.of(
                                        path
                                                + " has no explicit "
                                                + "protected toString()"
                                );
                            }

                            boolean protectedRendering =
                                    PROTECTION_MARKERS.stream()
                                            .anyMatch(
                                                    toStringBody::contains
                                            );

                            if (!protectedRendering) {
                                return java.util.stream.Stream.of(
                                        path
                                                + " has no protection "
                                                + "marker in toString()"
                                );
                            }

                            return dangerousToStringExpressions(
                                    path,
                                    toStringBody
                            ).stream();
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Unsafe sensitive toString implementation: "
                            + violations
            );
        }
    }

    @Test
    void nonSensitiveExplicitToStringDoesNotRenderPayloads()
            throws Exception {

        try (var paths = Files.walk(ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            !SENSITIVE_TYPES.contains(
                                    path.getFileName().toString()
                            )
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            String toStringBody =
                                    extractMethodBody(
                                            source,
                                            "String toString()"
                                    );

                            if (toStringBody == null) {
                                return java.util.stream.Stream.empty();
                            }

                            return dangerousToStringExpressions(
                                    path,
                                    toStringBody
                            ).stream();
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Potential sensitive toString rendering: "
                            + violations
            );
        }
    }

    private static List<String> dangerousToStringExpressions(
            Path path,
            String body
    ) {
        /*
         * On interdit l'utilisation directe des variables ou accesseurs
         * sensibles dans la construction du texte.
         *
         * Les noms présents dans des littéraux tels que
         * "legalName=[PROTECTED]" restent autorisés.
         */
        List<String> dangerousExpressions = List.of(
                "+ normalizedNiu",
                "+ legalName",
                "+ email",
                "+ phone",
                "+ accountBindingFingerprint",
                "+ maskedAccountReference",
                "+ payload",
                "normalizedNiu()",
                "legalName()",
                "email()",
                "phone()",
                "accountBindingFingerprint()",
                "maskedAccountReference()",
                "payload()",
                "String.valueOf(normalizedNiu",
                "String.valueOf(legalName",
                "String.valueOf(email",
                "String.valueOf(phone",
                "String.valueOf(accountBindingFingerprint",
                "String.valueOf(maskedAccountReference",
                "String.valueOf(payload",
                ".formatted(normalizedNiu",
                ".formatted(legalName",
                ".formatted(email",
                ".formatted(phone",
                ".formatted(accountBindingFingerprint",
                ".formatted(maskedAccountReference",
                ".formatted(payload"
        );

        return dangerousExpressions.stream()
                .filter(body::contains)
                .map(expression ->
                        path
                                + " exposes sensitive expression "
                                + expression
                                + " in toString()"
                )
                .toList();
    }

    private static String extractMethodBody(
            String source,
            String methodSignature
    ) {
        int signatureIndex =
                source.indexOf(methodSignature);

        if (signatureIndex < 0) {
            return null;
        }

        int openingBrace =
                source.indexOf(
                        '{',
                        signatureIndex
                );

        if (openingBrace < 0) {
            return null;
        }

        int depth = 0;
        boolean insideString = false;
        boolean escaped = false;
        boolean insideCharacter = false;
        boolean insideLineComment = false;
        boolean insideBlockComment = false;

        for (int index = openingBrace;
             index < source.length();
             index++) {

            char current =
                    source.charAt(index);

            char next =
                    index + 1 < source.length()
                            ? source.charAt(index + 1)
                            : '\0';

            if (insideLineComment) {
                if (current == '\n') {
                    insideLineComment = false;
                }
                continue;
            }

            if (insideBlockComment) {
                if (current == '*' && next == '/') {
                    insideBlockComment = false;
                    index++;
                }
                continue;
            }

            if (!insideString && !insideCharacter) {
                if (current == '/' && next == '/') {
                    insideLineComment = true;
                    index++;
                    continue;
                }

                if (current == '/' && next == '*') {
                    insideBlockComment = true;
                    index++;
                    continue;
                }
            }

            if (insideString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    insideString = false;
                }
                continue;
            }

            if (insideCharacter) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '\'') {
                    insideCharacter = false;
                }
                continue;
            }

            if (current == '"') {
                insideString = true;
                continue;
            }

            if (current == '\'') {
                insideCharacter = true;
                continue;
            }

            if (current == '{') {
                depth++;
            } else if (current == '}') {
                depth--;

                if (depth == 0) {
                    return source.substring(
                            openingBrace + 1,
                            index
                    );
                }
            }
        }

        throw new IllegalStateException(
                "Unclosed toString() method body"
        );
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {

        try (var paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Final architecture violations: "
                            + violations
            );
        }
    }
}