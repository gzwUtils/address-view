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
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<ProjectDTO> getProjects(String category, String keyword, Long accountId) {
        List<Project> projects = (category == null || category.isBlank())
                ? projectMapper.findAll()
                : projectMapper.findByCategory(category);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return projects.stream()
                .filter(project -> normalizedKeyword.isEmpty() || Stream.of(
                                project.getProjectName(), project.getShortName(), project.getDescription(),
                                project.getCategory(), project.getType(), project.getPlatformUrl())
                        .filter(Objects::nonNull)
                        .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalizedKeyword)))
                .map(project -> {
                    ProjectDTO projectDTO = new ProjectDTO();
                    BeanUtils.copyProperties(project, projectDTO);
                    projectDTO.setCanEdit(accountId != null && accountId.equals(project.getOwnerAccountId()));
                    return projectDTO;
                })
                .toList();
    }

    public List<String> getAllCategories() {
        return projectMapper.findAllCategories();
    }

    @Transactional
    public void save(ProjectDTO projectDTO, Long accountId, String nickname) {
        if (projectDTO == null) {
            throw new IllegalArgumentException("Project data is required");
        }
        Project project = new Project();
        BeanUtils.copyProperties(projectDTO, project);
        if (project.getId() == null) {
            project.setOwnerId(null);
            project.setOwnerName(nickname);
            project.setOwnerAccountId(accountId);
            projectMapper.insert(project);
        } else {
            validateOwnership(project.getId(), accountId);
            projectMapper.updateContent(project);
        }
    }

    @Transactional
    public void deleteById(Long id, Long accountId) {
        validateOwnership(id, accountId);
        projectMapper.deleteById(id);
    }

    /**
     * 校验项目所有权
     * @param projectId 项目ID
     * @param accountId 由服务端会话验证的账户 ID
     * @throws SecurityException 如果校验失败
     */
    private void validateOwnership(Long projectId, Long accountId) {
        if (projectId == null) {
            return;
        }
        Project existingProject = projectMapper.findById(projectId);
        if (existingProject == null) {
            throw new IllegalArgumentException("项目不存在: " + projectId);
        }

        Long ownerAccountId = existingProject.getOwnerAccountId();
        if (ownerAccountId == null || !ownerAccountId.equals(accountId)) {
            log.warn("项目归属校验失败: projectId={}, accountId={}", projectId, accountId);
            throw new SecurityException("只能修改自己上传的项目");
        }
    }
}
