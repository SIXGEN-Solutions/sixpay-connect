package com.sixpay.accounting.infrastructure.tfj;

import com.sixpay.accounting.infrastructure.tfj.security.AmplitudeTfjSignatureProperties;
import com.sixpay.accounting.infrastructure.tfj.security.AmplitudeTfjSignatureVerifier;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AmplitudeTfjSignatureVerifierTest {

    private static final Instant NOW =
            Instant.parse("2026-09-08T22:00:00Z");

    @Test
    void verifiesApprovedContractSignatureFormula()
            throws Exception {
        byte[] secret =
                "test-secret-key".getBytes(
                        StandardCharsets.UTF_8
                );
        byte[] body =
                "{\"schemaVersion\":\"1.0\"}"
                        .getBytes(
                                StandardCharsets.UTF_8
                        );
        String timestamp =
                Long.toString(NOW.getEpochSecond());

        String bodyDigest =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(
                                MessageDigest
                                        .getInstance("SHA-256")
                                        .digest(body)
                        );

        String signedPayload =
                timestamp
                        + ".POST."
                        + AmplitudeTfjSignatureVerifier
                                .CALLBACK_TARGET
                        + "."
                        + bodyDigest;

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(
                new SecretKeySpec(
                        secret,
                        "HmacSHA256"
                )
        );

        String signature =
                "v1="
                        + Base64.getUrlEncoder()
                                .withoutPadding()
                                .encodeToString(
                                        mac.doFinal(
                                                signedPayload
                                                        .getBytes(
                                                                StandardCharsets.UTF_8
                                                        )
                                        )
                                );

        var verifier =
                new AmplitudeTfjSignatureVerifier(
                        new AmplitudeTfjSignatureProperties(
                                true,
                                Map.of(
                                        "key-1",
                                        Base64.getEncoder()
                                                .encodeToString(
                                                        secret
                                                )
                                )
                        ),
                        Clock.fixed(
                                NOW,
                                ZoneOffset.UTC
                        )
                );

        assertDoesNotThrow(
                () -> verifier.verify(
                        body,
                        signature,
                        "key-1",
                        timestamp
                )
        );
    }

    @Test
    void rejectsTimestampOlderThanFiveMinutes() {
        var verifier =
                new AmplitudeTfjSignatureVerifier(
                        new AmplitudeTfjSignatureProperties(
                                true,
                                Map.of(
                                        "key-1",
                                        Base64.getEncoder()
                                                .encodeToString(
                                                        "secret"
                                                                .getBytes(
                                                                        StandardCharsets.UTF_8
                                                                )
                                                )
                                )
                        ),
                        Clock.fixed(
                                NOW,
                                ZoneOffset.UTC
                        )
                );

        assertThrows(
                SecurityException.class,
                () -> verifier.verify(
                        new byte[0],
                        "v1=invalid",
                        "key-1",
                        Long.toString(
                                NOW.minus(
                                                Duration
                                                        .ofMinutes(6)
                                        )
                                        .getEpochSecond()
                        )
                )
        );
    }
}
