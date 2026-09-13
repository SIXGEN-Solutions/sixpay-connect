package com.sixpay.accounting.infrastructure.tfj.client;

import com.sixpay.accounting.infrastructure.tfj.configuration.AccountingTfjLookupProperties;
import com.sixpay.accounting.application.exception.AccountingProviderAuthenticationException;
import com.sixpay.accounting.application.exception.AccountingProviderUnavailableException;
import com.sixpay.accounting.application.port.output.AccountingIntegrationContext;
import com.sixpay.accounting.application.port.output.TfjConfirmationLookupGateway;
import com.sixpay.accounting.domain.model.TfjConfirmation;
import com.sixpay.accounting.domain.model.TfjObservationChannel;
import com.sixpay.accounting.infrastructure.tfj.dto.EndOfDayConfirmationDto;
import com.sixpay.accounting.infrastructure.tfj.mapper.AmplitudeTfjMapper;
import com.sixpay.integration.http.IntegrationHttpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

@Component
@ConditionalOnProperty(
        prefix = AccountingTfjLookupProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public final class AmplitudeTfjLookupClient
        implements TfjConfirmationLookupGateway {

    private final RestClient restClient;
    private final TfjLookupAccessTokenProvider tokenProvider;
    private final AccountingTfjLookupProperties properties;
    private final AmplitudeTfjMapper mapper;

    public AmplitudeTfjLookupClient(
            @Qualifier("tfjLookupRestClient")
            RestClient restClient,
            TfjLookupAccessTokenProvider tokenProvider,
            AccountingTfjLookupProperties properties,
            AmplitudeTfjMapper mapper
    ) {
        this.restClient = Objects.requireNonNull(restClient);
        this.tokenProvider = Objects.requireNonNull(tokenProvider);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public Optional<TfjConfirmation> lookup(
            String financialInstitutionCode,
            LocalDate businessDate,
            String paymentReference,
            String bankPostingReference,
            AccountingIntegrationContext context
    ) {
        try {
            EndOfDayConfirmationDto dto =
                    restClient.get()
                            .uri(builder -> builder
                                    .path(
                                            properties.lookupPath()
                                    )
                                    .queryParam(
                                            "financialInstitutionCode",
                                            financialInstitutionCode
                                    )
                                    .queryParam(
                                            "businessDate",
                                            businessDate
                                    )
                                    .queryParam(
                                            "paymentReference",
                                            paymentReference
                                    )
                                    .queryParam(
                                            "bankPostingReference",
                                            bankPostingReference
                                    )
                                    .build()
                            )
                            .header(
                                    HttpHeaders.AUTHORIZATION,
                                    "Bearer "
                                            + tokenProvider.accessToken()
                            )
                            .header(
                                    IntegrationHttpHeaders.CORRELATION_ID,
                                    context.correlationId().value()
                            )
                            .retrieve()
                            .body(
                                    EndOfDayConfirmationDto.class
                            );

            if (dto == null) {
                throw new AccountingProviderUnavailableException(
                        "TFJ lookup returned an empty body",
                        null
                );
            }

            return Optional.of(
                    mapper.toDomain(
                            dto,
                            "lookup:" + dto.confirmationId(),
                            context.correlationId().value(),
                            TfjObservationChannel.SCHEDULED_LOOKUP
                    )
            );
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        } catch (ResourceAccessException exception) {
            throw new AccountingProviderUnavailableException(
                    "TFJ lookup is unavailable",
                    exception
            );
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 401 || status == 403) {
                throw new AccountingProviderAuthenticationException(
                        "TFJ lookup authentication failed",
                        exception
                );
            }
            throw new AccountingProviderUnavailableException(
                    "TFJ lookup failed with HTTP " + status,
                    exception
            );
        }
    }
}
