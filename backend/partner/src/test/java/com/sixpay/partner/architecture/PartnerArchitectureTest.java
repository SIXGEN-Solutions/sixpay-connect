package com.sixpay.partner.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerArchitectureTest {

    private static final Path JAVA_ROOT = Path.of("src/main/java/com/sixpay/partner");
    private static final List<String> REQUIRED_TOP_LEVEL_PACKAGES = List.of(
            "api",
            "application",
            "domain",
            "infrastructure",
            "configuration",
            "events"
    );

    @Test
    void moduleContainsTheOfficialTopLevelPackages() {
        var missingPackages = REQUIRED_TOP_LEVEL_PACKAGES.stream()
                .map(JAVA_ROOT::resolve)
                .filter(path -> !Files.isDirectory(path))
                .map(Path::toString)
                .toList();

        assertThat(missingPackages).isEmpty();
    }

    @Test
    void moduleDeclaresOnlyThePlatformContractsItUses() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"));

        assertThat(pom)
                .contains("<artifactId>common</artifactId>")
                .contains("<artifactId>shared-kernel</artifactId>")
                .contains("<artifactId>security</artifactId>")
                .doesNotContain("<artifactId>integration</artifactId>");
    }

    @Test
    void moduleDoesNotRedefinePlatformIdentifierOrTimeContracts() {
        assertThat(JAVA_ROOT.resolve(
                "application/port/out/PartnerIdGenerator.java"
        )).doesNotExist();
        assertThat(JAVA_ROOT.resolve(
                "application/port/out/IntegrationEventIdGenerator.java"
        )).doesNotExist();
    }

    @Test
    void domainRemainsFrameworkAgnostic() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT.resolve("domain"),
                List.of(
                        "import org.springframework.",
                        "import jakarta.persistence.",
                        "import jakarta.servlet.",
                        "import org.hibernate.",
                        "import tools.jackson.",
                        "import com.sixpay.partner.api.",
                        "import com.sixpay.partner.application.",
                        "import com.sixpay.partner.infrastructure.",
                        "import com.sixpay.partner.configuration."
                )
        );
    }

    @Test
    void applicationDoesNotDependOnApiOrInfrastructure() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT.resolve("application"),
                List.of(
                        "import com.sixpay.partner.api.",
                        "import com.sixpay.partner.infrastructure.",
                        "import com.sixpay.partner.configuration."
                )
        );
    }

    @Test
    void infrastructureDoesNotDependOnApiOrConfiguration() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT.resolve("infrastructure"),
                List.of(
                        "import com.sixpay.partner.api.",
                        "import com.sixpay.partner.configuration."
                )
        );
    }

    @Test
    void partnerDoesNotDependOnAnotherBusinessDomain() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT,
                List.of(
                        "import com.sixpay.customer.",
                        "import com.sixpay.subscription.",
                        "import com.sixpay.payment.",
                        "import com.sixpay.accounting.",
                        "import com.sixpay.reporting.",
                        "import com.sixpay.notification.",
                        "import com.sixpay.administration."
                )
        );
    }

    @Test
    void moduleIsNotAnExecutableSpringBootApplication() throws IOException {
        assertSourcesDoNotContain(JAVA_ROOT, List.of("@SpringBootApplication"));
    }

    private static void assertSourcesDoNotContain(Path root, List<String> forbiddenTokens) throws IOException {
        try (var paths = Files.walk(root)) {
            var violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path, forbiddenTokens).stream())
                    .toList();
            assertThat(violations).isEmpty();
        }
    }

    private static List<String> violations(Path path, List<String> forbiddenTokens) {
        try {
            var content = Files.readString(path);
            return forbiddenTokens.stream()
                    .filter(content::contains)
                    .map(token -> path + " contains forbidden token " + token)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("cannot inspect " + path, exception);
        }
    }
}
