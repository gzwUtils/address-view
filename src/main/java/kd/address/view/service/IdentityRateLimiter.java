package kd.address.view.service;

import kd.address.view.common.TooManyRequestsException;
import kd.address.view.mapper.RateLimitMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class IdentityRateLimiter {
    private final RateLimitMapper mapper;

    public void checkIssue(String ip) { hit("issue-ip:" + ip, 20, false); }
    public void checkAdminLogin(String ip) { hit("admin-login-ip:" + ip, 10, false); }

    public void checkRestore(String ip, String publicId) {
        hit("restore-ip:" + ip, 30, false);
        hit("restore-id:" + publicId, 10, false);
    }

    public void checkAction(String action, Long accountId, String ip, int accountLimit, int ipLimit, boolean daily) {
        hit(action + "-account:" + accountId, accountLimit, daily);
        hit(action + "-ip:" + ip, ipLimit, daily);
    }

    private void hit(String key, int limit, boolean daily) {
        String digest = sha256(key);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime window = daily ? now.truncatedTo(ChronoUnit.DAYS) : now.truncatedTo(ChronoUnit.HOURS);
        mapper.hit(digest, window);
        Integer count = mapper.count(digest, window);
        if (count != null && count > limit) throw new TooManyRequestsException("操作过于频繁，请稍后再试");
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
