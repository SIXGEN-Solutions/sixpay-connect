package com.sixpay.customer.management.infrastructure.banking.amplitude;

import com.sixpay.customer.management.application.port.output.BankingCustomerLookupPort;
import com.sixpay.customer.verification.infrastructure.banking.dto.BankAccount;
import com.sixpay.customer.verification.infrastructure.banking.dto.AmplitudeCustomerIdentityResponse;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.time.Instant;
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

        SearchResult matches = restClient.get()
                .uri(uri -> {
                    var builder = uri.path("/api/v1/customers")
                            .queryParam("financialInstitutionCode", query.financialInstitutionCode());
                    if (!blank(query.niu())) builder.queryParam("niu", query.niu());
                    if (!blank(query.customerNumber())) {
                        builder.queryParam("customerNumber", query.customerNumber());
                    }
                    return builder.build();
                })
                .headers(h -> authenticate(h, query.correlationId()))
                .retrieve().body(SearchResult.class);
        if (matches == null || matches.customers() == null
                || matches.customers().size() != 1) {
            throw new IllegalStateException(
                    "Banking customer search must identify exactly one customer");
        }
        CustomerSummary summary = matches.customers().getFirst();
        if (!query.financialInstitutionCode().equals(summary.financialInstitutionCode())
                || (!blank(query.niu()) && !query.niu().equals(summary.niu()))
                || (!blank(query.customerNumber())
                    && !query.customerNumber().equals(summary.customerNumber()))) {
            throw new IllegalStateException("Banking customer search identity mismatch");
        }

        AmplitudeCustomerIdentityResponse identity = restClient.get()
                .uri(uri -> uri.path("/api/v1/customers/{customerReference}")
                        .build(summary.customerReference()))
                .headers(h -> {
                    authenticate(h, query.correlationId());
                    h.set("X-Financial-Institution-Code", query.financialInstitutionCode());
                })
                .retrieve().body(AmplitudeCustomerIdentityResponse.class);
        if (identity == null || !summary.customerReference().equals(identity.customerReference())
                || !query.financialInstitutionCode().equals(identity.financialInstitutionCode())
                || (!blank(query.niu()) && !query.niu().equals(identity.niu()))
                || (!blank(query.customerNumber())
                    && !query.customerNumber().equals(identity.customerNumber()))) {
            throw new IllegalStateException("Banking customer identity mismatch");
        }

        AccountSearchResult response = restClient.get()
                .uri(uri -> uri.path("/api/v1/customers/{customerReference}/accounts")
                        .build(summary.customerReference()))
                .headers(h -> {
                    authenticate(h, query.correlationId());
                    h.set("X-Financial-Institution-Code", query.financialInstitutionCode());
                })
                .retrieve().body(AccountSearchResult.class);
        if (response == null || response.accounts() == null) {
            throw new IllegalStateException("Banking account search response is missing");
        }

        Instant retrievedAt = Instant.now();
        List<BankingCustomerLookupPort.BankingAccount> bankAccounts = response.accounts()
                .stream().map(account -> {
                    if (account == null || !summary.customerReference().equals(account.cli())) {
                        throw new IllegalStateException("Banking account ownership mismatch");
                    }
                    return new BankingCustomerLookupPort.BankingAccount(
                            account.accountReference(), null, null,
                            maskedAccountNumber(account.ncp()), account.dev(),
                            account.typ(), account.status(), retrievedAt);
                }).toList();

        if (bankAccounts.stream().map(BankingCustomerLookupPort.BankingAccount::accountReference)
                .distinct().count() != bankAccounts.size()) {
            throw new IllegalStateException("Banking account search returned duplicate references");
        }

        // Legacy explicit account lookup is still needed by the add-account flow.
        // The preview supplies no accountReference, so it receives the complete list.
        BankingCustomerLookupPort.BankingAccount selected = null;
        if (!blank(query.accountReference())) {
            List<BankingCustomerLookupPort.BankingAccount> matchesByAccount = bankAccounts
                    .stream().filter(account -> query.accountReference()
                            .equals(account.accountReference())).toList();
            if (matchesByAccount.size() != 1) {
                throw new IllegalStateException(
                        "Banking account reference must identify exactly one account");
            }
            selected = matchesByAccount.getFirst();
        }

        return new BankingCustomerLookupPort.BankingCustomerProfile(
                identity.financialInstitutionCode(), identity.customerReference(),
                identity.customerNumber(), identity.niu(), identity.legalName(),
                identity.email(), identity.phoneNumber(), selected, bankAccounts);
    }

    // Presentation metadata derived locally. Not a bank-provided masked identifier.
    private static String maskedAccountNumber(String ncp) {
        if (blank(ncp)) {
            throw new IllegalStateException("Bank account number is missing");
        }
        String trimmed = ncp.strip();
        return "****" + trimmed.substring(Math.max(0, trimmed.length() - 4));
    }

    private void authenticate(HttpHeaders headers, String correlationId) {
        headers.set(IntegrationHttpHeaders.CORRELATION_ID, correlationId);
    }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    public record SearchResult(List<CustomerSummary> customers, Integer resultCount) {}
    public record CustomerSummary(String customerReference, String customerNumber,
                                  String financialInstitutionCode, String niu, String legalName) {}
    public record AccountSearchResult(List<BankAccount> accounts, Integer resultCount) {}
}
