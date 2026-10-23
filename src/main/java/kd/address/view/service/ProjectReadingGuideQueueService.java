package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.mapper.ExternalProjectMapper;
import kd.address.view.mapper.ProjectReadingGuideMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectReadingGuideQueueService {
    private static final String LEGACY_GITHUB_READ_ERROR = "读取 GitHub 项目资料失败";

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

    /** Recover once from failures written by the previous reader, which used an unreachable raw host. */
    public int retryLegacyGithubReadFailures(Long engineProfileId) {
        if (engineProfileId == null) return 0;
        List<Long> failedIds = guides.findFailedProjectIdsByError(LEGACY_GITHUB_READ_ERROR);
        for (Long projectId : failedIds) guides.enqueue(projectId, engineProfileId, true);
        return failedIds.size();
    }
}
