package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.session.web.http.CookieSerializer.CookieValue;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles({"test", "https"})
class SecureSessionCookieTests {
    @Autowired
    private DefaultCookieSerializer serializer;

    @Autowired
    private CsrfTokenRepository csrfTokenRepository;

    @Test
    void httpsProfileConfigurationProducesSecureSessionCookie() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        serializer.writeCookieValue(new CookieValue(new MockHttpServletRequest(), response, "session-id"));

        assertThat(response.getHeader("Set-Cookie"))
                .contains("SESSION=", "Path=/", "HttpOnly", "SameSite=Lax", "Secure");
    }

    @Test
    void httpsProfileConfigurationProducesSecureReadableXsrfCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CsrfToken token = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(token, request, response);

        assertThat(response.getCookie("XSRF-TOKEN").getAttribute("SameSite")).isEqualTo("Lax");
        assertThat(response.getHeader("Set-Cookie"))
                .contains("XSRF-TOKEN=", "Path=/", "Secure")
                .doesNotContain("HttpOnly");
    }
}
