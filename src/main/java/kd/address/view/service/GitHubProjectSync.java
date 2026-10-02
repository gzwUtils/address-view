package kd.address.view.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "portal.github", name = "sync-enabled", havingValue = "true", matchIfMissing = true)
public class GitHubProjectSync {
    private final GitHubRepositoryClient github;
    private final ExternalProjectService projects;

    // One request a day until the first snapshot exists, then every Monday morning.
    @Scheduled(initialDelay = 30_000, fixedDelay = 86_400_000)
    public void bootstrap() {
        if (!projects.hasFeatured()) refresh();
    }

    @Scheduled(cron = "0 0 9 * * MON", zone = "Asia/Shanghai")
    public void weeklyRefresh() {
        refresh();
    }

    private void refresh() {
        try {
            var selected = github.recentLicensedProjects();
            if (selected.isEmpty()) {
                log.warn("GitHub 项目收录跳过：没有获得符合许可证条件的仓库");
                return;
            }
            projects.replaceFeatured(selected);
            log.info("GitHub 项目收录完成：{} 个仓库", selected.size());
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            log.warn("GitHub 项目收录被中断");
        } catch (Exception error) {
            log.warn("GitHub 项目收录失败：{}", error.getMessage());
        }
    }
}
