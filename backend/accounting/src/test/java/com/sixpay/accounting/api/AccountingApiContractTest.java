package com.sixpay.accounting.api;

import com.sixpay.accounting.api.response.AccountingBatchDetailResponse;
import com.sixpay.accounting.api.response.AccountingBatchItemResponse;
import com.sixpay.accounting.api.response.AccountingBatchPageResponse;
import com.sixpay.accounting.api.response.AccountingBatchSummaryResponse;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.lang.reflect.RecordComponent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountingApiContractTest {

    private static final String CONTRACT =
            "documentation/contracts/internal/"
                    + "accounting-query-api-v1.yaml";

    @Test
    void freezesAccountingQueryPathsAndOperationIds()
            throws IOException {

        Map<String, Object> paths =
                map(contract().get("paths"));

        assertThat(paths).containsOnlyKeys(
                "/internal/api/v1/accounting-batches",
                "/internal/api/v1/accounting-batches/{batchId}"
        );

        assertOperation(
                paths,
                "/internal/api/v1/accounting-batches",
                "get",
                "searchAccountingBatches"
        );

        assertOperation(
                paths,
                "/internal/api/v1/accounting-batches/{batchId}",
                "get",
                "getAccountingBatch"
        );
    }

    @Test
    void freezesAccountingQueryStatuses()
            throws IOException {

        Map<String, Object> components =
                map(contract().get("components"));

        Map<String, Object> schemas =
                map(components.get("schemas"));

        Map<String, Object> status =
                map(schemas.get("AccountingBatchStatus"));

        assertThat(list(status.get("enum")))
                .containsExactly(
                        "COMPLETED",
                        "NOT_COMPLETED"
                );
    }

    @Test
    void freezesAccountingResponseFieldNames() {
        assertRecordFields(
                AccountingBatchSummaryResponse.class,
                "batchId",
                "businessDate",
                "financialInstitutionCode",
                "status",
                "itemCount",
                "createdAt"
        );

        assertRecordFields(
                AccountingBatchPageResponse.class,
                "content",
                "page",
                "size",
                "totalElements",
                "totalPages"
        );

        assertRecordFields(
                AccountingBatchDetailResponse.class,
                "batchId",
                "idempotencyKey",
                "businessDate",
                "financialInstitutionCode",
                "status",
                "itemCount",
                "createdAt",
                "items"
        );

        assertRecordFields(
                AccountingBatchItemResponse.class,
                "paymentId",
                "publicPaymentReference",
                "partnerId",
                "amount",
                "currency",
                "paymentOccurredAt",
                "paymentBusinessDate",
                "bankPostingReference",
                "tresorPayStatus",
                "tresorPayStatusCheckedAt",
                "status"
        );
    }

    private static void assertOperation(
            Map<String, Object> paths,
            String path,
            String method,
            String operationId
    ) {
        Map<String, Object> pathItem =
                map(paths.get(path));

        Map<String, Object> operation =
                map(pathItem.get(method));

        assertThat(operation)
                .containsEntry(
                        "operationId",
                        operationId
                );
    }

    private static void assertRecordFields(
            Class<?> type,
            String... expectedFields
    ) {
        List<String> fields =
                Arrays.stream(
                                type.getRecordComponents()
                        )
                        .map(
                                RecordComponent::getName
                        )
                        .toList();

        assertThat(fields)
                .containsExactly(
                        expectedFields
                );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contract()
            throws IOException {

        Path root = repositoryRoot();
        Path contract = root.resolve(CONTRACT);

        assertThat(contract)
                .as("Accounting Query OpenAPI contract")
                .isRegularFile();

        try (var reader =
                     Files.newBufferedReader(contract)) {
            return new Yaml().load(reader);
        }
    }

    private static Path repositoryRoot() {
        Path current =
                Path.of("")
                        .toAbsolutePath()
                        .normalize();

        while (current != null) {
            if (Files.isRegularFile(
                    current.resolve(
                            "ENGINEERING_CONTEXT.md"
                    )
            )) {
                return current;
            }

            current = current.getParent();
        }

        throw new IllegalStateException(
                "Cannot locate repository root"
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(
            Object value
    ) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private static List<String> list(
            Object value
    ) {
        return (List<String>) value;
    }
}
