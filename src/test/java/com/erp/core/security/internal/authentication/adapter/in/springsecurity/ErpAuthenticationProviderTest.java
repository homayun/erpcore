package com.erp.core.security.internal.authentication.adapter.in.springsecurity;

import com.erp.core.security.api.event.AuthenticationFailureReason;
import com.erp.core.security.internal.authentication.application.AuthenticationCommand;
import com.erp.core.security.internal.authentication.application.AuthenticationOrchestrator;
import com.erp.core.security.internal.authentication.application.AuthenticationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpAuthenticationProviderTest {

    @Mock private AuthenticationOrchestrator orchestrator;
    @InjectMocks private ErpAuthenticationProvider provider;

    @Test
    void supportsErpToken() {
        assertThat(provider.supports(ErpAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    @Test
    void returnsNullForForeignToken() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u", "p");
        assertThat(provider.authenticate(auth)).isNull();
    }

    @Test
    void succeedsWithErpUserDetails() {
        UUID tenantId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(orchestrator.authenticate(any(AuthenticationCommand.class)))
                .thenReturn(new AuthenticationResult.Success(actorId, tenantId, "alice"));

        Authentication result = provider.authenticate(
                new ErpAuthenticationToken(tenantId, "alice", "pwd"));

        assertThat(result).isInstanceOf(ErpAuthenticationToken.class);
        ErpAuthenticationToken token = (ErpAuthenticationToken) result;
        assertThat(token.isAuthenticated()).isTrue();
        assertThat(token.getPrincipal()).isInstanceOf(ErpUserDetails.class);
        ErpUserDetails user = (ErpUserDetails) token.getPrincipal();
        assertThat(user.actorId()).isEqualTo(actorId);
        assertThat(user.tenantId()).isEqualTo(tenantId);
        assertThat(token.getCredentials()).isNull();  // erased
    }

    @Test
    void throwsUniformBadCredentialsOnFailure() {
        UUID tenantId = UUID.randomUUID();
        when(orchestrator.authenticate(any()))
                .thenReturn(new AuthenticationResult.Failure(AuthenticationFailureReason.INVALID_CREDENTIALS));

        assertThatThrownBy(() -> provider.authenticate(
                new ErpAuthenticationToken(tenantId, "ghost", "pwd")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authentication failed");
    }
}