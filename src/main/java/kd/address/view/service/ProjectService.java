package kd.address.view.service;

import kd.address.view.dto.ProjectDTO;
import kd.address.view.entity.Project;
import kd.address.view.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<ProjectDTO> getProjectsByCategory(String category) {
        List<Project> projects = (category == null || category.isBlank())
                ? projectMapper.findAll()
                : projectMapper.findByCategory(category);
        return projects.stream()
                .map(project -> {
                    ProjectDTO projectDTO = new ProjectDTO();
                    BeanUtils.copyProperties(project, projectDTO);
                    return projectDTO;
                })
                .toList();
    }

    public List<String> getAllCategories() {
        return projectMapper.findAllCategories();
    }

    @Transactional
    public void save(ProjectDTO projectDTO) {
        if (projectDTO == null) {
            throw new IllegalArgumentException("Project data is required");
        }
        Project project = new Project();
        BeanUtils.copyProperties(projectDTO, project);
        if (project.getId() == null) {
            projectMapper.insert(project);
        } else {
            projectMapper.update(project);
        }
    }

    @Transactional
    public void deleteById(Long id) {
        projectMapper.deleteById(id);
    }
}