package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.mapper.ExternalProjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExternalProjectService {
    private final ExternalProjectMapper projects;

    public List<ExternalProject> featured() {
        return projects.findFeatured();
    }

    public boolean hasFeatured() {
        return projects.countFeatured() > 0;
    }

    @Transactional
    public void replaceFeatured(List<ExternalProject> selected) {
        if (selected == null || selected.isEmpty()) return;
        projects.clearFeatured();
        for (int index = 0; index < Math.min(5, selected.size()); index++) {
            ExternalProject project = selected.get(index);
            project.setDisplayRank(index + 1);
            projects.upsert(project);
        }
    }
}
