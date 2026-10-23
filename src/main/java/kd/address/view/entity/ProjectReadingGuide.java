package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectReadingGuide {
    private Long id;
    private Long externalProjectId;
    private Long engineProfileId;
    private Long generatedEngineProfileId;
    private Boolean forceRegenerate;
    private String status;
    private String sourceVersion;
    private String guideJson;
    private String lastError;
    private LocalDateTime queuedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime updateTime;
}
