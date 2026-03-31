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
            // 更新前校验权限
            validateOwnership(project.getId(), projectDTO.getOwnerId());
            projectMapper.update(project);
        }
    }

    @Transactional
    public void deleteById(Long id, String ownerId) {
        // 删除前校验权限
        validateOwnership(id, ownerId);
        projectMapper.deleteById(id);
    }

    /**
     * 校验项目所有权
     * @param projectId 项目ID
     * @param ownerId 所有者ID
     * @throws SecurityException 如果校验失败
     */
    private void validateOwnership(Long projectId, String ownerId) {
        if (projectId == null) {
            return;
        }
        if (ownerId == null || ownerId.isBlank()) {
            throw new SecurityException("缺少所有者ID，无法校验权限");
        }

        Project existingProject = projectMapper.findById(projectId);
        if (existingProject == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }

        String projectOwnerId = existingProject.getOwnerId();
        if (projectOwnerId == null || !projectOwnerId.equals(ownerId)) {
            log.warn("权限校验失败: projectId={}, 请求者={}, 实际所有者={}",
                    projectId, ownerId, projectOwnerId);
            throw new SecurityException("只能修改自己上传的项目");
        }
    }
}