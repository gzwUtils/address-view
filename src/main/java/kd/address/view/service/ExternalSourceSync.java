package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.entity.ExternalSource;
import kd.address.view.mapper.ExternalSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalSourceSync {
    private final ExternalSourceMapper sources;
    private final ExternalSourceService sourceService;
    private final ExternalProjectService projects;
    private final GitHubRepositoryClient github;
    private final RssProjectClient rss;

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public synchronized void runDue() {
        for (ExternalSource source : sources.findDue(LocalDateTime.now(ZoneOffset.UTC))) {
            run(source);
        }
    }

    public synchronized ExternalSource runNow(Long id) {
        run(sourceService.get(id));
        return sourceService.get(id);
    }

    private void run(ExternalSource source) {
        LocalDateTime started = LocalDateTime.now(ZoneOffset.UTC);
        String status = "SUCCESS";
        String error = null;
        try {
            List<ExternalProject> items = switch (source.getSourceType()) {
                case "github_search" -> github.recentLicensedProjects(source);
                case "rss" -> rss.fetch(source);
                default -> throw new IllegalArgumentException("不支持的来源类型");
            };
            if (items.isEmpty()) throw new IOException("本次没有可收录项目");
            projects.replaceFeatured(source.getCode(), items);
            log.info("站外来源 {} 收录完成：{} 条", source.getCode(), items.size());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            status = "FAILED";
            error = "任务已中断";
        } catch (Exception exception) {
            status = "FAILED";
            error = exception.getMessage() == null ? "收录失败" : exception.getMessage();
            log.warn("站外来源 {} 收录失败：{}", source.getCode(), error);
        }
        if (error != null && error.length() > 255) error = error.substring(0, 255);
        // A failed run waits for the configured interval; there is no immediate retry.
        sources.updateRun(source.getId(), status, error, started,
                started.plusHours(source.getIntervalHours()));
    }
}
