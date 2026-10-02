package kd.address.view.controller;

import kd.address.view.common.ApiResponse;
import kd.address.view.entity.ExternalProject;
import kd.address.view.service.ExternalProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/open-source")
public class ExternalProjectController {
    private final ExternalProjectService projects;

    @GetMapping("/featured")
    public ApiResponse<List<ExternalProject>> featured() {
        return ApiResponse.success(projects.featured());
    }
}
