package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentGoldenModuleStructureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/payment"
    );

    @Test
    void restControllersLiveOnlyInApiPackage()
            throws IOException {
        try (Stream<Path> paths = Files.walk(ROOT)) {
            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(
                                    "Controller.java"
                            )
                    )
                    .filter(path ->
                            !path.startsWith(
                                    ROOT.resolve("api")
                            )
                    )
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void apiUsesGoldenModulePackages()
            throws IOException {
        assertTrue(Files.isDirectory(
                ROOT.resolve("api")
        ));
        assertTrue(Files.isDirectory(
                ROOT.resolve("api/request")
        ));
        assertTrue(Files.isDirectory(
                ROOT.resolve("api/response")
        ));
        assertFalse(Files.exists(
                ROOT.resolve("infrastructure/web")
        ));
    }

    @Test
    void adaptersUseGoldenModuleNaming()
            throws IOException {
        Path persistence =
                ROOT.resolve("infrastructure/persistence");
        Path query =
                ROOT.resolve("infrastructure/query");

        assertTrue(Files.isRegularFile(
                persistence.resolve(
                        "PaymentRepositoryAdapter.java"
                )
        ));
        assertTrue(Files.isRegularFile(
                query.resolve(
                        "PaymentProjectionReadAdapter.java"
                )
        ));
        assertTrue(Files.isRegularFile(
                query.resolve(
                        "PaymentObjectAccessAdapter.java"
                )
        ));
    }

    @Test
    void applicationDoesNotDependOnApiOrInfrastructure()
            throws IOException {
        Path application = ROOT.resolve("application");

        try (Stream<Path> paths = Files.walk(application)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return List.of(
                                    "com.sixpay.payment.api.",
                                    "com.sixpay.payment.infrastructure."
                            ).stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(List.of(), violations);
        }
    }
}
