package com.gentleia.landingtarjetas.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyEnvironmentPostProcessorTests {

    private final ProductionSafetyEnvironmentPostProcessor postProcessor = new ProductionSafetyEnvironmentPostProcessor();

    @Test
    void prodProfileRejectsSecurityDisabledOverride() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("app.security.enabled", "false");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.enabled=false");
    }

    @Test
    void prodProfileRejectsMissingDatasourceProperties() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.enabled", "true");
        environment.setActiveProfiles("prod");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_DATASOURCE_URL");
    }

    @Test
    void prodProfileRejectsMissingOrBlankDatasourceUsername() {
        for (String username : new String[] { null, "", "   " }) {
            MockEnvironment environment = productionEnvironment(
                    "jdbc:h2:file:/var/lib/landing-tarjetas/app;DB_CLOSE_ON_EXIT=FALSE",
                    username,
                    "private-password");

            assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_DATASOURCE_USERNAME");
        }
    }

    @Test
    void prodProfileRejectsMissingOrBlankDatasourcePassword() {
        for (String password : new String[] { null, "", "   " }) {
            MockEnvironment environment = productionEnvironment(
                    "jdbc:h2:file:/var/lib/landing-tarjetas/app;DB_CLOSE_ON_EXIT=FALSE",
                    "landing_user",
                    password);

            assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_DATASOURCE_PASSWORD");
        }
    }

    @Test
    void prodProfileRejectsUnresolvedDatasourcePlaceholders() {
        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(productionEnvironment(
                        "${APP_DATASOURCE_URL}",
                        "landing_user",
                        "private-password"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_DATASOURCE_URL");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(productionEnvironment(
                        "jdbc:h2:file:/var/lib/landing-tarjetas/app;DB_CLOSE_ON_EXIT=FALSE",
                        "${APP_DATASOURCE_USERNAME}",
                        "private-password"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_DATASOURCE_USERNAME");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(productionEnvironment(
                        "jdbc:h2:file:/var/lib/landing-tarjetas/app;DB_CLOSE_ON_EXIT=FALSE",
                        "landing_user",
                        "${APP_DATASOURCE_PASSWORD}"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_DATASOURCE_PASSWORD");
    }

    @Test
    void prodProfileRejectsDefaultH2SaUsername() {
        MockEnvironment environment = productionEnvironment()
                .withProperty("spring.datasource.username", "sa");

        assertThatThrownBy(() -> postProcessor.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default H2 'sa' user");
    }

    @Test
    void prodProfileAcceptsExplicitNonDefaultDatasourceAndEnabledSecurity() {
        assertThatCode(() -> postProcessor.postProcessEnvironment(productionEnvironment(), null))
                .doesNotThrowAnyException();
    }

    private MockEnvironment productionEnvironment() {
        return productionEnvironment(
                "jdbc:h2:file:/var/lib/landing-tarjetas/app;DB_CLOSE_ON_EXIT=FALSE",
                "landing_user",
                "change-this-private-password");
    }

    private MockEnvironment productionEnvironment(String datasourceUrl, String datasourceUsername, String datasourcePassword) {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("app.security.enabled", "true");
        if (datasourceUrl != null) {
            environment.withProperty("spring.datasource.url", datasourceUrl);
        }
        if (datasourceUsername != null) {
            environment.withProperty("spring.datasource.username", datasourceUsername);
        }
        if (datasourcePassword != null) {
            environment.withProperty("spring.datasource.password", datasourcePassword);
        }
        environment.setActiveProfiles("prod");
        return environment;
    }
}
