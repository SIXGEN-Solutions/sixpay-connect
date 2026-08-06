package com.sixpay.customer.verification.infrastructure.banking.http;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sixpay.customer.verification.application.exception.*;
import com.sixpay.customer.verification.application.port.output.BankingVerificationResponse;
import com.sixpay.customer.verification.domain.model.VerificationCheckResult;
import com.sixpay.customer.verification.domain.model.VerificationCheckType;
import com.sixpay.customer.verification.infrastructure.banking.retry.RetryingBankingCustomerVerificationAdapter;
import com.sixpay.customer.verification.infrastructure.banking.support.BankingVerificationHttpTestSupport;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreBankingHttpIntegrationTest {

    private static final String VERIFICATION_PATH =
            "/v1/accounts/verify";

    private HttpServer server;
    private URI baseUrl;

    private final AtomicInteger calls =
            new AtomicInteger();

    private final List<String> correlations =
            new CopyOnWriteArrayList<>();

    private final List<String> requestBodies =
            new CopyOnWriteArrayList<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(
                        "127.0.0.1",
                        0
                ),
                0
        );

        server.setExecutor(
                Executors.newCachedThreadPool()
        );

        server.start();

        baseUrl = URI.create(
                "http://127.0.0.1:"
                        + server.getAddress().getPort()
        );
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void completePositiveResponseMapsAllElevenChecks() {
        register(index -> Response.json(
                200,
                BankingVerificationHttpTestSupport
                        .successJson()
        ));

        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        BankingVerificationResponse response =
                adapter(
                        registry,
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                );

        assertEquals(
                1,
                calls.get()
        );

        assertEquals(
                11,
                response.checks().size()
        );

        assertTrue(
                response.checks()
                        .stream()
                        .allMatch(check ->
                                check.result()
                                        == VerificationCheckResult.PASS
                        )
        );

        assertEquals(
                BankingVerificationHttpTestSupport
                        .CORRELATION_ID,
                correlations.getFirst()
        );
    }

    @Test
    void businessRejectionReturnsFailAndIsNeverRetried() {
        register(index -> Response.json(
                200,
                BankingVerificationHttpTestSupport
                        .businessFailureJson()
        ));

        BankingVerificationResponse response =
                adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                );

        assertEquals(
                1,
                calls.get()
        );

        assertEquals(
                VerificationCheckResult.FAIL,
                response.checks()
                        .stream()
                        .filter(check ->
                                check.type()
                                        == VerificationCheckType
                                        .ACCOUNT_EXISTS
                        )
                        .findFirst()
                        .orElseThrow()
                        .result()
        );
    }

    @Test
    void partialResponseIsNonRetryableInvalidResponseFailure() {
        register(index -> Response.json(
                200,
                BankingVerificationHttpTestSupport
                        .partialJson()
        ));

        assertThrows(
                BankingVerificationInvalidResponseException.class,
                () -> adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                )
        );

        assertEquals(
                1,
                calls.get()
        );
    }
    @Test
    void readTimeoutIsRetriedAndExhausted() {
        register(index -> Response.delayedJson(
                200,
                BankingVerificationHttpTestSupport
                        .successJson(),
                Duration.ofMillis(250)
        ));

        assertThrows(
                BankingVerificationTimeoutException.class,
                () -> adapter(
                        new SimpleMeterRegistry(),
                        2,
                        Duration.ofMillis(50)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                )
        );

        assertEquals(
                2,
                calls.get()
        );
    }

    @Test
    void http400And401AreNotRetried() {
        register(index -> Response.problem(
                400,
                BankingVerificationHttpTestSupport
                        .problemJson(
                                400,
                                "INVALID_REQUEST",
                                false
                        )
        ));

        assertThrows(
                BankingVerificationProtocolException.class,
                () -> adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                )
        );

        assertEquals(
                1,
                calls.get()
        );

        resetCapture();

        replaceContext(index -> Response.problem(
                401,
                BankingVerificationHttpTestSupport
                        .problemJson(
                                401,
                                "INVALID_TOKEN",
                                false
                        )
        ));

        assertThrows(
                BankingVerificationAuthenticationException.class,
                () -> adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                )
        );

        assertEquals(
                1,
                calls.get()
        );
    }

    @Test
    void http503RetriesThenSucceeds() {
        register(index -> index < 3
                        ? Response.problem(
                        503,
                        BankingVerificationHttpTestSupport
                                .problemJson(
                                        503,
                                        "TEMPORARILY_UNAVAILABLE",
                                        true
                                )
                )
                        : Response.json(
                        200,
                        BankingVerificationHttpTestSupport
                                .successJson()
                )
        );

        BankingVerificationResponse response =
                adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                );

        assertEquals(
                3,
                calls.get()
        );

        assertEquals(
                11,
                response.checks().size()
        );
    }

    @Test
    void http503ExhaustsConfiguredAttempts() {
        register(index -> Response.problem(
                503,
                BankingVerificationHttpTestSupport
                        .problemJson(
                                503,
                                "TEMPORARILY_UNAVAILABLE",
                                true
                        )
        ));

        assertThrows(
                BankingVerificationUnavailableException.class,
                () -> adapter(
                        new SimpleMeterRegistry(),
                        3,
                        Duration.ofSeconds(1)
                ).verify(
                        BankingVerificationHttpTestSupport
                                .query()
                )
        );

        assertEquals(
                3,
                calls.get()
        );
    }

    @Test
    void metricsCountRequestsRetriesErrorsAndLatency() {
        register(index -> index == 1
                        ? Response.problem(
                        503,
                        BankingVerificationHttpTestSupport
                                .problemJson(
                                        503,
                                        "TEMPORARILY_UNAVAILABLE",
                                        true
                                )
                )
                        : Response.json(
                        200,
                        BankingVerificationHttpTestSupport
                                .successJson()
                )
        );

        SimpleMeterRegistry registry =
                new SimpleMeterRegistry();

        adapter(
                registry,
                3,
                Duration.ofSeconds(1)
        ).verify(
                BankingVerificationHttpTestSupport
                        .query()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.requests"
                        )
                        .tag(
                                "institution",
                                "AMPLITUDE"
                        )
                        .tag(
                                "outcome",
                                "success"
                        )
                        .counter()
                        .count()
        );

        assertEquals(
                1.0,
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.retries"
                        )
                        .tag(
                                "institution",
                                "AMPLITUDE"
                        )
                        .counter()
                        .count()
        );

        assertNotNull(
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.duration"
                        )
                        .tag(
                                "institution",
                                "AMPLITUDE"
                        )
                        .tag(
                                "outcome",
                                "success"
                        )
                        .timer()
        );

        assertTrue(
                registry.get(
                                "sixpay.customer.verification."
                                        + "banking.duration"
                        )
                        .tag(
                                "institution",
                                "AMPLITUDE"
                        )
                        .tag(
                                "outcome",
                                "success"
                        )
                        .timer()
                        .totalTime(
                                TimeUnit.NANOSECONDS
                        ) > 0
        );
    }

    @Test
    void operationalLogsContainNoSensitivePayloadValues() {
        register(index -> Response.json(
                200,
                BankingVerificationHttpTestSupport
                        .successJson()
        ));

        Logger logger =
                (Logger) LoggerFactory.getLogger(
                        RetryingBankingCustomerVerificationAdapter.class
                );

        ListAppender<ILoggingEvent> appender =
                new ListAppender<>();

        appender.start();
        logger.addAppender(appender);

        try {
            adapter(
                    new SimpleMeterRegistry(),
                    1,
                    Duration.ofSeconds(1)
            ).verify(
                    BankingVerificationHttpTestSupport
                            .query()
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        String logs = appender.list
                .stream()
                .map(
                        ILoggingEvent::getFormattedMessage
                )
                .reduce(
                        "",
                        (left, right) ->
                                left + "\n" + right
                );

        assertFalse(
                logs.contains(
                        BankingVerificationHttpTestSupport
                                .ACCOUNT_REFERENCE
                )
        );

        assertFalse(
                logs.contains(
                        BankingVerificationHttpTestSupport
                                .NIU
                )
        );

        assertFalse(
                logs.contains(
                        BankingVerificationHttpTestSupport
                                .LEGAL_NAME
                )
        );

        assertFalse(
                logs.contains(
                        "test-access-token"
                )
        );

        assertTrue(
                logs.contains(
                        BankingVerificationHttpTestSupport
                                .CORRELATION_ID
                )
        );
    }

    @Test
    void rawSensitiveRequestIsSentOnlyToTheBankingEndpoint() {
        register(index -> Response.json(
                200,
                BankingVerificationHttpTestSupport
                        .successJson()
        ));

        adapter(
                new SimpleMeterRegistry(),
                1,
                Duration.ofSeconds(1)
        ).verify(
                BankingVerificationHttpTestSupport
                        .query()
        );

        assertEquals(
                1,
                requestBodies.size()
        );

        assertTrue(
                requestBodies.getFirst()
                        .contains(
                                BankingVerificationHttpTestSupport
                                        .ACCOUNT_REFERENCE
                        )
        );

        assertTrue(
                requestBodies.getFirst()
                        .contains(
                                BankingVerificationHttpTestSupport
                                        .NIU
                        )
        );
    }

    private RetryingBankingCustomerVerificationAdapter adapter(
            SimpleMeterRegistry registry,
            int maxAttempts,
            Duration readTimeout
    ) {
        return BankingVerificationHttpTestSupport
                .realHttpAdapter(
                        baseUrl,
                        readTimeout,
                        maxAttempts,
                        registry,
                        duration -> {
                            // No real backoff in integration tests.
                        }
                );
    }

    private void register(
            IntFunction<Response> responseProvider
    ) {
        server.createContext(
                VERIFICATION_PATH,
                exchange -> handle(
                        exchange,
                        responseProvider
                )
        );
    }

    private void replaceContext(
            IntFunction<Response> responseProvider
    ) {
        server.removeContext(
                VERIFICATION_PATH
        );

        register(responseProvider);
    }

    private void resetCapture() {
        calls.set(0);
        correlations.clear();
        requestBodies.clear();
    }

    private void handle(
            HttpExchange exchange,
            IntFunction<Response> responseProvider
    ) throws IOException {

        int index = calls.incrementAndGet();

        correlations.add(
                exchange.getRequestHeaders()
                        .getFirst(
                                "X-Correlation-ID"
                        )
        );

        requestBodies.add(
                new String(
                        exchange.getRequestBody()
                                .readAllBytes(),
                        StandardCharsets.UTF_8
                )
        );

        Response response =
                responseProvider.apply(index);

        applyDelay(response.delay());

        byte[] payload = response.body()
                .getBytes(
                        StandardCharsets.UTF_8
                );

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        response.contentType()
                );

        try {
            exchange.sendResponseHeaders(
                    response.status(),
                    payload.length
            );

            exchange.getResponseBody()
                    .write(payload);
        } catch (IOException ignored) {
            /*
             * Expected when the RestClient reaches its read
             * timeout and closes the socket before the test
             * server sends its delayed response.
             */
        } finally {
            exchange.close();
        }
    }

    private static void applyDelay(
            Duration delay
    ) {
        if (delay.isZero()) {
            return;
        }

        try {
            Thread.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                    .interrupt();

            throw new IllegalStateException(
                    "Test server delay interrupted",
                    exception
            );
        }
    }

    private record Response(
            int status,
            String body,
            String contentType,
            Duration delay
    ) {

        private Response {
            if (status < 100 || status > 599) {
                throw new IllegalArgumentException(
                        "Invalid HTTP status"
                );
            }

            body = body == null
                    ? ""
                    : body;

            if (contentType == null
                    || contentType.isBlank()) {
                throw new IllegalArgumentException(
                        "contentType is required"
                );
            }

            if (delay == null
                    || delay.isNegative()) {
                throw new IllegalArgumentException(
                        "delay must be non-negative"
                );
            }
        }

        private static Response json(
                int status,
                String body
        ) {
            return new Response(
                    status,
                    body,
                    "application/json",
                    Duration.ZERO
            );
        }

        private static Response problem(
                int status,
                String body
        ) {
            return new Response(
                    status,
                    body,
                    "application/problem+json",
                    Duration.ZERO
            );
        }

        private static Response delayedJson(
                int status,
                String body,
                Duration delay
        ) {
            return new Response(
                    status,
                    body,
                    "application/json",
                    delay
            );
        }
    }
}