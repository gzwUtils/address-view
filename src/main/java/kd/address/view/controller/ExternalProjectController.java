package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.entity.ExternalProject;
import kd.address.view.service.ExternalProjectService;
import kd.address.view.service.ProjectReadingGuideService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/open-source")
public class ExternalProjectController {
    private final ExternalProjectService projects;
    private final ProjectReadingGuideService readingGuides;

    @GetMapping("/featured")
    public ApiResponse<List<ExternalProject>> featured() {
        return ApiResponse.success(projects.featured());
    }

    @GetMapping("/{id}")
    public ApiResponse<ExternalProject> get(@PathVariable Long id) {
        return ApiResponse.success(projects.get(id));
    }

    @GetMapping("/{id}/reading-guide")
    public ApiResponse<ProjectReadingGuideService.GuideView> readingGuide(@PathVariable Long id) {
        return ApiResponse.success(readingGuides.get(id));
    }
}
