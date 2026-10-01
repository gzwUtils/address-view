package kd.address.view.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ConflictException;
import kd.address.view.common.UnauthorizedException;
import kd.address.view.entity.GuestAccount;
import kd.address.view.entity.GuestSession;
import kd.address.view.mapper.GuestAccountMapper;
import kd.address.view.mapper.GuestSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class GuestIdentityService {
    public static final String COOKIE_NAME = "PORTAL_GUEST";
    private static final String[] WORDS = {"星河", "清风", "山海", "微光", "远帆", "青禾", "松影", "云舟"};
    private static final SecureRandom RANDOM = new SecureRandom();
    private final GuestAccountMapper accounts;
    private final GuestSessionMapper sessions;
    private final IdentityRateLimiter rateLimiter;

    public record CreatedIdentity(GuestAccount account, String recoveryCode, String sessionToken) {}
    public record RestoredIdentity(GuestAccount account, String sessionToken) {}

    @Transactional
    public CreatedIdentity create(String ip) {
        rateLimiter.checkIssue(ip);
        GuestAccount account = new GuestAccount();
        account.setPublicId(newPublicId());
        account.setNickname(newNickname());
        account.setStatus("active");
        String recoveryCode = newToken(20);
        account.setRecoveryHash(hash(recoveryCode));
        accounts.insert(account);
        return new CreatedIdentity(account, recoveryCode, newSession(account.getId()));
    }

    @Transactional
    public RestoredIdentity restore(String publicId, String recoveryCode, String ip) {
        String normalized = publicId == null ? "" : publicId.trim().toUpperCase();
        rateLimiter.checkRestore(ip, normalized);
        GuestAccount account = accounts.findByPublicId(normalized);
        String expected = account == null ? "0".repeat(64) : account.getRecoveryHash();
        String actual = hash(recoveryCode == null ? "" : recoveryCode.trim());
        boolean valid = MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                actual.getBytes(StandardCharsets.US_ASCII));
        if (!valid || account == null || !"active".equals(account.getStatus())) {
            throw new UnauthorizedException("账户 ID 或恢复码不正确");
        }
        return new RestoredIdentity(account, newSession(account.getId()));
    }

    public GuestAccount optionalCurrent(HttpServletRequest request) {
        String token = cookieValue(request);
        if (token == null || token.isBlank()) return null;
        GuestSession session = sessions.findByTokenHash(hash(token));
        if (session == null) return null;
        GuestAccount account = accounts.findById(session.getAccountId());
        if (account == null || !"active".equals(account.getStatus())) return null;
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (session.getExpiresAt().isBefore(now.plusDays(30))) {
            sessions.renew(session.getId(), now.plusDays(365));
        }
        return account;
    }

    public GuestAccount requireCurrent(HttpServletRequest request) {
        GuestAccount account = optionalCurrent(request);
        if (account == null) throw new UnauthorizedException("请先恢复或创建账户");
        return account;
    }

    @Transactional
    public String rotateRecovery(Long accountId) {
        String code = newToken(20);
        accounts.updateRecoveryHash(accountId, hash(code));
        return code;
    }

    @Transactional
    public GuestAccount rename(Long accountId, String requested, String ip) {
        rateLimiter.checkAction("nickname", accountId, ip, 5, 30, true);
        String name = requested == null ? "" : requested.trim();
        int length = name.codePointCount(0, name.length());
        if (length < 2 || length > 20 || name.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("昵称需要 2–20 个可见字符");
        }
        GuestAccount existing = accounts.findByNickname(name);
        if (existing != null && !existing.getId().equals(accountId)) throw new ConflictException("昵称已被使用");
        accounts.updateNickname(accountId, name);
        return accounts.findById(accountId);
    }

    private String newSession(Long accountId) {
        String token = newToken(32);
        GuestSession session = new GuestSession();
        session.setAccountId(accountId);
        session.setTokenHash(hash(token));
        session.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(365));
        sessions.insert(session);
        return token;
    }

    private String newPublicId() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String candidate = "P-" + String.format("%010d", RANDOM.nextLong(10_000_000_000L));
            if (accounts.findByPublicId(candidate) == null) return candidate;
        }
        throw new IllegalStateException("无法生成账户 ID");
    }

    private String newNickname() {
        for (int attempt = 0; attempt < 40; attempt++) {
            String candidate = WORDS[RANDOM.nextInt(WORDS.length)] + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (accounts.findByNickname(candidate) == null) return candidate;
        }
        throw new IllegalStateException("无法生成昵称");
    }

    private static String newToken(int bytes) {
        byte[] value = new byte[bytes];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static String cookieValue(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }
}
