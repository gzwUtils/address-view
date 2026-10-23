package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectReadingEngineProfile {
    private Long id;
    private String name;
    private String protocol;
    private String baseUrl;
    private String modelName;
    private String apiKeyCiphertext;
    private Integer timeoutSeconds;
    private Boolean enabled;
    private Boolean defaultForPublic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
