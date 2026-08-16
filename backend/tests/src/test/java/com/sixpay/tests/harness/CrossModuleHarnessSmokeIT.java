package com.sixpay.tests.harness;

import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class CrossModuleHarnessSmokeIT
        extends CrossModulePostgreSqlTestSupport {

    @Test
    void postgresHarnessIsReachable() throws Exception {
        try (
                var connection = DriverManager.getConnection(
                        jdbcUrl(),
                        jdbcUsername(),
                        jdbcPassword()
                );
                var statement = connection.createStatement();
                var result = statement.executeQuery("select 1")
        ) {
            assertThat(result.next()).isTrue();
            assertThat(result.getInt(1)).isEqualTo(1);
        }
    }
}
