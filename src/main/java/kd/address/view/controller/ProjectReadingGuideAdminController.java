package kd.address.view.controller;

import jakarta.servlet.http.HttpServletRequest;
import kd.address.view.common.ApiResponse;
import kd.address.view.service.AdminSessionService;
import kd.address.view.service.ProjectReadingGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import kd.address.view.entity.ExternalProject;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/open-source")
public class ProjectReadingGuideAdminController {
    private final AdminSessionService admin;
    private final ProjectReadingGuideService guides;

    @org.springframework.web.bind.annotation.GetMapping("/reading-guides")
    public ApiResponse<List<ExternalProject>> candidates(HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(guides.listCandidates());
    }

    @org.springframework.web.bind.annotation.GetMapping("/{id}/reading-guide")
    public ApiResponse<ProjectReadingGuideService.GuideView> get(@PathVariable Long id,
                                                                  HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(guides.getForAdmin(id));
    }

    @PostMapping("/{id}/reading-guide")
    public ApiResponse<ProjectReadingGuideService.GuideView> enqueue(@PathVariable Long id,
                                                                      HttpServletRequest request) {
        admin.requireAdmin(request);
        return ApiResponse.success(guides.enqueue(id));
    }
}
