package com.sixpay.reporting.application.service;

import com.sixpay.reporting.application.query.RequestPaymentAuditExportCommand;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.stream.Collectors;

final class AuditExportRequestFingerprint {

    private AuditExportRequestFingerprint() {
    }

    static String compute(
            RequestPaymentAuditExportCommand command
    ) {
        String canonical = String.join(
                "\n",
                command.occurredFrom().toString(),
                command.occurredTo().toString(),
                command.paymentIds().stream()
                        .map(Object::toString)
                        .sorted()
                        .collect(Collectors.joining(",")),
                command.financialInstitutionCodes()
                        .stream()
                        .sorted()
                        .collect(Collectors.joining(",")),
                command.actions()
                        .stream()
                        .sorted()
                        .collect(Collectors.joining(",")),
                command.results()
                        .stream()
                        .map(Enum::name)
                        .sorted()
                        .collect(Collectors.joining(",")),
                command.businessPurpose(),
                command.format().name(),
                command.requestedBy()
        );

        try {
            byte[] digest = MessageDigest
                    .getInstance("SHA-256")
                    .digest(
                            canonical.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }
}
