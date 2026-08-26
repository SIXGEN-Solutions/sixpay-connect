package com.sixpay.tests.gate;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Phase 8.3.7 — Final Cross-module Gate.
 *
 * <p>The runtime assertions remain owned by the existing 8.3 integration
 * tests. This fast structural gate prevents Maven configuration drift from
 * silently skipping them or leaking bootstrap into focused test classpaths.</p>
 */
class FinalCrossModuleGateTest {

    private static final List<String> REQUIRED_ASSEMBLED_TESTS = List.of(
            "AssembledApplicationContextIT.java",
            "PaymentAccountingReportingReadinessIT.java",
            "PaymentNotificationReadinessIT.java",
            "HybridSecurityAssemblyIT.java",
            "PilotCriticalFlowMatrixIT.java"
    );

    private static final List<String> PAYMENT_CUSTOMER_OBSERVATION_MARKERS =
            List.of(
                    "Payment -> Observed Customer projection",
                    "ObservedCustomerProjectionPort",
                    "PaymentObservedCustomerProjectionService"
            );

    @Test
    void requiredCrossModuleAssemblyAssetsExist() {
        Path assembled = testsModuleRoot()
                .resolve("src/test/java/com/sixpay/tests/assembled");

        for (String fileName : REQUIRED_ASSEMBLED_TESTS) {
            assertThat(assembled.resolve(fileName))
                    .as("Missing canonical Phase 8.3 assembled test: %s", fileName)
                    .isRegularFile();
        }
    }

    @Test
    void paymentCustomerObservationBoundaryRemainsCoveredByFinalMatrix()
            throws IOException {

        Path matrix = testsModuleRoot()
                .resolve(
                        "src/test/java/com/sixpay/tests/assembled/"
                                + "PilotCriticalFlowMatrixIT.java"
                );

        assertThat(matrix).isRegularFile();

        String content = Files.readString(matrix);

        for (String marker : PAYMENT_CUSTOMER_OBSERVATION_MARKERS) {
            assertThat(content)
                    .as(
                            "8.3.2 Payment / Customer Observation "
                                    + "coverage marker is missing: %s",
                            marker
                    )
                    .contains(marker);
        }
    }

    @Test
    void finalGateProfileIsFailClosedAndAssemblyScoped()
            throws Exception {

        Document pom = parsePom();
        Element profile = profileById(pom, "cross-module-gate");

        assertThat(profile)
                .as("cross-module-gate Maven profile must exist")
                .isNotNull();

        assertThat(
                directChildText(
                        directChild(profile, "properties"),
                        "skipITs"
                )
        ).isEqualTo("false");

        Element dependency =
                dependencyByArtifactId(
                        directChild(profile, "dependencies"),
                        "bootstrap"
                );

        assertThat(dependency)
                .as(
                        "cross-module-gate must assemble the production "
                                + "module graph through bootstrap"
                )
                .isNotNull();

        assertThat(directChildText(dependency, "scope"))
                .isEqualTo("test");

        Element failsafe =
                pluginByArtifactId(
                        directChild(
                                directChild(profile, "build"),
                                "plugins"
                        ),
                        "maven-failsafe-plugin"
                );

        assertThat(failsafe)
                .as("cross-module-gate must configure Maven Failsafe")
                .isNotNull();

        Element configuration =
                directChild(failsafe, "configuration");

        assertThat(
                directChildText(configuration, "skipITs")
        ).isEqualTo("false");

        assertThat(
                directChildText(configuration, "failIfNoTests")
        ).isEqualTo("true");

        assertThat(
                directChildText(
                        directChild(configuration, "includes"),
                        "include"
                )
        ).isEqualTo("**/assembled/**/*IT.java");

        assertThat(
                directChildText(
                        directChild(
                                configuration,
                                "systemPropertyVariables"
                        ),
                        "sixpay.assembled.tests"
                )
        ).isEqualTo("true");
    }

    @Test
    void bootstrapDoesNotLeakIntoBaselineDependencies()
            throws Exception {

        Document pom = parsePom();
        Element project = pom.getDocumentElement();

        Element baselineDependencies =
                directChild(project, "dependencies");

        assertThat(
                dependencyByArtifactId(
                        baselineDependencies,
                        "bootstrap"
                )
        )
                .as(
                        "bootstrap must remain profile-scoped; "
                                + "putting it in baseline dependencies "
                                + "reintroduces focused-test regressions"
                )
                .isNull();
    }

    private static Document parsePom()
            throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(false);

        return factory
                .newDocumentBuilder()
                .parse(
                        testsModuleRoot()
                                .resolve("pom.xml")
                                .toFile()
                );
    }

    private static Element profileById(
            Document document,
            String profileId
    ) {
        NodeList profiles =
                document.getElementsByTagName("profile");

        for (int index = 0; index < profiles.getLength(); index++) {
            Element profile =
                    (Element) profiles.item(index);

            if (profileId.equals(
                    directChildText(profile, "id")
            )) {
                return profile;
            }
        }

        return null;
    }

    private static Element dependencyByArtifactId(
            Element dependencies,
            String artifactId
    ) {
        return childByArtifactId(
                dependencies,
                "dependency",
                artifactId
        );
    }

    private static Element pluginByArtifactId(
            Element plugins,
            String artifactId
    ) {
        return childByArtifactId(
                plugins,
                "plugin",
                artifactId
        );
    }

    private static Element childByArtifactId(
            Element container,
            String childName,
            String artifactId
    ) {
        if (container == null) {
            return null;
        }

        for (Node child = container.getFirstChild();
             child != null;
             child = child.getNextSibling()) {

            if (!(child instanceof Element element)) {
                continue;
            }

            if (!childName.equals(element.getTagName())) {
                continue;
            }

            if (artifactId.equals(
                    directChildText(element, "artifactId")
            )) {
                return element;
            }
        }

        return null;
    }

    private static Element directChild(
            Element parent,
            String childName
    ) {
        if (parent == null) {
            return null;
        }

        for (Node child = parent.getFirstChild();
             child != null;
             child = child.getNextSibling()) {

            if (child instanceof Element element
                    && childName.equals(element.getTagName())) {
                return element;
            }
        }

        return null;
    }

    private static String directChildText(
            Element parent,
            String childName
    ) {
        Element child = directChild(parent, childName);

        return child == null
                ? null
                : child.getTextContent().trim();
    }

    private static Path testsModuleRoot() {
        Path current =
                Path.of("")
                        .toAbsolutePath()
                        .normalize();

        if ("tests".equals(fileName(current))
                && Files.isRegularFile(current.resolve("pom.xml"))) {
            return current;
        }

        if ("backend".equals(fileName(current))) {
            Path candidate = current.resolve("tests");

            if (Files.isRegularFile(candidate.resolve("pom.xml"))) {
                return candidate;
            }
        }

        Path candidate = current.resolve("backend/tests");

        if (Files.isRegularFile(candidate.resolve("pom.xml"))) {
            return candidate;
        }

        throw new IllegalStateException(
                "Unable to resolve backend/tests root from " + current
        );
    }

    private static String fileName(Path path) {
        return path.getFileName() == null
                ? ""
                : path.getFileName().toString();
    }
}
