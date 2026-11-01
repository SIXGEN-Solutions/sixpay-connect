package com.sixpay.customer.management.infrastructure.banking.amplitude;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class RestAmplitudeCustomerLookupClientTest {
    private static MockResponse json(String body) {
        return new MockResponse().setResponseCode(200)
                .addHeader("Content-Type", "application/json").setBody(body);
    }

    @Test
    void looksUpCustomerAndAccountThroughApprovedGetOperations() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(json("""
                    {"customers":[{"customerReference":"C-1","customerNumber":"123",
                    "financialInstitutionCode":"LRB","niu":"N-1","legalName":"Customer One"}],"resultCount":1}
                    """));
            server.enqueue(json("""
                    {"customerReference":"C-1","customerNumber":"123","financialInstitutionCode":"LRB",
                    "niu":"N-1","legalName":"Customer One","email":"c@example.com","phoneNumber":"+237600000000",
                    "kycStatus":"COMPLETE","kycFields":[],"source":"AMPLITUDE","retrievedAt":"2026-10-01T12:00:00Z"}
                    """));
            server.enqueue(json("""
                    {"accounts":[{"accountReference":"A-1","customerReference":"C-1","financialInstitutionCode":"LRB",
                    "maskedAccountIdentifier":"****0001","currency":"XAF","accountType":"CURRENT","status":"ACTIVE",
                    "restrictions":[],"source":"AMPLITUDE","retrievedAt":"2026-10-01T12:00:00Z"}],"resultCount":1}
                    """));
            var client = new RestAmplitudeCustomerLookupClient(
                    RestClient.builder().baseUrl(server.url("/").toString()).build());
            var result = client.lookup(new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                    "LRB", "N-1", null, "A-1", UUID.randomUUID().toString()));
            assertThat(result.customerReference()).isEqualTo("C-1");
            assertThat(result.account().accountReference()).isEqualTo("A-1");
            var search = server.takeRequest();
            assertThat(search.getMethod()).isEqualTo("GET");
            assertThat(search.getPath()).startsWith("/api/v1/customers?");
            assertThat(search.getHeader("Authorization")).isNull();
            assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/customers/C-1");
            assertThat(server.takeRequest().getPath()).isEqualTo("/api/v1/customers/C-1/accounts");
        }
    }

    @Test
    void refusesAmbiguousCustomerWithoutFurtherCalls() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.start();
            server.enqueue(json("{\"customers\":[],\"resultCount\":0}"));
            var client = new RestAmplitudeCustomerLookupClient(
                    RestClient.builder().baseUrl(server.url("/").toString()).build());
            assertThatThrownBy(() -> client.lookup(new BankingCustomerLookupPort.BankingCustomerLookupQuery(
                    "LRB", "N-1", null, "A-1", "corr-1")))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(server.getRequestCount()).isEqualTo(1);
        }
    }
}
