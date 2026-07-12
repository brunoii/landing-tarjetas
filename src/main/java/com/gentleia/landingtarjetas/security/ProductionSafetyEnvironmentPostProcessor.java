package com.gentleia.landingtarjetas.security;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Profiles;
import org.springframework.util.PlaceholderResolutionException;

public final class ProductionSafetyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROD_PROFILE = "prod";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!environment.acceptsProfiles(Profiles.of(PROD_PROFILE))) {
            return;
        }

        requireSecurityEnabled(environment);
        requireProductionDatasource(environment);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    private void requireSecurityEnabled(ConfigurableEnvironment environment) {
        String enabled = environment.getProperty("app.security.enabled", "true");
        if ("false".equalsIgnoreCase(enabled.trim())) {
            throw new IllegalStateException("Production profile cannot start with app.security.enabled=false.");
        }
    }

    private void requireProductionDatasource(ConfigurableEnvironment environment) {
        required(environment, "spring.datasource.url", "APP_DATASOURCE_URL");
        String username = required(environment, "spring.datasource.username", "APP_DATASOURCE_USERNAME");
        required(environment, "spring.datasource.password", "APP_DATASOURCE_PASSWORD");

        if ("sa".equalsIgnoreCase(username.trim())) {
            throw new IllegalStateException("APP_DATASOURCE_USERNAME must not use the default H2 'sa' user in prod.");
        }
    }

    private String required(ConfigurableEnvironment environment, String propertyName, String envName) {
        String value;
        try {
            value = environment.getProperty(propertyName);
        } catch (PlaceholderResolutionException exception) {
            throw new IllegalStateException(envName + " must be set before starting with the prod profile.", exception);
        }
        if (value == null || value.isBlank() || isUnresolvedPlaceholder(value)) {
            throw new IllegalStateException(envName + " must be set before starting with the prod profile.");
        }
        return value;
    }

    private boolean isUnresolvedPlaceholder(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
    }
}
