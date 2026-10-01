package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kd.address.view.common.ApiResponse;
import kd.address.view.entity.GuestAccount;
import kd.address.view.service.GuestIdentityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class GuestIdentityController {
    private final GuestIdentityService identities;

    @Value("${portal.cookie-secure:true}")
    private boolean secureCookie;

    public record Profile(String publicId, String nickname) {
        static Profile from(GuestAccount account) { return new Profile(account.getPublicId(), account.getNickname()); }
    }
    public record CreatedProfile(String publicId, String nickname, String recoveryCode) {}
    public record RestoreRequest(String publicId, String recoveryCode) {}
    public record RenameRequest(String nickname) {}
    public record RecoveryCode(String recoveryCode) {}

    @GetMapping("/me")
    public ApiResponse<Profile> me(HttpServletRequest request, HttpServletResponse response) {
        GuestAccount account = identities.requireCurrent(request);
        issueCookie(response, GuestIdentityService.cookieValue(request));
        return ApiResponse.success(Profile.from(account));
    }

    @PostMapping("/guest-sessions")
    public ApiResponse<CreatedProfile> create(HttpServletRequest request, HttpServletResponse response) {
        GuestIdentityService.CreatedIdentity created = identities.create(request.getRemoteAddr());
        issueCookie(response, created.sessionToken());
        GuestAccount account = created.account();
        return ApiResponse.success(new CreatedProfile(account.getPublicId(), account.getNickname(), created.recoveryCode()));
    }

    @PostMapping("/guest-sessions/restore")
    public ApiResponse<Profile> restore(@RequestBody RestoreRequest body, HttpServletRequest request,
                                        HttpServletResponse response) {
        GuestIdentityService.RestoredIdentity restored = identities.restore(body.publicId(), body.recoveryCode(), request.getRemoteAddr());
        issueCookie(response, restored.sessionToken());
        return ApiResponse.success(Profile.from(restored.account()));
    }

    @PatchMapping("/me")
    public ApiResponse<Profile> rename(@RequestBody RenameRequest body, HttpServletRequest request) {
        GuestAccount current = identities.requireCurrent(request);
        return ApiResponse.success(Profile.from(identities.rename(current.getId(), body.nickname(), request.getRemoteAddr())));
    }

    @PostMapping("/me/recovery-code/rotate")
    public ApiResponse<RecoveryCode> rotate(HttpServletRequest request) {
        GuestAccount current = identities.requireCurrent(request);
        return ApiResponse.success(new RecoveryCode(identities.rotateRecovery(current.getId())));
    }

    private void issueCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(GuestIdentityService.COOKIE_NAME, token)
                .httpOnly(true).secure(secureCookie).sameSite("Lax").path("/api")
                .maxAge(Duration.ofDays(365)).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
