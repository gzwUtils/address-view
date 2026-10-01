package kd.address.view.service;

import kd.address.view.common.UnauthorizedException;
import kd.address.view.entity.GuestAccount;
import kd.address.view.entity.GuestSession;
import kd.address.view.mapper.GuestAccountMapper;
import kd.address.view.mapper.GuestSessionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GuestIdentityServiceTest {
    private GuestAccountMapper accounts;
    private GuestSessionMapper sessions;
    private GuestIdentityService service;
    private final AtomicReference<GuestAccount> stored = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        accounts = mock(GuestAccountMapper.class);
        sessions = mock(GuestSessionMapper.class);
        IdentityRateLimiter limiter = mock(IdentityRateLimiter.class);
        service = new GuestIdentityService(accounts, sessions, limiter);
        doAnswer(invocation -> {
            GuestAccount account = invocation.getArgument(0);
            account.setId(17L);
            stored.set(account);
            return null;
        }).when(accounts).insert(any(GuestAccount.class));
        when(accounts.findByPublicId(anyString())).thenAnswer(invocation -> {
            GuestAccount account = stored.get();
            return account != null && account.getPublicId().equals(invocation.getArgument(0)) ? account : null;
        });
        when(accounts.findById(17L)).thenAnswer(invocation -> stored.get());
    }

    @Test
    void restoreCreatesAnotherSessionForTheSameAccount() {
        GuestIdentityService.CreatedIdentity created = service.create("127.0.0.1");
        GuestIdentityService.RestoredIdentity restored = service.restore(created.account().getPublicId(), created.recoveryCode(), "127.0.0.2");
        assertNotEquals(created.sessionToken(), restored.sessionToken());
        assertEquals(created.account().getId(), restored.account().getId());
        assertEquals(17L, created.account().getId());
        verify(sessions, times(2)).insert(any(GuestSession.class));
    }

    @Test
    void publicIdAloneDoesNotRecoverAccount() {
        GuestIdentityService.CreatedIdentity created = service.create("127.0.0.1");
        assertThrows(UnauthorizedException.class,
                () -> service.restore(created.account().getPublicId(), "", "127.0.0.2"));
        verify(sessions, times(1)).insert(any(GuestSession.class));
    }

    @Test
    void rotationInvalidatesThePreviousCode() {
        GuestIdentityService.CreatedIdentity created = service.create("127.0.0.1");
        when(accounts.updateRecoveryHash(eq(17L), anyString())).thenAnswer(invocation -> {
            stored.get().setRecoveryHash(invocation.getArgument(1));
            return 1;
        });
        String newCode = service.rotateRecovery(17L);
        assertThrows(UnauthorizedException.class,
                () -> service.restore(created.account().getPublicId(), created.recoveryCode(), "127.0.0.2"));
        assertDoesNotThrow(() -> service.restore(created.account().getPublicId(), newCode, "127.0.0.2"));
    }
}
