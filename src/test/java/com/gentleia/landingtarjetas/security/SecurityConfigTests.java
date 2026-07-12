package com.gentleia.landingtarjetas.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SecurityConfigTests {

    private static final String VALID_PASSWORD_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi/YR6DnFbFJdMP2XPAtw9rAPc7lwmK";
    private static final String BCRYPT_ALPHABET_VALUE = "A".repeat(53);

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    void securityEnabledRejectsMissingOrBlankUsername() {
        for (String username : new String[] { null, "", "   " }) {
            AppSecurityProperties properties = securityProperties(username, VALID_PASSWORD_HASH);

            assertThatThrownBy(() -> securityConfig.userDetailsService(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_SECURITY_USER");
        }
    }

    @Test
    void securityEnabledRejectsMissingOrBlankPasswordHash() {
        for (String passwordHash : new String[] { null, "", "   " }) {
            AppSecurityProperties properties = securityProperties("prod-user", passwordHash);

            assertThatThrownBy(() -> securityConfig.userDetailsService(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_SECURITY_PASSWORD_HASH");
        }
    }

    @Test
    void securityEnabledRejectsMalformedAndPlaintextPasswordHashes() {
        for (String passwordHash : new String[] {
                "plaintext-password",
                "{noop}plaintext-password",
                "$2a$10$too-short",
                "$2a$10$" + "A".repeat(52) + "!",
                "$argon2id$v=19$m=65536,t=3,p=4$abc$def"
        }) {
            AppSecurityProperties properties = securityProperties("prod-user", passwordHash);

            assertThatThrownBy(() -> securityConfig.userDetailsService(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_SECURITY_PASSWORD_HASH must be a BCrypt hash.");
        }
    }

    @Test
    void securityEnabledRejectsInvalidBCryptCostBoundaries() {
        for (String passwordHash : new String[] {
                "$2a$03$" + BCRYPT_ALPHABET_VALUE,
                "$2a$32$" + BCRYPT_ALPHABET_VALUE
        }) {
            AppSecurityProperties properties = securityProperties("prod-user", passwordHash);

            assertThatThrownBy(() -> securityConfig.userDetailsService(properties))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("APP_SECURITY_PASSWORD_HASH must use a BCrypt cost between 04 and 31.");
        }
    }

    @Test
    void securityEnabledAcceptsValidBCryptPasswordHash() {
        AppSecurityProperties properties = securityProperties("prod-user", VALID_PASSWORD_HASH);

        assertThatCode(() -> securityConfig.userDetailsService(properties))
                .doesNotThrowAnyException();
    }

    @Test
    void securityEnabledRejectsUnresolvedCredentialPlaceholders() {
        assertThatThrownBy(() -> securityConfig.userDetailsService(securityProperties("${APP_SECURITY_USER}", VALID_PASSWORD_HASH)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_SECURITY_USER");

        assertThatThrownBy(() -> securityConfig.userDetailsService(securityProperties("prod-user", "${APP_SECURITY_PASSWORD_HASH}")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("APP_SECURITY_PASSWORD_HASH");
    }

    private AppSecurityProperties securityProperties(String username, String passwordHash) {
        AppSecurityProperties properties = new AppSecurityProperties();
        properties.setEnabled(true);
        properties.setUser(username);
        properties.setPasswordHash(passwordHash);
        return properties;
    }
}
