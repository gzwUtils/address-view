package kd.address.view.controller;
import kd.address.view.dto.ProjectDTO;
import kd.address.view.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@SuppressWarnings("unused")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping("/categories")
    public List<String> getCategories() {
        return projectService.getAllCategories();
    }

    @GetMapping
    public List<ProjectDTO> getProjects(@RequestParam String category) {
        return projectService.getProjectsByCategory(category);
    }

    @PostMapping
    public void createProject(@RequestBody ProjectDTO project) {
        projectService.save(project);
    }

    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteById(id);
    }
}
