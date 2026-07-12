package com.gentleia.landingtarjetas;

import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.security.enabled=true")
@AutoConfigureMockMvc
class SecurityBehaviorTests {

    private static final String USERNAME = "security-test-user";
    private static final String PASSWORD = "security-test-password";

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void securityProperties(DynamicPropertyRegistry registry) {
        registry.add("app.security.user", () -> USERNAME);
        registry.add("app.security.password-hash", () -> new BCryptPasswordEncoder().encode(PASSWORD));
    }

    @Test
    void unauthenticatedDashboardRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void unauthenticatedApiRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/api/dashboard/months"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void loginAndStaticAssetsArePublicAndIssueCsrfCookie() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresar al panel")))
                .andExpect(cookie().exists("XSRF-TOKEN"));

        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/js/login.js"))
                .andExpect(status().isOk());
    }

    @Test
    void invalidCredentialsShowGenericLoginError() throws Exception {
        mockMvc.perform(formLogin("/login").user(USERNAME).password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(get("/login?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("login-feedback")))
                .andExpect(content().string(containsString("/js/login.js?v=20260711-security-login")));
    }

    @Test
    void unsafeApiPostsRequireValidCsrfToken() throws Exception {
        MockHttpSession session = loginSession();
        String payload = "{\"name\":\"Blocked by CSRF\"}";

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/categories")
                        .session(session)
                        .with(csrf().useInvalidToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());
    }

    @Test
    void validCredentialsShowDashboardThenLogoutClosesSession() throws Exception {
        MockHttpSession session = loginSession();

        mockMvc.perform(get("/index.html").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"primary-tab-summary\"")));

        mockMvc.perform(post("/logout")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"))
                .andExpect(header().string("Location", "/login?logout"));

        assertThat(session.isInvalid()).isTrue();
        mockMvc.perform(get("/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/login"));
    }

    private MockHttpSession loginSession() throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user(USERNAME).password(PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
