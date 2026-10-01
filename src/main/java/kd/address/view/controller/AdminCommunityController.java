package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kd.address.view.common.ApiResponse;
import kd.address.view.common.ConflictException;
import kd.address.view.common.NotFoundException;
import kd.address.view.common.PageResponse;
import kd.address.view.entity.CommunityReport;
import kd.address.view.entity.GuestAccount;
import kd.address.view.entity.OperationLog;
import kd.address.view.mapper.GuestAccountMapper;
import kd.address.view.mapper.OperationLogMapper;
import kd.address.view.mapper.ProjectMapper;
import kd.address.view.service.AdminSessionService;
import kd.address.view.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class AdminCommunityController {
    private final AdminSessionService admin;
    private final ReportService reports;
    private final GuestAccountMapper accounts;
    private final ProjectMapper projects;
    private final OperationLogMapper logs;

    public record PasswordRequest(String password) {}
    public record ResolveRequest(boolean hideTarget) {}
    public record OwnerRequest(String publicId) {}

    @PostMapping("/session")
    public ApiResponse<Boolean> login(@RequestBody PasswordRequest body, HttpServletRequest request,
                                       HttpServletResponse response) {
        String token = admin.login(body.password(), request.getRemoteAddr());
        response.addHeader(HttpHeaders.SET_COOKIE, admin.cookie(token));
        return ApiResponse.success(true);
    }

    @GetMapping("/me")
    public ApiResponse<Boolean> me(HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(true);
    }

    @GetMapping("/community/reports")
    public ApiResponse<PageResponse<CommunityReport>> openReports(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(reports.open(page, size));
    }

    @PatchMapping("/community/reports/{id}")
    public ApiResponse<Boolean> resolve(@PathVariable Long id, @RequestBody ResolveRequest body,
                                         HttpServletRequest request) {
        admin.requireAdmin(request);
        reports.resolve(id, body.hideTarget());
        return ApiResponse.success(true);
    }

    @PatchMapping("/projects/{id}/owner")
    @Transactional
    public ApiResponse<Boolean> assignOwner(@PathVariable Long id, @RequestBody OwnerRequest body,
                                             HttpServletRequest request) {
        admin.requireAdmin(request);
        GuestAccount account = accounts.findByPublicId(body.publicId() == null ? "" : body.publicId().trim());
        if (account == null) throw new NotFoundException("账户不存在");
        if (projects.findById(id) == null) throw new NotFoundException("项目不存在");
        if (projects.assignLegacyOwner(id, account.getId()) == 0)
            throw new ConflictException("项目已有归属，不可重新认领");
        OperationLog log = new OperationLog();
        log.setModule("project");
        log.setAction("assign_owner");
        log.setTargetType("project");
        log.setTargetName(String.valueOf(id));
        log.setOperatorName("admin");
        log.setDetail("归属账户 " + account.getPublicId());
        logs.insert(log);
        return ApiResponse.success(true);
    }
}
