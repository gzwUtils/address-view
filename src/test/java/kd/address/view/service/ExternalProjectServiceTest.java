package kd.address.view.service;

import kd.address.view.entity.ExternalProject;
import kd.address.view.mapper.ExternalProjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalProjectServiceTest {
    @Test
    void emptySyncDoesNotReplaceExistingSelection() {
        RecordingMapper mapper = new RecordingMapper();
        new ExternalProjectService(mapper).replaceFeatured("github", List.of());
        assertThat(mapper.clearCount).isZero();
    }

    @Test
    void successfulSyncStoresConfiguredSelectionWithoutFixedFiveItemLimit() {
        RecordingMapper mapper = new RecordingMapper();
        List<ExternalProject> incoming = new ArrayList<>();
        for (int i = 0; i < 7; i++) incoming.add(new ExternalProject());

        new ExternalProjectService(mapper).replaceFeatured("github", incoming);

        assertThat(mapper.clearCount).isEqualTo(1);
        assertThat(mapper.saved).hasSize(7);
        assertThat(mapper.saved).extracting(ExternalProject::getDisplayRank).containsExactly(1, 2, 3, 4, 5, 6, 7);
        assertThat(mapper.saved).extracting(ExternalProject::getSourcePlatform).containsOnly("github");
    }

    private static class RecordingMapper implements ExternalProjectMapper {
        int clearCount;
        List<ExternalProject> saved = new ArrayList<>();

        public List<ExternalProject> findFeatured() { return saved; }
        public void clearFeatured(String sourceCode) { clearCount++; }
        public void upsert(ExternalProject project) { saved.add(project); }
    }
}
