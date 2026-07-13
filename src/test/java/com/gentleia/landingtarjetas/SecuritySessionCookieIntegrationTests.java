package com.gentleia.landingtarjetas;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.security.enabled=true",
        "server.servlet.session.cookie.secure=false",
        "server.servlet.session.cookie.http-only=true",
        "server.servlet.session.cookie.same-site=lax",
        "server.servlet.session.tracking-modes=cookie",
        "spring.datasource.url=jdbc:h2:mem:security-session-cookie;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class SecuritySessionCookieIntegrationTests {

    private static final String USERNAME = "security-session-user";
    private static final String PASSWORD = "security-session-password";

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.user", () -> USERNAME);
        registry.add("app.security.password-hash", () -> new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder()
                .encode(PASSWORD));
    }

    @Test
    void loginPageIssuesSingleNormalReadableCsrfCookie() throws Exception {
        HttpResponse<String> response = sendGet("/login", Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> xsrfCookies = setCookiesNamed(response, "XSRF-TOKEN");
        assertThat(xsrfCookies).hasSize(1);
        String xsrfCookie = xsrfCookies.get(0);
        assertThat(cookieValue(xsrfCookie)).hasSizeBetween(20, 128);
        assertThat(xsrfCookie).doesNotContain("HttpOnly");
    }

    @Test
    void loginPageIssuesHttpSessionCookieWithoutSecureFlag() throws Exception {
        HttpResponse<String> response = sendGet("/login", Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> sessionCookies = setCookiesNamed(response, "JSESSIONID");
        assertThat(sessionCookies).hasSize(1);
        assertThat(sessionCookies.get(0)).contains("HttpOnly");
        assertThat(sessionCookies.get(0)).contains("SameSite=Lax");
        assertThat(sessionCookies.get(0)).doesNotContain("Secure");
    }

    @Test
    void validLoginKeepsCookieSessionAndDoesNotRewriteRedirectUrl() throws Exception {
        Map<String, String> cookies = new LinkedHashMap<>();
        HttpResponse<String> loginPage = sendGet("/login", cookies);
        mergeSetCookies(cookies, loginPage);

        HttpResponse<String> login = sendFormPost("/login", cookies, browserLoginForm(cookies, PASSWORD));
        mergeSetCookies(cookies, login);

        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(locationPathAndQuery(login)).isEqualTo("/");
        assertThat(login.headers().firstValue("Location").orElse(""))
                .doesNotContain(";jsessionid=");

        HttpResponse<String> dashboard = sendGet("/", cookies);
        assertThat(dashboard.statusCode()).isEqualTo(200);
    }

    @Test
    void invalidLoginRedirectsToLoginError() throws Exception {
        Map<String, String> cookies = new LinkedHashMap<>();
        HttpResponse<String> loginPage = sendGet("/login", cookies);
        mergeSetCookies(cookies, loginPage);

        HttpResponse<String> login = sendFormPost("/login", cookies, browserLoginForm(cookies, "wrong-password"));

        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(locationPathAndQuery(login)).isEqualTo("/login?error");
    }

    @Test
    void healthEndpointRemainsPublic() throws Exception {
        HttpResponse<String> response = sendGet("/actuator/health", Map.of());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
    }

    @Test
    void unauthenticatedDashboardRedirectUsesCookieOnlySessionTracking() throws Exception {
        HttpResponse<String> response = sendGet("/", Map.of());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElse(""))
                .contains("/login");
        assertThat(response.headers().firstValue("Location").orElse(""))
                .doesNotContain(";jsessionid=");
    }

    @Test
    void unsafeApiPostsStillRequireCsrfToken() throws Exception {
        Map<String, String> cookies = authenticatedCookies();

        HttpResponse<String> response = sendJsonPost("/api/categories", cookies, "{\"name\":\"Blocked by CSRF\"}");

        assertThat(response.statusCode()).isEqualTo(403);
    }

    private Map<String, String> authenticatedCookies() throws Exception {
        Map<String, String> cookies = new LinkedHashMap<>();
        HttpResponse<String> loginPage = sendGet("/login", cookies);
        mergeSetCookies(cookies, loginPage);

        HttpResponse<String> login = sendFormPost("/login", cookies, browserLoginForm(cookies, PASSWORD));
        mergeSetCookies(cookies, login);
        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(cookies).containsKey("JSESSIONID");
        return cookies;
    }

    private HttpResponse<String> sendGet(String path, Map<String, String> cookies) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path)).GET(), cookies);
    }

    private HttpResponse<String> sendJsonPost(String path, Map<String, String> cookies, String body)
            throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)), cookies);
    }

    private HttpResponse<String> sendFormPost(String path, Map<String, String> cookies, Map<String, String> form)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)));
        return send(builder, cookies);
    }

    private Map<String, String> browserLoginForm(Map<String, String> cookies, String password) {
        assertThat(cookies).containsKey("XSRF-TOKEN");
        return Map.of(
                "username", USERNAME,
                "password", password,
                "_csrf", cookies.get("XSRF-TOKEN"));
    }

    private HttpResponse<String> send(HttpRequest.Builder builder, Map<String, String> cookies)
            throws IOException, InterruptedException {
        if (!cookies.isEmpty()) {
            builder.header("Cookie", cookieHeader(cookies));
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private String formBody(Map<String, String> form) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : form.entrySet()) {
            values.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return String.join("&", values);
    }

    private String cookieHeader(Map<String, String> cookies) {
        List<String> values = new ArrayList<>();
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            values.add(entry.getKey() + "=" + entry.getValue());
        }
        return String.join("; ", values);
    }

    private void mergeSetCookies(Map<String, String> cookies, HttpResponse<String> response) {
        for (String setCookie : response.headers().allValues("Set-Cookie")) {
            int equalsIndex = setCookie.indexOf('=');
            int semicolonIndex = setCookie.indexOf(';');
            if (equalsIndex > 0 && semicolonIndex > equalsIndex) {
                cookies.put(setCookie.substring(0, equalsIndex), setCookie.substring(equalsIndex + 1, semicolonIndex));
            }
        }
    }

    private List<String> setCookiesNamed(HttpResponse<String> response, String name) {
        return response.headers().allValues("Set-Cookie").stream()
                .filter(cookie -> cookie.startsWith(name + "="))
                .toList();
    }

    private String cookieValue(String setCookie) {
        int equalsIndex = setCookie.indexOf('=');
        int semicolonIndex = setCookie.indexOf(';');
        return setCookie.substring(equalsIndex + 1, semicolonIndex);
    }

    private String locationPathAndQuery(HttpResponse<String> response) {
        URI location = URI.create(response.headers().firstValue("Location").orElseThrow());
        if (location.getQuery() == null) {
            return location.getPath();
        }
        return location.getPath() + "?" + location.getQuery();
    }
}
