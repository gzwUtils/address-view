package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ExternalSource {
    private Long id;
    private String code;
    private String displayName;
    private String sourceType;
    private String feedUrl;
    private String queryText;
    private Integer periodDays;
    private Integer minStars;
    private Integer maxItems;
    private Integer intervalHours;
    private Boolean enabled;
    private Boolean deleted;
    private String lastStatus;
    private String lastError;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
