package kd.address.view.service;

import kd.address.view.common.NotFoundException;
import kd.address.view.entity.ProjectReadingEngineProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectReadingEngineRegistry {
    private final List<ProjectReadingEngineAdapter> adapters;

    public String complete(ProjectReadingEngineProfile profile, String apiKey,
                           String systemPrompt, String userPrompt) {
        ProjectReadingEngineAdapter adapter = adapters.stream()
                .filter(candidate -> candidate.supports(profile.getProtocol()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("尚未安装该解读引擎适配器"));
        return adapter.complete(profile, apiKey, systemPrompt, userPrompt);
    }
}
