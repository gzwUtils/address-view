package kd.address.view.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectReadingGuideProcessor {
    private final ProjectReadingGuideService guides;

    @Scheduled(initialDelay = 20_000, fixedDelay = 5_000)
    public void processNext() {
        guides.processNext();
    }
}
