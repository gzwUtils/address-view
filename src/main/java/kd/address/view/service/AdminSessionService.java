package kd.address.view.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.UnauthorizedException;
import kd.address.view.entity.AdminSession;
import kd.address.view.mapper.AdminSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AdminSessionService {
    private static final String COOKIE_NAME = "PORTAL_ADMIN";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AdminSessionMapper sessions;
    private final IdentityRateLimiter limiter;

    @Value("${portal.admin-password-hash:}")
    private String configuredHash;
    @Value("${portal.cookie-secure:true}")
    private boolean secureCookie;

    public String login(String password, String ip) {
        limiter.checkAdminLogin(ip);
        if (configuredHash == null || configuredHash.isBlank())
            throw new UnauthorizedException("管理员入口尚未配置");
        if (password == null || !new BCryptPasswordEncoder().matches(password, configuredHash))
            throw new UnauthorizedException("管理密码不正确");
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AdminSession session = new AdminSession();
        session.setTokenHash(hash(token));
        session.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        sessions.insert(session);
        return token;
    }

    public void requireAdmin(HttpServletRequest request) {
        String token = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (COOKIE_NAME.equals(cookie.getName())) token = cookie.getValue();
            }
        }
        if (token == null || sessions.findActive(hash(token)) == null)
            throw new UnauthorizedException("请先登录管理后台");
    }

    public String cookie(String token) {
        return ResponseCookie.from(COOKIE_NAME, token).httpOnly(true).secure(secureCookie)
                .sameSite("Lax").path("/api").maxAge(Duration.ofDays(7)).build().toString();
    }

    private static String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
