package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.dto.ProjectDTO;
import kd.address.view.service.ProjectService;
import kd.address.view.service.GuestIdentityService;
import kd.address.view.entity.GuestAccount;
import jakarta.servlet.http.HttpServletRequest;
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
    private final GuestIdentityService identities;

    @GetMapping("/categories")
    public ApiResponse<List<String>> getCategories() {
        return ApiResponse.success(projectService.getAllCategories());
    }

    @GetMapping
    public ApiResponse<List<ProjectDTO>> getProjects(@RequestParam(required = false) String category,
                                                     @RequestParam(required = false) String keyword,
                                                     HttpServletRequest request) {
        GuestAccount account = identities.optionalCurrent(request);
        return ApiResponse.success(projectService.getProjects(category, keyword, account == null ? null : account.getId()));
    }

    @PostMapping
    public ApiResponse<Void> createProject(@RequestBody ProjectDTO project, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        projectService.save(project, account.getId(), account.getNickname());
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteProject(@PathVariable Long id, HttpServletRequest request) {
        GuestAccount account = identities.requireCurrent(request);
        projectService.deleteById(id, account.getId());
        return ApiResponse.success(Boolean.TRUE);
    }
}
