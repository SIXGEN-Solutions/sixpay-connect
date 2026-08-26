package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryRestApiArchitectureTest {

    private static final Path API_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/api"
    );

    @Test
    void apiContainsTheApprovedLot475Types()
            throws Exception {

        assertEquals(
                Set.of(
                        "ObservedCustomerQueryController.java",
                        "package-info.java"
                ),
                javaFiles(
                        API_ROOT.resolve("controller")
                )
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerSummaryResponse.java",
                        "ObservedCustomerDetailResponse.java",
                        "ObservedCustomerSearchPageResponse.java",
                        "ObservedCustomerPaymentResponse.java",
                        "ObservedCustomerPaymentPageResponse.java",
                        "InstitutionObservationResponse.java",
                        "MaskedAccountReferenceResponse.java",
                        "MaskedIdentifierResponse.java",
                        "package-info.java"
                ),
                javaFiles(
                        API_ROOT.resolve("dto")
                )
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerQueryApiMapper.java",
                        "package-info.java"
                ),
                javaFiles(
                        API_ROOT.resolve("mapper")
                )
        );

        assertEquals(
                Set.of(
                        "ObservedCustomerQueryExceptionHandler.java",
                        "ObservedCustomerNotFoundException.java",
                        "package-info.java"
                ),
                javaFiles(
                        API_ROOT.resolve("error")
                )
        );
    }

    @Test
    void controllerExposesOnlyTheThreeInternalGetEndpoints()
            throws Exception {

        String source = normalized(
                API_ROOT.resolve(
                        "controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        for (String required : List.of(
                "@RestController",
                "@RequestMapping("
                        + "\"/internal/api/v1/observed-customers\""
                        + ")",
                "@GetMapping",
                "@GetMapping(\"/{observedCustomerId}\")",
                "@GetMapping("
                        + "\"/{observedCustomerId}/payments\""
                        + ")",
                "SearchObservedCustomersUseCase",
                "GetObservedCustomerUseCase",
                "ListObservedCustomerPaymentsUseCase",
                "searchUseCase.search(query)",
                "getUseCase.get(",
                "paymentsUseCase.listPayments(query)",
                "mapper.toResponse("
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing REST orchestration: "
                            + required
            );
        }
    }

    @Test
    void controllerContainsNoPersistenceProtectionOrPaymentDependency()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        for (String forbidden : List.of(
                "EntityManager",
                "JdbcTemplate",
                "jakarta.persistence",
                "ObservedCustomerDataProtector",
                ".reveal(",
                ".searchHash(",
                "accountBindingFingerprint",
                "import com.sixpay.payment.",
                "Amplitude",
                "createNativeQuery",
                "Repository"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden controller dependency: "
                            + forbidden
            );
        }
    }

    @Test
    void mapperOwnsResponseMappingAndNoMaskingAlgorithm()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "mapper/"
                                + "ObservedCustomerQueryApiMapper.java"
                )
        );

        assertTrue(source.contains(
                "ObservedCustomerSearchPageResponse"
        ));
        assertTrue(source.contains(
                "ObservedCustomerDetailResponse"
        ));
        assertTrue(source.contains(
                "ObservedCustomerPaymentPageResponse"
        ));

        for (String forbidden : List.of(
                "replaceAll(",
                "substring(",
                "repeat(",
                "ObservedCustomerDataProtector",
                "accountBindingFingerprint",
                "import com.sixpay.payment."
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "API mapper applies forbidden logic: "
                            + forbidden
            );
        }
    }

    @Test
    void exceptionHandlerSeparates400404And503()
            throws Exception {

        String source = Files.readString(
                API_ROOT.resolve(
                        "error/"
                                + "ObservedCustomerQueryExceptionHandler.java"
                )
        );

        assertTrue(source.contains("HttpStatus.BAD_REQUEST"));
        assertTrue(source.contains("HttpStatus.NOT_FOUND"));
        assertTrue(source.contains(
                "HttpStatus.SERVICE_UNAVAILABLE"
        ));
        assertTrue(source.contains(
                "InvalidObservedCustomerCursorException"
        ));
        assertTrue(source.contains(
                "ObservedCustomerQueryUnavailableException"
        ));

        assertFalse(source.contains("normalizedNiu"));
        assertFalse(source.contains("legalName"));
        assertFalse(source.contains("payload"));
    }

    private static Set<String> javaFiles(
            Path root
    ) throws Exception {

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }

    private static String normalized(Path path)
            throws Exception {
        return Files.readString(path)
                .replaceAll("\\s+", "");
    }
}
