package com.sixpay.reporting.configuration;

import org.springframework.context.annotation.Configuration;

/**
 * Reporting module composition root.
 *
 * <p>Concrete query/export beans are introduced by the dedicated Phase 6
 * implementation lots. Keeping the configuration explicit prevents Reporting
 * from becoming an executable application of its own.</p>
 */
@Configuration(proxyBeanMethods = false)
public class ReportingConfiguration {
}
