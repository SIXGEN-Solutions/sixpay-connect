package com.sixpay.payment.infrastructure.callback;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackDetachedJwsSignerTest {

    @Test
    void signsCallbackWithDetachedJwsAndKeyIdentifier()
            throws Exception {
        var pair = KeyPairGenerator
                .getInstance("RSA")
                .generateKeyPair();

        CallbackSigningKeyProvider provider = () ->
                new CallbackSigningKeyProvider.SigningKey(
                        "tresorpay-callback-2026-01",
                        "RS256",
                        pair.getPrivate()
                );

        PaymentCallbackDetachedJwsSigner signer =
                new PaymentCallbackDetachedJwsSigner(
                        JsonMapper.builder().build(),
                        provider
                );

        String jws = signer.sign(
                "{\"eventId\":\"test\"}"
                        .getBytes(StandardCharsets.UTF_8)
        );

        assertThat(jws).contains("..");
        assertThat(jws.split("\\.", -1)).hasSize(3);
        assertThat(jws.split("\\.", -1)[1]).isEmpty();
    }
}
