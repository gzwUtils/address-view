package kd.address.view.service;

import kd.address.view.entity.ExternalSource;
import kd.address.view.mapper.ExternalSourceMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpTimeoutException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class ExternalSourceSyncTest {
    @Test
    void timeoutMarksFailureAndSchedulesNextConfiguredRunWithoutReplacingProjects() throws Exception {
        ExternalSourceMapper mapper = mock(ExternalSourceMapper.class);
        ExternalSourceService sourceService = mock(ExternalSourceService.class);
        ExternalProjectService projects = mock(ExternalProjectService.class);
        GitHubRepositoryClient github = mock(GitHubRepositoryClient.class);
        RssProjectClient rss = mock(RssProjectClient.class);
        ExternalSource source = new ExternalSource();
        source.setId(9L);
        source.setCode("github");
        source.setSourceType("github_search");
        source.setIntervalHours(48);
        when(sourceService.get(9L)).thenReturn(source);
        when(github.recentLicensedProjects(source)).thenThrow(new HttpTimeoutException("request timed out"));

        new ExternalSourceSync(mapper, sourceService, projects, github, rss).runNow(9L);

        verify(projects, never()).replaceFeatured(any(), any());
        ArgumentCaptor<LocalDateTime> started = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> next = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(mapper).updateRun(eq(9L), eq("FAILED"), contains("timed out"), started.capture(), next.capture());
        assertThat(next.getValue()).isEqualTo(started.getValue().plusHours(48));
    }
}
