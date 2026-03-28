package kd.address.view.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GrowthCapsule {

    private Long id;
    private String userId;
    private String capsuleName;
    private String tagline;
    private String aiBrief;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
