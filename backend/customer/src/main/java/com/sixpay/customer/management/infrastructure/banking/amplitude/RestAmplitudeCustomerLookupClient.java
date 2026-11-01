package com.sixpay.customer.management.infrastructure.banking.amplitude;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeBankAccountResponse;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerIdentityResponse;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

/** Read-only lookup over the approved Amplitude Customer and Account GET contract. */
public final class RestAmplitudeCustomerLookupClient implements AmplitudeCustomerLookupClient {
    private final RestClient restClient;

    public RestAmplitudeCustomerLookupClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient);
    }

    @Override
    public BankingCustomerLookupPort.BankingCustomerProfile lookup(
            BankingCustomerLookupPort.BankingCustomerLookupQuery query) {
        Objects.requireNonNull(query, "query is required");
        if (blank(query.niu()) && blank(query.customerNumber())) {
            throw new IllegalArgumentException("niu or customerNumber is required");
        }
        if (blank(query.accountReference())) {
            throw new IllegalArgumentException("accountReference is required");
        }
        SearchResult matches = restClient.get()
                .uri(uri -> {
                    var b = uri.path("/api/v1/customers")
                            .queryParam("financialInstitutionCode", query.financialInstitutionCode());
                    if (!blank(query.niu())) b.queryParam("niu", query.niu());
                    if (!blank(query.customerNumber())) b.queryParam("customerNumber", query.customerNumber());
                    return b.build();
                })
                .headers(h -> authenticate(h, query.correlationId()))
                .retrieve().body(SearchResult.class);
        if (matches == null || matches.customers() == null || matches.customers().size() != 1) {
            throw new IllegalStateException("Banking customer search must identify exactly one customer");
        }
        CustomerSummary summary = matches.customers().get(0);
        if (!query.financialInstitutionCode().equals(summary.financialInstitutionCode())
                || (!blank(query.niu()) && !query.niu().equals(summary.niu()))
                || (!blank(query.customerNumber()) && !query.customerNumber().equals(summary.customerNumber()))) {
            throw new IllegalStateException("Banking customer search identity mismatch");
        }
        AmplitudeCustomerIdentityResponse identity = restClient.get()
                .uri(uri -> uri.path("/api/v1/customers/{customerReference}")
                        .build(summary.customerReference()))
                .headers(h -> { authenticate(h, query.correlationId());
                    h.set("X-Financial-Institution-Code", query.financialInstitutionCode()); })
                .retrieve().body(AmplitudeCustomerIdentityResponse.class);
        if (identity == null || !summary.customerReference().equals(identity.customerReference())
                || !query.financialInstitutionCode().equals(identity.financialInstitutionCode())
                || (!blank(query.niu()) && !query.niu().equals(identity.niu()))
                || (!blank(query.customerNumber()) && !query.customerNumber().equals(identity.customerNumber()))) {
            throw new IllegalStateException("Banking customer identity mismatch");
        }
        AccountSearchResult accounts = restClient.get()
                .uri(uri -> uri.path("/api/v1/customers/{customerReference}/accounts")
                        .build(summary.customerReference()))
                .headers(h -> { authenticate(h, query.correlationId());
                    h.set("X-Financial-Institution-Code", query.financialInstitutionCode()); })
                .retrieve().body(AccountSearchResult.class);
        if (accounts == null || accounts.accounts() == null) {
            throw new IllegalStateException("Banking account search response is missing");
        }
        List<AmplitudeBankAccountResponse> selected = accounts.accounts().stream()
                .filter(a -> query.accountReference().equals(a.accountReference())).toList();
        if (selected.size() != 1) {
            throw new IllegalStateException("Banking account reference must identify exactly one account");
        }
        var account = selected.get(0);
        if (!summary.customerReference().equals(account.customerReference())
                || !query.financialInstitutionCode().equals(account.financialInstitutionCode())) {
            throw new IllegalStateException("Banking account ownership mismatch");
        }
        return new BankingCustomerLookupPort.BankingCustomerProfile(
                identity.financialInstitutionCode(), identity.customerReference(),
                identity.customerNumber(), identity.niu(), identity.legalName(),
                identity.email(), identity.phoneNumber(),
                new BankingCustomerLookupPort.BankingAccount(
                        account.accountReference(), null, null,
                        account.maskedAccountIdentifier(), account.currency(),
                        account.accountType(), account.retrievedAt()));
    }

    private void authenticate(HttpHeaders headers, String correlationId) {
        headers.set(IntegrationHttpHeaders.CORRELATION_ID, correlationId);
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    public record SearchResult(List<CustomerSummary> customers, Integer resultCount) {}
    public record CustomerSummary(String customerReference, String customerNumber,
                                  String financialInstitutionCode, String niu, String legalName) {}
    public record AccountSearchResult(List<AmplitudeBankAccountResponse> accounts, Integer resultCount) {}
}
