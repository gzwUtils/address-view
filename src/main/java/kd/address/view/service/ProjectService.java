package kd.address.view.service;

import kd.address.view.dto.ProjectDTO;
import kd.address.view.entity.Project;
import kd.address.view.mapper.ProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
@RequiredArgsConstructor
@Service
public class ProjectService {

    private final ProjectMapper projectMapper;

    public List<ProjectDTO> getProjectsByCategory(String category) {
        List<Project> byCategory = projectMapper.findByCategory(category);
        List<ProjectDTO> arrayList = new ArrayList<>(byCategory.size());
        byCategory.forEach(project ->{
            ProjectDTO projectDTO = new ProjectDTO();
            BeanUtils.copyProperties(project, projectDTO);
            arrayList.add(projectDTO);
        });
        return arrayList;
    }

    public List<String> getAllCategories() {
        return projectMapper.findAllCategories();
    }

    @Transactional
    public void save(ProjectDTO projectDTO) {
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
