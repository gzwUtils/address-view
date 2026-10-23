package kd.address.view.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kd.address.view.entity.ExternalProject;
import kd.address.view.entity.ProjectReadingEngineProfile;
import kd.address.view.entity.ProjectReadingGuide;
import kd.address.view.mapper.ExternalProjectMapper;
import kd.address.view.mapper.ProjectReadingGuideMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProjectReadingGuideServiceTest {
    @Test
    void periodicSyncDoesNotAutomaticallyRetryFailedGuide() {
        ExternalProjectMapper projects = mock(ExternalProjectMapper.class);
        ProjectReadingGuideMapper guides = mock(ProjectReadingGuideMapper.class);
        ProjectReadingEngineProfileService profiles = mock(ProjectReadingEngineProfileService.class);
        ExternalProject project = new ExternalProject();
        project.setId(12L);
        project.setSourcePlatform("github");
        project.setSourceType("github_search");
        when(projects.findFeatured()).thenReturn(List.of(project));
        when(profiles.defaultForPublic()).thenReturn(new ProjectReadingEngineProfile());
        ProjectReadingGuide failed = new ProjectReadingGuide();
        failed.setStatus("FAILED");
        when(guides.findByProjectId(12L)).thenReturn(failed);

        ProjectReadingGuideService service = new ProjectReadingGuideService(projects, profiles,
                mock(ProjectReadingEngineRegistry.class), guides, List.of(), mock(ProjectReadingSecretCipher.class),
                new ObjectMapper());
        service.enqueueFeaturedFromSource("github");

        verify(guides, never()).enqueue(anyLong(), anyLong(), anyBoolean());
    }

    @Test
    void generatedGuideKeepsOnlyValidSourceCitationsAndMarksUncitedInsightsForReview() throws Exception {
        ExternalProjectMapper projects = mock(ExternalProjectMapper.class);
        ProjectReadingGuideMapper guides = mock(ProjectReadingGuideMapper.class);
        ProjectReadingEngineProfileService profiles = mock(ProjectReadingEngineProfileService.class);
        ProjectReadingEngineRegistry engines = mock(ProjectReadingEngineRegistry.class);
        ProjectReadingContextReader reader = mock(ProjectReadingContextReader.class);
        ProjectReadingSecretCipher cipher = mock(ProjectReadingSecretCipher.class);
        ObjectMapper json = new ObjectMapper();

        ExternalProject project = new ExternalProject();
        project.setId(7L);
        project.setSourceType("github_search");
        when(projects.findById(7L)).thenReturn(project);

        ProjectReadingGuide job = new ProjectReadingGuide();
        job.setId(11L);
        job.setExternalProjectId(7L);
        job.setEngineProfileId(3L);
        when(guides.findNextPending()).thenReturn(job);
        when(guides.claim(11L)).thenReturn(1);
        when(guides.findByProjectId(7L)).thenReturn(job);

        ProjectReadingEngineProfile profile = new ProjectReadingEngineProfile();
        profile.setId(3L);
        when(profiles.requireEnabled(3L)).thenReturn(profile);
        when(profiles.decryptApiKey(profile)).thenReturn("test-key");
        when(reader.supports(project)).thenReturn(true);
        ProjectReadingSource source = new ProjectReadingSource("README.md", "README.md", "README",
                "https://github.com/acme/demo/blob/main/README.md", "A small documented demo.");
        ProjectReadingContext context = new ProjectReadingContext("acme/demo", "demo", "https://github.com/acme/demo",
                "main", "tree-v1", "Java", "MIT", List.of(source));
        when(reader.read(project)).thenReturn(context);
        when(engines.complete(eq(profile), eq("test-key"), anyString(), anyString())).thenReturn("""
                {"projectPurpose":{"text":"一个小型示例项目","sourceIds":["README.md","invented.md"]},
                 "readingPath":[{"title":"阅读 README","why":"确认项目目标","sourceIds":["README.md","invented.md"]}],
                 "designInsights":[{"title":"目录设计","observation":"待核验","tradeoff":"待核验","tryInOwnProject":"待核验","sourceIds":["invented.md"]}]}
                """);

        ProjectReadingGuideService service = new ProjectReadingGuideService(projects, profiles, engines, guides,
                List.of(reader), cipher, json);
        service.processNext();

        var guideJson = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(guides).complete(eq(11L), eq("tree-v1"), eq(3L), guideJson.capture());
        JsonNode result = json.readTree(guideJson.getValue());
        assertThat(result.path("projectPurpose").path("sourceIds").size()).isEqualTo(1);
        assertThat(result.path("projectPurpose").path("evidenceStatus").asText()).isEqualTo("source-backed");
        assertThat(result.path("whoIsItFor").path("evidenceStatus").asText()).isEqualTo("needs-review");
        assertThat(result.path("readingPath").get(0).path("sourceIds").size()).isEqualTo(1);
        assertThat(result.path("readingPath").get(0).path("evidenceStatus").asText()).isEqualTo("source-backed");
        assertThat(result.path("designInsights").get(0).path("sourceIds")).isEmpty();
        assertThat(result.path("designInsights").get(0).path("evidenceStatus").asText()).isEqualTo("needs-review");
        assertThat(result.path("sourceReferences").get(0).path("url").asText()).isEqualTo(source.url());
        verify(guides, never()).fail(anyLong(), anyString());
    }

    @Test
    void manualEnqueueAlwaysRequestsRegeneration() {
        ExternalProjectMapper projects = mock(ExternalProjectMapper.class);
        ProjectReadingGuideMapper guides = mock(ProjectReadingGuideMapper.class);
        ProjectReadingEngineProfileService profiles = mock(ProjectReadingEngineProfileService.class);
        ExternalProject project = new ExternalProject();
        project.setId(17L);
        project.setSourceType("github_search");
        when(projects.findById(17L)).thenReturn(project);
        ProjectReadingEngineProfile engine = new ProjectReadingEngineProfile();
        engine.setId(5L);
        when(profiles.defaultForPublic()).thenReturn(engine);

        ProjectReadingGuideService service = new ProjectReadingGuideService(projects, profiles,
                mock(ProjectReadingEngineRegistry.class), guides, List.of(), mock(ProjectReadingSecretCipher.class),
                new ObjectMapper());

        service.enqueue(17L);

        verify(guides).enqueue(17L, 5L, true);
    }

    @Test
    void publicGuideDoesNotExposeInternalFailureDetails() {
        ExternalProjectMapper projects = mock(ExternalProjectMapper.class);
        ProjectReadingGuideMapper guides = mock(ProjectReadingGuideMapper.class);
        ProjectReadingEngineProfileService profiles = mock(ProjectReadingEngineProfileService.class);
        ExternalProject project = new ExternalProject();
        project.setId(9L);
        when(projects.findById(9L)).thenReturn(project);
        ProjectReadingGuide failed = new ProjectReadingGuide();
        failed.setExternalProjectId(9L);
        failed.setEngineProfileId(2L);
        failed.setStatus("FAILED");
        failed.setLastError("provider unauthorized: secret detail");
        when(guides.findByProjectId(9L)).thenReturn(failed);

        ProjectReadingGuideService service = new ProjectReadingGuideService(projects, profiles,
                mock(ProjectReadingEngineRegistry.class), guides, List.of(), mock(ProjectReadingSecretCipher.class),
                new ObjectMapper());

        assertThat(service.get(9L).lastError()).isNull();
        assertThat(service.getForAdmin(9L).lastError()).isEqualTo("provider unauthorized: secret detail");
    }
}
