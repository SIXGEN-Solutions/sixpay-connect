package com.sixpay.integration.http;

import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;

public final class StandardRestClientFactory {
    private final RestClient.Builder builder;
    public StandardRestClientFactory(RestClient.Builder builder) {
        this.builder = Objects.requireNonNull(builder);
    }
    public RestClient create(
            URI baseUrl,
            HttpTimeoutPolicy timeoutPolicy,
            SSLContext sslContext,
            List<ClientHttpRequestInterceptor> interceptors
    ) {
        Objects.requireNonNull(baseUrl, "baseUrl is required");
        Objects.requireNonNull(timeoutPolicy, "timeoutPolicy is required");
        if (!"https".equalsIgnoreCase(baseUrl.getScheme())) {
            throw new IllegalArgumentException("External baseUrl must use HTTPS");
        }
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(timeoutPolicy.connectTimeout());
        if (sslContext != null) clientBuilder.sslContext(sslContext);
        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(clientBuilder.build());
        requestFactory.setReadTimeout(timeoutPolicy.readTimeout());
        List<ClientHttpRequestInterceptor> safe =
                interceptors == null ? List.of() : List.copyOf(interceptors);
        return builder.clone()
                .baseUrl(baseUrl.toString())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptors(items -> items.addAll(safe))
                .build();
    }
}
