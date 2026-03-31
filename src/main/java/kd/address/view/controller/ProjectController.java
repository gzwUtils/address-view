package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.dto.ProjectDTO;
import kd.address.view.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(projectService.getAllCategories());
    }

    @GetMapping
    public ApiResponse<List<ProjectDTO>> getProjects(@RequestParam(required = false) String category) {
        return ApiResponse.success(projectService.getProjectsByCategory(category));
    }

    @PostMapping
    public ApiResponse<Void> createProject(@RequestBody ProjectDTO project) {
        projectService.save(project);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteProject(@PathVariable Long id, @RequestHeader(value = "X-User-Id", required = true) String userId) {
        projectService.deleteById(id, userId);
        return ApiResponse.success(Boolean.TRUE);
    }
}