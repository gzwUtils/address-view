package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.mapper.ExternalProjectMapper;
import kd.address.view.mapper.ProjectReadingGuideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectReadingGuideQueueService {
    private final ExternalProjectMapper projects;
    private final ProjectReadingGuideMapper guides;

    /** Queue only featured GitHub projects that have never had a guide record. */
    public int enqueueMissingFeatured(Long engineProfileId) {
        if (engineProfileId == null) return 0;
        int queued = 0;
        for (ExternalProject project : projects.findFeatured()) {
            if (project == null || !"github_search".equals(project.getSourceType())
                    || project.getReadingGuideStatus() != null) continue;
            guides.enqueue(project.getId(), engineProfileId, false);
            queued++;
        }
        return queued;
    }
}
