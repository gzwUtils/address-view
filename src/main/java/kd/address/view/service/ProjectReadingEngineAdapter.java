package kd.address.view.service;

import kd.address.view.entity.ProjectReadingEngineProfile;

public interface ProjectReadingEngineAdapter {
    boolean supports(String protocol);

    String complete(ProjectReadingEngineProfile profile, String apiKey, String systemPrompt, String userPrompt);
}
