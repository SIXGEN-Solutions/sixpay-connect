package com.sixpay.partner.api;

import com.sixpay.partner.api.response.PartnerAuditPageResponse;
import com.sixpay.partner.api.response.PartnerAuditResponse;
import com.sixpay.partner.api.response.PartnerConnectionInfoResponse;
import com.sixpay.partner.api.response.PartnerResponse;
import com.sixpay.partner.api.response.PartnerStatusResponse;
import com.sixpay.partner.api.response.ValidationThresholdResponse;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStreamReader;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PartnerApiContractTest {

    private static final String CONTRACT =
            "openapi/partner-api-v1.yaml";

    @Test
    void freezesPartnerPathsAndOperationIds() {
        Map<String, Object> contract = contract();
        Map<String, Object> paths = map(contract.get("paths"));

        assertThat(paths).containsOnlyKeys(
                "/api/v1/partners",
                "/api/v1/partners/{partnerId}",
                "/api/v1/partners/{partnerId}/validation",
                "/api/v1/partners/{partnerId}/suspension",
                "/api/v1/partners/{partnerId}/reactivation",
                "/api/v1/partners/{partnerId}/validation-thresholds/{transactionType}",
                "/api/v1/partners/{partnerId}/status",
                "/api/v1/partners/{partnerId}/audit"
        );

        assertOperation(paths, "/api/v1/partners", "post", "createPartner");
        assertOperation(paths, "/api/v1/partners/{partnerId}", "get", "getPartner");
        assertOperation(paths, "/api/v1/partners/{partnerId}/validation", "post", "decidePartner");
        assertOperation(paths, "/api/v1/partners/{partnerId}/suspension", "post", "suspendPartner");
        assertOperation(paths, "/api/v1/partners/{partnerId}/reactivation", "post", "reactivatePartner");
        assertOperation(
                paths,
                "/api/v1/partners/{partnerId}/validation-thresholds/{transactionType}",
                "put",
                "configurePartnerValidationThreshold"
        );
        assertOperation(paths, "/api/v1/partners/{partnerId}/status", "get", "getPartnerStatus");
        assertOperation(paths, "/api/v1/partners/{partnerId}/audit", "get", "getPartnerAuditTrail");
    }

    @Test
    void freezesPublicResponseFieldNames() {
        assertRecordFields(PartnerResponse.class,
                "id",
                "legalName",
                "technicalContactName",
                "technicalContactEmail",
                "authorizedTransactionTypes",
                "status",
                "statusReason",
                "validationThresholds",
                "createdAt",
                "updatedAt");
        assertRecordFields(ValidationThresholdResponse.class,
                "transactionType", "currency", "amount", "validationLevels");
        assertRecordFields(PartnerStatusResponse.class,
                "partnerId", "status", "statusReason", "connection", "updatedAt");
        assertRecordFields(PartnerConnectionInfoResponse.class,
                "apiBasePath", "supportedAuthenticationMethods", "newTransactionsAllowed");
        assertRecordFields(PartnerAuditPageResponse.class,
                "items", "page", "size", "totalElements", "totalPages");
        assertRecordFields(PartnerAuditResponse.class,
                "partnerId",
                "action",
                "result",
                "actorId",
                "correlationId",
                "details",
                "occurredAt");
    }

    @Test
    void freezesRequiredMutationHeaders() {
        Map<String, Object> components = map(contract().get("components"));
        Map<String, Object> parameters = map(components.get("parameters"));
        Map<String, Object> idempotency = map(parameters.get("RequiredIdempotencyKey"));
        Map<String, Object> idempotencySchema = map(idempotency.get("schema"));
        Map<String, Object> correlation = map(parameters.get("OptionalCorrelationId"));
        Map<String, Object> correlationSchema = map(correlation.get("schema"));

        assertThat(idempotency)
                .containsEntry("name", "Idempotency-Key")
                .containsEntry("in", "header")
                .containsEntry("required", true);
        assertThat(idempotencySchema)
                .containsEntry("minLength", 1)
                .containsEntry("maxLength", 150);
        assertThat(correlation)
                .containsEntry("name", "X-Correlation-ID")
                .containsEntry("required", false);
        assertThat(correlationSchema).containsEntry("maxLength", 150);
    }

    private static void assertOperation(
            Map<String, Object> paths,
            String path,
            String method,
            String operationId
    ) {
        Map<String, Object> pathItem = map(paths.get(path));
        Map<String, Object> operation = map(pathItem.get(method));
        assertThat(operation).containsEntry("operationId", operationId);
    }

    private static void assertRecordFields(Class<?> type, String... expectedFields) {
        List<String> fields = Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .toList();
        assertThat(fields).containsExactly(expectedFields);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contract() {
        var stream = PartnerApiContractTest.class.getClassLoader()
                .getResourceAsStream(CONTRACT);
        assertThat(stream)
                .as("frozen Partner OpenAPI contract")
                .isNotNull();
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return new Yaml().load(reader);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("cannot read Partner API contract", exception);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }
}
