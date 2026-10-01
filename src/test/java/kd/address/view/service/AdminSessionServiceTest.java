package kd.address.view.service;

import kd.address.view.common.UnauthorizedException;
import kd.address.view.entity.AdminSession;
import kd.address.view.mapper.AdminSessionMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminSessionServiceTest {
    @Test
    void passwordMustMatchConfiguredHash() {
        AdminSessionMapper sessions = mock(AdminSessionMapper.class);
        AdminSessionService service = new AdminSessionService(sessions, mock(IdentityRateLimiter.class));
        ReflectionTestUtils.setField(service, "configuredHash", new BCryptPasswordEncoder().encode("correct-password"));

        assertThrows(UnauthorizedException.class, () -> service.login("wrong-password", "127.0.0.1"));
        verify(sessions, never()).insert(any());

        String token = service.login("correct-password", "127.0.0.1");
        assertTrue(token.length() >= 40);
        verify(sessions).insert(any(AdminSession.class));
    }

    @Test
    void missingConfigurationFailsClosed() {
        AdminSessionMapper sessions = mock(AdminSessionMapper.class);
        AdminSessionService service = new AdminSessionService(sessions, mock(IdentityRateLimiter.class));
        ReflectionTestUtils.setField(service, "configuredHash", "");

        assertThrows(UnauthorizedException.class, () -> service.login("anything", "127.0.0.1"));
        verify(sessions, never()).insert(any());
    }
}
