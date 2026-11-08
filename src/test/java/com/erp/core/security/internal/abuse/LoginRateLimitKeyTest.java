package com.erp.core.security.internal.abuse;

import com.erp.core.security.internal.abuse.filter.LoginRateLimitFilter;
import com.erp.core.security.internal.abuse.port.RateLimitPort;
import com.erp.core.security.internal.platform.config.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginRateLimitKeyTest {

    @Test
    void spoofedForwardedForHeaderDoesNotChangeTheBucket() throws Exception {
        RateLimitPort port = mock(RateLimitPort.class);
        when(port.tryConsume(anyString())).thenReturn(true);
        SecurityProperties props = new SecurityProperties(true, null, null, null, null, null, null, null, null, null);
        LoginRateLimitFilter filter = new LoginRateLimitFilter(port, props);

        for (String spoofed : new String[] {"1.1.1.1", "2.2.2.2"}) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
            req.setRemoteAddr("10.0.0.5");
            req.addHeader("X-Forwarded-For", spoofed);
            filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        }

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(port, times(2)).tryConsume(keys.capture());
        assertThat(keys.getAllValues()).containsOnly("login:10.0.0.5");
    }
}