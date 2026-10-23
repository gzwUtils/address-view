package kd.address.view.service;

import kd.address.view.entity.ProjectReadingEngineProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProjectReadingGuideStartupBackfill {
    private final ProjectReadingEngineProfileService profiles;
    private final ProjectReadingGuideQueueService queue;

    @EventListener(ApplicationReadyEvent.class)
    public void enqueueMissingGuides() {
        ProjectReadingEngineProfile engine = profiles.defaultForPublic();
        if (engine == null) return;
        int recovered = queue.retryLegacyReaderFailures(engine.getId());
        int queued = queue.enqueueMissingFeatured(engine.getId());
        if (recovered > 0 || queued > 0)
            log.info("默认解读引擎已就绪，恢复旧读取失败项目 {} 个，补排未生成项目 {} 个", recovered, queued);
    }
}
