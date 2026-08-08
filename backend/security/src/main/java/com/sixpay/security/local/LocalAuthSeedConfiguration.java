package com.sixpay.security.local;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LocalAuthSeedProperties.class)
@ConditionalOnProperty(
        prefix = "sixpay.security.local",
        name = "seed-enabled",
        havingValue = "true"
)
public class LocalAuthSeedConfiguration {
}
