package com.sixpay.tests.gate;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BackendGoldenCoverageGateTest {

    private static final List<CoverageDocument> REQUIRED_DOCUMENTS =
            List.of(
                    new CoverageDocument(
                            "customer",
                            "CUSTOMER-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "subscription",
                            "SUBSCRIPTION-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "payment",
                            "PAYMENT-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "accounting",
                            "ACCOUNTING-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "reporting",
                            "REPORTING-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "notification",
                            "NOTIFICATION-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "administration",
                            "ADMINISTRATION-TEST-COVERAGE.md"
                    ),
                    new CoverageDocument(
                            "security",
                            "SECURITY-TEST-COVERAGE.md"
                    )
            );

    private static final List<String> BLOCKING_MARKERS =
            List.of(
                    "PARTIAL",
                    "TO_VERIFY",
                    "UNVERIFIED",
                    "TODO",
                    "TBD"
            );

    @Test
    void allGoldenCoverageDocumentsExistAtCanonicalModuleLocations() {
        Path backend = backendRoot();

        for (CoverageDocument document : REQUIRED_DOCUMENTS) {
            Path expected =
                    backend.resolve(document.module())
                            .resolve(document.fileName());

            assertThat(expected)
                    .as(
                            "Missing canonical coverage document for %s",
                            document.module()
                    )
                    .isRegularFile();
        }
    }

    @Test
    void noGoldenCoverageDocumentContainsBlockingStatus()
            throws IOException {

        Path backend = backendRoot();

        for (CoverageDocument document : REQUIRED_DOCUMENTS) {
            Path path =
                    backend.resolve(document.module())
                            .resolve(document.fileName());

            if (!Files.isRegularFile(path)) {
                continue;
            }

            String content = Files.readString(path);

            for (String marker : BLOCKING_MARKERS) {
                assertThat(content)
                        .as(
                                "%s still contains blocking marker %s",
                                path,
                                marker
                        )
                        .doesNotContain(marker);
            }
        }
    }

    @Test
    void securityCoverageAssetsAreNotNestedUnderGoldenPartnerModule() {
        Path backend = backendRoot();

        assertThat(
                backend.resolve("partner/security")
        )
                .as(
                        "Security is a sibling module of partner; "
                                + "backend/partner/security must not exist"
                )
                .doesNotExist();
    }

    @Test
    void acceptedDeferredModulesUseExplicitNonImplementedClassification()
            throws IOException {

        Path backend = backendRoot();

        assertDeferredOrNotImplemented(
                backend.resolve(
                        "subscription/SUBSCRIPTION-TEST-COVERAGE.md"
                )
        );

        assertDeferredOrNotImplemented(
                backend.resolve(
                        "administration/ADMINISTRATION-TEST-COVERAGE.md"
                )
        );
    }

    private static void assertDeferredOrNotImplemented(
            Path document
    ) throws IOException {

        assertThat(document).isRegularFile();

        String content = Files.readString(document);

        boolean explicitlyDeferred =
                content.contains("DEFERRED")
                        || content.contains("NOT_IMPLEMENTED")
                        || content.contains("NOT IMPLEMENTED");

        assertThat(explicitlyDeferred)
                .as(
                        "%s must explicitly classify the absent "
                                + "implementation instead of pretending "
                                + "golden coverage exists",
                        document
                )
                .isTrue();
    }

    private static Path backendRoot() {
        Path current =
                Path.of("")
                        .toAbsolutePath()
                        .normalize();

        if ("tests".equals(fileName(current))
                && Files.isRegularFile(
                current.resolve("pom.xml")
        )) {
            return current.getParent();
        }

        if ("backend".equals(fileName(current))
                && Files.isRegularFile(
                current.resolve("pom.xml")
        )) {
            return current;
        }

        Path candidate = current.resolve("backend");
        if (Files.isRegularFile(
                candidate.resolve("pom.xml")
        )) {
            return candidate;
        }

        throw new IllegalStateException(
                "Unable to resolve backend root from "
                        + current
        );
    }

    private static String fileName(Path path) {
        return path.getFileName() == null
                ? ""
                : path.getFileName().toString();
    }

    private record CoverageDocument(
            String module,
            String fileName
    ) {
    }
}
