package com.gentleia.landingtarjetas.security;

import java.util.function.Supplier;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
public class SecurityConfig {

    private static final Pattern BCRYPT_HASH = Pattern.compile("^\\$2[aby]\\$(\\d{2})\\$[./A-Za-z0-9]{53}$");
    private static final int MIN_BCRYPT_COST = 4;
    private static final int MAX_BCRYPT_COST = 31;
    private static final String BCRYPT_VALIDATION_PROBE = "startup-bcrypt-validation-probe";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppSecurityProperties properties) throws Exception {
        if (!properties.isEnabled()) {
            http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
            return http.build();
        }

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/actuator/health",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .permitAll())
                .sessionManagement(session -> session.sessionFixation(sessionFixation -> sessionFixation.migrateSession()));

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService(AppSecurityProperties properties) {
        if (!properties.isEnabled()) {
            return new InMemoryUserDetailsManager();
        }

        String username = required(properties.getUser(), "APP_SECURITY_USER");
        String passwordHash = required(properties.getPasswordHash(), "APP_SECURITY_PASSWORD_HASH");
        validateBCryptHash(passwordHash);

        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password(passwordHash)
                .roles("USER")
                .build());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void validateBCryptHash(String passwordHash) {
        var matcher = BCRYPT_HASH.matcher(passwordHash);
        if (!matcher.matches()) {
            throw new IllegalStateException("APP_SECURITY_PASSWORD_HASH must be a BCrypt hash.");
        }

        int cost = Integer.parseInt(matcher.group(1));
        if (cost < MIN_BCRYPT_COST || cost > MAX_BCRYPT_COST) {
            throw new IllegalStateException("APP_SECURITY_PASSWORD_HASH must use a BCrypt cost between 04 and 31.");
        }

        try {
            BCrypt.checkpw(BCRYPT_VALIDATION_PROBE, passwordHash);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("APP_SECURITY_PASSWORD_HASH must be a valid BCrypt hash.", ex);
        }
    }

    private String required(String value, String envName) {
        if (value == null || value.isBlank() || isUnresolvedPlaceholder(value)) {
            throw new IllegalStateException(envName + " must be set before starting the secured application.");
        }
        return value.trim();
    }

    private boolean isUnresolvedPlaceholder(String value) {
        String trimmed = value.trim();
        return trimmed.startsWith("${") && trimmed.endsWith("}");
    }
}

final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String headerValue = request.getHeader(csrfToken.getHeaderName());
        if (StringUtils.hasText(headerValue)) {
            return plain.resolveCsrfTokenValue(request, csrfToken);
        }
        String parameterValue = request.getParameter(csrfToken.getParameterName());
        if (StringUtils.hasText(parameterValue) && parameterValue.equals(csrfToken.getToken())) {
            return plain.resolveCsrfTokenValue(request, csrfToken);
        }
        return xor.resolveCsrfTokenValue(request, csrfToken);
    }
}
