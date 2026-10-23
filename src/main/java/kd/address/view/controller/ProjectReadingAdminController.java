package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.service.AdminSessionService;
import kd.address.view.service.ProjectReadingEngineProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/project-reading/engines")
public class ProjectReadingAdminController {
    private final AdminSessionService admin;
    private final ProjectReadingEngineProfileService profiles;

    @GetMapping
    public ApiResponse<List<ProjectReadingEngineProfileService.ProfileView>> list(HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(profiles.list());
    }

    @PostMapping
    public ApiResponse<ProjectReadingEngineProfileService.SaveResult> save(
            @RequestBody ProjectReadingEngineProfileService.ProfileInput input, HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(profiles.save(input));
    }

    @PostMapping("/{id}/test")
    public ApiResponse<ProjectReadingEngineProfileService.TestResult> test(
            @PathVariable Long id, HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(profiles.test(id));
    }
}
